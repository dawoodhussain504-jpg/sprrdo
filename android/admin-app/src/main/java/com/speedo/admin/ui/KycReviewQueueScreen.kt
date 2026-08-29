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
                                    Text(
                                        text = "${captain.phone} • ${captain.vehicleType.uppercase()} (${captain.vehicleNumber})",
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
                        text = "Vehicle: ${capt.vehicleNumber} (${capt.vehicleType.uppercase()})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpeedoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Submitted Documents (Tap to Enlarge):",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Document Preview Cards
                    val docList = listOf(
                        "Vehicle RC" to (capt.avatarUrl ?: "https://picsum.photos/seed/rc/500/300"),
                        "Aadhaar Card" to "https://picsum.photos/seed/aadhaar/500/300",
                        "Live Driver Selfie" to "https://picsum.photos/seed/selfie/500/300",
                        "UPI Payment QR" to (capt.paymentQrUrl ?: "https://picsum.photos/seed/qr/500/300")
                    )

                    docList.forEach { (docTitle, docUrl) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { previewImageUrl = docUrl },
                            shape = RoundedCornerShape(10.dp),
                            color = SpeedoSurfaceVariant,
                            border = BorderStroke(1.dp, SpeedoDivider)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(50.dp)
                                ) {
                                    AsyncImage(
                                        model = docUrl,
                                        contentDescription = docTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = docTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "Tap to view high-res image", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                                }
                                Icon(Icons.Default.ZoomIn, contentDescription = null, tint = SpeedoOrange)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SpeedoTextField(
                        value = reviewRemarks,
                        onValueChange = { reviewRemarks = it },
                        label = "Admin Remarks / Rejection Reason",
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
                            Text("Approve KYC")
                        }

                        Button(
                            onClick = {
                                viewModel.reviewKyc(capt.id, "rejected", reviewRemarks) {
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
