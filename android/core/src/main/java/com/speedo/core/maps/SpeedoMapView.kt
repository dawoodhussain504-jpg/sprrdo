package com.speedo.core.maps

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.osmdroid.util.GeoPoint

/**
 * Universal Speedo Map Composable
 * Automatically renders Google Maps Native Vector Engine when Google Play Services and Maps SDK are verified functional,
 * and gracefully falls back to OpenStreetMap (OsmMapView) on devices without Play Services or if initialization fails.
 */
@Composable
fun SpeedoMapView(
    modifier: Modifier = Modifier,
    centerLat: Double = 12.9716,
    centerLng: Double = 77.5946,
    zoomLevel: Double = 16.0,
    recenterTrigger: Long = 0L,
    markers: List<MapMarkerData> = emptyList(),
    polylinePoints: List<GeoPoint> = emptyList(),
    driverPolylinePoints: List<GeoPoint> = emptyList(),
    autoFitBounds: Boolean = true,
    forceOsm: Boolean = false,
    onMapClick: ((GeoPoint) -> Unit)? = null,
    onMapMoveEnd: ((GeoPoint) -> Unit)? = null,
    onMapTouchStateChanged: ((Boolean) -> Unit)? = null,
    onMarkerClick: ((MapMarkerData) -> Unit)? = null
) {
    val context = LocalContext.current
    var isGoogleMapsReady by remember {
        mutableStateOf(SpeedoMapConfig.isGoogleMapsReady(context))
    }

    LaunchedEffect(context) {
        if (!forceOsm && !isGoogleMapsReady) {
            isGoogleMapsReady = SpeedoMapConfig.initGoogleMaps(context)
        }
    }

    if (!forceOsm && isGoogleMapsReady) {
        GoogleMapView(
            modifier = modifier,
            centerLat = centerLat,
            centerLng = centerLng,
            zoomLevel = zoomLevel,
            recenterTrigger = recenterTrigger,
            markers = markers,
            polylinePoints = polylinePoints,
            driverPolylinePoints = driverPolylinePoints,
            autoFitBounds = autoFitBounds,
            onMapClick = onMapClick,
            onMapMoveEnd = onMapMoveEnd,
            onMapTouchStateChanged = onMapTouchStateChanged,
            onMarkerClick = onMarkerClick
        )
    } else {
        OsmMapView(
            modifier = modifier,
            centerLat = centerLat,
            centerLng = centerLng,
            zoomLevel = zoomLevel,
            recenterTrigger = recenterTrigger,
            markers = markers,
            polylinePoints = polylinePoints,
            driverPolylinePoints = driverPolylinePoints,
            autoFitBounds = autoFitBounds,
            onMapClick = onMapClick,
            onMapMoveEnd = onMapMoveEnd,
            onMapTouchStateChanged = onMapTouchStateChanged,
            onMarkerClick = onMarkerClick
        )
    }
}
