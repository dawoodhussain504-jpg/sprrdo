package com.speedo.captain.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.captain.viewmodel.CaptainViewModel
import com.speedo.core.components.SpeedoOutlinedButton
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.components.StatusBadge
import com.speedo.core.theme.*

@Composable
fun KycStatusScreen(
    viewModel: CaptainViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToUpload: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val kycStatus = uiState.kycStatus
    val status = kycStatus?.kycStatus ?: uiState.captain?.kycStatus ?: "pending"

    LaunchedEffect(Unit) {
        viewModel.fetchKycStatus()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(title = "KYC Verification Status")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status Icon Circle
            val (icon, tintColor, titleText, descText) = when (status) {
                "approved" -> Quadruple(
                    Icons.Default.CheckCircle,
                    SpeedoSuccess,
                    "KYC Verified & Approved! 🎉",
                    "Your documents have been verified by the Speedo admin team. You are now authorized to go ONLINE and accept ride requests."
                )
                "under_review" -> Quadruple(
                    Icons.Default.HourglassTop,
                    SpeedoOrange,
                    "Under Review ⏳",
                    "We have received all your submitted documents. Our verification team is reviewing them. This usually takes 15–30 minutes."
                )
                "rejected" -> Quadruple(
                    Icons.Default.Cancel,
                    SpeedoError,
                    "KYC Verification Rejected ⚠️",
                    "One or more documents could not be verified. Please review the admin remarks below and re-upload clear photos."
                )
                else -> Quadruple(
                    Icons.Default.PendingActions,
                    SpeedoAmber,
                    "Documents Pending Upload 📋",
                    "Please upload all 4 required KYC documents to submit your application for review."
                )
            }

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(tintColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoTextPrimary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = descText,
                style = MaterialTheme.typography.bodyMedium,
                color = SpeedoTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Admin Remarks card if rejected or remarks present
            val remarks = kycStatus?.adminRemarks ?: uiState.captain?.adminRemarks
            if (!remarks.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (status == "rejected") SpeedoErrorContainer else SpeedoSurfaceVariant,
                    border = BorderStroke(1.dp, if (status == "rejected") SpeedoError else SpeedoDivider)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Admin Remarks:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (status == "rejected") SpeedoError else SpeedoTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = remarks,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpeedoTextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Action Buttons
            if (status == "approved") {
                SpeedoPrimaryButton(
                    text = "Go to Captain Dashboard",
                    onClick = onNavigateToDashboard
                )
            } else {
                SpeedoPrimaryButton(
                    text = "Upload / Update Documents",
                    onClick = onNavigateToUpload
                )
                Spacer(modifier = Modifier.height(12.dp))
                SpeedoOutlinedButton(
                    text = "Refresh Status",
                    onClick = { viewModel.fetchKycStatus() }
                )
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
