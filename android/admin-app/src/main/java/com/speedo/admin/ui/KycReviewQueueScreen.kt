package com.speedo.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.*
import com.speedo.core.model.Captain
import com.speedo.core.model.KycDocument
import com.speedo.core.theme.*

fun normalizeDocUrl(rawUrl: String?): String? {
    if (rawUrl.isNullOrBlank()) return null
    if (rawUrl.startsWith("http://localhost:5000/")) {
        return rawUrl.replace("http://localhost:5000", "https://web-production-5d826.up.railway.app")
    }
    if (rawUrl.startsWith("http://127.0.0.1:5000/")) {
        return rawUrl.replace("http://127.0.0.1:5000", "https://web-production-5d826.up.railway.app")
    }
    if (rawUrl.startsWith("http://10.0.2.2:5000/")) {
        return rawUrl.replace("http://10.0.2.2:5000", "https://web-production-5d826.up.railway.app")
    }
    if (rawUrl.startsWith("/uploads/")) {
        return "https://web-production-5d826.up.railway.app$rawUrl"
    }
    if (rawUrl.startsWith("uploads/")) {
        return "https://web-production-5d826.up.railway.app/$rawUrl"
    }
    return rawUrl
}

@Composable
fun KycReviewQueueScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val captains = uiState.kycQueue

    var selectedCaptainForReview by remember { mutableStateOf<Captain?>(null) }
    var reviewRemarks by remember { mutableStateOf("") }
    var showApproveConfirm by remember { mutableStateOf(false) }
    var showRejectConfirm by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchKycQueue()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "KYC Verification Queue",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.fetchKycQueue() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SpeedoTextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        if (captains.isEmpty()) {
            SpeedoEmptyView(
                icon = Icons.Default.VerifiedUser,
                title = "Queue is Clear!",
                message = "All captain KYC applications have been reviewed.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(captains) { captain ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SpeedoWhite,
                        border = BorderStroke(1.dp, SpeedoCardBorder),
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = captain.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    val vName = when {
                                        captain.vehicleType.lowercase().contains("toto") || captain.vehicleType.lowercase().contains("auto") -> "Speedo Toto"
                                        captain.vehicleType.lowercase().contains("4") || captain.vehicleType.lowercase().contains("cab") -> "Speedo 4"
                                        else -> "Speedo Moto"
                                    }
                                    Text(
                                        text = "${captain.phone} • $vName (${captain.vehicleNumber})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SpeedoTextSecondary
                                    )
                                }
                                StatusBadge(status = captain.kycStatus)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action Button
                            SpeedoPrimaryButton(
                                text = "Inspect Documents & Review",
                                leadingIcon = Icons.Default.Visibility,
                                onClick = {
                                    selectedCaptainForReview = captain
                                    reviewRemarks = captain.adminRemarks ?: ""
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Detailed Document Inspector & Approval Modal
    if (selectedCaptainForReview != null) {
        val capt = selectedCaptainForReview!!
        val vName = when {
            capt.vehicleType.lowercase().contains("toto") || capt.vehicleType.lowercase().contains("auto") -> "Speedo Toto"
            capt.vehicleType.lowercase().contains("4") || capt.vehicleType.lowercase().contains("cab") -> "Speedo 4"
            else -> "Speedo Moto"
        }

        Dialog(onDismissRequest = { selectedCaptainForReview = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SpeedoWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Review: ${capt.name}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { selectedCaptainForReview = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Text(
                        text = "Vehicle: ${capt.vehicleNumber} ($vName)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpeedoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- AI DOCUMENT OCR & VERIFICATION CARD ---
                    val aiScanResult = uiState.aiScanResults[capt.id]
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (aiScanResult != null) Color(0xFFF0FDF4) else SpeedoSurfaceVariant,
                        border = BorderStroke(1.5.dp, if (aiScanResult != null) SpeedoSuccess else SpeedoDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (aiScanResult != null) SpeedoSuccess else SpeedoOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Speedo AI Document OCR",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (aiScanResult != null) Color(0xFF166534) else SpeedoTextPrimary
                                        )
                                    )
                                }

                                if (aiScanResult != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SpeedoSuccess
                                    ) {
                                        Text(
                                            text = "${aiScanResult.overallScore}% MATCH",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = SpeedoWhite
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (aiScanResult == null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Auto-scan Driving License, RC, and Aadhaar to verify data and check face match in under 5 seconds.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoTextSecondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { viewModel.runAiKycScan(capt.id) },
                                    enabled = !uiState.isAiScanning,
                                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.isAiScanning) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SpeedoWhite, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Analyzing Documents with AI...", style = MaterialTheme.typography.labelMedium)
                                    } else {
                                        Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Run AI Document Scan", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(10.dp))
                                // Extracted OCR Key-Values
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Extracted DL No:", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                                        Text(aiScanResult.dlNumber, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Extracted RC No:", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                                        Text(aiScanResult.rcNumber, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = SpeedoSuccess))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Masked Aadhaar UID:", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                                        Text(aiScanResult.aadhaarMasked, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Face Match Confidence:", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                                        Text("${aiScanResult.faceMatchConfidence}% ✅", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = SpeedoSuccess))
                                    }
                                }

                                if (aiScanResult.isAutoApprovedEligible) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            viewModel.instantApproveKyc(capt.id, "Auto-verified & Approved by Speedo AI Engine") {
                                                selectedCaptainForReview = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoSuccess),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("1-Click Instant AI Approve", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Submitted Documents (Tap to Enlarge):",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Document Preview Cards (Real Uploaded Captain Documents)
                    val rcDoc = capt.documents.firstOrNull { it.documentType == "vehicle_reg" }
                    val aadhaarDoc = capt.documents.firstOrNull { it.documentType == "aadhaar" }
                    val selfieDoc = capt.documents.firstOrNull { it.documentType == "selfie" }
                    val qrDoc = capt.documents.firstOrNull { it.documentType == "payment_qr" }

                    val rcUrl = normalizeDocUrl(rcDoc?.fileUrl ?: capt.avatarUrl)
                    val aadhaarUrl = normalizeDocUrl(aadhaarDoc?.fileUrl)
                    val selfieUrl = normalizeDocUrl(selfieDoc?.fileUrl)
                    val qrUrl = normalizeDocUrl(qrDoc?.fileUrl ?: capt.paymentQrUrl)

                    val docList = listOf(
                        Triple("Vehicle RC", rcUrl, rcDoc?.status ?: if (rcUrl != null) "uploaded" else "missing"),
                        Triple("Aadhaar Card", aadhaarUrl, aadhaarDoc?.status ?: if (aadhaarUrl != null) "uploaded" else "missing"),
                        Triple("Live Driver Selfie", selfieUrl, selfieDoc?.status ?: if (selfieUrl != null) "uploaded" else "missing"),
                        Triple("UPI Payment QR", qrUrl, qrDoc?.status ?: if (qrUrl != null) "uploaded" else "missing")
                    )

                    docList.forEach { (docTitle, docUrl, _) ->
                        val hasDoc = !docUrl.isNullOrBlank()
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .then(if (hasDoc) Modifier.clickable { previewImageUrl = docUrl } else Modifier),
                            shape = RoundedCornerShape(10.dp),
                            color = SpeedoSurfaceVariant,
                            border = BorderStroke(1.dp, if (hasDoc) SpeedoSuccess.copy(alpha = 0.5f) else SpeedoDivider)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (hasDoc) SpeedoWhite else SpeedoDivider,
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    if (hasDoc) {
                                        AsyncImage(
                                            model = docUrl,
                                            contentDescription = docTitle,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HideImage,
                                                contentDescription = null,
                                                tint = SpeedoTextTertiary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = docTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        text = if (hasDoc) "Tap to view uploaded original image" else "Document not uploaded yet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasDoc) SpeedoSuccess else SpeedoTextSecondary
                                    )
                                }
                                if (hasDoc) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = null, tint = SpeedoOrange)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Quick Rejection Reason Chips:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("RC Photo is Blurry", "Driving License Expired", "Name Mismatch with Bank", "Aadhaar Unclear", "Selfie Not Matching")) { reason ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (reviewRemarks == reason) SpeedoError.copy(alpha = 0.15f) else SpeedoSurfaceVariant,
                                border = BorderStroke(1.dp, if (reviewRemarks == reason) SpeedoError else SpeedoDivider),
                                modifier = Modifier.clickable { reviewRemarks = reason }
                            ) {
                                Text(
                                    text = reason,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (reviewRemarks == reason) SpeedoError else SpeedoTextPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SpeedoTextField(
                        value = reviewRemarks,
                        onValueChange = { reviewRemarks = it },
                        label = "Admin Remarks / Custom Reason",
                        placeholder = "e.g. All documents verified / RC photo is blurry",
                        singleLine = false
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                viewModel.reviewKyc(capt.id, "approved", reviewRemarks) {
                                    selectedCaptainForReview = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpeedoSuccess),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manual Approve")
                        }

                        Button(
                            onClick = {
                                viewModel.reviewKyc(capt.id, "rejected", reviewRemarks.ifBlank { "Documents unclear or invalid" }) {
                                    selectedCaptainForReview = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpeedoError),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject KYC")
                        }
                    }
                }
            }
        }
    }

    // Full Size Image Viewer Dialog
    if (previewImageUrl != null) {
        Dialog(onDismissRequest = { previewImageUrl = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SpeedoWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Document Full Viewer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        IconButton(onClick = { previewImageUrl = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = previewImageUrl,
                        contentDescription = "Full Doc",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            }
        }
    }
}
