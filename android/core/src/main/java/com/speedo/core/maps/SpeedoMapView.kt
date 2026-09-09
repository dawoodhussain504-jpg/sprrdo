package com.speedo.core.maps

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.core.theme.*
import org.osmdroid.util.GeoPoint

/**
 * Universal Speedo Map Composable with Rapido Map Interaction Engine.
 *
 * Features:
 * - Fluid Rapido Gesture Handling: Panning, dragging, and pinching NEVER snap back.
 * - Floating Rapido HUD: 1-tap Recenter/Locate Me target button and sleek stacked Zoom In/Out pill.
 * - Asymmetrical Auto-Framing: Auto-fit bounds respects bottom sheets and top banners so markers remain fully visible.
 * - Interactive Center Pin: Spring-bounce animation when selecting locations directly on the map.
 * - Seamless Engine Switching: Native Google Maps Vector Engine with verified fallback to OpenStreetMap.
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
    // Floating Rapido Controls HUD
    showControls: Boolean = true,
    showRecenterButton: Boolean = true,
    showZoomControls: Boolean = true,
    controlsBottomPadding: Dp = 16.dp,
    controlsTopPadding: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onRecenterClick: (() -> Unit)? = null,
    // Interactive Center Pin Mode
    showCenterPin: Boolean = false,
    centerPinLabel: String? = null,
    // Callbacks
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

    // Interaction & Animation Triggers
    var userHasPanned by remember { mutableStateOf(false) }
    var zoomInTrigger by remember { mutableStateOf(0L) }
    var zoomOutTrigger by remember { mutableStateOf(0L) }
    var internalRecenterTrigger by remember { mutableStateOf(0L) }
    var isTouchingMap by remember { mutableStateOf(false) }

    val effectiveRecenterTrigger = remember(recenterTrigger, internalRecenterTrigger) {
        if (internalRecenterTrigger > recenterTrigger) internalRecenterTrigger else recenterTrigger
    }

    Box(modifier = modifier) {
        // 1. Primary Vector / Tile Map Engine
        if (!forceOsm && isGoogleMapsReady) {
            GoogleMapView(
                modifier = Modifier.fillMaxSize(),
                centerLat = centerLat,
                centerLng = centerLng,
                zoomLevel = zoomLevel,
                recenterTrigger = effectiveRecenterTrigger,
                zoomInTrigger = zoomInTrigger,
                zoomOutTrigger = zoomOutTrigger,
                contentPadding = contentPadding,
                userHasPanned = userHasPanned,
                onUserPannedChanged = { panned ->
                    userHasPanned = panned
                },
                markers = markers,
                polylinePoints = polylinePoints,
                driverPolylinePoints = driverPolylinePoints,
                autoFitBounds = autoFitBounds,
                onMapClick = onMapClick,
                onMapMoveEnd = onMapMoveEnd,
                onMapTouchStateChanged = { touching ->
                    isTouchingMap = touching
                    onMapTouchStateChanged?.invoke(touching)
                },
                onMarkerClick = onMarkerClick
            )
        } else {
            OsmMapView(
                modifier = Modifier.fillMaxSize(),
                centerLat = centerLat,
                centerLng = centerLng,
                zoomLevel = zoomLevel,
                recenterTrigger = effectiveRecenterTrigger,
                zoomInTrigger = zoomInTrigger,
                zoomOutTrigger = zoomOutTrigger,
                contentPadding = contentPadding,
                userHasPanned = userHasPanned,
                onUserPannedChanged = { panned ->
                    userHasPanned = panned
                },
                markers = markers,
                polylinePoints = polylinePoints,
                driverPolylinePoints = driverPolylinePoints,
                autoFitBounds = autoFitBounds,
                onMapClick = onMapClick,
                onMapMoveEnd = onMapMoveEnd,
                onMapTouchStateChanged = { touching ->
                    isTouchingMap = touching
                    onMapTouchStateChanged?.invoke(touching)
                },
                onMarkerClick = onMarkerClick
            )
        }

        // 2. Interactive Rapido Center Pin (when in location picker mode)
        if (showCenterPin) {
            val pinOffsetY by animateDpAsState(
                targetValue = if (isTouchingMap) (-16).dp else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "centerPinBounce"
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = pinOffsetY),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (centerPinLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SpeedoTextPrimary,
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = centerPinLabel,
                            color = SpeedoWhite,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Center Reticle Pin",
                    tint = SpeedoOrange,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // 3. Floating Rapido HUD Controls (Recenter & Kinetic Zoom Stack)
        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = controlsBottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Floating Recenter / Locate Me Target Button
                if (showRecenterButton) {
                    Surface(
                        shape = CircleShape,
                        color = SpeedoWhite,
                        shadowElevation = 6.dp,
                        border = BorderStroke(
                            1.dp,
                            if (userHasPanned) SpeedoOrange.copy(alpha = 0.5f) else SpeedoCardBorder
                        ),
                        modifier = Modifier
                            .size(46.dp)
                            .clickable {
                                internalRecenterTrigger = System.currentTimeMillis()
                                userHasPanned = false
                                onRecenterClick?.invoke()
                            }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Recenter / Locate Me",
                                tint = if (userHasPanned) SpeedoOrange else Color(0xFF212121),
                                modifier = Modifier.size(22.dp)
                            )
                            // Subtle orange dot indicator when map has been manually panned away
                            if (userHasPanned) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-8).dp, y = 8.dp)
                                        .clip(CircleShape)
                                        .background(SpeedoOrange)
                                )
                            }
                        }
                    }
                }

                // Floating Kinetic Zoom Controls Pill (+ and -)
                if (showZoomControls) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SpeedoWhite,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, SpeedoCardBorder),
                        modifier = Modifier.width(46.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Zoom In (+)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable {
                                        zoomInTrigger = System.currentTimeMillis()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Zoom In",
                                    tint = Color(0xFF212121),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = SpeedoDivider,
                                thickness = 1.dp
                            )

                            // Zoom Out (-)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable {
                                        zoomOutTrigger = System.currentTimeMillis()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Zoom Out",
                                    tint = Color(0xFF212121),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
