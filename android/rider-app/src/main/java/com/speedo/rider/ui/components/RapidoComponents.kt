package com.speedo.rider.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.core.maps.AddressSuggestion
import com.speedo.core.maps.LocationSearchHelper
import com.speedo.core.model.VehicleCategory
import com.speedo.core.theme.*
import kotlinx.coroutines.delay

// Signature Rapido Theme Colors
val RapidoYellow = Color(0xFFFFCC00)
val RapidoYellowDark = Color(0xFFF5B800)
val RapidoYellowLight = Color(0xFFFFF8E1)
val RapidoBlack = Color(0xFF1E1E1E)
val RapidoCardBg = Color(0xFFFFFFFF)
val RapidoGreen = Color(0xFF00C853)
val RapidoRed = Color(0xFFFF3D00)

/**
 * Animated Radar Pulse Overlay displayed while searching for nearby Captains
 */
@Composable
fun RadarPulseSearchingOverlay(
    vehicleType: String,
    nearbyCount: Int,
    pickupAddress: String,
    dropAddress: String,
    fare: Int,
    onCancelBooking: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_transition")

    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )

    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RapidoBlack.copy(alpha = 0.85f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        // Radar Pulse Waves
        Box(contentAlignment = Alignment.Center) {
            // Pulse wave 1
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulse1)
                    .clip(CircleShape)
                    .background(RapidoYellow.copy(alpha = alpha1))
            )
            // Pulse wave 2
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulse2)
                    .clip(CircleShape)
                    .background(RapidoYellow.copy(alpha = alpha2))
            )

            // Center Pulse Icon
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(RapidoYellow, RapidoYellowDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (vehicleType.lowercase()) {
                        "bike" -> Icons.Default.TwoWheeler
                        "auto" -> Icons.Default.ElectricRickshaw
                        else -> Icons.Default.DirectionsCar
                    },
                    contentDescription = null,
                    tint = RapidoBlack,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Bottom Info & Cancel Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = SpeedoWhite,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = RapidoYellowDark,
                    trackColor = RapidoYellowLight
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Connecting with nearby Captains...",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = RapidoBlack
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Searching ${if (nearbyCount > 0) "$nearbyCount" else "active"} ${vehicleType.uppercase()} captains within 2.5 km",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpeedoTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Route summary pill
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SpeedoSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "To: $dropAddress",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Guaranteed Fare • No Surge",
                                style = MaterialTheme.typography.labelSmall,
                                color = SpeedoSuccess
                            )
                        }
                        Text(
                            text = "₹$fare",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = RapidoBlack
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onCancelBooking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, SpeedoError),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SpeedoError)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Cancel Request", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Rapido-style Vehicle Option Card with live ETA, Strikethrough Discount & Badges
 */
@Composable
fun RapidoVehicleOptionCard(
    category: VehicleCategory,
    isSelected: Boolean,
    fare: Int,
    etaMinutes: Int,
    discountFare: Int? = null,
    badgeText: String? = null,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) RapidoYellowLight else RapidoCardBg,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) RapidoYellowDark else SpeedoCardBorder
        ),
        shadowElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle Icon Container with Rapido Style
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when (category) {
                            VehicleCategory.BIKE -> RapidoYellow.copy(alpha = 0.25f)
                            VehicleCategory.AUTO -> Color(0xFF00C853).copy(alpha = 0.18f)
                            VehicleCategory.CAB -> Color(0xFF2979FF).copy(alpha = 0.18f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (category) {
                        VehicleCategory.BIKE -> Icons.Default.TwoWheeler
                        VehicleCategory.AUTO -> Icons.Default.ElectricRickshaw
                        VehicleCategory.CAB -> Icons.Default.DirectionsCar
                    },
                    contentDescription = null,
                    tint = when (category) {
                        VehicleCategory.BIKE -> Color(0xFFD68A00)
                        VehicleCategory.AUTO -> Color(0xFF008937)
                        VehicleCategory.CAB -> Color(0xFF1565C0)
                    },
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Vehicle Name & ETA
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (category) {
                            VehicleCategory.BIKE -> "Rapido Bike"
                            VehicleCategory.AUTO -> "Rapido Auto"
                            VehicleCategory.CAB -> "Speedo Cab Economy"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RapidoBlack
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Capacity pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = SpeedoTextSecondary
                        )
                        Text(
                            text = when (category) {
                                VehicleCategory.BIKE -> "1"
                                VehicleCategory.AUTO -> "3"
                                VehicleCategory.CAB -> "4"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = SpeedoTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ETA Pill
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "⚡ $etaMinutes min away",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = RapidoYellowLight,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, RapidoYellowDark)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB78103)
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SpeedoTextSecondary,
                    maxLines = 1
                )
            }

            // Fare with strikethrough discount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹$fare",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = RapidoBlack
                    )
                )
                if (discountFare != null) {
                    Text(
                        text = "₹$discountFare",
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.LineThrough,
                            color = SpeedoTextSecondary
                        )
                    )
                }
            }
        }
    }
}

/**
 * Rapido-style Live Location Search Dialog with Leaflet / OpenStreetMap / Photon Autocomplete
 */
@Composable
fun RapidoLocationSearchDialog(
    currentPickup: String,
    currentDrop: String,
    onPickupSelected: (String, Double, Double) -> Unit,
    onDropSelected: (String, Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var dropInput by remember { mutableStateOf(currentDrop) }
    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Live Geocoding Autocomplete with 250ms debouncing
    LaunchedEffect(dropInput) {
        isSearching = true
        delay(250) // Debounce typing
        try {
            suggestions = LocationSearchHelper.searchAddress(
                context = context,
                query = dropInput,
                userLat = 12.9716,
                userLng = 77.5946
            )
        } finally {
            isSearching = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SpeedoWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Select Destination",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pickup & Drop input card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SpeedoSurfaceVariant,
                border = BorderStroke(1.dp, SpeedoCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Pickup Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(RapidoGreen)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = currentPickup.ifEmpty { "Current Location (Indiranagar, Bangalore)" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = SpeedoDivider)

                    // Drop Row with live typing
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(RapidoRed)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = dropInput,
                            onValueChange = { dropInput = it },
                            placeholder = { Text("Search landmark, road, building, area...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RapidoYellowDark,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (dropInput.isNotEmpty()) {
                                    IconButton(onClick = { dropInput = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (isSearching) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = RapidoYellowDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (dropInput.isEmpty()) "POPULAR DESTINATIONS IN BANGALORE" else "SEARCH RESULTS & ADDRESS SUGGESTIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = SpeedoTextSecondary,
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location suggestions list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(suggestions, key = { "${it.title}_${it.lat}_${it.lng}" }) { suggestion ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDropSelected(suggestion.fullAddress, suggestion.lat, suggestion.lng)
                                onDismiss()
                            },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SpeedoSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        suggestion.title.contains("Metro", true) -> Icons.Default.DirectionsSubway
                                        suggestion.title.contains("Airport", true) -> Icons.Default.Flight
                                        suggestion.title.contains("Mall", true) -> Icons.Default.ShoppingBag
                                        suggestion.title.contains("Station", true) -> Icons.Default.Train
                                        suggestion.title.contains("Hospital", true) -> Icons.Default.LocalHospital
                                        suggestion.title.contains("Park", true) -> Icons.Default.Business
                                        else -> Icons.Default.LocationOn
                                    },
                                    contentDescription = null,
                                    tint = RapidoBlack,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = suggestion.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = suggestion.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (suggestion.distanceKm != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format("%.1f km", suggestion.distanceKm),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = SpeedoTextSecondary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.NorthWest,
                                contentDescription = null,
                                tint = SpeedoTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Divider(color = SpeedoDivider.copy(alpha = 0.5f))
                }
            }
        }
    }
}

/**
 * 24x7 Safety Shield Dialog with SOS, Police 100/112 dialer, and Emergency Contact sharing
 */
@Composable
fun RapidoSafetySheet(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = SpeedoWhite,
        shadowElevation = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = SpeedoError, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Speedo Safety Toolkit",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Police SOS 112 Button
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SpeedoError)
            ) {
                Icon(Icons.Default.LocalPolice, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Call Police (112)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Share Trip Button
            OutlinedButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Speedo Live Trip Tracking")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "I'm riding with Speedo! Track my live ride location here: https://web-production-5d826.up.railway.app/ride/track"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Live Ride with Friends/Family"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, RapidoBlack)
            ) {
                Icon(Icons.Default.ShareLocation, contentDescription = null, tint = RapidoBlack)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Share Live Trip with Family", color = RapidoBlack, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 24x7 Safety Helpline
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:18001234567"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, SpeedoSuccess)
            ) {
                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = SpeedoSuccess)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "24x7 Safety Helpline (Free)", color = SpeedoSuccess, fontWeight = FontWeight.Bold)
            }
        }
    }
}
