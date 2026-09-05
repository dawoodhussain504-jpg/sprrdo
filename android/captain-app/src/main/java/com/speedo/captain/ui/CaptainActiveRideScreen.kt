package com.speedo.captain.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.captain.ui.components.*
import com.speedo.captain.viewmodel.CaptainViewModel
import com.speedo.core.components.*
import com.speedo.core.maps.MapMarkerData
import com.speedo.core.maps.MarkerType
import com.speedo.core.maps.OsmMapView
import com.speedo.core.maps.RouteHelper
import com.speedo.core.socket.SpeedoSocketManager
import com.speedo.core.theme.*
import org.osmdroid.util.GeoPoint

@Composable
fun CaptainActiveRideScreen(
    viewModel: CaptainViewModel,
    onRideFinished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentRide = uiState.pendingPaymentRide ?: uiState.activeRide
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchProfile()
        viewModel.fetchKycStatus()
    }

    var showOtpKeypad by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showSosDialog by remember { mutableStateOf(false) }
    var recenterTrigger by remember { mutableStateOf(1L) }
    var captainLat by remember { mutableStateOf(currentRide?.pickupLat ?: 12.9716) }
    var captainLng by remember { mutableStateOf(currentRide?.pickupLng ?: 77.5946) }
    var captainBearing by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val locHelper = com.speedo.core.maps.LocationHelper(context)
        locHelper.getCurrentLiveLocation(
            onSuccess = { loc ->
                captainLat = loc.lat
                captainLng = loc.lng
                captainBearing = loc.bearing
                recenterTrigger = System.currentTimeMillis()
            }
        )
    }

    LaunchedEffect(currentRide?.id) {
        if (currentRide != null) {
            SpeedoSocketManager.getInstance(context).joinRideRoom(currentRide.id)
            viewModel.loadChatMessages(currentRide.id)
        }
    }

    LaunchedEffect(showChatSheet, currentRide?.id) {
        if (showChatSheet && currentRide != null) {
            viewModel.loadChatMessages(currentRide.id)
            while (showChatSheet) {
                kotlinx.coroutines.delay(3000)
                viewModel.loadChatMessages(currentRide.id)
            }
        }
    }

    if (currentRide == null) {
        SpeedoEmptyView(
            icon = Icons.Default.DirectionsCar,
            title = "No Active Ride",
            message = "You are currently not on any active trip.",
            actionButton = {
                SpeedoPrimaryButton(text = "Go to Dashboard", onClick = onRideFinished)
            }
        )
        return
    }

    val ride = currentRide

    val mapMarkers = remember(ride.pickupLat, ride.dropLat, ride.status, captainLat, captainLng, captainBearing, uiState.captain) {
        val list = mutableListOf<MapMarkerData>()

        // 1. Captain's Live Moving Top-View Vehicle
        list.add(
            MapMarkerData(
                id = "captain_live_vehicle",
                lat = captainLat,
                lng = captainLng,
                title = "You (${uiState.captain?.vehicleType?.uppercase() ?: "BIKE"})",
                snippet = uiState.captain?.vehicleNumber ?: "Speedo Captain",
                markerType = MarkerType.CAPTAIN,
                bearing = captainBearing,
                vehicleType = uiState.captain?.vehicleType ?: "bike"
            )
        )

        // 2. Rider Pickup Marker (Shown until trip is ongoing)
        if (ride.status != "ongoing") {
            list.add(
                MapMarkerData(
                    id = "pickup",
                    lat = ride.pickupLat,
                    lng = ride.pickupLng,
                    title = "Rider Pickup: ${ride.pickupAddress}",
                    markerType = MarkerType.PICKUP
                )
            )
        }

        // 3. Destination Marker
        list.add(
            MapMarkerData(
                id = "drop",
                lat = ride.dropLat,
                lng = ride.dropLng,
                title = "Destination: ${ride.dropAddress}",
                markerType = MarkerType.DROP
            )
        )

        list
    }

    // Driver-to-pickup amber polyline
    val driverPolyline = remember(captainLat, captainLng, ride.pickupLat, ride.pickupLng, ride.status) {
        if (ride.status in listOf("accepted", "arrived")) {
            RouteHelper.generateSplineGeoPoints(
                GeoPoint(captainLat, captainLng),
                GeoPoint(ride.pickupLat, ride.pickupLng)
            )
        } else {
            emptyList()
        }
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Navigation Vector Map with Road-Snapped Curves
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            centerLat = if (ride.status == "ongoing") ride.dropLat else if (ride.status in listOf("accepted", "arrived")) captainLat else ride.pickupLat,
            centerLng = if (ride.status == "ongoing") ride.dropLng else if (ride.status in listOf("accepted", "arrived")) captainLng else ride.pickupLng,
            zoomLevel = 16.0,
            recenterTrigger = recenterTrigger,
            markers = mapMarkers,
            polylinePoints = polylinePoints,
            driverPolylinePoints = driverPolyline,
            autoFitBounds = true
        )

        // 2. Floating Top Turn-by-Turn Navigation Banner
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TurnRight,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = uiState.currentManeuver ?: when (ride.status) {
                                "accepted" -> "Navigate to Pickup"
                                "arrived" -> "Arrived at Pickup • Enter OTP"
                                "ongoing" -> "Trip in Progress to Destination"
                                "completed" -> "Trip Completed 🎉"
                                else -> ride.status.uppercase()
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = RapidoCaptainBlack
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = if (ride.status == "ongoing") "Drop: ${ride.dropAddress}" else "Pickup: ${ride.pickupAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpeedoTextSecondary,
                            maxLines = 1
                        )
                    }
                }

                // Google Maps Navigation Intent Button
                IconButton(
                    onClick = {
                        val lat = if (ride.status == "ongoing") ride.dropLat else ride.pickupLat
                        val lng = if (ride.status == "ongoing") ride.dropLng else ride.pickupLng
                        val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        context.startActivity(mapIntent)
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(RapidoCaptainGreenLight)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = "Navigate", tint = RapidoCaptainGreenDark)
                }
            }
        }

        // 3. Floating Bottom Control Tray
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
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Rider Info & Call / Chat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF9C4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(28.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ride.riderName ?: "Passenger",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = RapidoCaptainBlack
                            )
                        )
                        Text(
                            text = "Guaranteed Earnings: ₹${ride.fare.toInt()}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = RapidoCaptainGreenDark
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Chat Rider Button
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
                            Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color(0xFF1976D2))
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Call Rider Button
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919811223344"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(RapidoCaptainGreenLight)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = RapidoCaptainGreenDark)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Emergency SOS Button
                        IconButton(
                            onClick = { showSosDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE))
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "SOS", tint = SpeedoError)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stage Actions:
                when (ride.status) {
                    "accepted" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.updateRideStatus(ride.id, "arrived") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RapidoCaptainGreen)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "ARRIVED", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }

                            Button(
                                onClick = { showOtpKeypad = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RapidoCaptainYellowDark,
                                    contentColor = RapidoCaptainBlack
                                )
                            ) {
                                Icon(Icons.Default.Pin, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "ENTER OTP", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                        }
                    }

                    "arrived" -> {
                        Button(
                            onClick = { showOtpKeypad = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RapidoCaptainYellowDark, contentColor = RapidoCaptainBlack)
                        ) {
                            Icon(Icons.Default.Pin, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "ENTER 4-DIGIT RIDER PIN", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }

                    "ongoing" -> {
                        Button(
                            onClick = {
                                viewModel.initiatePayment(ride)
                                showPaymentSheet = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "COMPLETE TRIP (₹${ride.fare.toInt()})", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }

                    "completed" -> {
                        Button(
                            onClick = {
                                viewModel.initiatePayment(ride)
                                showPaymentSheet = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RapidoCaptainGreen)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "COLLECT PAYMENT (₹${ride.fare.toInt()})", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Floating Rapido Recenter Route FAB
        Surface(
            shape = CircleShape,
            color = SpeedoWhite,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, SpeedoCardBorder),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 260.dp)
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

        // 4. OTP Keypad Bottom Sheet Modal
        if (showOtpKeypad) {
            CaptainOtpKeypadSheet(
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onVerifyOtp = { enteredOtp ->
                    viewModel.startRideWithOtp(ride.id, enteredOtp) { success ->
                        if (success) {
                            showOtpKeypad = false
                        }
                    }
                },
                onDismiss = { showOtpKeypad = false }
            )
        }

        // 5. Dynamic UPI QR Payment Sheet (Persists until Captain explicitly taps PAID)
        if (uiState.pendingPaymentRide != null || showPaymentSheet) {
            val paymentTargetRide = uiState.pendingPaymentRide ?: ride
            val captainQr = uiState.captain?.paymentQrUrl ?: uiState.kycStatus?.paymentQrUrl
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                DynamicUpiQrPaymentSheet(
                    fare = paymentTargetRide.fare.toInt(),
                    rideId = paymentTargetRide.id,
                    riderName = paymentTargetRide.riderName,
                    uploadedQrUrl = captainQr,
                    onPaymentCollected = {
                        viewModel.confirmPaymentAndFinishRide(paymentTargetRide.id) {
                            showPaymentSheet = false
                            onRideFinished()
                        }
                    }
                )
            }
        }

        // 6. In-App Real-Time Chat Modal
        if (showChatSheet) {
            val chatMessages by viewModel.chatMessages.collectAsState()
            SpeedoChatSheet(
                rideId = ride.id,
                currentUserId = uiState.captain?.id ?: "",
                currentUserRole = "captain",
                peerName = ride.riderName ?: "Passenger",
                peerSubtitle = "Drop: ${ride.dropAddress}",
                peerPhone = "+919811223344",
                messages = chatMessages,
                onSendMessage = { text, type ->
                    viewModel.sendChatMessage(text, type)
                },
                onDismiss = { showChatSheet = false }
            )
        }

        // 7. Emergency SOS Confirmation Dialog
        if (showSosDialog) {
            AlertDialog(
                onDismissRequest = { showSosDialog = false },
                title = { Text("🚨 Trigger Emergency SOS?") },
                text = {
                    Text("This will immediately transmit your real-time vehicle GPS coordinates and ride incident signal to the Speedo HQ Emergency Command Center and prompt you to dial 112 Police.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSosDialog = false
                            viewModel.triggerSosEmergency(
                                rideId = ride.id,
                                lat = ride.pickupLat,
                                lng = ride.pickupLng,
                                address = ride.pickupAddress
                            )
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoError)
                    ) {
                        Text("Broadcast SOS & Dial 112", color = SpeedoWhite, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSosDialog = false }) {
                        Text("Cancel", color = SpeedoTextPrimary)
                    }
                }
            )
        }
    }
}
