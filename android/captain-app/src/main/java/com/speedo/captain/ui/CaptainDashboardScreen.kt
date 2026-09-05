package com.speedo.captain.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.captain.audio.CaptainVoiceCueManager
import com.speedo.captain.ui.components.*
import com.speedo.captain.viewmodel.CaptainViewModel
import com.speedo.core.maps.MapMarkerData
import com.speedo.core.maps.MarkerType
import com.speedo.core.maps.OsmMapView
import com.speedo.core.theme.*
import com.speedo.core.utils.InAppUpdateManager
import com.speedo.core.model.PopularDestinationsData
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.speedo.captain.ui.components.CaptainHotspotThumbnailCard

@Composable
fun CaptainDashboardScreen(
    viewModel: CaptainViewModel,
    onNavigateToActiveRide: () -> Unit,
    onNavigateToKyc: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val captain = uiState.captain
    val isKycApproved = uiState.kycStatus?.isApproved == true || captain?.kycStatus == "approved"
    val context = androidx.compose.ui.platform.LocalContext.current

    var captainLat by remember { mutableStateOf(captain?.lat ?: 12.9716) }
    var captainLng by remember { mutableStateOf(captain?.lng ?: 77.5946) }
    var captainBearing by remember { mutableStateOf(captain?.bearing?.toFloat() ?: 0f) }
    var recenterTrigger by remember { mutableStateOf(1L) }

    val liveDestinations by com.speedo.core.repository.PopularDestinationRepository.getInstance(context).destinationsFlow.collectAsState()
    val rankedDestinations = remember(liveDestinations, captainLat, captainLng) {
        PopularDestinationsData.filterAndRankForLocation(liveDestinations, captainLat, captainLng)
    }
    val popularDestinations = rankedDestinations.destinations

    LaunchedEffect(Unit) {
        viewModel.fetchProfile()
        viewModel.fetchKycStatus()
        viewModel.checkActiveRide()

        // Fetch live high-accuracy GPS coordinates
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

    LaunchedEffect(uiState.activeRide) {
        val ride = uiState.activeRide
        if (ride != null && ride.status in listOf("accepted", "arrived", "ongoing")) {
            onNavigateToActiveRide()
        }
    }

    val voiceCueManager = remember { CaptainVoiceCueManager.getInstance(context) }
    var lastAnnouncedRideId by remember { mutableStateOf<String?>(null) }

    // Vernacular voice announcement for new incoming ride requests
    LaunchedEffect(uiState.incomingRequests) {
        if (uiState.incomingRequests.isNotEmpty()) {
            val first = uiState.incomingRequests.first()
            if (first.id != lastAnnouncedRideId) {
                lastAnnouncedRideId = first.id
                val dist = if (first.distanceKm > 0) first.distanceKm else 2.5
                voiceCueManager.speakIncomingRide(
                    fare = first.fare.toInt(),
                    distanceKm = dist,
                    pickup = first.pickupAddress,
                    vehicleType = first.vehicleType
                )
            }
        } else {
            lastAnnouncedRideId = null
        }
    }

    val mapMarkers = remember(captain?.id, uiState.isOnline, captainLat, captainLng, captainBearing) {
        listOf(
            MapMarkerData(
                id = captain?.id ?: "captain_pin",
                lat = captainLat,
                lng = captainLng,
                title = "${captain?.name ?: "Captain"} (You)",
                snippet = "${captain?.vehicleNumber ?: "KA-01-EQ-9876"} • ${if (uiState.isOnline) "ONLINE" else "OFFLINE"}",
                markerType = MarkerType.CAPTAIN,
                bearing = captainBearing,
                vehicleType = captain?.vehicleType ?: "bike"
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-Screen Interactive Driver GPS Map (Rapido Voyager)
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            centerLat = captainLat,
            centerLng = captainLng,
            zoomLevel = 16.5,
            recenterTrigger = recenterTrigger,
            markers = mapMarkers
        )

        // 2. Floating Top Driver HUD (Earnings & Online Switch)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            // Clickable App Update Notification Banner (if update available)
            val appUpdate = uiState.appUpdateState
            if (appUpdate.isUpdateAvailable) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable {
                            InAppUpdateManager.startDownloadAndInstall(
                                context,
                                appUpdate.updateUrl
                            )
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = SpeedoOrange.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, SpeedoOrange),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SpeedoOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = SpeedoWhite, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ Update Available (v${appUpdate.latestVersionName})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoOrange)
                            )
                            Text(
                                text = "Tap here to download & install latest Speedo Captain APK",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpeedoTextSecondary
                            )
                        }
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = SpeedoOrange)
                    }
                }
            }

            // KYC Alert Banner (if pending)
            if (!isKycApproved) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToKyc() },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, Color(0xFFFF9800)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "KYC Verification Pending",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            )
                            Text(
                                text = "Upload documents to activate online mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpeedoTextSecondary
                            )
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFFFF9800))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Floating Driver Status Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SpeedoWhite,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, SpeedoCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Today's Earnings Summary Pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(RapidoCaptainGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = RapidoCaptainGreenDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Today's Earnings",
                                style = MaterialTheme.typography.labelSmall.copy(color = SpeedoTextSecondary)
                            )
                            Text(
                                text = "₹${captain?.totalEarnings?.toInt() ?: 8520}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RapidoCaptainBlack
                                )
                            )
                        }
                    }

                    // Online / Offline Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (uiState.isOnline) RapidoCaptainGreenLight else SpeedoSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (uiState.isOnline) RapidoCaptainGreen else Color.Transparent
                            )
                        ) {
                            Text(
                                text = if (uiState.isOnline) "ONLINE" else "OFFLINE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (uiState.isOnline) RapidoCaptainGreenDark else SpeedoTextSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = uiState.isOnline,
                            onCheckedChange = { viewModel.toggleOnline(it) },
                            enabled = isKycApproved,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SpeedoWhite,
                                checkedTrackColor = RapidoCaptainGreen,
                                uncheckedThumbColor = SpeedoTextSecondary,
                                uncheckedTrackColor = SpeedoSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // 3. Floating Bottom Control HUD
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
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Demand Heatmap Strip & High-Demand Passenger Hubs
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFDE7),
                    border = BorderStroke(1.dp, RapidoCaptainYellowDark)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Surge Active in High-Demand Zones • Extra ₹15 - ₹40 per trip",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB78103)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // High Demand Passenger Hubs Carousel with Place Images
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (rankedDestinations.matchedCity != null) "High Demand in ${rankedDestinations.matchedCity}" else "High Ride Demand Hotspots",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = RapidoCaptainBlack
                            )
                        )
                    }
                    Text(
                        text = "Tap to navigate",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SpeedoTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(items = popularDestinations, key = { it.id }) { dest ->
                        CaptainHotspotThumbnailCard(
                            destination = dest,
                            onClick = {
                                captainLat = dest.lat
                                captainLng = dest.lng
                                recenterTrigger = System.currentTimeMillis()
                                android.widget.Toast.makeText(context, "Navigating to High Demand Hub: ${dest.title}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Online / Offline Action Button
                if (!uiState.isOnline) {
                    Button(
                        onClick = { viewModel.toggleOnline(true) },
                        enabled = isKycApproved,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RapidoCaptainGreen,
                            contentColor = SpeedoWhite
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GO ONLINE TO GET RIDES",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.toggleOnline(false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, SpeedoError),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpeedoError)
                    ) {
                        Icon(Icons.Default.StopCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GO OFFLINE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }
        }

        // Floating Controls (Compact Right HUD): Voice Cues Toggle + Recenter FAB
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 280.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CaptainVoiceControlHud()

            Surface(
                shape = CircleShape,
                color = SpeedoWhite,
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, SpeedoCardBorder),
                modifier = Modifier.clickable {
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
            ) {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter GPS",
                        tint = RapidoCaptainGreenDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 4. Full-Screen Incoming Ride Flash Overlay (Rapido Signature)
        if (uiState.incomingRequests.isNotEmpty()) {
            val firstRequest = uiState.incomingRequests.first()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.BottomCenter
            ) {
                IncomingRideFlashBanner(
                    ride = firstRequest,
                    onAccept = {
                        viewModel.acceptRide(firstRequest.id) {
                            onNavigateToActiveRide()
                        }
                    },
                    onReject = {
                        viewModel.rejectRide(firstRequest.id)
                    }
                )
            }
        }
    }
}
