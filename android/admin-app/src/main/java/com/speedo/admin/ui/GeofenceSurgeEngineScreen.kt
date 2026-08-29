package com.speedo.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.*
import com.speedo.core.maps.MapMarkerData
import com.speedo.core.maps.MarkerType
import com.speedo.core.maps.OsmMapView
import com.speedo.core.model.SurgeZone
import com.speedo.core.theme.*

@Composable
fun GeofenceSurgeEngineScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val zones = uiState.surgeZones

    var showCreateDialog by remember { mutableStateOf(false) }
    var recenterTrigger by remember { mutableStateOf(1L) }

    LaunchedEffect(Unit) {
        viewModel.fetchSurgeZones()
    }

    val mapMarkers = remember(zones) {
        zones.map { zone ->
            MapMarkerData(
                id = zone.id,
                lat = zone.centerLat,
                lng = zone.centerLng,
                title = "${zone.name} (${zone.surgeMultiplier}x)",
                snippet = "Radius: ${zone.radiusKm} km • Status: " + (if (zone.isActive) "ACTIVE" else "PAUSED"),
                markerType = MarkerType.DROP
            )
        }
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "Geofenced Surge Engine",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.fetchSurgeZones() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SpeedoTextPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = SpeedoOrange,
                contentColor = SpeedoWhite,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Surge Zone")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                OsmMapView(
                    modifier = Modifier.fillMaxSize(),
                    centerLat = 12.9716,
                    centerLng = 77.5946,
                    zoomLevel = 12.5,
                    recenterTrigger = recenterTrigger,
                    markers = mapMarkers,
                    autoFitBounds = mapMarkers.isNotEmpty()
                )

                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(10.dp),
                    color = SpeedoWhite.copy(alpha = 0.95f),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, SpeedoCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val activeCount = zones.count { it.isActive }
                        Text(
                            text = "$activeCount Active Surge Zones",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configured Surge Zones",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "${zones.size} Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpeedoTextSecondary
                )
            }

            if (zones.isEmpty()) {
                SpeedoEmptyView(
                    icon = Icons.Default.Bolt,
                    title = "No Surge Zones",
                    message = "Tap the + button to define custom surge pricing for airports, tech parks, or rain hotspots."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(zones) { zone ->
                        SurgeZoneCard(
                            zone = zone,
                            onToggleActive = { active -> viewModel.toggleSurgeZone(zone.id, active) },
                            onDelete = { viewModel.deleteSurgeZone(zone.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateSurgeZoneDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, type, lat, lng, radius, surge, baseMul, perKmMul ->
                viewModel.createSurgeZone(name, type, lat, lng, radius, surge, baseMul, perKmMul) {
                    showCreateDialog = false
                    recenterTrigger = System.currentTimeMillis()
                }
            }
        )
    }
}

@Composable
fun SurgeZoneCard(
    zone: SurgeZone,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, if (zone.isActive) SpeedoOrange.copy(alpha = 0.5f) else SpeedoDivider),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (zone.isActive) SpeedoOrangeContainer else SpeedoSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (zone.zoneType) {
                                "airport" -> Icons.Default.FlightTakeoff
                                "tech_park" -> Icons.Default.Business
                                "railway_station" -> Icons.Default.Train
                                else -> Icons.Default.Bolt
                            },
                            contentDescription = null,
                            tint = if (zone.isActive) SpeedoOrange else SpeedoTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = zone.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${zone.radiusKm} km radius • (${zone.centerLat}, ${zone.centerLng})",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpeedoTextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (zone.isActive) SpeedoOrange else SpeedoTextSecondary
                ) {
                    Text(
                        text = "${zone.surgeMultiplier}x SURGE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = SpeedoWhite
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = zone.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SpeedoWhite,
                            checkedTrackColor = SpeedoSuccess,
                            uncheckedThumbColor = SpeedoWhite,
                            uncheckedTrackColor = SpeedoSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (zone.isActive) "Active Live" else "Paused",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (zone.isActive) SpeedoSuccess else SpeedoTextSecondary
                        )
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = SpeedoError)
                }
            }
        }
    }
}

@Composable
fun CreateSurgeZoneDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Double, Double, Double, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var zoneType by remember { mutableStateOf("airport") }
    var latText by remember { mutableStateOf("12.9716") }
    var lngText by remember { mutableStateOf("77.5946") }
    var radiusKm by remember { mutableStateOf(3.0f) }
    var surgeMultiplier by remember { mutableStateOf(1.5f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SpeedoWhite,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Surge Zone",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SpeedoTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Zone Name",
                    placeholder = "e.g. Airport Surge Zone"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SpeedoTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = "Center Lat",
                        modifier = Modifier.weight(1f)
                    )
                    SpeedoTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = "Center Lng",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Radius: " + "%.1f".format(radiusKm) + " km",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Slider(
                    value = radiusKm,
                    onValueChange = { radiusKm = it },
                    valueRange = 1f..15f,
                    colors = SliderDefaults.colors(thumbColor = SpeedoOrange, activeTrackColor = SpeedoOrange)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Surge Multiplier: " + "%.2f".format(surgeMultiplier) + "x",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = SpeedoOrange)
                )
                Slider(
                    value = surgeMultiplier,
                    onValueChange = { surgeMultiplier = it },
                    valueRange = 1.1f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = SpeedoOrange, activeTrackColor = SpeedoOrange)
                )

                Spacer(modifier = Modifier.height(16.dp))

                SpeedoPrimaryButton(
                    text = "Save & Activate Surge Zone",
                    leadingIcon = Icons.Default.Check,
                    onClick = {
                        val lat = latText.toDoubleOrNull() ?: 12.9716
                        val lng = lngText.toDoubleOrNull() ?: 77.5946
                        if (name.isNotBlank()) {
                            onCreate(
                                name.trim(),
                                zoneType,
                                lat,
                                lng,
                                radiusKm.toDouble(),
                                Math.round(surgeMultiplier * 100.0) / 100.0,
                                1.25,
                                1.25
                            )
                        }
                    }
                )
            }
        }
    }
}
