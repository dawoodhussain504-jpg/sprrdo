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
    onNavigateToMap: () -> Unit,
    onNavigateToRides: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToUsers: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState.dashboardStats

    LaunchedEffect(Unit) {
        viewModel.startDashboardPolling()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "Platform Dashboard",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.startDashboardPolling() }) {
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
                        text = "₹${stats?.totalRevenue?.toInt() ?: 0}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = SpeedoWhite,
                            fontSize = 34.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Completed Rides: ${stats?.completedRides ?: 0}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = SpeedoWhite)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Key Metrics",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2x2 Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminMetricCard(
                    title = "Active Rides",
                    value = "${stats?.activeRides ?: 0}",
                    icon = Icons.Default.DirectionsCar,
                    accentColor = SpeedoSuccess,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToRides
                )
                AdminMetricCard(
                    title = "Pending KYC",
                    value = "${stats?.pendingKycCount ?: 0}",
                    icon = Icons.Default.VerifiedUser,
                    accentColor = SpeedoAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToKyc
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminMetricCard(
                    title = "Online Captains",
                    value = "${stats?.onlineCaptains ?: 0}",
                    icon = Icons.Default.TwoWheeler,
                    accentColor = SpeedoOrange,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMap
                )
                AdminMetricCard(
                    title = "Total Riders",
                    value = "${stats?.totalRiders ?: 0}",
                    icon = Icons.Default.People,
                    accentColor = SpeedoInfo,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToUsers
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Operational Shortcuts",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            AdminShortcutRow(
                icon = Icons.Default.SupportAgent,
                title = "24/7 Support & Queries Desk",
                subtitle = "Manage rider & captain complaints and live chat threads",
                onClick = onNavigateToSupport
            )

            Spacer(modifier = Modifier.height(10.dp))

            AdminShortcutRow(
                icon = Icons.Default.AssignmentLate,
                title = "KYC Verification Queue",
                subtitle = "Review driver vehicle, Aadhaar, selfie & QR docs",
                onClick = onNavigateToKyc
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
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SpeedoTextTertiary)
        }
    }
}
