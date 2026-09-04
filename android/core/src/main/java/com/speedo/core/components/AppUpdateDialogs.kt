package com.speedo.core.components

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.speedo.core.model.AppUpdatePromptState
import com.speedo.core.theme.*

/**
 * Safely launches the browser to download and install the latest APK
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
        Toast.makeText(context, "Opening browser to download update...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Downloads the APK directly in background using Android's native DownloadManager
 */
fun downloadApkViaDownloadManager(context: Context, url: String?, fileName: String = "speedo-update.apk") {
    try {
        val targetUrl = if (!url.isNullOrBlank()) {
            url
        } else {
            "https://web-production-5d826.up.railway.app/downloads/"
        }
        val request = DownloadManager.Request(Uri.parse(targetUrl)).apply {
            setTitle("Speedo App Update")
            setDescription("Downloading latest build ($fileName)...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Downloading update in background! Pull down notification bar to check status.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        // Fallback to browser download if DownloadManager fails
        openBrowserForUpdate(context, url)
    }
}

/**
 * Backwards compatibility delegate
 */
fun openUpdateUrl(context: Context, url: String?) {
    openBrowserForUpdate(context, url)
}

/**
 * Non-dismissible full-screen overlay for Mandatory (Force) App Updates
 */
@Composable
fun ForceUpdateOverlay(
    promptState: AppUpdatePromptState
) {
    val context = LocalContext.current
    val config = promptState.config

    Dialog(
        onDismissRequest = { /* Non-dismissible: block back press & touch outside */ },
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
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Rocket / Alert Icon Badge
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(SpeedoOrange.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = SpeedoOrange,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Mandatory Badge Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SpeedoError.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SpeedoError.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SpeedoError, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MANDATORY UPDATE REQUIRED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = SpeedoError
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = config?.title ?: "Time to Update Speedo!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = SpeedoTextPrimary,
                        letterSpacing = (-0.5).sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Version comparison Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SpeedoSurfaceVariant
                ) {
                    Text(
                        text = "Installed: v${promptState.currentVersionName} (Build ${promptState.currentVersionCode})  ➔  Latest: v${config?.latestVersionName ?: "1.1.0"}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SpeedoTextSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Message Text
                Text(
                    text = config?.message ?: "A new version of Speedo is required to continue booking rides with the latest security and real-time backend enhancements.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SpeedoTextSecondary,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                // Release Notes Card (if available)
                if (!config?.releaseNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF7F8FA),
                        border = BorderStroke(1.dp, SpeedoCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "WHAT'S NEW IN THIS VERSION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SpeedoOrange
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = config!!.releaseNotes!!,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = SpeedoTextPrimary,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Installation Instructions Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SpeedoOrange.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, SpeedoOrange.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HOW TO INSTALL UPDATE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SpeedoOrange
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. Tap 'Download in Browser & Install' below.\n2. When prompted in your browser, tap Download.\n3. Open the downloaded APK file from your notifications to install.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SpeedoTextSecondary,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Big Primary Update Button (Browser Download)
                SpeedoPrimaryButton(
                    text = "DOWNLOAD IN BROWSER & INSTALL",
                    leadingIcon = Icons.Default.OpenInBrowser,
                    onClick = {
                        openBrowserForUpdate(context, config?.updateUrl)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary In-App Direct Download Button
                OutlinedButton(
                    onClick = {
                        val fileName = "speedo-${config?.appId ?: "app"}-v${config?.latestVersionName ?: "latest"}.apk"
                        downloadApkViaDownloadManager(context, config?.updateUrl, fileName)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, SpeedoOrange)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DIRECT DOWNLOAD IN APP", fontWeight = FontWeight.Bold, color = SpeedoOrange)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "You cannot use older versions due to essential real-time database updates.",
                    style = MaterialTheme.typography.labelSmall.copy(color = SpeedoTextTertiary),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Flexible (Optional) Update Dialog with "Download in Browser", "Direct Download", and "Later"
 */
@Composable
fun FlexibleUpdateDialog(
    promptState: AppUpdatePromptState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val config = promptState.config

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(SpeedoOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = SpeedoOrange,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = config?.title ?: "New Version Available 🚀",
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SpeedoSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Current: v${promptState.currentVersionName}  ➔  New: v${config?.latestVersionName ?: "1.1.0"}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoOrange)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = config?.message ?: "Upgrade to the latest version of Speedo to enjoy smoother rides, new destinations, and bug fixes.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                )

                if (!config?.releaseNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = config!!.releaseNotes!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = SpeedoTextSecondary, lineHeight = 18.sp)
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        openBrowserForUpdate(context, config?.updateUrl)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download in Browser & Install", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val fileName = "speedo-${config?.appId ?: "app"}-v${config?.latestVersionName ?: "latest"}.apk"
                        downloadApkViaDownloadManager(context, config?.updateUrl, fileName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SpeedoOrange)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Direct Download in App", fontWeight = FontWeight.Bold, color = SpeedoOrange)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remind Me Later", color = SpeedoTextSecondary)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = SpeedoWhite
    )
}
