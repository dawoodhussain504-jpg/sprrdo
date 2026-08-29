package com.speedo.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.SpeedoEmptyView
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.components.StatusBadge
import com.speedo.core.theme.*

@Composable
fun UserManagementScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Captains, 1: Riders

    LaunchedEffect(Unit) {
        viewModel.fetchUsers()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "User & Captain Moderation",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.fetchUsers() }) {
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SpeedoWhite,
                contentColor = SpeedoOrange
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Captains (${uiState.captains.ifEmpty { uiState.kycQueue }.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Riders (${uiState.riders.size})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                val captains = uiState.captains.ifEmpty { uiState.kycQueue }
                if (captains.isEmpty()) {
                    SpeedoEmptyView(
                        icon = Icons.Default.People,
                        title = "No Captains Found",
                        message = "No registered captains on the platform yet."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(captains) { capt ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = SpeedoWhite,
                                border = BorderStroke(1.dp, SpeedoCardBorder),
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = capt.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            StatusBadge(status = capt.kycStatus)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val vName = when {
                                            capt.vehicleType.lowercase().contains("toto") || capt.vehicleType.lowercase().contains("auto") -> "Speedo Toto"
                                            capt.vehicleType.lowercase().contains("4") || capt.vehicleType.lowercase().contains("cab") -> "Speedo 4"
                                            else -> "Speedo Moto"
                                        }
                                        Text(text = "Vehicle: ${capt.vehicleNumber} ($vName) • Rating: ★ ${capt.rating}", style = MaterialTheme.typography.bodySmall, color = SpeedoOrange)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            viewModel.toggleUserStatus("captain", capt.id, capt.isActive == 1)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (capt.isActive == 1) SpeedoErrorContainer else SpeedoSuccessContainer,
                                            contentColor = if (capt.isActive == 1) SpeedoError else SpeedoSuccess
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (capt.isActive == 1) "Suspend" else "Activate",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val riders = uiState.riders
                if (riders.isEmpty()) {
                    SpeedoEmptyView(
                        icon = Icons.Default.People,
                        title = "No Riders Found",
                        message = "No registered riders found on the platform."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(riders) { rider ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = SpeedoWhite,
                                border = BorderStroke(1.dp, SpeedoCardBorder),
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = rider.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "${rider.phone} • ${rider.email}", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                                        Text(text = "Status: ${if (rider.isActive == 1) "ACTIVE" else "SUSPENDED"}", style = MaterialTheme.typography.bodySmall, color = if (rider.isActive == 1) SpeedoSuccess else SpeedoError)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            viewModel.toggleUserStatus("rider", rider.id, rider.isActive == 1)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (rider.isActive == 1) SpeedoErrorContainer else SpeedoSuccessContainer,
                                            contentColor = if (rider.isActive == 1) SpeedoError else SpeedoSuccess
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (rider.isActive == 1) "Suspend" else "Activate",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
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
