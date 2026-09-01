package com.speedo.captain.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.core.model.Ride
import com.speedo.core.theme.*
import kotlinx.coroutines.delay

// Rapido Captain Signature Colors
val RapidoCaptainGreen = Color(0xFF00C853)
val RapidoCaptainGreenDark = Color(0xFF009624)
val RapidoCaptainGreenLight = Color(0xFFE8F5E9)
val RapidoCaptainYellow = Color(0xFFFFCC00)
val RapidoCaptainYellowDark = Color(0xFFF5B800)
val RapidoCaptainBlack = Color(0xFF1E1E1E)

/**
 * 15-Second Circular Countdown Flash Banner for Incoming Ride Requests
 */
@Composable
fun IncomingRideFlashBanner(
    ride: Ride,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    var timeLeft by remember(ride.id) { mutableStateOf(30) }

    LaunchedEffect(ride.id) {
        timeLeft = 30
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        if (timeLeft == 0) {
            onReject()
        }
    }

    val progress = (timeLeft / 30f).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = SpeedoWhite,
        shadowElevation = 24.dp,
        border = BorderStroke(2.dp, RapidoCaptainYellowDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with 15-second Circular Countdown Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(RapidoCaptainGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NEW RIDE REQUEST",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RapidoCaptainGreenDark,
                            letterSpacing = 1.sp
                        )
                    )
                }

                // Countdown Timer Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (timeLeft <= 5) Color(0xFFFFEBEE) else RapidoCaptainGreenLight,
                    border = BorderStroke(1.dp, if (timeLeft <= 5) SpeedoError else RapidoCaptainGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(16.dp),
                            color = if (timeLeft <= 5) SpeedoError else RapidoCaptainGreen,
                            strokeWidth = 2.5.dp,
                            trackColor = Color.Transparent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${timeLeft}s",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (timeLeft <= 5) SpeedoError else RapidoCaptainGreenDark
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Guaranteed Earnings Highlight Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFFDE7),
                border = BorderStroke(1.dp, RapidoCaptainYellowDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Guaranteed Earnings",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57F17)
                            )
                        )
                        Text(
                            text = "Cash / UPI on Drop",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpeedoTextSecondary
                        )
                    }
                    Text(
                        text = "₹${ride.fare.toInt()}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RapidoCaptainBlack
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pickup & Drop Summary
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Pickup
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RapidoCaptainGreen)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PICKUP • 0.8 km (2 min away)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RapidoCaptainGreenDark)
                        )
                        Text(
                            text = ride.pickupAddress,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Drop
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3D00))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DROP • ${ride.distanceKm} km ride",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoTextSecondary)
                        )
                        Text(
                            text = ride.dropAddress,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Accept & Reject Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, SpeedoError),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SpeedoError)
                ) {
                    Text(text = "Decline", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RapidoCaptainGreen,
                        contentColor = SpeedoWhite
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "ACCEPT RIDE", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * 4-Digit OTP PIN Verification Keypad for the Captain before starting the ride
 */
@Composable
fun CaptainOtpKeypadSheet(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onVerifyOtp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            localError = errorMessage
        }
    }

    // Full-screen modal overlay with dark scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                if (!isLoading) onDismiss()
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { /* intercept click so sheet doesn't close */ },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = SpeedoWhite,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Enter Rider Start PIN",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = RapidoCaptainBlack
                            )
                        )
                        Text(
                            text = "Ask passenger for the 4-digit OTP",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpeedoTextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = RapidoCaptainBlack)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4 PIN Digit Display Boxes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (0..3).forEach { index ->
                        val digit = pin.getOrNull(index)?.toString() ?: ""
                        val hasDigit = digit.isNotEmpty()
                        val isCurrentIndex = pin.length == index
                        val hasError = localError != null

                        Surface(
                            modifier = Modifier.size(58.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                hasError -> SpeedoErrorContainer
                                hasDigit -> RapidoCaptainGreenLight
                                else -> SpeedoSurfaceVariant
                            },
                            border = BorderStroke(
                                2.dp,
                                when {
                                    hasError -> SpeedoError
                                    isCurrentIndex -> SpeedoOrange
                                    hasDigit -> RapidoCaptainGreen
                                    else -> SpeedoCardBorder
                                }
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = digit,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            hasError -> SpeedoError
                                            hasDigit -> RapidoCaptainGreenDark
                                            else -> RapidoCaptainBlack
                                        }
                                    )
                                )
                            }
                        }
                    }
                }

                if (localError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localError ?: "Incorrect OTP. Please check with rider.",
                        color = SpeedoError,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Numeric Dialpad (1-9, Clear, 0, Backspace)
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "C" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFEBEE))
                                            .clickable(enabled = !isLoading) {
                                                pin = ""
                                                localError = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "CLR",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = SpeedoError
                                            )
                                        )
                                    }
                                }
                                "DEL" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(SpeedoSurfaceVariant)
                                            .clickable(enabled = !isLoading) {
                                                if (pin.isNotEmpty()) {
                                                    pin = pin.dropLast(1)
                                                    localError = null
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Backspace,
                                            contentDescription = "Delete",
                                            tint = RapidoCaptainBlack
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(SpeedoSurfaceVariant)
                                            .clickable(enabled = !isLoading) {
                                                if (pin.length < 4) {
                                                    val newPin = pin + key
                                                    pin = newPin
                                                    localError = null
                                                    if (newPin.length == 4) {
                                                        onVerifyOtp(newPin)
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = RapidoCaptainBlack
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Big Explicit "START TRIP" Action Button
                Button(
                    onClick = {
                        if (pin.length == 4 && !isLoading) {
                            onVerifyOtp(pin)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = pin.length == 4 && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RapidoCaptainGreen,
                        contentColor = SpeedoWhite,
                        disabledContainerColor = SpeedoSurfaceVariant,
                        disabledContentColor = SpeedoTextSecondary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = SpeedoWhite,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "VERIFYING PIN...", fontWeight = FontWeight.ExtraBold)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (pin.length == 4) "START TRIP ($pin)" else "ENTER 4-DIGIT PIN TO START",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Dynamic UPI QR Payment Sheet for Captain to collect fare at trip end
 * Displays the Captain's uploaded onboarding QR code (with fallback to dynamic UPI QR)
 */
@Composable
fun DynamicUpiQrPaymentSheet(
    fare: Int,
    rideId: String = "",
    riderName: String? = null,
    uploadedQrUrl: String? = null,
    onPaymentCollected: () -> Unit
) {
    val context = LocalContext.current
    val rawQrUrl = uploadedQrUrl?.trim()

    val resolvedQrUrl = remember(rawQrUrl, fare, rideId) {
        if (!rawQrUrl.isNullOrBlank()) {
            if (rawQrUrl.startsWith("http://") || rawQrUrl.startsWith("https://")) {
                rawQrUrl
            } else {
                val base = com.speedo.core.utils.Constants.getBaseUrl(context).removeSuffix("api/").removeSuffix("/")
                val cleanPath = if (rawQrUrl.startsWith("/")) rawQrUrl.substring(1) else rawQrUrl
                "$base/$cleanPath"
            }
        } else {
            val upiPayload = "upi://pay?pa=speedo.pay@upi&pn=Speedo%20Ride&am=$fare&cu=INR&tn=SpeedoRide-${rideId.takeLast(6)}"
            val encodedUpi = java.net.URLEncoder.encode(upiPayload, "UTF-8")
            "https://api.qrserver.com/v1/create-qr-code/?size=350x350&data=$encodedUpi"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = SpeedoWhite,
        shadowElevation = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(RapidoCaptainGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = RapidoCaptainGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Trip Completed! 🎉",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = RapidoCaptainBlack
                )
            )

            Text(
                text = if (!riderName.isNullOrBlank()) "Collect ₹$fare from $riderName" else "Ask rider to scan & pay or accept cash",
                style = MaterialTheme.typography.bodySmall,
                color = SpeedoTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFFDE7),
                border = BorderStroke(1.5.dp, RapidoCaptainYellowDark)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Fare:",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SpeedoTextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "₹$fare",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RapidoCaptainBlack
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Captain's Uploaded QR Code Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SpeedoWhite,
                border = BorderStroke(1.5.dp, if (!rawQrUrl.isNullOrBlank()) RapidoCaptainGreen else Color(0xFFE0E0E0)),
                shadowElevation = 4.dp,
                modifier = Modifier.size(220.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = resolvedQrUrl,
                        contentDescription = "Captain's Onboarding Payment QR Code",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = if (!rawQrUrl.isNullOrBlank()) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (!rawQrUrl.isNullOrBlank()) RapidoCaptainGreen else Color(0xFF90CAF9))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (!rawQrUrl.isNullOrBlank()) Icons.Default.Verified else Icons.Default.QrCode,
                        contentDescription = null,
                        tint = if (!rawQrUrl.isNullOrBlank()) RapidoCaptainGreenDark else Color(0xFF1565C0),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!rawQrUrl.isNullOrBlank()) "Captain's Verified UPI QR" else "Scan with any UPI App",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (!rawQrUrl.isNullOrBlank()) RapidoCaptainGreenDark else Color(0xFF1565C0)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Big Green PAID Confirmation Button
            Button(
                onClick = onPaymentCollected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RapidoCaptainGreen,
                    contentColor = SpeedoWhite
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpeedoWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PAID (Payment Received ₹$fare)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Visual Thumbnail Card for High Demand Zones / Hotspots for Captains
 */
@Composable
fun CaptainHotspotThumbnailCard(
    destination: com.speedo.core.model.PopularDestination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder),
        shadowElevation = 4.dp,
        modifier = modifier
            .width(185.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(SpeedoSurfaceVariant)
            ) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(destination.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = destination.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Scrim gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                                startY = 35f
                            )
                        )
                )

                // Surge / High Demand Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF57F17),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = SpeedoWhite,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "HIGH DEMAND",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = SpeedoWhite,
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                if (destination.distanceKm != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SpeedoWhite.copy(alpha = 0.95f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = SpeedoOrange,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f km", destination.distanceKm),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RapidoCaptainBlack,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = destination.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = RapidoCaptainBlack
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = destination.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = SpeedoTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

