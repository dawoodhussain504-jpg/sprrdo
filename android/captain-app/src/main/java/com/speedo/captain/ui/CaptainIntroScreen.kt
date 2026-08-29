package com.speedo.captain.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.captain.ui.components.*
import com.speedo.core.components.SpeedoAppIconBadge
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.theme.*
import kotlinx.coroutines.launch

data class CaptainIntroSlide(
    val title: String,
    val subtitle: String,
    val badge: String,
    val badgeColor: Color,
    val illustrationType: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CaptainIntroScreen(
    onFinishIntro: () -> Unit
) {
    val slides = remember {
        listOf(
            CaptainIntroSlide(
                title = "Drive & Earn on Your Terms",
                subtitle = "Accept Speedo Moto, Toto & Speedo 4 requests in seconds with transparent upfront fares and guaranteed earnings.",
                badge = "⚡ INSTANT RIDE ALERTS",
                badgeColor = RapidoCaptainGreenDark,
                illustrationType = 1
            ),
            CaptainIntroSlide(
                title = "Road-Snapped GPS Navigation",
                subtitle = "Follow smooth road-snapped routes to rider pickup & destination, and verify passengers seamlessly with a 4-digit PIN.",
                badge = "📍 TURN-BY-TURN ROUTING",
                badgeColor = SpeedoOrange,
                illustrationType = 2
            ),
            CaptainIntroSlide(
                title = "Instant UPI QR Collections",
                subtitle = "Collect trip fares directly into your personal UPI ID or QR code at destination with 100% earnings and zero commission delays.",
                badge = "💰 ZERO DELAY PAYOUTS",
                badgeColor = Color(0xFF2979FF),
                illustrationType = 3
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SpeedoWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar with Brand Badge & Skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SpeedoAppIconBadge(sizeDp = 34)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Captian",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = SpeedoOrange
                        )
                    )
                }

                if (pagerState.currentPage < slides.size - 1) {
                    TextButton(onClick = onFinishIntro) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SpeedoTextSecondary
                            )
                        )
                    }
                }
            }

            // Horizontal Pager for Intro Slides
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val slide = slides[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // App UI Mockup / Visual Card
                    when (slide.illustrationType) {
                        1 -> CaptainFlashRequestIllustration()
                        2 -> CaptainNavigationOtpIllustration()
                        3 -> CaptainUpiQrIllustration()
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Badge Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = slide.badgeColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, slide.badgeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = slide.badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = slide.badgeColor
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title
                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RapidoCaptainBlack,
                            letterSpacing = (-0.5).sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Subtitle
                    Text(
                        text = slide.subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SpeedoTextSecondary,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Navigation Footer (Dots Indicator + Action Button)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Pager Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(slides.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(if (isSelected) RapidoCaptainGreen else SpeedoSurfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Next / Start Earning Button
                val isLastPage = pagerState.currentPage == slides.size - 1
                SpeedoPrimaryButton(
                    text = if (isLastPage) "START EARNING" else "CONTINUE",
                    onClick = {
                        if (isLastPage) {
                            onFinishIntro()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLastPage) RapidoCaptainGreen else SpeedoOrange,
                        contentColor = SpeedoWhite
                    ),
                    leadingIcon = if (isLastPage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward
                )
            }
        }
    }
}

/**
 * Slide 1 Mockup: 15-Second Circular Flash Incoming Ride Banner
 */
@Composable
fun CaptainFlashRequestIllustration() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.5.dp, RapidoCaptainGreen)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            color = RapidoCaptainGreenDark
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = "12s left",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fare & Ride Type Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFE8F5E9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Guaranteed Earning", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                        Text(text = "Speedo Moto • 4.2 km", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RapidoCaptainGreenDark))
                    }
                    Text(text = "₹180", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = RapidoCaptainGreenDark))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pickup / Drop route
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = RapidoCaptainGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Pickup: Indiranagar 100ft Rd (0.8 km)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFFF3D00), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Drop: MG Road Metro Station", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RapidoCaptainGreen)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "ACCEPT RIDE (₹180)", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/**
 * Slide 2 Mockup: Turn-by-Turn Maneuver & 4-Digit OTP Entry Dialpad
 */
@Composable
fun CaptainNavigationOtpIllustration() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Turn Maneuver Header
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFE8F5E9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC8E6C9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TurnRight, contentDescription = null, tint = Color(0xFF2E7D32))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "In 200m Turn Right onto Main Rd", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(text = "Arriving at Passenger Pickup", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ENTER 4-DIGIT RIDER PIN",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = SpeedoTextSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // OTP PIN Boxes
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("4", "8", "9", "2").forEach { digit ->
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = RapidoCaptainGreenLight,
                            border = BorderStroke(1.5.dp, RapidoCaptainGreen)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = digit,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RapidoCaptainGreenDark
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RapidoCaptainGreen)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "START TRIP NOW", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Slide 3 Mockup: Captain UPI QR Code & Instant Fare Collection
 */
@Composable
fun CaptainUpiQrIllustration() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Today's Earnings Summary Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFE8F5E9),
                border = BorderStroke(1.dp, RapidoCaptainGreen)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = RapidoCaptainGreenDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Today's Earnings: ₹8,520",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RapidoCaptainGreenDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulated UPI QR Code
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SpeedoWhite,
                border = BorderStroke(1.5.dp, RapidoCaptainGreen),
                shadowElevation = 4.dp,
                modifier = Modifier.size(130.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = "Captain UPI QR",
                        tint = RapidoCaptainBlack,
                        modifier = Modifier.size(90.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Scan & Pay Direct to Captain UPI",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = SpeedoTextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RapidoCaptainGreen)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "PAID (₹180 Received)", fontWeight = FontWeight.Bold)
            }
        }
    }
}
