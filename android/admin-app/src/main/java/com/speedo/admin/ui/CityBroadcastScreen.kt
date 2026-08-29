package com.speedo.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.*
import com.speedo.core.model.BroadcastAnnouncement
import com.speedo.core.theme.*

@Composable
fun CityBroadcastScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val broadcasts = uiState.broadcasts

    var showComposeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchBroadcasts()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "City-Wide Broadcasts",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.fetchBroadcasts() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SpeedoTextPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showComposeDialog = true },
                containerColor = SpeedoOrange,
                contentColor = SpeedoWhite,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Campaign, contentDescription = "Compose Broadcast")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                color = SpeedoOrangeContainer,
                border = BorderStroke(1.dp, SpeedoOrange.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SpeedoOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = SpeedoWhite, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Instant Mass Notification Engine",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = SpeedoTextPrimary)
                        )
                        Text(
                            text = "Send flash discounts to riders or surge bonuses to captains across entire cities in real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpeedoTextSecondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Broadcast History",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "${broadcasts.size} Sent",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpeedoTextSecondary
                )
            }

            if (broadcasts.isEmpty()) {
                SpeedoEmptyView(
                    icon = Icons.Default.Campaign,
                    title = "No Broadcasts Sent",
                    message = "Tap the announcement button to compose and dispatch city-wide push campaigns."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(broadcasts) { bcast ->
                        BroadcastCard(broadcast = bcast)
                    }
                }
            }
        }
    }

    if (showComposeDialog) {
        ComposeBroadcastDialog(
            onDismiss = { showComposeDialog = false },
            onSend = { title, message, audience, city, coupon, discount, bonus ->
                viewModel.sendBroadcast(title, message, audience, city, coupon, discount, bonus) {
                    showComposeDialog = false
                }
            }
        )
    }
}

@Composable
fun BroadcastCard(broadcast: BroadcastAnnouncement) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (broadcast.targetAudience) {
                            "riders" -> SpeedoOrange
                            "captains" -> SpeedoSuccess
                            else -> SpeedoTextPrimary
                        }
                    ) {
                        Text(
                            text = broadcast.targetAudience.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = SpeedoWhite
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SpeedoSurfaceVariant
                    ) {
                        Text(
                            text = broadcast.targetCity,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SpeedoTextPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "${broadcast.totalRecipients} Reached",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SpeedoSuccess
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = broadcast.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = broadcast.message,
                style = MaterialTheme.typography.bodySmall,
                color = SpeedoTextSecondary
            )

            if (!broadcast.couponCode.isNullOrBlank() || broadcast.bonusAmount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!broadcast.couponCode.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, SpeedoSuccess)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocalOffer, contentDescription = null, tint = SpeedoSuccess, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Coupon: " + broadcast.couponCode + " (" + broadcast.discountPercent.toInt() + "% OFF)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoSuccess)
                                )
                            }
                        }
                    }
                    if (broadcast.bonusAmount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SpeedoOrangeContainer,
                            border = BorderStroke(1.dp, SpeedoOrange.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "+₹" + broadcast.bonusAmount.toInt() + " Driver Incentive Bonus",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoTextPrimary),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComposeBroadcastDialog(
    onDismiss: () -> Unit,
    onSend: (String, String, String, String, String?, Double, Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetAudience by remember { mutableStateOf("all") }
    var targetCity by remember { mutableStateOf("All Cities") }
    var couponCode by remember { mutableStateOf("") }
    var discountPercent by remember { mutableStateOf(20f) }
    var bonusAmount by remember { mutableStateOf(50f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SpeedoWhite,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Compose City Broadcast",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Target Audience:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "all" to "All Users 👥",
                        "riders" to "Riders Only 🚗",
                        "captains" to "Captains Only 🛵"
                    ).forEach { (audKey, audLabel) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (targetAudience == audKey) SpeedoOrange else SpeedoSurfaceVariant,
                            modifier = Modifier.clickable { targetAudience = audKey }
                        ) {
                            Text(
                                text = audLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetAudience == audKey) SpeedoWhite else SpeedoTextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SpeedoTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Broadcast Headline",
                    placeholder = "e.g. Weekend Monsoon Offer! 🌧️"
                )

                Spacer(modifier = Modifier.height(10.dp))

                SpeedoTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = "Push Notification Body",
                    placeholder = "e.g. Enjoy flat discounts on all Speedo Moto and Speedo Toto rides today.",
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SpeedoTextField(
                        value = targetCity,
                        onValueChange = { targetCity = it },
                        label = "City Target",
                        modifier = Modifier.weight(1f)
                    )
                    SpeedoTextField(
                        value = couponCode,
                        onValueChange = { couponCode = it },
                        label = "Promo Code (Optional)",
                        placeholder = "e.g. RAIN30",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SpeedoPrimaryButton(
                    text = "Send Broadcast Now 🚀",
                    leadingIcon = Icons.Default.Send,
                    onClick = {
                        if (title.isNotBlank() && message.isNotBlank()) {
                            onSend(
                                title.trim(),
                                message.trim(),
                                targetAudience,
                                targetCity.trim().ifBlank { "All Cities" },
                                couponCode.trim().ifBlank { null },
                                discountPercent.toDouble(),
                                if (targetAudience == "captains" || targetAudience == "all") bonusAmount.toDouble() else 0.0
                            )
                        }
                    }
                )
            }
        }
    }
}
