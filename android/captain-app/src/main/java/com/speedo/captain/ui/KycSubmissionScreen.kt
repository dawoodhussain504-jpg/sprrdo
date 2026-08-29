package com.speedo.captain.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.speedo.captain.viewmodel.CaptainViewModel
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.components.StatusBadge
import com.speedo.core.theme.*
import java.io.File
import java.io.FileOutputStream

@Composable
fun KycSubmissionScreen(
    viewModel: CaptainViewModel,
    onNavigateToStatus: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val kycStatus = uiState.kycStatus
    val docs = kycStatus?.documents ?: emptyList()

    // Helper to create temp image file for camera capture
    fun createTempImageFile(): File {
        val storageDir = context.cacheDir
        return File.createTempFile("kyc_camera_", ".jpg", storageDir)
    }

    var activeUploadingDocType by remember { mutableStateOf<String?>(null) }
    var currentTempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var currentTempFile by remember { mutableStateOf<File?>(null) }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && activeUploadingDocType != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                viewModel.uploadKycDocument(activeUploadingDocType!!, tempFile)
            } catch (e: Exception) {
                // Upload failed
            }
        }
    }

    // Camera Capture launcher (Required for Live Selfie)
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && currentTempFile != null && activeUploadingDocType != null) {
            viewModel.uploadKycDocument(activeUploadingDocType!!, currentTempFile!!)
        }
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(title = "KYC Document Verification")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SpeedoOrangeContainer,
                border = BorderStroke(1.dp, SpeedoOrange)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = SpeedoOrange,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Mandatory Driver Verification",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = SpeedoOnOrangeContainer)
                        )
                        Text(
                            text = "Upload all 4 documents to activate your account and start accepting rides.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpeedoTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Document items list
            val requiredDocuments = listOf(
                Triple("vehicle_reg", "Vehicle Registration (RC)", "Upload a clear photo of your RC book / smart card"),
                Triple("aadhaar", "Aadhaar Card / Govt ID", "Front photo of your Aadhaar or Driving License"),
                Triple("selfie", "Live Driver Selfie", "Take a live photo of your face (Camera required)"),
                Triple("payment_qr", "UPI Payment QR Code", "Upload your personal UPI QR code image to receive rider payments")
            )

            requiredDocuments.forEach { (type, title, desc) ->
                val doc = docs.firstOrNull { it.documentType == type }
                val isUploaded = doc?.isUploaded == true || doc?.fileUrl != null

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = SpeedoWhite,
                    border = BorderStroke(1.dp, if (isUploaded) SpeedoSuccess else SpeedoCardBorder),
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isUploaded) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = if (isUploaded) SpeedoSuccess else SpeedoOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            if (isUploaded) {
                                StatusBadge(status = doc?.status ?: "pending")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = desc, style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)

                        if (doc?.adminRemarks != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Admin Remarks: ${doc.adminRemarks}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = SpeedoError)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (type == "selfie") {
                                // Live Camera capture required for selfie
                                Button(
                                    onClick = {
                                        activeUploadingDocType = type
                                        val tempFile = createTempImageFile()
                                        currentTempFile = tempFile
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            tempFile
                                        )
                                        currentTempPhotoUri = uri
                                        cameraLauncher.launch(uri)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isUploaded) "Re-take Selfie" else "Take Selfie", fontSize = 13.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        activeUploadingDocType = type
                                        galleryLauncher.launch("image/*")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isUploaded) "Change Photo" else "Upload Image", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SpeedoPrimaryButton(
                text = "View KYC Verification Status",
                onClick = onNavigateToStatus
            )
        }
    }
}
