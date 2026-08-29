package com.speedo.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.SpeedoEmptyView
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.components.StatusBadge
import com.speedo.core.theme.*

@Composable
fun RidesMonitoringScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val rides = uiState.rides
    val currentFilter = uiState.selectedRideFilter

    val filterOptions = listOf(
        "all" to "All Rides",
        "requested" to "Requested",
        "accepted" to "Accepted",
        "arrived" to "Arrived",
        "ongoing" to "Ongoing",
        "completed" to "Completed",
        "cancelled" to "Cancelled"
    )

    LaunchedEffect(currentFilter) {
        viewModel.fetchRides(currentFilter)
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "Ride Monitoring",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.fetchRides(currentFilter) }) {
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
            // Horizontal Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { (key, label) ->
                    val isSelected = currentFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.fetchRides(key) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpeedoOrange,
                            selectedLabelColor = SpeedoWhite
                        )
                    )
                }
            }

            if (rides.isEmpty()) {
                SpeedoEmptyView(
                    icon = Icons.Default.DirectionsCar,
                    title = "No Rides Found",
                    message = "No rides match the selected filter '$currentFilter'."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rides) { ride ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = SpeedoWhite,
                            border = BorderStroke(1.dp, SpeedoCardBorder),
                            shadowElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Trip ID: ${ride.id.takeLast(8)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = SpeedoOrange)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (ride.status.equals("completed", ignoreCase = true)) {
                                            Surface(
                                                color = Color(0xFFE8F5E9),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, Color(0xFF4CAF50))
                                            ) {
                                                Text(
                                                    text = "PAID",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        StatusBadge(status = ride.status)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(text = "Pickup: ${ride.pickupAddress}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(text = "Drop: ${ride.dropAddress}", style = MaterialTheme.typography.bodyMedium, color = SpeedoTextSecondary)

                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = SpeedoDivider)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Rider: ${ride.riderName ?: "User"}", style = MaterialTheme.typography.bodySmall)
                                        val vName = when {
                                            ride.vehicleType.lowercase().contains("toto") || ride.vehicleType.lowercase().contains("auto") -> "Speedo Toto"
                                            ride.vehicleType.lowercase().contains("4") || ride.vehicleType.lowercase().contains("cab") -> "Speedo 4"
                                            else -> "Speedo Moto"
                                        }
                                        Text(text = "Captain: ${ride.captainName ?: "Unassigned"} • $vName (${ride.vehicleNumber ?: "N/A"})", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                                    }
                                    Text(
                                        text = "₹${ride.fare.toInt()}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = SpeedoOrange)
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
