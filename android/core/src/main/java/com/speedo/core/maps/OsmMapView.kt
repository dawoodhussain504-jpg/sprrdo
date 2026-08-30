package com.speedo.core.maps

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

data class MapMarkerData(
    val id: String,
    val lat: Double,
    val lng: Double,
    val title: String,
    val snippet: String? = null,
    val markerType: MarkerType,
    val bearing: Float = 0f,
    val vehicleType: String = "bike"
)

enum class MarkerType {
    USER_LOCATION,
    PICKUP,
    DROP,
    CAPTAIN
}

@Composable
fun OsmMapView(
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
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var lastBoundsKey by remember { mutableStateOf<String>("") }
    var lastRecenterTrigger by remember { mutableStateOf<Long>(0L) }

    // Lifecycle event handling for MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapViewRef?.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    AndroidView(
        factory = { ctx ->
            SpeedoMapConfig.init(ctx)

            MapView(ctx).apply {
                // 100% Free, NO Watermark, NO API Key Required Tile Engine (OpenStreetMap HOT)
                setTileSource(SpeedoMapConfig.DEFAULT_TILE_SOURCE)

                setUseDataConnection(true)
                setMultiTouchControls(true)
                setTilesScaledToDpi(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                controller.setZoom(zoomLevel)
                controller.setCenter(GeoPoint(centerLat, centerLng))

                if (onMapClick != null) {
                    val overlayEvents = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                onMapClick.invoke(p)
                                return true
                            }
                            return false
                        }
                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    })
                    overlays.add(0, overlayEvents)
                }

                var isUserInteracting = false

                setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN,
                        android.view.MotionEvent.ACTION_MOVE -> {
                            isUserInteracting = true
                            onMapTouchStateChanged?.invoke(true)
                        }
                        android.view.MotionEvent.ACTION_UP,
                        android.view.MotionEvent.ACTION_CANCEL -> {
                            if (isUserInteracting) {
                                onMapTouchStateChanged?.invoke(false)
                                if (onMapMoveEnd != null) {
                                    postDelayed({
                                        (mapCenter as? GeoPoint)?.let { onMapMoveEnd.invoke(it) }
                                    }, 350)
                                }
                            }
                            isUserInteracting = false
                        }
                    }
                    false
                }

                mapViewRef = this
            }
        },
        update = { mapView ->
            mapView.post {
                try {
                    val baseOverlay = if (onMapClick != null && mapView.overlays.isNotEmpty()) mapView.overlays.firstOrNull() else null
                    synchronized(mapView.overlays) {
                        mapView.overlays.clear()
                        if (baseOverlay != null) {
                            mapView.overlays.add(baseOverlay)
                        }

                        // 1. Draw Driver-to-Pickup Polyline (Rapido Signature Amber/Gold #FFA000)
                        if (driverPolylinePoints.size >= 2) {
                            val driverCasing = Polyline().apply {
                                setPoints(driverPolylinePoints)
                                outlinePaint.color = AndroidColor.parseColor("#263238")
                                outlinePaint.strokeWidth = 14f
                                outlinePaint.strokeCap = Paint.Cap.ROUND
                                outlinePaint.strokeJoin = Paint.Join.ROUND
                            }
                            val driverInner = Polyline().apply {
                                setPoints(driverPolylinePoints)
                                outlinePaint.color = AndroidColor.parseColor("#FFB300")
                                outlinePaint.strokeWidth = 9f
                                outlinePaint.strokeCap = Paint.Cap.ROUND
                                outlinePaint.strokeJoin = Paint.Join.ROUND
                            }
                            mapView.overlays.add(driverCasing)
                            mapView.overlays.add(driverInner)
                        }

                        // 2. Draw Main Road Polyline (Rapido Electric Green #00E676 with Dark Outer Glow)
                        if (polylinePoints.size >= 2) {
                            val polylineCasing = Polyline().apply {
                                setPoints(polylinePoints)
                                outlinePaint.color = AndroidColor.parseColor("#1B5E20")
                                outlinePaint.strokeWidth = 16f
                                outlinePaint.strokeCap = Paint.Cap.ROUND
                                outlinePaint.strokeJoin = Paint.Join.ROUND
                            }
                            val polylineInner = Polyline().apply {
                                setPoints(polylinePoints)
                                outlinePaint.color = AndroidColor.parseColor("#00E676")
                                outlinePaint.strokeWidth = 10f
                                outlinePaint.strokeCap = Paint.Cap.ROUND
                                outlinePaint.strokeJoin = Paint.Join.ROUND
                            }
                            mapView.overlays.add(polylineCasing)
                            mapView.overlays.add(polylineInner)
                        }

                        // 3. Add Custom Rapido Styled Markers
                        markers.forEach { markerData ->
                            val marker = Marker(mapView).apply {
                                position = GeoPoint(markerData.lat, markerData.lng)
                                title = markerData.title
                                snippet = markerData.snippet
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                                icon = when (markerData.markerType) {
                                    MarkerType.PICKUP -> MapMarkerUtils.createPinDrawable(context, AndroidColor.parseColor("#00C853"), "P")
                                    MarkerType.DROP -> MapMarkerUtils.createPinDrawable(context, AndroidColor.parseColor("#D50000"), "D")
                                    MarkerType.USER_LOCATION -> {
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                        MapMarkerUtils.createUserLocationDrawable(context)
                                    }
                                    MarkerType.CAPTAIN -> {
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                        rotation = markerData.bearing
                                        MapMarkerUtils.createVehicleDrawable(context, markerData.vehicleType, markerData.bearing)
                                    }
                                }

                                setOnMarkerClickListener { _, _ ->
                                    onMarkerClick?.invoke(markerData)
                                    showInfoWindow()
                                    true
                                }
                            }
                            mapView.overlays.add(marker)
                        }
                    }

                    // 4. Smooth Recenter Trigger Handling
                    if (recenterTrigger != 0L && recenterTrigger != lastRecenterTrigger) {
                        lastRecenterTrigger = recenterTrigger
                        if (centerLat != 0.0 && centerLng != 0.0) {
                            mapView.controller.animateTo(GeoPoint(centerLat, centerLng), zoomLevel, 600L)
                        }
                    } else if (autoFitBounds && (polylinePoints.size >= 2 || markers.size >= 2)) {
                        // 5. Safe Auto-fit camera framing with padding
                        val allPoints = mutableListOf<GeoPoint>()
                        if (polylinePoints.isNotEmpty()) allPoints.addAll(polylinePoints)
                        if (driverPolylinePoints.isNotEmpty()) allPoints.addAll(driverPolylinePoints)
                        markers.forEach { allPoints.add(GeoPoint(it.lat, it.lng)) }

                        if (allPoints.isNotEmpty()) {
                            var minLat = 90.0
                            var maxLat = -90.0
                            var minLng = 180.0
                            var maxLng = -180.0

                            allPoints.forEach { pt ->
                                if (pt.latitude < minLat) minLat = pt.latitude
                                if (pt.latitude > maxLat) maxLat = pt.latitude
                                if (pt.longitude < minLng) minLng = pt.longitude
                                if (pt.longitude > maxLng) maxLng = pt.longitude
                            }

                            val boundsKey = "$minLat,$maxLat,$minLng,$maxLng"
                            if (boundsKey != lastBoundsKey) {
                                lastBoundsKey = boundsKey
                                if (mapView.width > 0 && mapView.height > 0) {
                                    val padding = 0.003
                                    val boundingBox = BoundingBox(maxLat + padding, maxLng + padding, minLat - padding, minLng - padding)
                                    mapView.zoomToBoundingBox(boundingBox, true, 110)
                                }
                            }
                        }
                    } else if (centerLat != 0.0 && centerLng != 0.0) {
                        val currentCenter = mapView.mapCenter
                        if (Math.abs(currentCenter.latitude - centerLat) > 0.0001 || Math.abs(currentCenter.longitude - centerLng) > 0.0001) {
                            mapView.controller.animateTo(GeoPoint(centerLat, centerLng))
                        }
                    }

                    mapView.invalidate()
                } catch (e: Exception) {
                    android.util.Log.e("OsmMapView", "Map update error", e)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
