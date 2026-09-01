package com.speedo.rider.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.speedo.core.maps.AddressSuggestion
import com.speedo.core.maps.LocationSearchHelper
import com.speedo.core.model.PopularDestination
import com.speedo.core.model.PopularDestinationsData
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

                val vehicleDisplayName = when {
                    vehicleType.lowercase().contains("toto") || vehicleType.lowercase().contains("auto") -> "Speedo Toto"
                    vehicleType.lowercase().contains("4") || vehicleType.lowercase().contains("cab") -> "Speedo 4"
                    else -> "Speedo Moto"
                }

                Text(
                    text = "Searching ${if (nearbyCount > 0) "$nearbyCount" else "active"} $vehicleDisplayName captains within 2.5 km",
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
                        text = category.displayName,
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

enum class LocationFieldType {
    PICKUP,
    DROP
}

/**
 * Visual Thumbnail Card for Popular Destinations with Real Photography
 */
@Composable
fun PopularDestinationThumbnailCard(
    destination: PopularDestination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder),
        shadowElevation = 4.dp,
        modifier = modifier
            .width(185.dp)
            .clickable { onClick() }
    ) {
        Column {
            // Thumbnail Image with Overlay Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .background(SpeedoSurfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(destination.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = destination.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Scrim gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                                startY = 40f
                            )
                        )
                )

                // Category & Badge tag overlay
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = RapidoBlack.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = destination.badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SpeedoWhite,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (destination.distanceKm != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SpeedoWhite.copy(alpha = 0.95f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = SpeedoOrange,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f km", destination.distanceKm),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RapidoBlack,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            // Destination details
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = destination.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = RapidoBlack
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = destination.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = SpeedoTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Rapido-style Live Location Search Dialog with Editable Pickup & Drop Location Search Boxes
 * plus Popular Destinations Suggestions in Thumbnails View with Place Images
 */
@Composable
fun RapidoLocationSearchDialog(
    currentPickup: String,
    currentDrop: String,
    initialFocusOnPickup: Boolean = false,
    userLat: Double = 12.9716,
    userLng: Double = 77.5946,
    onPickupSelected: (String, Double, Double) -> Unit,
    onDropSelected: (String, Double, Double) -> Unit,
    onUseCurrentLocation: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeField by remember { mutableStateOf(if (initialFocusOnPickup) LocationFieldType.PICKUP else LocationFieldType.DROP) }
    var pickupInput by remember { mutableStateOf(currentPickup) }
    var dropInput by remember { mutableStateOf(currentDrop) }
    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val popularDestinations = remember(userLat, userLng) {
        PopularDestinationsData.getDestinationsWithDistance(userLat, userLng)
    }

    val activeQuery = if (activeField == LocationFieldType.PICKUP) pickupInput else dropInput

    // Live Geocoding Autocomplete with 250ms debouncing based on active field
    LaunchedEffect(activeField, activeQuery) {
        val cleanQuery = activeQuery.trim()
        if (cleanQuery.isEmpty()) {
            suggestions = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(250) // Debounce typing
        try {
            suggestions = LocationSearchHelper.searchAddress(
                context = context,
                query = cleanQuery,
                userLat = userLat,
                userLng = userLng
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
                    text = if (activeField == LocationFieldType.PICKUP) "Select Pickup Location" else "Select Destination",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Dual Editable Pickup & Drop Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SpeedoSurfaceVariant,
                border = BorderStroke(1.dp, SpeedoCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Pickup Row (Editable)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(RapidoGreen)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = pickupInput,
                            onValueChange = {
                                pickupInput = it
                                activeField = LocationFieldType.PICKUP
                            },
                            placeholder = { Text("Search pickup address or landmark...") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeField = LocationFieldType.PICKUP },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (activeField == LocationFieldType.PICKUP) RapidoGreen else Color.Transparent,
                                unfocusedBorderColor = if (activeField == LocationFieldType.PICKUP) RapidoGreen.copy(alpha = 0.5f) else Color.Transparent,
                                focusedContainerColor = SpeedoWhite,
                                unfocusedContainerColor = SpeedoWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                if (pickupInput.isNotEmpty()) {
                                    IconButton(onClick = { pickupInput = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = SpeedoTextSecondary)
                                    }
                                }
                            }
                        )
                    }

                    // Middle action row (GPS Reset Button & Swap Button)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SpeedoWhite,
                            border = BorderStroke(0.8.dp, RapidoGreen.copy(alpha = 0.6f)),
                            modifier = Modifier.clickable {
                                onUseCurrentLocation()
                                pickupInput = "Current Location"
                                activeField = LocationFieldType.DROP
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = RapidoGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Use Current Location",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = RapidoGreen,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val temp = pickupInput
                                pickupInput = dropInput
                                dropInput = temp
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Swap Locations", tint = SpeedoOrange)
                        }
                    }

                    // Drop Row (Editable)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(RapidoRed)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = dropInput,
                            onValueChange = {
                                dropInput = it
                                activeField = LocationFieldType.DROP
                            },
                            placeholder = { Text("Search drop destination or area...") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeField = LocationFieldType.DROP },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (activeField == LocationFieldType.DROP) RapidoRed else Color.Transparent,
                                unfocusedBorderColor = if (activeField == LocationFieldType.DROP) RapidoRed.copy(alpha = 0.5f) else Color.Transparent,
                                focusedContainerColor = SpeedoWhite,
                                unfocusedContainerColor = SpeedoWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                if (dropInput.isNotEmpty()) {
                                    IconButton(onClick = { dropInput = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = SpeedoTextSecondary)
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
                    color = if (activeField == LocationFieldType.PICKUP) RapidoGreen else RapidoYellowDark
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // When no query is typed: Show Popular Destinations in Thumbnails View
            if (activeQuery.trim().isEmpty()) {
                Text(
                    text = "POPULAR DESTINATIONS IN BANGALORE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = SpeedoTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Thumbnails Carousel View with Place Images
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(popularDestinations, key = { it.id }) { dest ->
                        PopularDestinationThumbnailCard(
                            destination = dest,
                            onClick = {
                                if (activeField == LocationFieldType.PICKUP) {
                                    onPickupSelected(dest.fullAddress, dest.lat, dest.lng)
                                    pickupInput = dest.title
                                    activeField = LocationFieldType.DROP
                                } else {
                                    onDropSelected(dest.fullAddress, dest.lat, dest.lng)
                                    onDismiss()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "FAVORITE TRANSIT & LANDMARKS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = SpeedoTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(popularDestinations) { dest ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (activeField == LocationFieldType.PICKUP) {
                                        onPickupSelected(dest.fullAddress, dest.lat, dest.lng)
                                        pickupInput = dest.title
                                        activeField = LocationFieldType.DROP
                                    } else {
                                        onDropSelected(dest.fullAddress, dest.lat, dest.lng)
                                        onDismiss()
                                    }
                                },
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(SpeedoSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (dest.category) {
                                            "AIRPORT" -> Icons.Default.Flight
                                            "METRO" -> Icons.Default.DirectionsSubway
                                            "MALL" -> Icons.Default.ShoppingBag
                                            "TECH PARK" -> Icons.Default.Business
                                            "TRANSIT" -> Icons.Default.Train
                                            "CAFE" -> Icons.Default.LocalCafe
                                            "PARK" -> Icons.Default.Park
                                            else -> Icons.Default.LocationOn
                                        },
                                        contentDescription = null,
                                        tint = RapidoBlack,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dest.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = dest.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SpeedoTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (dest.distanceKm != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = String.format("%.1f km", dest.distanceKm),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = SpeedoTextSecondary
                                        )
                                    )
                                }
                            }
                        }
                        Divider(color = SpeedoDivider.copy(alpha = 0.5f))
                    }
                }
            } else {
                // When searching: Show Autocomplete address results
                Text(
                    text = "SEARCH RESULTS FOR ${if (activeField == LocationFieldType.PICKUP) "PICKUP" else "DESTINATION"}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = SpeedoTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(suggestions, key = { "${it.title}_${it.lat}_${it.lng}" }) { suggestion ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (activeField == LocationFieldType.PICKUP) {
                                        onPickupSelected(suggestion.fullAddress, suggestion.lat, suggestion.lng)
                                        pickupInput = suggestion.title
                                        activeField = LocationFieldType.DROP
                                    } else {
                                        onDropSelected(suggestion.fullAddress, suggestion.lat, suggestion.lng)
                                        onDismiss()
                                    }
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
}


/**
 * 24x7 Safety Shield Dialog with SOS, Police 100/112 dialer, and Emergency Contact sharing
 */
@Composable
fun RapidoSafetySheet(
    onTriggerSos: () -> Unit = {},
    onDismiss: () -> Unit
) {
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

            // Real-Time Emergency SOS Button
            Button(
                onClick = {
                    onTriggerSos()
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                    context.startActivity(intent)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SpeedoError)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = SpeedoWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "🚨 Broadcast SOS & Call 112", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = SpeedoWhite)
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
