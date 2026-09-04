package com.speedo.core.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.speedo.core.model.AppUpdatePromptState
import com.speedo.core.theme.*
import com.speedo.core.utils.DownloadStatus
import com.speedo.core.utils.InAppUpdateManager

/**
 * Safely launches the browser as a fallback to download the latest APK
 */
fun openBrowserForUpdate(context: Context, url: String?) {
    try {
        val targetUrl = if (!url.isNullOrBlank()) {
            url
        } else {
            "https://web-production-5d826.up.railway.app/downloads/"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
        Toast.makeText(context, "Opening browser for update...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Backwards compatibility delegate
 */
fun openUpdateUrl(context: Context, url: String?) {
    openBrowserForUpdate(context, url)
}

/**
 * Clean & direct Update Overlay for Mandatory (Force) App Updates
 * Features in-app streaming download with progress bar and auto-installer launch.
 */
@Composable
fun ForceUpdateOverlay(
    promptState: AppUpdatePromptState,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val config = promptState.config
    val downloadStatus by InAppUpdateManager.status.collectAsState()

    val targetUrl = promptState.updateUrl

    Dialog(
        onDismissRequest = { /* Non-dismissible */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SpeedoWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Rocket Icon Badge
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            when (downloadStatus) {
                                is DownloadStatus.Completed -> SpeedoSuccess.copy(alpha = 0.15f)
                                is DownloadStatus.Failed -> SpeedoError.copy(alpha = 0.15f)
                                else -> SpeedoOrange.copy(alpha = 0.12f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (downloadStatus) {
                            is DownloadStatus.Completed -> Icons.Default.CheckCircle
                            is DownloadStatus.Failed -> Icons.Default.ErrorOutline
                            else -> Icons.Default.RocketLaunch
                        },
                        contentDescription = null,
                        tint = when (downloadStatus) {
                            is DownloadStatus.Completed -> SpeedoSuccess
                            is DownloadStatus.Failed -> SpeedoError
                            else -> SpeedoOrange
                        },
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = when (downloadStatus) {
                        is DownloadStatus.Downloading -> "Downloading Update..."
                        is DownloadStatus.Completed -> "Update Ready to Install! 🚀"
                        is DownloadStatus.Failed -> "Download Issue Detected"
                        else -> config?.title?.ifBlank { null } ?: "Mandatory Update Required"
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = SpeedoTextPrimary,
                        letterSpacing = (-0.5).sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Message / Description
                Text(
                    text = when (downloadStatus) {
                        is DownloadStatus.Downloading -> "Speedo is downloading the latest update in the background. Please wait a moment."
                        is DownloadStatus.Completed -> "Download finished. Tap below if the Android installer did not open automatically."
                        is DownloadStatus.Failed -> (downloadStatus as DownloadStatus.Failed).error
                        else -> config?.message?.ifBlank { null }
                            ?: "A mandatory update is required to continue using Speedo. Update now to enjoy the latest performance improvements."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (downloadStatus is DownloadStatus.Failed) SpeedoError else SpeedoTextSecondary,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // In-App Download Progress Indicator
                AnimatedVisibility(visible = downloadStatus is DownloadStatus.Downloading) {
                    val status = downloadStatus as? DownloadStatus.Downloading
                    if (status != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { status.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = SpeedoOrange,
                                trackColor = SpeedoOrange.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(status.progress * 100).toInt()}% Complete",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SpeedoOrange
                                    )
                                )
                                Text(
                                    text = "${InAppUpdateManager.formatFileSize(status.downloadedBytes)} / ${InAppUpdateManager.formatFileSize(status.totalBytes)}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = SpeedoTextTertiary)
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                when (val status = downloadStatus) {
                    is DownloadStatus.Downloading -> {
                        Button(
                            onClick = { /* In progress */ },
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = SpeedoOrange.copy(alpha = 0.5f),
                                disabledContentColor = SpeedoWhite
                            )
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SpeedoWhite,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("DOWNLOADING UPDATE...", fontWeight = FontWeight.Bold)
                        }
                    }
                    is DownloadStatus.Completed -> {
                        SpeedoPrimaryButton(
                            text = "INSTALL UPDATE NOW 🚀",
                            leadingIcon = Icons.Default.SystemUpdate,
                            onClick = {
                                InAppUpdateManager.installApk(context, status.apkFile)
                            }
                        )
                    }
                    is DownloadStatus.Failed -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SpeedoPrimaryButton(
                                text = "RETRY IN-APP DOWNLOAD",
                                leadingIcon = Icons.Default.RocketLaunch,
                                onClick = {
                                    InAppUpdateManager.startDownloadAndInstall(context, targetUrl)
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { openBrowserForUpdate(context, targetUrl) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Open in Browser", color = SpeedoOrange, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> { // Idle
                        SpeedoPrimaryButton(
                            text = "UPDATE NOW",
                            leadingIcon = Icons.Default.SystemUpdate,
                            onClick = {
                                InAppUpdateManager.startDownloadAndInstall(context, targetUrl)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Direct Browser Fallback Link
                Text(
                    text = "Having trouble? Tap here to download in browser",
                    style = MaterialTheme.typography.bodySmall.copy(color = SpeedoTextTertiary),
                    modifier = Modifier.clickable {
                        openBrowserForUpdate(context, targetUrl)
                    }
                )
            }
        }
    }
}

/**
 * Clean & direct Update Dialog for Flexible (Optional) Updates
 * Features in-app streaming download with progress bar and auto-installer launch.
 */
@Composable
fun FlexibleUpdateDialog(
    promptState: AppUpdatePromptState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val config = promptState.config
    val downloadStatus by InAppUpdateManager.status.collectAsState()

    val targetUrl = promptState.updateUrl

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        when (downloadStatus) {
                            is DownloadStatus.Completed -> SpeedoSuccess.copy(alpha = 0.15f)
                            is DownloadStatus.Failed -> SpeedoError.copy(alpha = 0.15f)
                            else -> SpeedoOrange.copy(alpha = 0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (downloadStatus) {
                        is DownloadStatus.Completed -> Icons.Default.CheckCircle
                        is DownloadStatus.Failed -> Icons.Default.ErrorOutline
                        else -> Icons.Default.RocketLaunch
                    },
                    contentDescription = null,
                    tint = when (downloadStatus) {
                        is DownloadStatus.Completed -> SpeedoSuccess
                        is DownloadStatus.Failed -> SpeedoError
                        else -> SpeedoOrange
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = when (downloadStatus) {
                    is DownloadStatus.Downloading -> "Downloading Update..."
                    is DownloadStatus.Completed -> "Update Ready to Install! 🚀"
                    is DownloadStatus.Failed -> "Update Download Notice"
                    else -> config?.title?.ifBlank { null } ?: "New Update Available"
                },
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (downloadStatus) {
                        is DownloadStatus.Downloading -> "Downloading latest version in background..."
                        is DownloadStatus.Completed -> "Download finished! Click Install below to complete the update."
                        is DownloadStatus.Failed -> (downloadStatus as DownloadStatus.Failed).error
                        else -> config?.message?.ifBlank { null }
                            ?: "A new version of Speedo is available. Update now to enjoy the latest improvements."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (downloadStatus is DownloadStatus.Failed) SpeedoError else SpeedoTextSecondary,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Progress Bar
                AnimatedVisibility(visible = downloadStatus is DownloadStatus.Downloading) {
                    val status = downloadStatus as? DownloadStatus.Downloading
                    if (status != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { status.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SpeedoOrange,
                                trackColor = SpeedoOrange.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(status.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SpeedoOrange
                                    )
                                )
                                Text(
                                    text = "${InAppUpdateManager.formatFileSize(status.downloadedBytes)} / ${InAppUpdateManager.formatFileSize(status.totalBytes)}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = SpeedoTextTertiary)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (val status = downloadStatus) {
                is DownloadStatus.Downloading -> {
                    Button(
                        onClick = { /* Downloading */ },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = SpeedoOrange.copy(alpha = 0.5f),
                            disabledContentColor = SpeedoWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = SpeedoWhite,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Downloading...", fontWeight = FontWeight.Bold)
                    }
                }
                is DownloadStatus.Completed -> {
                    Button(
                        onClick = {
                            onDismiss()
                            InAppUpdateManager.installApk(context, status.apkFile)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Install Update Now", fontWeight = FontWeight.Bold)
                    }
                }
                is DownloadStatus.Failed -> {
                    Button(
                        onClick = {
                            InAppUpdateManager.startDownloadAndInstall(context, targetUrl) {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Retry In-App", fontWeight = FontWeight.Bold)
                    }
                }
                else -> { // Idle
                    Button(
                        onClick = {
                            InAppUpdateManager.startDownloadAndInstall(context, targetUrl)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Update Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (downloadStatus !is DownloadStatus.Downloading) {
                OutlinedButton(
                    onClick = {
                        InAppUpdateManager.reset()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Later", color = SpeedoTextSecondary)
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = SpeedoWhite
    )
}
