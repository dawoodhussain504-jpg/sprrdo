package com.speedo.rider.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.core.components.*
import com.speedo.core.maps.MapMarkerData
import com.speedo.core.maps.MarkerType
import com.speedo.core.maps.OsmMapView
import com.speedo.core.maps.RouteHelper
import com.speedo.core.socket.SpeedoSocketManager
import com.speedo.core.theme.*
import com.speedo.rider.ui.components.*
import com.speedo.rider.viewmodel.RiderViewModel
import org.osmdroid.util.GeoPoint

@Composable
fun ActiveRideScreen(
    viewModel: RiderViewModel,
    onRideCompleted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val ride = uiState.activeRide
    val context = LocalContext.current

    var showCancelDialog by remember { mutableStateOf(false) }
    var showSafetySheet by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableStateOf(5) }
    var selectedTip by remember { mutableStateOf(0) }
    var sentMessageText by remember { mutableStateOf<String?>(null) }
    var recenterTrigger by remember { mutableStateOf(1L) }

    LaunchedEffect(ride?.id) {
        if (ride != null) {
            SpeedoSocketManager.getInstance(context).joinRideRoom(ride.id)
            viewModel.loadChatMessages(ride.id)
        }
    }

    LaunchedEffect(showChatSheet, ride?.id) {
        if (showChatSheet && ride != null) {
            viewModel.loadChatMessages(ride.id)
            while (showChatSheet) {
                kotlinx.coroutines.delay(3000)
                viewModel.loadChatMessages(ride.id)
            }
        }
    }

    val quickChatMessages = listOf(
        "I'm at the main gate 📍",
        "Please bring a helmet 🪖",
        "Coming down in 1 min ⏳",
        "Waiting near pickup point 🚶"
    )

    if (ride == null) {
        SpeedoEmptyView(
            icon = Icons.Default.DirectionsCar,
            title = "No Active Ride",
            message = "You don't have any ongoing or requested rides at the moment.",
            actionButton = {
                SpeedoPrimaryButton(text = "Book a Ride", onClick = onRideCompleted)
            }
        )
        return
    }

    // Build Map Markers
    val mapMarkers = remember(ride.status, ride.liveCaptainLat, ride.liveCaptainLng, ride.pickupLat, ride.dropLat) {
        val list = mutableListOf<MapMarkerData>()

        list.add(
            MapMarkerData(
                id = "pickup",
                lat = ride.pickupLat,
                lng = ride.pickupLng,
                title = "Pickup: ${ride.pickupAddress}",
                markerType = MarkerType.PICKUP
            )
        )

        list.add(
            MapMarkerData(
                id = "drop",
                lat = ride.dropLat,
                lng = ride.dropLng,
                title = "Drop: ${ride.dropAddress}",
                markerType = MarkerType.DROP
            )
        )

        val captLat = ride.liveCaptainLat ?: ride.captainLat
        val captLng = ride.liveCaptainLng ?: ride.captainLng
        if (captLat != null && captLng != null) {
            list.add(
                MapMarkerData(
                    id = "captain_live",
                    lat = captLat,
                    lng = captLng,
                    title = ride.captainName ?: "Captain",
                    snippet = "${ride.vehicleNumber} • Live Location",
                    markerType = MarkerType.CAPTAIN,
                    bearing = ride.liveCaptainBearing?.toFloat() ?: 0f,
                    vehicleType = ride.vehicleType
                )
            )
        }
        list
    }

    val polylinePoints = remember(uiState.roadPolyline, ride.pickupLat, ride.pickupLng, ride.dropLat, ride.dropLng) {
        if (uiState.roadPolyline.isNotEmpty()) {
            uiState.roadPolyline
        } else {
            RouteHelper.generateSplineGeoPoints(
                GeoPoint(ride.pickupLat, ride.pickupLng),
                GeoPoint(ride.dropLat, ride.dropLng)
            )
        }
    }

    // If ride is completed, show celebration & feedback summary
    if (ride.status == "completed") {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SpeedoWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SpeedoSuccess,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Ride Completed!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = RapidoBlack
                    )
                )

                Text(
                    text = "Hope you had a smooth trip with Speedo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpeedoTextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Total Fare Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SpeedoSurfaceVariant,
                    border = BorderStroke(1.dp, SpeedoCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Amount Paid", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                            Text(text = "Cash / UPI to Driver", style = MaterialTheme.typography.labelSmall, color = SpeedoSuccess)
                        }
                        Text(
                            text = "₹${(ride.fare + selectedTip).toInt()}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = RapidoBlack
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Rate Captain ${ride.captainName ?: ""}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Star Rating Row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { selectedRating = star },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star Stars",
                                tint = if (star <= selectedRating) RapidoYellowDark else SpeedoTextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tip Options
                Text(text = "Add Tip for Captain", style = MaterialTheme.typography.labelMedium.copy(color = SpeedoTextSecondary))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(0, 10, 20, 50).forEach { tipAmount ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTip == tipAmount) RapidoYellowLight else SpeedoSurfaceVariant,
                            border = BorderStroke(1.dp, if (selectedTip == tipAmount) RapidoYellowDark else Color.Transparent),
                            modifier = Modifier.clickable { selectedTip = tipAmount }
                        ) {
                            Text(
                                text = if (tipAmount == 0) "No Tip" else "+₹$tipAmount",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTip == tipAmount) RapidoBlack else SpeedoTextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onRideCompleted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RapidoYellow, contentColor = RapidoBlack)
                ) {
                    Text(text = "Done & Back to Home", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
        return
    }

    // Active Ride Live Tracking Layout
    Box(modifier = Modifier.fillMaxSize()) {
        val centerLat = ride.liveCaptainLat ?: ride.pickupLat
        val centerLng = ride.liveCaptainLng ?: ride.pickupLng

        // 1. Full-Screen Live Tracking Map with Road-Snapped Curves
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            centerLat = centerLat,
            centerLng = centerLng,
            zoomLevel = 16.0,
            recenterTrigger = recenterTrigger,
            markers = mapMarkers,
            polylinePoints = polylinePoints,
            driverPolylinePoints = uiState.driverPolyline,
            autoFitBounds = true
        )

        // 2. Floating Top Arrival Status Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(18.dp),
            color = SpeedoWhite,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, SpeedoCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    when (ride.status) {
                                        "arrived" -> SpeedoSuccess
                                        "ongoing" -> Color(0xFF2979FF)
                                        else -> RapidoYellowDark
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when (ride.status) {
                                    "requested" -> "Looking for Captain..."
                                    "accepted" -> "Captain is on the way"
                                    "arrived" -> "Captain arrived at pickup"
                                    "ongoing" -> "Trip in progress to Drop"
                                    else -> ride.status.uppercase()
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RapidoBlack
                                )
                            )
                            if (ride.status == "accepted") {
                                Text(
                                    text = "Arriving in ~${ride.etaMinutes ?: 3} mins (${ride.captainDistanceKm ?: 0.8} km away)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoTextSecondary
                                )
                            } else if (ride.status == "ongoing") {
                                Text(
                                    text = "Heading to: ${ride.dropAddress.substringBefore(",")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoTextSecondary
                                )
                            }
                        }
                    }

                    // Safety Shield Button
                    IconButton(onClick = { showSafetySheet = true }) {
                        Icon(Icons.Default.Shield, contentDescription = "Safety", tint = SpeedoError)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = when (ride.status) {
                        "arrived" -> SpeedoSuccess
                        "ongoing" -> Color(0xFF2979FF)
                        else -> RapidoYellowDark
                    },
                    trackColor = SpeedoSurfaceVariant
                )
            }
        }

        // 3. Floating Bottom Tray (OTP, Driver Details, Quick Chat, Call)
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
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Large High-Visibility OTP Banner (Rapido Signature Feature)
                if (ride.status in listOf("requested", "accepted", "arrived")) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = RapidoYellowLight,
                        border = BorderStroke(1.5.dp, RapidoYellowDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "START RIDE PIN (OTP)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB78103)
                                    )
                                )
                                Text(
                                    text = "Share with driver before boarding",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoTextSecondary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SpeedoWhite,
                                border = BorderStroke(1.dp, RapidoYellowDark),
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = ride.otp,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RapidoBlack,
                                        letterSpacing = 6.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Captain Profile & Contact Actions
                if (ride.captainName != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Driver Photo Avatar
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(RapidoYellowLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = RapidoYellowDark,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Driver Name, Rating & Vehicle Plate
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ride.captainName ?: "Captain",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RapidoBlack
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFFFF9C4),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${ride.captainRating ?: 4.9}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ride.vehicleNumber ?: "KA-01-EQ-9876",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RapidoBlack
                                    )
                                )
                            }
                            Text(
                                text = "${ride.vehicleType.uppercase()} • Honda Activa 6G",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpeedoTextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Chat Driver Button
                            IconButton(
                                onClick = {
                                    viewModel.loadChatMessages(ride.id)
                                    showChatSheet = true
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE3F2FD))
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "Chat with Driver", tint = Color(0xFF1976D2))
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Call Driver Button
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9))
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call Driver", tint = SpeedoSuccess)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Chat Preset Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickChatMessages) { msg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (sentMessageText == msg) Color(0xFFE8F5E9) else SpeedoSurfaceVariant,
                                border = BorderStroke(1.dp, if (sentMessageText == msg) SpeedoSuccess else Color.Transparent),
                                modifier = Modifier.clickable {
                                    sentMessageText = msg
                                    viewModel.sendChatMessage(msg, "quick_chip")
                                    showChatSheet = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (sentMessageText == msg) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = SpeedoSuccess, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (sentMessageText == msg) SpeedoSuccess else SpeedoTextPrimary,
                                            fontWeight = if (sentMessageText == msg) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Fare & Cancel Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Total Fare (Cash / UPI QR)", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                        Text(
                            text = "₹${ride.fare.toInt()}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = RapidoBlack
                            )
                        )
                    }

                    if (ride.status in listOf("requested", "accepted")) {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SpeedoError),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SpeedoError)
                        ) {
                            Text(text = "Cancel Ride", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Floating Rapido Recenter / Focus Route FAB (Top-Right HUD)
        Surface(
            shape = CircleShape,
            color = SpeedoWhite,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, SpeedoCardBorder),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 80.dp, end = 16.dp)
                .clickable {
                    recenterTrigger = System.currentTimeMillis()
                }
        ) {
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter Location",
                    tint = SpeedoOrange,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 4. Cancel Ride Confirmation Dialog
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Cancel Ride?") },
                text = { Text("Your captain is already on the way. Are you sure you want to cancel?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showCancelDialog = false
                            viewModel.cancelRide(ride.id) { onRideCompleted() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoError)
                    ) {
                        Text("Yes, Cancel", color = SpeedoWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Keep Ride", color = SpeedoTextPrimary)
                    }
                }
            )
        }

        // 5. Safety Sheet Modal
        if (showSafetySheet) {
            RapidoSafetySheet(
                onTriggerSos = {
                    viewModel.triggerSosEmergency(
                        rideId = ride.id,
                        lat = ride.pickupLat,
                        lng = ride.pickupLng,
                        address = ride.pickupAddress
                    )
                },
                onDismiss = { showSafetySheet = false }
            )
        }

        // 6. In-App Real-Time Chat Sheet Modal
        if (showChatSheet) {
            val chatMessages by viewModel.chatMessages.collectAsState()
            SpeedoChatSheet(
                rideId = ride.id,
                currentUserId = uiState.currentUserId ?: "",
                currentUserRole = "rider",
                peerName = ride.captainName ?: "Captain",
                peerSubtitle = "${ride.vehicleNumber ?: "KA-01-EQ-9876"} • ${ride.vehicleType.uppercase()}",
                peerPhone = "+919876543210",
                messages = chatMessages,
                onSendMessage = { text, type ->
                    viewModel.sendChatMessage(text, type)
                },
                onDismiss = { showChatSheet = false }
            )
        }
    }
}
