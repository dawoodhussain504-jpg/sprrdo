package com.speedo.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.maps.MapMarkerData
import com.speedo.core.maps.MarkerType
import com.speedo.core.maps.OsmMapView
import com.speedo.core.theme.*

@Composable
fun LiveFleetMapScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val liveData = uiState.liveMapData

    LaunchedEffect(Unit) {
        viewModel.startLiveMapPolling()
    }

    // Build map markers
    val mapMarkers = remember(liveData) {
        val list = mutableListOf<MapMarkerData>()

        // Online Captains
        liveData?.onlineCaptains?.forEach { capt ->
            val lat = capt.lat
            val lng = capt.lng
            if (lat != null && lng != null) {
                list.add(
                    MapMarkerData(
                        id = capt.id,
                        lat = lat,
                        lng = lng,
                        title = "${capt.name} (${capt.vehicleType.uppercase()})",
                        snippet = "${capt.vehicleNumber} • Rating: ★ ${capt.rating}",
                        markerType = MarkerType.CAPTAIN,
                        bearing = capt.bearing?.toFloat() ?: 0f,
                        vehicleType = capt.vehicleType
                    )
                )
            }
        }

        // Active Rides (Pickups and Drops)
        liveData?.activeRides?.forEach { ride ->
            list.add(
                MapMarkerData(
                    id = "ride_pickup_${ride.id}",
                    lat = ride.pickupLat,
                    lng = ride.pickupLng,
                    title = "Pickup: ${ride.pickupAddress}",
                    snippet = "Rider: ${ride.riderName ?: "User"} (₹${ride.fare.toInt()})",
                    markerType = MarkerType.PICKUP
                )
            )
            list.add(
                MapMarkerData(
                    id = "ride_drop_${ride.id}",
                    lat = ride.dropLat,
                    lng = ride.dropLng,
                    title = "Drop: ${ride.dropAddress}",
                    markerType = MarkerType.DROP
                )
            )
        }

        list
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "Live Fleet & Rides Map",
                onMenuClick = onMenuClick
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // OSM Map
            OsmMapView(
                modifier = Modifier.fillMaxSize(),
                centerLat = 12.9716,
                centerLng = 77.5946,
                zoomLevel = 13.5,
                markers = mapMarkers
            )

            // Real-Time Fleet Counters Overlay
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(14.dp),
                color = SpeedoWhite,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, SpeedoCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Online Fleet", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                        Text(
                            text = "${liveData?.onlineCaptains?.size ?: 0}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = SpeedoOrange)
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp),
                        color = SpeedoDivider
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Active Rides", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                        Text(
                            text = "${liveData?.activeRides?.size ?: 0}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = SpeedoSuccess)
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp),
                        color = SpeedoDivider
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Polling Rate", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                        Text(
                            text = "6 sec",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = SpeedoTextPrimary)
                        )
                    }
                }
            }
        }
    }
}
