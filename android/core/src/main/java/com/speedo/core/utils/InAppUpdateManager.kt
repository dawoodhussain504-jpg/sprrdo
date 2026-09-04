package com.speedo.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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
    private val _status = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val status: StateFlow<DownloadStatus> = _status.asStateFlow()

    private var downloadJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
     * Downloads the APK file inside the app with real-time progress, then immediately launches the installer
     */
    fun startDownloadAndInstall(
        context: Context,
        downloadUrl: String,
        onInstallTriggered: (() -> Unit)? = null
    ) {
        // Cancel existing job if running
        downloadJob?.cancel()

        _status.value = DownloadStatus.Downloading(0f, 0L, 0L)
        Toast.makeText(context, "Downloading update in app...", Toast.LENGTH_SHORT).show()

        downloadJob = scope.launch(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                var currentUrl = downloadUrl
                var redirects = 0
                val maxRedirects = 5

                // Handle HTTP redirects (301, 302, 307)
                while (redirects < maxRedirects) {
                    val url = URL(currentUrl)
                    connection = url.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    connection.setRequestProperty("User-Agent", "Speedo-InApp-Updater/1.0")
                    connection.connect()

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == 307 || responseCode == 308
                    ) {
                        val location = connection.getHeaderField("Location")
                        if (!location.isNullOrBlank()) {
                            currentUrl = location
                            connection.disconnect()
                            redirects++
                            continue
                        }
                    }
                    break
                }

                if (connection == null || connection.responseCode !in 200..299) {
                    throw IllegalStateException("Server returned HTTP ${connection?.responseCode ?: "connection failure"}")
                }

                val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: (21L * 1024L * 1024L) // fallback 21MB
                inputStream = connection.inputStream

                // Destination file inside app storage
                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                if (!downloadDir.exists()) downloadDir.mkdirs()
                val destFile = File(downloadDir, "speedo_update.apk")

                if (destFile.exists()) {
                    destFile.delete()
                }

                outputStream = FileOutputStream(destFile)
                val buffer = ByteArray(16 * 1024)
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastProgressUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (!isActive) {
                        destFile.delete()
                        return@launch
                    }

                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate > 100 || downloadedBytes >= totalBytes) {
                        lastProgressUpdate = now
                        val rawProgress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        _status.value = DownloadStatus.Downloading(
                            progress = rawProgress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes
                        )
                    }
                }

                outputStream.flush()

                // LOOP-BREAKER GUARD: Inspect downloaded APK before installing
                val pInfo = try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (_: Exception) { null }
                val installedCode = pInfo?.let { androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(it).toInt() } ?: 0

                val archiveInfo = context.packageManager.getPackageArchiveInfo(destFile.absolutePath, 0)
                val downloadedCode = archiveInfo?.let {
                    androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(it).toInt()
                } ?: 0

                if (downloadedCode in 1..installedCode) {
                    // LOOP BROKEN: Downloaded APK is not newer than currently installed app!
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

                // Record that installation was initiated to prevent re-prompt loop
                try {
                    val prefs = context.getSharedPreferences("speedo_update_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putInt("dismissed_update_version_code", downloadedCode).apply()
                } catch (_: Exception) {}

                // Trigger package installation on Main thread
                withContext(Dispatchers.Main) {
                    if (canRequestPackageInstalls(context)) {
                        installApk(context, destFile)
                    } else {
                        openInstallPermissionSettings(context)
                    }
                    onInstallTriggered?.invoke()
                }

            } catch (e: CancellationException) {
                // Job cancelled
            } catch (e: Exception) {
                _status.value = DownloadStatus.Failed(e.message ?: "Failed to download update")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "In-app download failed: ${e.message}. You can update via browser.", Toast.LENGTH_LONG).show()
                }
            } finally {
                try { outputStream?.close() } catch (_: Exception) {}
                try { inputStream?.close() } catch (_: Exception) {}
                try { connection?.disconnect() } catch (_: Exception) {}
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
