package com.speedo.rider.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.speedo.core.theme.*
import com.speedo.rider.voice.VoiceBookingHelper
import com.speedo.rider.voice.VoiceBookingResult
import com.speedo.rider.voice.VoiceIntentParser

@Composable
fun VoiceBookingSheet(
    onDismiss: () -> Unit,
    onConfirmed: (destination: String, vehicleType: String?) -> Unit
) {
    val context = LocalContext.current
    val voiceHelper = remember { VoiceBookingHelper(context) }

    var selectedLang by remember { mutableStateOf("hi-IN") }
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
        if (isGranted) {
            voiceHelper.startListening(selectedLang)
        }
    }

    val isListening by voiceHelper.isListening.collectAsState()
    val liveTranscript by voiceHelper.liveTranscript.collectAsState()
    val parsedResult by voiceHelper.parsedResult.collectAsState()
    val audioVolume by voiceHelper.audioVolume.collectAsState()
    val errorMessage by voiceHelper.errorMessage.collectAsState()

    // Pulse animation for outer mic ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Automatically trigger speech listening when permission granted
    LaunchedEffect(hasRecordPermission, selectedLang) {
        if (hasRecordPermission) {
            voiceHelper.startListening(selectedLang)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceHelper.destroy()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Prevent dismiss on sheet click
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                color = SpeedoWhite,
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Handle Bar
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFE0E0E0))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Top Bar: Title & Language Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFCC00)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color(0xFF1E293B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Speech-to-Ride",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1E293B)
                                    )
                                )
                                Text(
                                    text = "Speak destination & ride type",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Language Toggle Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(modifier = Modifier.padding(3.dp)) {
                                val isHindi = selectedLang == "hi-IN"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isHindi) Color(0xFFFFCC00) else Color.Transparent)
                                        .clickable {
                                            selectedLang = "hi-IN"
                                            if (hasRecordPermission) voiceHelper.startListening("hi-IN")
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "हिंदी",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isHindi) Color(0xFF1E293B) else Color(0xFF64748B)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (!isHindi) Color(0xFFFFCC00) else Color.Transparent)
                                        .clickable {
                                            selectedLang = "en-IN"
                                            if (hasRecordPermission) voiceHelper.startListening("en-IN")
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "ENG",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (!isHindi) Color(0xFF1E293B) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Center Animated Microphone & Waves
                    Box(
                        modifier = Modifier
                            .size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulsing outer halo when listening
                        if (isListening) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFCC00).copy(alpha = pulseAlpha))
                            )
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .scale(1f + (audioVolume * 0.4f))
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF9900).copy(alpha = 0.25f))
                            )
                        }

                        // Core Mic Button
                        Surface(
                            shape = CircleShape,
                            color = if (isListening) Color(0xFFFFCC00) else Color(0xFFF1F5F9),
                            shadowElevation = if (isListening) 10.dp else 2.dp,
                            border = BorderStroke(
                                2.dp,
                                if (isListening) Color(0xFFF59E0B) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .size(76.dp)
                                .clickable {
                                    if (isListening) {
                                        voiceHelper.stopListening()
                                    } else {
                                        if (hasRecordPermission) {
                                            voiceHelper.startListening(selectedLang)
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                                    contentDescription = "Microphone",
                                    tint = if (isListening) Color(0xFF1E293B) else Color(0xFF64748B),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // Sound Wave Bars (Visualizer)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val barCount = 9
                        for (i in 0 until barCount) {
                            val factor = kotlin.math.sin((i.toDouble() / barCount) * Math.PI).toFloat()
                            val barHeight = if (isListening) {
                                (8 + (audioVolume * 18 * factor) + (if (i % 2 == 0) 4 else 0)).dp
                            } else {
                                4.dp
                            }
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .width(4.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (isListening) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Spoken Status / Live Transcript Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (liveTranscript.isNotBlank()) {
                                Text(
                                    text = "\"$liveTranscript\"",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        fontSize = 17.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = when {
                                        isListening -> "Listening... Please speak clearly"
                                        errorMessage != null -> errorMessage ?: "Tap mic to retry"
                                        else -> "Tap the microphone and speak"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (errorMessage != null) SpeedoError else Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }

                    // Parsed Result Card (Destination & Vehicle Extracted)
                    val parsed = parsedResult
                    if (parsed != null && parsed.cleanDestination.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.5.dp, Color(0xFF3B82F6))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDBEAFE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DETECTED DESTINATION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2563EB)
                                    )
                                    Text(
                                        text = parsed.cleanDestination,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                parsed.vehicleType?.let { vehicle ->
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFFFFCC00),
                                        shadowElevation = 2.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = when (vehicle) {
                                                    "bike" -> Icons.Default.TwoWheeler
                                                    "auto" -> Icons.Default.ElectricRickshaw
                                                    else -> Icons.Default.DirectionsCar
                                                },
                                                contentDescription = null,
                                                tint = Color(0xFF1E293B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = vehicle.uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF1E293B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Example Suggestions Chips
                    Text(
                        text = "Or try tapping one of these:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val quickPhrases = listOf(
                        Triple("Ranchi Station auto se", "Ranchi Railway Station", "auto"),
                        Triple("Kolkata Airport bike ride", "Kolkata Airport", "bike"),
                        Triple("Main Road Mall cab", "Main Road Shopping Mall", "car"),
                        Triple("MG Road Metro auto", "MG Road Metro Station", "auto")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickPhrases) { (phrase, dest, vehicle) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.clickable {
                                    onConfirmed(dest, vehicle)
                                    onDismiss()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = phrase,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF334155),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Bottom Confirm & Search Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Text(
                                "Cancel",
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val canConfirm = parsed != null && parsed.cleanDestination.isNotBlank()
                        Button(
                            onClick = {
                                if (parsed != null && parsed.cleanDestination.isNotBlank()) {
                                    onConfirmed(parsed.cleanDestination, parsed.vehicleType)
                                    onDismiss()
                                }
                            },
                            enabled = canConfirm,
                            modifier = Modifier
                                .weight(2f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFCC00),
                                disabledContainerColor = Color(0xFFE2E8F0)
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (canConfirm) Color(0xFF1E293B) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Find Ride",
                                    color = if (canConfirm) Color(0xFF1E293B) else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
