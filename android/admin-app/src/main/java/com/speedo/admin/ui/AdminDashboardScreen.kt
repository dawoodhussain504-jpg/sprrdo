package com.speedo.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.theme.*

@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {},
    onNavigateToKyc: () -> Unit,
    onNavigateToSurge: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToBroadcasts: () -> Unit,
    onNavigateToDestinations: () -> Unit = {},
    onNavigateToMap: () -> Unit,
    onNavigateToRides: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToDeletions: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState.dashboardStats

    // Dynamic stats computation from loaded state if server stats empty
    val totalRevenue = stats?.totalRevenue ?: uiState.rides.filter { it.status == "completed" }.sumOf { it.fare }
    val completedRides = stats?.completedRides ?: uiState.rides.count { it.status == "completed" }
    val activeRides = stats?.activeRides ?: uiState.rides.count { it.status in listOf("requested", "accepted", "arrived", "ongoing") }
    val pendingKycCount = stats?.pendingKycCount ?: uiState.kycQueue.size
    val onlineCaptains = stats?.onlineCaptains ?: uiState.captains.count { it.isOnline }
    val totalRiders = stats?.totalRiders ?: uiState.riders.size
    val activeSosCount = uiState.activeSosCount

    LaunchedEffect(Unit) {
        viewModel.startDashboardPolling()
        viewModel.fetchKycQueue()
        viewModel.fetchRides()
        viewModel.fetchUsers()
        viewModel.fetchSurgeZones()
        viewModel.fetchSosAlerts()
        viewModel.fetchBroadcasts()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "Platform Dashboard",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = {
                        viewModel.startDashboardPolling()
                        viewModel.fetchKycQueue()
                        viewModel.fetchRides()
                        viewModel.fetchUsers()
                        viewModel.fetchSurgeZones()
                        viewModel.fetchSosAlerts()
                        viewModel.fetchBroadcasts()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SpeedoTextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // SOS Emergency Flashing Banner
            if (activeSosCount > 0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToSos),
                    shape = RoundedCornerShape(16.dp),
                    color = SpeedoError,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(SpeedoWhite)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "🚨 $activeSosCount ACTIVE EMERGENCY",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SpeedoWhite
                                    )
                                )
                                Text(
                                    text = "Tap to open SOS Command Center & dispatch police",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoWhite.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SpeedoWhite)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Main Revenue Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SpeedoOrange,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "TOTAL PLATFORM REVENUE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoOrangeContainer)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${totalRevenue.toInt()}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = SpeedoWhite,
                            fontSize = 34.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Completed Rides: $completedRides",
                        style = MaterialTheme.typography.bodyMedium.copy(color = SpeedoWhite)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Key Metrics & Operations",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2x2 Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminMetricCard(
                    title = "Active Rides",
                    value = "$activeRides",
                    icon = Icons.Default.DirectionsCar,
                    accentColor = SpeedoSuccess,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToRides
                )
                AdminMetricCard(
                    title = "Pending KYC",
                    value = "$pendingKycCount",
                    icon = Icons.Default.VerifiedUser,
                    accentColor = SpeedoAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToKyc
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminMetricCard(
                    title = "Surge Zones",
                    value = "${uiState.surgeZones.size}",
                    icon = Icons.Default.Bolt,
                    accentColor = SpeedoOrange,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSurge
                )
                AdminMetricCard(
                    title = "SOS Alerts",
                    value = "$activeSosCount",
                    icon = Icons.Default.Shield,
                    accentColor = if (activeSosCount > 0) SpeedoError else SpeedoSuccess,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSos
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Enterprise Operations Shortcuts",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            AdminShortcutRow(
                icon = Icons.Default.VerifiedUser,
                title = "AI Document OCR & KYC Queue",
                subtitle = "Auto-scan driver documents & 1-click approve",
                onClick = onNavigateToKyc
            )

            Spacer(modifier = Modifier.height(10.dp))

            AdminShortcutRow(
                icon = Icons.Default.Bolt,
                title = "Geofenced Surge Pricing Engine",
                subtitle = "Configure live multipliers for airports & peak areas",
                onClick = onNavigateToSurge
            )

            Spacer(modifier = Modifier.height(10.dp))

            AdminShortcutRow(
                icon = Icons.Default.Shield,
                title = "Live SOS Emergency Command Center",
                subtitle = "Real-time incident feed, police dispatch & user safety",
                onClick = onNavigateToSos
            )

            Spacer(modifier = Modifier.height(10.dp))


            AdminShortcutRow(
                icon = Icons.Default.Place,
                title = "Popular Destinations Management",
                subtitle = "Add, edit, or delete live thumbnail landmarks for riders & captains",
                onClick = onNavigateToDestinations
            )

            Spacer(modifier = Modifier.height(10.dp))
            AdminShortcutRow(
                icon = Icons.Default.Campaign,
                title = "Targeted City-Wide Broadcasts",
                subtitle = "Dispatch instant push offers, promo codes & bonuses",
                onClick = onNavigateToBroadcasts
            )

            Spacer(modifier = Modifier.height(10.dp))

            AdminShortcutRow(
                icon = Icons.Default.Map,
                title = "Live Fleet & Rides Map",
                subtitle = "Real-time osmdroid view of active Bangalore fleet",
                onClick = onNavigateToMap
            )

            Spacer(modifier = Modifier.height(10.dp))

            AdminShortcutRow(
                icon = Icons.Default.DirectionsBike,
                title = "Ride Monitoring & History",
                subtitle = "Inspect ride status lifecycles, fares, and routes",
                onClick = onNavigateToRides
            )

            Spacer(modifier = Modifier.height(10.dp))


            AdminShortcutRow(
                icon = Icons.Default.DeleteForever,
                title = "Account Deletion Requests",
                subtitle = "Review 24-hour deletion requests & approve permanent database purge",
                badge = if (uiState.pendingDeletionCount > 0) "${uiState.pendingDeletionCount} PENDING" else null,
                onClick = onNavigateToDeletions
            )

            Spacer(modifier = Modifier.height(10.dp))
            AdminShortcutRow(
                icon = Icons.Default.People,
                title = "User Moderation & Access Control",
                subtitle = "Inspect riders and captains, manage accounts and permissions",
                onClick = onNavigateToUsers
            )
        }
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
        }
    }
}

@Composable
fun AdminShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SpeedoError
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SpeedoWhite
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SpeedoTextTertiary)
        }
    }
}
