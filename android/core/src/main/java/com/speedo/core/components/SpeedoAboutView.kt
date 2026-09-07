package com.speedo.core.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.core.theme.*

/**
 * Professional Speedo About, Terms of Service, and Privacy Policy View.
 * Displayed inside User Profile (Rider and Captain apps).
 */
@Composable
fun SpeedoAboutView(
    modifier: Modifier = Modifier,
    isCaptain: Boolean = false
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("About Speedo", "Terms of Service", "Privacy Policy")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Sub-Tab Pill Selector
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SpeedoSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedSubTab == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SpeedoOrange else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedSubTab = index }
                    ) {
                        Text(
                            text = title,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.5.sp,
                                color = if (isSelected) SpeedoWhite else SpeedoTextSecondary
                            ),
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        AnimatedContent(
            targetState = selectedSubTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "AboutTabContent"
        ) { tabIndex ->
            when (tabIndex) {
                0 -> AboutSpeedoSection(isCaptain = isCaptain)
                1 -> TermsAndConditionsSection(isCaptain = isCaptain)
                2 -> PrivacyPolicySection()
            }
        }
    }
}

@Composable
private fun AboutSpeedoSection(isCaptain: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Brand Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SpeedoOrangeContainer.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, SpeedoOrange.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SpeedoOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Speedo",
                        tint = SpeedoWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "SPEEDO MOBILITY",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = SpeedoOrange
                    )
                )

                Text(
                    text = "India's Fastest, Smartest & Fair Ride Platform",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SpeedoTextPrimary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Speedo is built to revolutionize intra-city and regional transportation across India's Tier-1, Tier-2, and Tier-3 cities. We connect passengers with verified drivers through cutting-edge technology, transparent pricing, and instant real-time dispatch.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SpeedoTextSecondary,
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Core Pillars
        Text(
            text = "WHY CHOOSE SPEEDO",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = SpeedoOrange,
                letterSpacing = 0.5.sp
            )
        )

        FeatureHighlightCard(
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            iconTint = Color(0xFF2E7D32),
            title = "Zero Surge Price Exploitation",
            description = "We calculate rides strictly on actual distance, travel duration, and fuel benchmarks. No artificial 2x or 3x surge multipliers during rain or rush hours."
        )

        FeatureHighlightCard(
            icon = Icons.Default.Shield,
            iconTint = Color(0xFF1976D2),
            title = "Passenger & Driver Safety First",
            description = "Live GPS tracking, 4-digit ride start OTP verification, verified KYC documentation, and 24/7 one-touch emergency SOS dispatcher for peace of mind."
        )

        FeatureHighlightCard(
            icon = Icons.Default.Mic,
            iconTint = Color(0xFFE65100),
            title = "Speech-to-Ride Multilingual AI",
            description = "Book rides seamlessly in your mother tongue. Speak naturally in Hindi or English ('Bhaiya, Station jana hai auto se') without tedious typing."
        )

        if (isCaptain) {
            FeatureHighlightCard(
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = Color(0xFF00796B),
                title = "Instant UPI Settlements for Captains",
                description = "Captains keep maximum take-home earnings with zero hidden commissions and instant UPI bank payouts after each trip."
            )
        } else {
            FeatureHighlightCard(
                icon = Icons.Default.ElectricRickshaw,
                iconTint = Color(0xFF6A1B9A),
                title = "Multi-Modal Fleet Availability",
                description = "Choose between cost-effective Bike Taxis, comfortable Auto Rickshaws, and spacious Cabs tailored for solo commutes or family travel."
            )
        }

        // Company Details Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SpeedoWhite,
            border = BorderStroke(1.dp, SpeedoCardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "COMPANY & SUPPORT INFORMATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SpeedoOrange
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(label = "Entity", value = "Speedo Mobility Technologies Pvt. Ltd.")
                InfoRow(label = "Headquarters", value = "India")
                InfoRow(label = "Customer Care", value = "support@speedo.com")
                InfoRow(label = "Grievance Officer", value = "grievance@speedo.com")
                InfoRow(label = "Platform Version", value = "v1.0.11 (Production)")
                InfoRow(label = "Infrastructure", value = "Enterprise Cloud & Encrypted WebSockets")
            }
        }
    }
}

@Composable
private fun TermsAndConditionsSection(isCaptain: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF5F5F5)
        ) {
            Text(
                text = "Last Updated: September 2026 • Please read these terms carefully before utilizing Speedo services.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SpeedoTextSecondary,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(12.dp)
            )
        }

        LegalClauseCard(
            clauseNumber = "1",
            title = "Acceptance of Terms",
            content = "By downloading, registering, or accessing the Speedo mobile application ('Platform'), you enter into a legally binding contract with Speedo Mobility Technologies Pvt. Ltd. If you do not agree with any provision of these terms, you must discontinue using our services immediately."
        )

        LegalClauseCard(
            clauseNumber = "2",
            title = "User Eligibility & Account Security",
            content = "You must be at least 18 years of age to register an account and book rides. You are solely responsible for maintaining the confidentiality of your credentials and One-Time Passwords (OTPs). Any activity conducted through your authenticated account is presumed to be authorized by you."
        )

        LegalClauseCard(
            clauseNumber = "3",
            title = "Ride Bookings & Fare Calculation",
            content = "Fares are calculated based on travel distance, trip duration, vehicle category (Bike, Auto, Cab), and applicable toll/statutory surcharges. All prices are denominated in Indian Rupees (₹). Accepted payment methods include direct Cash to Captain, In-App UPI, and digital payments. In cash rides, the rider agrees to settle the complete fare immediately upon trip arrival."
        )

        LegalClauseCard(
            clauseNumber = "4",
            title = "Cancellation & Waiting Guidelines",
            content = "Riders may cancel a ride without fee within 2 minutes of Captain assignment. Beyond this window, or if the Captain arrives at the pickup point and waits exceeding 5 minutes, a standard nominal cancellation charge may apply to reimburse driver fuel expenditure."
        )

        LegalClauseCard(
            clauseNumber = "5",
            title = "Safety & Code of Conduct",
            content = "Speedo enforces a strict Zero Tolerance Policy against harassment, verbal abuse, intoxication, physical violence, and discrimination based on gender, religion, caste, or disability. Transport of illegal narcotics, hazardous substances, or unlicensed weapons is strictly prohibited and results in immediate permanent ban and police reporting."
        )

        if (isCaptain) {
            LegalClauseCard(
                clauseNumber = "6",
                title = "Captain Partner Obligations & Compliance",
                content = "Captains must possess a valid Indian Driving Licence, active vehicle commercial registration (RC), valid insurance, and pollution certificate (PUC). Captains agree to strictly follow all Indian Motor Vehicle Act regulations, wear helmets/seatbelts, and verify passenger OTP before commencing any ride."
            )
        }

        LegalClauseCard(
            clauseNumber = if (isCaptain) "7" else "6",
            title = "Limitation of Platform Liability",
            content = "Speedo operates as an electronic technology marketplace intermediary connecting passengers with independent commercial vehicle operators. While Speedo enforces rigorous driver KYC background verification, Speedo shall not be liable for unforeseen delays caused by traffic congestion, natural calamities, or third-party negligence."
        )

        LegalClauseCard(
            clauseNumber = if (isCaptain) "8" else "7",
            title = "Governing Law & Jurisdiction",
            content = "These Terms are governed by and construed in accordance with the laws of India. Any disputes arising out of or in connection with the Platform shall be subject to the exclusive jurisdiction of the competent courts in India."
        )
    }
}

@Composable
private fun PrivacyPolicySection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFE8F5E9)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Compliant with Digital Personal Data Protection Act (DPDP Act) & Google Play Policies.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF1B5E20),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        LegalClauseCard(
            clauseNumber = "1",
            title = "Information We Collect",
            content = "We collect necessary information to provide seamless ride-hailing services:\n• Identity Data: Full Name, mobile phone number, email address.\n• Trip Data: Pickup location, destination coordinates, timestamps, and fare details.\n• Device Telemetry: Hardware model, OS version, IP address, and crash logs for diagnostics."
        )

        LegalClauseCard(
            clauseNumber = "2",
            title = "Location Data Usage & Disclosure",
            content = "High-precision GPS location is gathered in both foreground and active ride sessions to:\n1. Match riders with the closest available Captain in real time.\n2. Calculate exact driving routes, turn-by-turn navigation, and distance-based fares.\n3. Power live trip monitoring and the 24/7 Emergency SOS feature.\nLocation tracking ceases automatically upon completion or cancellation of a trip."
        )

        LegalClauseCard(
            clauseNumber = "3",
            title = "Data Security & Encryption",
            content = "Speedo employs enterprise-grade cryptographic security measures. All communications between your device and our servers are encrypted in transit via TLS 1.3. Stored database records are protected with AES-256 encryption at rest with strict role-based access restrictions."
        )

        LegalClauseCard(
            clauseNumber = "4",
            title = "Third-Party Data Non-Disclosure",
            content = "Speedo does NOT sell, monetize, rent, or trade your personal information to advertisers or external data brokers. Limited data (e.g. pickup pin and passenger first name) is shared only with the assigned Captain for the sole purpose of trip fulfillment, or with lawful authorities when subpoenaed under Indian law."
        )

        LegalClauseCard(
            clauseNumber = "5",
            title = "Data Retention & Right to Erasure",
            content = "You retain full ownership of your personal data. In compliance with Google Play Store User Data policies, you may request permanent deletion of your account and records at any time directly through the 'Delete Profile & Account' option in the Profile tab. Once reviewed (24 hours), all identifiable records are permanently purged from active databases."
        )

        LegalClauseCard(
            clauseNumber = "6",
            title = "Grievance Redressal & Inquiries",
            content = "For any data privacy questions, consent withdrawal, or redressal requests, you may contact our appointed Data Protection & Grievance Officer:\n\nEmail: grievance@speedo.com\nSpeedo Mobility Technologies Pvt. Ltd.\nResponse Time: Within 48 business hours."
        )
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SpeedoTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SpeedoTextSecondary,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun LegalClauseCard(
    clauseNumber: String,
    title: String,
    content: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SpeedoOrangeContainer
                ) {
                    Text(
                        text = clauseNumber,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = SpeedoOrange
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SpeedoTextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SpeedoTextSecondary,
                    lineHeight = 19.sp
                )
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = SpeedoTextSecondary)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = SpeedoTextPrimary
            )
        )
    }
}
