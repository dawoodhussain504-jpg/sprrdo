package com.speedo.captain.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.speedo.captain.viewmodel.CaptainViewModel
import com.speedo.core.components.SpeedoEmptyView
import com.speedo.core.components.SpeedoOutlinedButton
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.components.StatusBadge
import com.speedo.core.theme.*

@Composable
fun EarningsScreen(
    viewModel: CaptainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val captain = uiState.captain
    val rides by viewModel.cachedRides.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        viewModel.fetchProfile()
        viewModel.syncHistory()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "Earnings & Payouts"
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Earnings Summary Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SpeedoOrange,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "TOTAL EARNINGS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoOrangeContainer)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${captain?.totalEarnings?.toInt() ?: 0}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = SpeedoWhite,
                                fontSize = 36.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Trips: ${captain?.totalRides ?: 0}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = SpeedoWhite)
                            )
                            Text(
                                text = "Rating: ★ ${captain?.rating ?: 5.0}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = SpeedoWhite)
                            )
                        }
                    }
                }
            }

            // Daily Payout & Performance Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SpeedoWhite,
                    border = BorderStroke(1.dp, SpeedoCardBorder),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SpeedoOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Payouts & Direct Settlement",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Settlement Cycle", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                                Text(text = "Daily Instant Payout", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = SpeedoSuccess))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Vehicle Type", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                                Text(text = captain?.vehicleType?.uppercase() ?: "AUTO", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "⚡ Trip payments are collected via dynamic QR at the end of each trip and credited directly to your account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpeedoTextSecondary
                        )
                    }
                }
            }

            // Trip History Section
            item {
                Text(
                    text = "Completed Trips",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (rides.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SpeedoWhite,
                        border = BorderStroke(1.dp, SpeedoCardBorder)
                    ) {
                        Text(
                            text = "No completed trips yet. Go online to earn!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpeedoTextSecondary,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
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
                                    text = "Rider: ${ride.riderName ?: "Passenger"}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "+ ₹${ride.fare.toInt()}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SpeedoSuccess
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "${ride.pickupAddress} → ${ride.dropAddress}", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                        }
                    }
                }
            }
        }
    }
}
