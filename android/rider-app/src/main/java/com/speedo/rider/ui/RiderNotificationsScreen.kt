package com.speedo.rider.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.core.components.SpeedoEmptyView
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.components.openBrowserForUpdate
import com.speedo.core.theme.*
import com.speedo.core.utils.InAppUpdateManager
import com.speedo.core.utils.DownloadStatus
import com.speedo.rider.viewmodel.RiderViewModel

@Composable
fun RiderNotificationsScreen(
    viewModel: RiderViewModel
) {
    val context = LocalContext.current
    val notifications by viewModel.cachedNotifications.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val appUpdate = uiState.appUpdateState

    LaunchedEffect(Unit) {
        viewModel.syncNotifications()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(title = "Notifications")
        }
    ) { padding ->
        if (notifications.isEmpty() && !appUpdate.isUpdateAvailable) {
            SpeedoEmptyView(
                icon = Icons.Default.NotificationsNone,
                title = "No Notifications",
                message = "Updates on your ride requests and promos will appear here.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (appUpdate.isUpdateAvailable) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    InAppUpdateManager.startDownloadAndInstall(context, appUpdate.updateUrl)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = SpeedoOrange.copy(alpha = 0.09f),
                            border = BorderStroke(1.5.dp, SpeedoOrange),
                            shadowElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SpeedoOrange,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = "⚡ APP UPDATE READY",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoWhite),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "Speedo Rider v${appUpdate.latestVersionName} (Build #${appUpdate.latestVersionCode})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "A new version of Speedo Rider is ready to download and install.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SpeedoTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { InAppUpdateManager.startDownloadAndInstall(context, appUpdate.updateUrl) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("DOWNLOAD & INSTALL UPDATE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                items(notifications) { notif ->
                    val isUnread = notif.isRead == 0
                    val isUpdate = notif.isAppUpdateNotification()

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isUnread) viewModel.markNotificationRead(notif.id)
                                if (isUpdate) {
                                    val url = notif.extractUpdateUrl("rider")
                                    InAppUpdateManager.startDownloadAndInstall(context, url)
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isUpdate -> SpeedoOrange.copy(alpha = 0.07f)
                            isUnread -> SpeedoOrangeContainer.copy(alpha = 0.35f)
                            else -> SpeedoWhite
                        },
                        border = BorderStroke(
                            width = if (isUpdate) 1.5.dp else 1.dp,
                            color = when {
                                isUpdate -> SpeedoOrange
                                isUnread -> SpeedoOrange.copy(alpha = 0.5f)
                                else -> SpeedoCardBorder
                            }
                        ),
                        shadowElevation = if (isUpdate) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (isUpdate) Icons.Default.RocketLaunch else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (isUpdate || isUnread) SpeedoOrange else SpeedoTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                if (isUpdate) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SpeedoOrange.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = "⚡ APP UPDATE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = SpeedoOrange
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = notif.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isUnread || isUpdate) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = notif.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SpeedoTextSecondary
                                )

                                // Clickable Update Badge / Button
                                if (isUpdate) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            if (isUnread) viewModel.markNotificationRead(notif.id)
                                            val url = notif.extractUpdateUrl("rider")
                                            InAppUpdateManager.startDownloadAndInstall(context, url)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("UPDATE NOW (DOWNLOAD & INSTALL)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
