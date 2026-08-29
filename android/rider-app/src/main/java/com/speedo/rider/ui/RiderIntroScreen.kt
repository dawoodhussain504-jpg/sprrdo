package com.speedo.rider.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.core.components.SpeedoAppIconBadge
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.theme.*
import kotlinx.coroutines.launch

data class RiderIntroSlide(
    val title: String,
    val subtitle: String,
    val badge: String,
    val badgeColor: Color,
    val illustrationType: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RiderIntroScreen(
    onFinishIntro: () -> Unit
) {
    val slides = remember {
        listOf(
            RiderIntroSlide(
                title = "Choose Your Ride",
                subtitle = "Book Speedo Moto for quick commutes, Speedo Toto for city autos, or Speedo 4 for comfortable 4-wheelers with transparent pricing.",
                badge = "⚡ 3 RIDE OPTIONS",
                badgeColor = SpeedoOrange,
                illustrationType = 1
            ),
            RiderIntroSlide(
                title = "Live GPS Tracking",
                subtitle = "Track your captain approaching live on Leaflet OpenStreetMap with accurate ETA, real-time driver coordinates & road-snapped curves.",
                badge = "📍 REAL-TIME MAPS",
                badgeColor = Color(0xFF00C853),
                illustrationType = 2
            ),
            RiderIntroSlide(
                title = "4-Digit Safety OTP & UPI",
                subtitle = "Enjoy 100% secure trips verified by a 4-digit ride start PIN, live in-app passenger-driver chat, and seamless cashless UPI payments.",
                badge = "🛡️ 100% SAFE & CASHLESS",
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
                        text = "Speedo",
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
                        1 -> RiderRideOptionsIllustration()
                        2 -> RiderMapTrackingIllustration()
                        3 -> RiderSecurityOtpIllustration()
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
                            color = SpeedoTextPrimary,
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
                                .background(if (isSelected) SpeedoOrange else SpeedoSurfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Next / Get Started Button
                val isLastPage = pagerState.currentPage == slides.size - 1
                SpeedoPrimaryButton(
                    text = if (isLastPage) "GET STARTED" else "CONTINUE",
                    onClick = {
                        if (isLastPage) {
                            onFinishIntro()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    leadingIcon = if (isLastPage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward
                )
            }
        }
    }
}

/**
 * Slide 1 Mockup: Speedo Moto, Speedo Toto & Speedo 4 Card Showcase
 */
@Composable
fun RiderRideOptionsIllustration() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELECT YOUR RIDE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = SpeedoTextSecondary
                    )
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = "UPFRONT FARES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vehicle Option 1: Speedo Moto
            RiderOptionItemMockup(
                title = "Speedo Moto",
                subtitle = "Fastest bike • 2 min away",
                fare = "₹45",
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                color = SpeedoOrange,
                isSelected = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Vehicle Option 2: Speedo Toto
            RiderOptionItemMockup(
                title = "Speedo Toto",
                subtitle = "Spacious auto • 3 min away",
                fare = "₹75",
                icon = Icons.Default.ElectricRickshaw,
                color = Color(0xFF00B0FF),
                isSelected = false
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Vehicle Option 3: Speedo 4
            RiderOptionItemMockup(
                title = "Speedo 4",
                subtitle = "AC cab ride • 5 min away",
                fare = "₹120",
                icon = Icons.Default.DirectionsCar,
                color = Color(0xFF7C4DFF),
                isSelected = false
            )
        }
    }
}

@Composable
fun RiderOptionItemMockup(
    title: String,
    subtitle: String,
    fare: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color.copy(alpha = 0.08f) else SpeedoSurfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.5.dp, if (isSelected) color else SpeedoCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                }
            }
            Text(text = fare, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = SpeedoTextPrimary))
        }
    }
}

/**
 * Slide 2 Mockup: Live Road-Snapped GPS Map & Driver ETA
 */
@Composable
fun RiderMapTrackingIllustration() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Simulated Map Route Graphic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFE8F5E9), Color(0xFFE0F2F1), Color(0xFFFFF9C4))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SpeedoWhite,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, Color(0xFF00C853))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00C853))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Captain Arriving in 2 mins",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1B5E20)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(20.dp))
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFF3D00), modifier = Modifier.size(36.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Driver Details Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = SpeedoOrange)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Rajesh Kumar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                        Text(text = "Speedo Moto • KA-01-EQ-9876", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF8E1),
                    border = BorderStroke(1.dp, Color(0xFFFFD54F))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "4.9", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Slide 3 Mockup: 4-Digit Security OTP & Instant UPI Settlement
 */
@Composable
fun RiderSecurityOtpIllustration() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "YOUR TRIP START PIN",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = SpeedoTextSecondary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4 OTP Boxes
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("8", "2", "4", "5").forEach { digit ->
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F5E9),
                        border = BorderStroke(2.dp, Color(0xFF00C853))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = digit,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1B5E20)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Share with captain only after boarding",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = SpeedoTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Safety Features Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SecurityFeatureBadge(icon = Icons.Default.Shield, label = "SOS Guard")
                SecurityFeatureBadge(icon = Icons.Default.Chat, label = "Live Chat")
                SecurityFeatureBadge(icon = Icons.Default.QrCode, label = "UPI Pay")
            }
        }
    }
}

@Composable
fun SecurityFeatureBadge(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SpeedoSurfaceVariant,
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}
