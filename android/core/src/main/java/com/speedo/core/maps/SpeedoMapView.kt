package com.speedo.core.maps

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.osmdroid.util.GeoPoint

/**
 * Universal Speedo Map Composable
 * Automatically renders Google Maps Native Vector Engine when Google Play Services is available,
 * and gracefully falls back to OpenStreetMap (OsmMapView) on devices without Play Services.
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
    val isGooglePlayServicesAvailable = remember(context) {
        try {
            val availability = GoogleApiAvailability.getInstance()
            availability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        } catch (_: Exception) {
            false
        }
    }

    if (!forceOsm && isGooglePlayServicesAvailable) {
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
