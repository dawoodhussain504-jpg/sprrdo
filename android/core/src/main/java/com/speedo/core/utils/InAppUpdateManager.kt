package com.speedo.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.speedo.core.network.SpeedoResilientDns
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadStatus()
    data class Completed(val apkFile: File) : DownloadStatus()
    data class Failed(val error: String) : DownloadStatus()
}

object InAppUpdateManager {
    private const val TAG = "InAppUpdateManager"
    private val _status = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val status: StateFlow<DownloadStatus> = _status.asStateFlow()

    private var downloadJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Dedicated high-performance HTTP client for APK binary streaming.
     * Configured with resilient DoH DNS, high socket buffers, and connection pooling.
     */
    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(SpeedoResilientDns)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Checks if the app is allowed to request package installation on Android 8.0+
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens Android System Settings to allow installing unknown apps from Speedo
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Toast.makeText(
                    context,
                    "Please toggle 'Allow from this source' to install Speedo updates",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    /**
     * Triggers the native Android PackageInstaller dialog for the downloaded APK
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists() || apkFile.length() <= 0) {
            Toast.makeText(context, "Update file is missing or corrupted. Please retry.", Toast.LENGTH_SHORT).show()
            _status.value = DownloadStatus.Failed("Downloaded APK file not found")
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open installer: ${e.message}", Toast.LENGTH_LONG).show()
            _status.value = DownloadStatus.Failed(e.message ?: "Installer launch failed")
        }
    }

    /**
     * Downloads the APK file inside the app using Turbo multi-thread chunking when supported,
     * or high-speed buffered streams, then immediately prompts for installation.
     */
    fun startDownloadAndInstall(
        context: Context,
        downloadUrl: String,
        onInstallTriggered: (() -> Unit)? = null
    ) {
        // Cancel existing job if running
        downloadJob?.cancel()

        _status.value = DownloadStatus.Downloading(0f, 0L, 0L)
        Toast.makeText(context, "Downloading update at high speed... You can continue using Speedo.", Toast.LENGTH_LONG).show()

        downloadJob = scope.launch(Dispatchers.IO) {
            try {
                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                if (!downloadDir.exists()) downloadDir.mkdirs()
                val destFile = File(downloadDir, "speedo_update.apk")

                if (destFile.exists()) {
                    destFile.delete()
                }

                Log.i(TAG, "⚡ [TURBO DOWNLOAD START] Target URL: $downloadUrl")

                // Step 1: Probe server capabilities (HTTP Range & Content-Length)
                var totalBytes = 22L * 1024L * 1024L // fallback 22MB
                var acceptsRanges = false

                try {
                    val probeReq = Request.Builder()
                        .url(downloadUrl)
                        .header("User-Agent", "Speedo-Turbo-Updater/2.0")
                        .header("Accept-Encoding", "identity")
                        .header("Range", "bytes=0-1")
                        .build()

                    downloadClient.newCall(probeReq).execute().use { probeResp ->
                        val code = probeResp.code
                        val crHeader = probeResp.header("Content-Range")
                        val arHeader = probeResp.header("Accept-Ranges")
                        val clHeader = probeResp.header("Content-Length")

                        if (code == 206 && crHeader != null) {
                            acceptsRanges = true
                            val slashIdx = crHeader.lastIndexOf('/')
                            if (slashIdx != -1) {
                                crHeader.substring(slashIdx + 1).trim().toLongOrNull()?.let {
                                    if (it > 0) totalBytes = it
                                }
                            }
                        } else if (arHeader.equals("bytes", ignoreCase = true)) {
                            acceptsRanges = true
                        }

                        if (totalBytes <= 22L * 1024L * 1024L && clHeader != null) {
                            clHeader.toLongOrNull()?.let { if (it > 0) totalBytes = it }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Range probe warning: ${e.message}. Proceeding with high-speed buffered download.")
                }

                Log.i(TAG, "⚡ [TURBO DOWNLOAD] AcceptsRanges=$acceptsRanges, totalBytes=$totalBytes")

                // Step 2: Download via 4-worker parallel chunking OR high-speed buffered stream
                val success = if (acceptsRanges && totalBytes > 3 * 1024 * 1024) {
                    try {
                        downloadParallelChunks(downloadUrl, destFile, totalBytes)
                        true
                    } catch (pe: Exception) {
                        Log.w(TAG, "Parallel download encountered issue (${pe.message}), falling back to single-stream.", pe)
                        if (destFile.exists()) destFile.delete()
                        downloadSingleStream(downloadUrl, destFile, totalBytes)
                        true
                    }
                } else {
                    downloadSingleStream(downloadUrl, destFile, totalBytes)
                    true
                }

                if (!success || !destFile.exists() || destFile.length() <= 0) {
                    throw IOException("Download completed with empty or missing file")
                }

                // Step 3: Validate APK integrity
                val archiveInfo = context.packageManager.getPackageArchiveInfo(destFile.absolutePath, 0)
                if (archiveInfo == null) {
                    destFile.delete()
                    throw IOException("Downloaded package corrupted. Please retry.")
                }

                val pInfo = try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (_: Exception) { null }
                val installedCode = pInfo?.let { androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(it).toInt() } ?: 0
                val downloadedCode = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(archiveInfo).toInt()

                if (downloadedCode in 1..installedCode) {
                    // Loop prevention guard
                    val prefs = context.getSharedPreferences("speedo_update_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putInt("dismissed_update_version_code", downloadedCode).apply()
                    _status.value = DownloadStatus.Idle
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "App is already up to date (Build #$installedCode). No update needed.",
                            Toast.LENGTH_LONG
                        ).show()
                        onInstallTriggered?.invoke()
                    }
                    return@launch
                }

                _status.value = DownloadStatus.Completed(destFile)

                // Record update initiated
                try {
                    val prefs = context.getSharedPreferences("speedo_update_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putInt("dismissed_update_version_code", downloadedCode).apply()
                } catch (_: Exception) {}

                // Trigger package installation on Main thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Update downloaded! Launching installer...", Toast.LENGTH_SHORT).show()
                    if (canRequestPackageInstalls(context)) {
                        installApk(context, destFile)
                    } else {
                        openInstallPermissionSettings(context)
                    }
                    onInstallTriggered?.invoke()
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Download job cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                _status.value = DownloadStatus.Failed(e.message ?: "Failed to download update")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}. Please tap to retry.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Downloads file using 4 parallel concurrent workers with Range headers.
     * Saturates full 4G/5G carrier bandwidth.
     */
    private suspend fun downloadParallelChunks(
        url: String,
        destFile: File,
        totalBytes: Long
    ) = coroutineScope {
        val numWorkers = 4
        val chunkSize = totalBytes / numWorkers
        val totalBytesDownloaded = AtomicLong(0L)
        var lastProgressUpdate = 0L

        // Pre-allocate empty file on disk
        RandomAccessFile(destFile, "rw").use { it.setLength(totalBytes) }

        val workers = (0 until numWorkers).map { index ->
            val startByte = index * chunkSize
            val endByte = if (index == numWorkers - 1) totalBytes - 1 else (startByte + chunkSize - 1)

            async(Dispatchers.IO) {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Speedo-Turbo-Updater/2.0")
                    .header("Accept-Encoding", "identity")
                    .header("Range", "bytes=$startByte-$endByte")
                    .build()

                downloadClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful || resp.body == null) {
                        throw IOException("Worker $index failed with HTTP ${resp.code}")
                    }
                    val body = resp.body!!
                    val inputStream = BufferedInputStream(body.byteStream(), 64 * 1024)
                    val raf = RandomAccessFile(destFile, "rw")
                    try {
                        raf.seek(startByte)
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            ensureActive()
                            raf.write(buffer, 0, read)
                            val current = totalBytesDownloaded.addAndGet(read.toLong())
                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate > 200 || current >= totalBytes) {
                                lastProgressUpdate = now
                                val rawProgress = (current.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                _status.value = DownloadStatus.Downloading(
                                    progress = rawProgress,
                                    downloadedBytes = current,
                                    totalBytes = totalBytes
                                )
                            }
                        }
                    } finally {
                        try { raf.close() } catch (_: Exception) {}
                        try { inputStream.close() } catch (_: Exception) {}
                    }
                }
            }
        }

        workers.awaitAll()
    }

    /**
     * High-speed single-stream fallback with 128KB buffered I/O.
     */
    private suspend fun downloadSingleStream(
        url: String,
        destFile: File,
        expectedTotalBytes: Long
    ) = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Speedo-Turbo-Updater/2.0")
            .header("Accept-Encoding", "identity")
            .build()

        downloadClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful || resp.body == null) {
                throw IOException("Server returned HTTP ${resp.code}")
            }
            val body = resp.body!!
            val totalBytes = body.contentLength().takeIf { it > 0 } ?: expectedTotalBytes
            val bufferedInput = BufferedInputStream(body.byteStream(), 128 * 1024)
            val bufferedOutput = BufferedOutputStream(FileOutputStream(destFile), 128 * 1024)
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var downloadedBytes = 0L
            var lastProgressUpdate = 0L

            try {
                while (bufferedInput.read(buffer).also { bytesRead = it } != -1) {
                    ensureActive()
                    bufferedOutput.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate > 200 || downloadedBytes >= totalBytes) {
                        lastProgressUpdate = now
                        val rawProgress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        _status.value = DownloadStatus.Downloading(
                            progress = rawProgress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes
                        )
                    }
                }
                bufferedOutput.flush()
            } finally {
                try { bufferedOutput.close() } catch (_: Exception) {}
                try { bufferedInput.close() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Resets status back to Idle
     */
    fun reset() {
        downloadJob?.cancel()
        _status.value = DownloadStatus.Idle
    }

    /**
     * Formats bytes into human readable format: e.g. "14.5 MB"
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }
}

