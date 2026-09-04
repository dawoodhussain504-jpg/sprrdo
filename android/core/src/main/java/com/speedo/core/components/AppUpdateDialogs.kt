package com.speedo.core.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
 * Backwards compatibility delegate
 */
fun openUpdateUrl(context: Context, url: String?) {
    openBrowserForUpdate(context, url)
}

/**
 * Clean & direct Update Now overlay for Mandatory (Force) App Updates
 * Displays only title, message, and a prominent "UPDATE NOW" button.
 */
@Composable
fun ForceUpdateOverlay(
    promptState: AppUpdatePromptState,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val config = promptState.config

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

                Spacer(modifier = Modifier.height(24.dp))

                // Clean Title
                Text(
                    text = config?.title?.ifBlank { null } ?: "Update Available",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = SpeedoTextPrimary,
                        letterSpacing = (-0.5).sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Clean Message
                Text(
                    text = config?.message?.ifBlank { null }
                        ?: "A new version of Speedo is available. Please update now to continue.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SpeedoTextSecondary,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Clean Primary Update Now Button
                SpeedoPrimaryButton(
                    text = "UPDATE NOW",
                    leadingIcon = Icons.Default.SystemUpdate,
                    onClick = {
                        openBrowserForUpdate(context, config?.updateUrl)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Dismiss", color = SpeedoTextTertiary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * Clean & direct Update Now Dialog for Flexible (Optional) Updates
 * Displays only title, message, "Update Now" and "Later".
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
                text = config?.title?.ifBlank { null } ?: "Update Available",
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = config?.message?.ifBlank { null }
                    ?: "A new version of Speedo is available. Please update now to continue enjoying the latest features.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SpeedoTextSecondary,
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    openBrowserForUpdate(context, config?.updateUrl)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Update Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Later", color = SpeedoTextSecondary)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = SpeedoWhite
    )
}
