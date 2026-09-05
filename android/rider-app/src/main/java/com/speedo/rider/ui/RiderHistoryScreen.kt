package com.speedo.rider.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speedo.core.components.SpeedoEmptyView
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.components.StatusBadge
import com.speedo.core.theme.*
import com.speedo.rider.viewmodel.RiderViewModel

@Composable
fun RiderHistoryScreen(
    viewModel: RiderViewModel
) {
    val rides by viewModel.cachedRides.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        viewModel.syncHistory()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(title = "My Trips")
        }
    ) { padding ->
        if (rides.isEmpty()) {
            SpeedoEmptyView(
                icon = Icons.Default.History,
                title = "No Trips Yet",
                message = "Your completed and past rides will appear here.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rides) { ride ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SpeedoWhite,
                        border = BorderStroke(1.dp, SpeedoCardBorder),
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${ride.vehicleType.uppercase()} RIDE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = SpeedoOrange)
                                )
                                StatusBadge(status = ride.status)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "From: ${ride.pickupAddress}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "To: ${ride.dropAddress}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SpeedoTextSecondary
                            )

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = SpeedoDivider)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${ride.distanceKm} km • ${ride.captainName ?: "Speedo Driver"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoTextSecondary
                                )
                                Text(
                                    text = "₹${ride.fare.toInt()}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SpeedoOrange
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
