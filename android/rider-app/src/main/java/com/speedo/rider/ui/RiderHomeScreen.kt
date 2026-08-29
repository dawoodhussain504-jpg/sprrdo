package com.speedo.rider.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.core.maps.MapMarkerData
import com.speedo.core.maps.MarkerType
import com.speedo.core.maps.OsmMapView
import com.speedo.core.maps.RouteHelper
import com.speedo.core.model.VehicleCategory
import com.speedo.core.theme.*
import com.speedo.rider.ui.components.*
import com.speedo.rider.viewmodel.RiderViewModel
import org.osmdroid.util.GeoPoint

@Composable
fun RiderHomeScreen(
    viewModel: RiderViewModel,
    onNavigateToActiveRide: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // 1. Instant auto-navigation when active ride is established
    LaunchedEffect(uiState.activeRide) {
        if (uiState.activeRide != null) {
            onNavigateToActiveRide()
        }
    }

    // Auto-fetch device live location on launch
    LaunchedEffect(Unit) {
        viewModel.fetchCurrentLocation()
    }

    var showSearchDialog by remember { mutableStateOf(false) }
    var showSafetySheet by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("cash") } // "cash", "upi", "wallet"
    var isCouponApplied by remember { mutableStateOf(true) }
    var recenterTrigger by remember { mutableStateOf(1L) }
    var isDrawerCollapsed by remember { mutableStateOf(false) }

    // Quick destination shortcuts
    val quickShortcuts = listOf(
        Triple("Home", "Koramangala 5th Block", Pair(12.9352, 77.6245)),
        Triple("Work", "Whitefield ITPL Main Gate", Pair(12.9850, 77.7289)),
        Triple("Metro", "MG Road Metro Station", Pair(12.9756, 77.6066)),
        Triple("Mall", "Forum Mall, Koramangala", Pair(12.9345, 77.6112))
    )

    // Build Map Markers
    val mapMarkers = remember(uiState.pickupLat, uiState.pickupLng, uiState.dropLat, uiState.dropLng, uiState.dropAddress, uiState.nearbyCaptains) {
        val list = mutableListOf<MapMarkerData>()

        // Pickup / Current User Location Marker
        list.add(
            MapMarkerData(
                id = "pickup_pin",
                lat = uiState.pickupLat,
                lng = uiState.pickupLng,
                title = "Pickup: ${uiState.pickupAddress}",
                markerType = if (uiState.dropAddress.isEmpty()) MarkerType.USER_LOCATION else MarkerType.PICKUP
            )
        )

        // Drop Marker (Only added when drop location is set)
        if (uiState.dropAddress.isNotEmpty() && uiState.dropLat != 0.0) {
            list.add(
                MapMarkerData(
                    id = "drop_pin",
                    lat = uiState.dropLat,
                    lng = uiState.dropLng,
                    title = "Drop: ${uiState.dropAddress}",
                    markerType = MarkerType.DROP
                )
            )
        }

        // Nearby Captains with accurate vehicle headings
        uiState.nearbyCaptains.forEach { capt ->
            val lat = capt.lat
            val lng = capt.lng
            if (lat != null && lng != null) {
                list.add(
                    MapMarkerData(
                        id = capt.id,
                        lat = lat,
                        lng = lng,
                        title = "${capt.name} (${capt.vehicleType.uppercase()})",
                        snippet = "★ ${capt.rating} • ${capt.vehicleNumber}",
                        markerType = MarkerType.CAPTAIN,
                        bearing = capt.bearing?.toFloat() ?: 0f,
                        vehicleType = capt.vehicleType
                    )
                )
            }
        }
        list
    }

    // Polyline Route Points (Only when drop location is selected)
    val polylinePoints = remember(uiState.roadPolyline, uiState.pickupLat, uiState.pickupLng, uiState.dropLat, uiState.dropLng, uiState.dropAddress) {
        if (uiState.dropAddress.isNotEmpty() && uiState.dropLat != 0.0) {
            if (uiState.roadPolyline.isNotEmpty()) {
                uiState.roadPolyline
            } else {
                RouteHelper.generateSplineGeoPoints(
                    GeoPoint(uiState.pickupLat, uiState.pickupLng),
                    GeoPoint(uiState.dropLat, uiState.dropLng)
                )
            }
        } else {
            emptyList()
        }
    }

    // Fare calculation with coupon
    val estimates = uiState.fareEstimates?.estimates
    val selectedCategory = VehicleCategory.values().firstOrNull { it.key == uiState.selectedVehicleType } ?: VehicleCategory.BIKE
    val rawFare = estimates?.get(uiState.selectedVehicleType)?.totalFare?.toInt() ?: 45
    val discount = if (isCouponApplied) 15 else 0
    val finalFare = (rawFare - discount).coerceAtLeast(25)

    val defaultLat = if (uiState.pickupLat != 0.0) uiState.pickupLat else 12.9716
    val defaultLng = if (uiState.pickupLng != 0.0) uiState.pickupLng else 77.5946

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-Screen Rapido-Styled Voyager Interactive Map
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            centerLat = if (uiState.dropLat != 0.0) (uiState.pickupLat + uiState.dropLat) / 2 else defaultLat,
            centerLng = if (uiState.dropLng != 0.0) (uiState.pickupLng + uiState.dropLng) / 2 else defaultLng,
            zoomLevel = 16.5,
            recenterTrigger = recenterTrigger,
            markers = mapMarkers,
            polylinePoints = polylinePoints,
            autoFitBounds = uiState.dropAddress.isNotEmpty() && uiState.dropLat != 0.0 && !isDrawerCollapsed,
            onMapTouchStateChanged = { isDragging ->
                if (isDragging && uiState.dropAddress.isNotEmpty()) {
                    isDrawerCollapsed = true
                }
            },
            onMapClick = { geoPoint ->
                viewModel.setPinDropLocation(geoPoint.latitude, geoPoint.longitude)
            }
        )

        // 2. Floating Rapido Top HUD (Safety Shield, Location & Menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile / Brand Pill
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SpeedoWhite,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, SpeedoCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.speedo.core.components.SpeedoAppIconBadge(sizeDp = 28)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Speedo",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RapidoBlack
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Safety Shield Button
                Surface(
                    shape = CircleShape,
                    color = SpeedoWhite,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, SpeedoCardBorder),
                    modifier = Modifier.clickable { showSafetySheet = true }
                ) {
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Safety Shield",
                            tint = SpeedoError,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 3. Floating Search Bar & Quick Location Shortcuts (Top)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 80.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(18.dp),
            color = SpeedoWhite,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, SpeedoCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Search Trigger Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSearchDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = SpeedoSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(RapidoRed)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (uiState.dropAddress.isNotEmpty()) uiState.dropAddress else "Where are you going?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.dropAddress.isNotEmpty()) RapidoBlack else SpeedoTextSecondary
                            ),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.dropAddress.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = SpeedoTextSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { viewModel.clearDropLocation() }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = RapidoYellowDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Shortcut Chips (Home, Work, Metro, Mall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickShortcuts) { (label, address, coords) ->
                        val isSelected = uiState.dropAddress == address
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) RapidoYellowLight else SpeedoSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) RapidoYellowDark else Color.Transparent
                            ),
                            modifier = Modifier.clickable {
                                viewModel.updateDropLocation(address, coords.first, coords.second)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (label) {
                                        "Home" -> Icons.Default.Home
                                        "Work" -> Icons.Default.Work
                                        "Metro" -> Icons.Default.DirectionsSubway
                                        else -> Icons.Default.ShoppingBag
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) RapidoBlack else SpeedoTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) RapidoBlack else SpeedoTextPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Floating Rapido My Location / Recenter Target Button (Compact & Positioned Below Search Box to avoid any overlap)
        Surface(
            shape = CircleShape,
            color = SpeedoWhite,
            shadowElevation = 5.dp,
            border = BorderStroke(1.dp, SpeedoCardBorder),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 220.dp, end = 16.dp)
                .clickable {
                    viewModel.fetchCurrentLocation()
                    recenterTrigger = System.currentTimeMillis()
                }
        ) {
            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter Location",
                    tint = SpeedoOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 5. Bottom Sheet Vehicle Selection & Booking Tray (Only when drop location is set)
        if (uiState.dropAddress.isNotBlank()) {
            if (isDrawerCollapsed) {
                // Collapsed State: Sleek pill at bottom allowing unobstructed map view & quick expand
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .clickable { isDrawerCollapsed = false },
                    shape = RoundedCornerShape(20.dp),
                    color = SpeedoWhite,
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.5.dp, SpeedoOrange)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SpeedoOrange)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Drop Pin Adjusted",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RapidoBlack
                                    )
                                )
                                Text(
                                    text = "Tap to choose Speedo rides & fares",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoTextSecondary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SpeedoOrange
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SELECT RIDE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SpeedoWhite
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = SpeedoWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Expanded State: Full Vehicle Selection Drawer
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = SpeedoWhite,
                    shadowElevation = 24.dp,
                    border = BorderStroke(1.dp, SpeedoCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        // Top Drag Handle & Minimize Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDrawerCollapsed = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(SpeedoSurfaceVariant)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Header, Distance Pill & Minimize Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Choose Your Ride",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RapidoBlack
                                    )
                                )
                                Text(
                                    text = "Fastest pick-ups in your area",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoTextSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.fareEstimates != null) {
                                    Surface(
                                        color = RapidoYellowLight,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, RapidoYellowDark)
                                    ) {
                                        Text(
                                            text = "${uiState.fareEstimates!!.distanceKm} km",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = RapidoBlack
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = SpeedoSurfaceVariant,
                                    modifier = Modifier.clickable { isDrawerCollapsed = true }
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Collapse to Map",
                                        tint = SpeedoTextSecondary,
                                        modifier = Modifier.padding(4.dp).size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Vehicle Category Options (Compact Rapido style)
                        VehicleCategory.values().forEach { category ->
                            val isSelected = uiState.selectedVehicleType == category.key
                            val est = estimates?.get(category.key)
                            val catFare = est?.totalFare?.toInt() ?: when (category) {
                                VehicleCategory.BIKE -> 35
                                VehicleCategory.AUTO -> 58
                                VehicleCategory.CAB -> 115
                            }
                            val catEta = est?.estimatedTimeMin ?: when (category) {
                                VehicleCategory.BIKE -> 2
                                VehicleCategory.AUTO -> 3
                                VehicleCategory.CAB -> 5
                            }

                            RapidoVehicleOptionCard(
                                category = category,
                                isSelected = isSelected,
                                fare = (catFare - discount).coerceAtLeast(20),
                                discountFare = if (isCouponApplied) catFare else null,
                                etaMinutes = catEta,
                                badgeText = when (category) {
                                    VehicleCategory.BIKE -> "FASTEST"
                                    VehicleCategory.AUTO -> "POPULAR"
                                    VehicleCategory.CAB -> "COMFY"
                                },
                                onSelect = { viewModel.selectVehicleType(category.key) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Payment Method & Coupon Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Payment Selector Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SpeedoSurfaceVariant,
                                modifier = Modifier.clickable {
                                    paymentMethod = when (paymentMethod) {
                                        "cash" -> "upi"
                                        "upi" -> "wallet"
                                        else -> "cash"
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (paymentMethod) {
                                            "upi" -> Icons.Default.QrCode
                                            "wallet" -> Icons.Default.AccountBalanceWallet
                                            else -> Icons.Default.Payments
                                        },
                                        contentDescription = null,
                                        tint = SpeedoSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (paymentMethod) {
                                            "upi" -> "UPI QR"
                                            "wallet" -> "Speedo Wallet"
                                            else -> "Cash on Drop"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            // Coupon Chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCouponApplied) Color(0xFFE8F5E9) else SpeedoSurfaceVariant,
                                border = BorderStroke(1.dp, if (isCouponApplied) SpeedoSuccess else Color.Transparent),
                                modifier = Modifier.clickable { isCouponApplied = !isCouponApplied }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = if (isCouponApplied) SpeedoSuccess else SpeedoTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isCouponApplied) "SPEEDO50 (-₹15)" else "Apply Coupon",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCouponApplied) SpeedoSuccess else SpeedoTextPrimary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Big Rapido Book Button (100% Reliable Clickable)
                        Button(
                            onClick = {
                                viewModel.bookRide {
                                    onNavigateToActiveRide()
                                }
                            },
                            enabled = !uiState.isBookingRide,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RapidoYellow,
                                contentColor = RapidoBlack,
                                disabledContainerColor = RapidoYellow.copy(alpha = 0.7f),
                                disabledContentColor = RapidoBlack.copy(alpha = 0.7f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (uiState.isBookingRide) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = RapidoBlack,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Book ${selectedCategory.displayName}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RapidoBlack
                                        )
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₹$finalFare",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = RapidoBlack
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = RapidoBlack
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Radar Pulse Searching Overlay when booking is in progress
        if (uiState.isBookingRide) {
            RadarPulseSearchingOverlay(
                vehicleType = uiState.selectedVehicleType,
                nearbyCount = uiState.nearbyCaptains.size,
                pickupAddress = uiState.pickupAddress,
                dropAddress = uiState.dropAddress,
                fare = finalFare,
                onCancelBooking = {
                    // Cancel search
                }
            )
        }

        // 6. Search Dialog Modal
        if (showSearchDialog) {
            RapidoLocationSearchDialog(
                currentPickup = uiState.pickupAddress,
                currentDrop = uiState.dropAddress,
                onPickupSelected = { addr, lat, lng ->
                    viewModel.updatePickupLocation(addr, lat, lng)
                },
                onDropSelected = { addr, lat, lng ->
                    viewModel.updateDropLocation(addr, lat, lng)
                },
                onDismiss = { showSearchDialog = false }
            )
        }

        // 7. Safety Shield Sheet
        if (showSafetySheet) {
            RapidoSafetySheet(onDismiss = { showSafetySheet = false })
        }
    }
}
