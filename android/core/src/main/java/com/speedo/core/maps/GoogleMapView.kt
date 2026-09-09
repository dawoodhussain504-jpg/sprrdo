package com.speedo.core.maps

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import org.osmdroid.util.GeoPoint

fun GeoPoint.toLatLng(): LatLng = LatLng(this.latitude, this.longitude)

@Composable
fun GoogleMapView(
    modifier: Modifier = Modifier,
    centerLat: Double = 12.9716, // Bangalore default
    centerLng: Double = 77.5946,
    zoomLevel: Double = 16.0,
    recenterTrigger: Long = 0L,
    markers: List<MapMarkerData> = emptyList(),
    polylinePoints: List<GeoPoint> = emptyList(),
    driverPolylinePoints: List<GeoPoint> = emptyList(),
    autoFitBounds: Boolean = true,
    onMapClick: ((GeoPoint) -> Unit)? = null,
    onMapMoveEnd: ((GeoPoint) -> Unit)? = null,
    onMapTouchStateChanged: ((Boolean) -> Unit)? = null,
    onMarkerClick: ((MapMarkerData) -> Unit)? = null
) {
    val context = LocalContext.current

    val initialPosition = remember { LatLng(centerLat, centerLng) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, zoomLevel.toFloat())
    }

    // Cache BitmapDescriptors
    val userLocationIcon = remember(context) {
        BitmapDescriptorFactory.fromBitmap(MapMarkerUtils.getUserLocationBitmap(context))
    }
    val pickupIcon = remember(context) {
        BitmapDescriptorFactory.fromBitmap(
            MapMarkerUtils.getPinBitmap(context, AndroidColor.parseColor("#00C853"), "P")
        )
    }
    val dropIcon = remember(context) {
        BitmapDescriptorFactory.fromBitmap(
            MapMarkerUtils.getPinBitmap(context, AndroidColor.parseColor("#D50000"), "D")
        )
    }
    val vehicleIcons = remember(context) {
        mutableMapOf<String, BitmapDescriptor>()
    }

    // Map properties & styling
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false
        )
    }
    val properties = remember {
        MapProperties(
            isTrafficEnabled = true,
            mapType = MapType.NORMAL
        )
    }

    // Camera animation for bounds / recenter
    var lastBoundsKey by remember { mutableStateOf("") }
    var lastRecenterTrigger by remember { mutableStateOf(0L) }

    val convertedPolyline = remember(polylinePoints) {
        polylinePoints.map { it.toLatLng() }
    }
    val convertedDriverPolyline = remember(driverPolylinePoints) {
        driverPolylinePoints.map { it.toLatLng() }
    }

    // Handle camera movement & recentering
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0L && recenterTrigger != lastRecenterTrigger) {
            lastRecenterTrigger = recenterTrigger
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(centerLat, centerLng),
                    zoomLevel.toFloat()
                ),
                600
            )
        }
    }

    // Handle auto-fit bounds when routes or multiple markers are active
    LaunchedEffect(autoFitBounds, convertedPolyline, markers) {
        if (autoFitBounds) {
            val builder = LatLngBounds.builder()
            var count = 0

            if (convertedPolyline.size >= 2) {
                convertedPolyline.forEach {
                    builder.include(it)
                    count++
                }
            } else {
                markers.forEach {
                    if (it.lat != 0.0 && it.lng != 0.0) {
                        builder.include(LatLng(it.lat, it.lng))
                        count++
                    }
                }
            }

            if (count >= 2) {
                try {
                    val bounds = builder.build()
                    val key = "${bounds.northeast.latitude}_${bounds.southwest.latitude}_$count"
                    if (key != lastBoundsKey) {
                        lastBoundsKey = key
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(bounds, 130),
                            800
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Detect camera movement end
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving && onMapMoveEnd != null) {
            val target = cameraPositionState.position.target
            onMapMoveEnd(GeoPoint(target.latitude, target.longitude))
        }
        onMapTouchStateChanged?.invoke(cameraPositionState.isMoving)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = uiSettings,
        onMapClick = { latLng ->
            onMapClick?.invoke(GeoPoint(latLng.latitude, latLng.longitude))
        }
    ) {
        // 1. Primary Trip Polyline (Blue vibrant path)
        if (convertedPolyline.size >= 2) {
            Polyline(
                points = convertedPolyline,
                color = Color(0xFF2979FF),
                width = 16f,
                jointType = JointType.ROUND,
                startCap = RoundCap(),
                endCap = RoundCap(),
                zIndex = 2f
            )
            // Polyline outline for depth
            Polyline(
                points = convertedPolyline,
                color = Color(0xFF1565C0),
                width = 20f,
                jointType = JointType.ROUND,
                startCap = RoundCap(),
                endCap = RoundCap(),
                zIndex = 1f
            )
        }

        // 2. Captain Driver Polyline (Green dashed path)
        if (convertedDriverPolyline.size >= 2) {
            Polyline(
                points = convertedDriverPolyline,
                color = Color(0xFF00E676),
                width = 12f,
                pattern = listOf(Dash(24f), Gap(12f)),
                jointType = JointType.ROUND,
                startCap = RoundCap(),
                endCap = RoundCap(),
                zIndex = 3f
            )
        }

        // 3. Markers
        markers.forEach { markerData ->
            if (markerData.lat != 0.0 && markerData.lng != 0.0) {
                val pos = LatLng(markerData.lat, markerData.lng)
                when (markerData.markerType) {
                    MarkerType.USER_LOCATION -> {
                        Marker(
                            state = rememberMarkerState(key = markerData.id, position = pos),
                            title = markerData.title,
                            snippet = markerData.snippet,
                            icon = userLocationIcon,
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                            zIndex = 10f,
                            onClick = {
                                onMarkerClick?.invoke(markerData)
                                false
                            }
                        )
                    }
                    MarkerType.PICKUP -> {
                        Marker(
                            state = rememberMarkerState(key = markerData.id, position = pos),
                            title = markerData.title,
                            snippet = markerData.snippet,
                            icon = pickupIcon,
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.9f),
                            zIndex = 8f,
                            onClick = {
                                onMarkerClick?.invoke(markerData)
                                false
                            }
                        )
                    }
                    MarkerType.DROP -> {
                        Marker(
                            state = rememberMarkerState(key = markerData.id, position = pos),
                            title = markerData.title,
                            snippet = markerData.snippet,
                            icon = dropIcon,
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.9f),
                            zIndex = 8f,
                            onClick = {
                                onMarkerClick?.invoke(markerData)
                                false
                            }
                        )
                    }
                    MarkerType.CAPTAIN -> {
                        val icon = vehicleIcons.getOrPut(markerData.vehicleType) {
                            BitmapDescriptorFactory.fromBitmap(
                                MapMarkerUtils.getVehicleBitmap(context, markerData.vehicleType)
                            )
                        }
                        Marker(
                            state = rememberMarkerState(key = markerData.id, position = pos),
                            title = markerData.title,
                            snippet = markerData.snippet,
                            icon = icon,
                            rotation = markerData.bearing,
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                            flat = true,
                            zIndex = 9f,
                            onClick = {
                                onMarkerClick?.invoke(markerData)
                                false
                            }
                        )
                    }
                }
            }
        }
    }
}
