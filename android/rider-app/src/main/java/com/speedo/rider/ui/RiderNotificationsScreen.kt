package com.speedo.rider.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speedo.core.components.SpeedoEmptyView
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.theme.*
import com.speedo.rider.viewmodel.RiderViewModel

@Composable
fun RiderNotificationsScreen(
    viewModel: RiderViewModel
) {
    val notifications by viewModel.cachedNotifications.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        viewModel.syncNotifications()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(title = "Notifications")
        }
    ) { padding ->
        if (notifications.isEmpty()) {
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications) { notif ->
                    val isUnread = notif.isRead == 0

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isUnread) viewModel.markNotificationRead(notif.id)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUnread) SpeedoOrangeContainer.copy(alpha = 0.4f) else SpeedoWhite,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isUnread) SpeedoOrange.copy(alpha = 0.5f) else SpeedoCardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (isUnread) SpeedoOrange else SpeedoTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SpeedoTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
