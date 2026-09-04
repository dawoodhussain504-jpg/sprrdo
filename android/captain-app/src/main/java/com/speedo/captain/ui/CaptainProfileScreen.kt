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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.captain.viewmodel.CaptainViewModel
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.theme.*

@Composable
fun CaptainProfileScreen(
    viewModel: CaptainViewModel,
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val captain = uiState.captain

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchProfile()
        viewModel.checkDeletionStatus()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(title = "Captain Profile")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Pending Deletion Warning Banner (24-Hour Review Notice)
            if (uiState.deletionRequest != null && uiState.deletionRequest?.status == "pending") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, Color(0xFFFF9800)),
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Account Deletion Requested",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Deletion review period takes 24 hours. Your request is currently under review by Speedo Admin. Once approved, your driver profile and KYC records will be permanently deleted from the database.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF5D4037))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            border = BorderStroke(1.dp, Color(0xFFE65100)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel Deletion Request", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 2. Avatar & Badge
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E7D32)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = captain?.name?.take(1)?.uppercase() ?: "C",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = SpeedoWhite
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = captain?.name ?: "Speedo Captain",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${captain?.vehicleNumber ?: "KA-01-EQ-9876"} • ${(captain?.vehicleType ?: "BIKE").uppercase()}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SpeedoOrange
                )
            )
            Text(
                text = captain?.email ?: "captain@speedo.com",
                style = MaterialTheme.typography.bodySmall,
                color = SpeedoTextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Driver Highlights Card
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
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rating", style = MaterialTheme.typography.labelSmall, color = SpeedoTextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("4.9", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    VerticalDivider(modifier = Modifier.height(36.dp), color = SpeedoCardBorder)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Trips", style = MaterialTheme.typography.labelSmall, color = SpeedoTextSecondary)
                        Text("${captain?.totalRides ?: 142}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    VerticalDivider(modifier = Modifier.height(36.dp), color = SpeedoCardBorder)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("KYC Status", style = MaterialTheme.typography.labelSmall, color = SpeedoTextSecondary)
                        Text(
                            text = (captain?.kycStatus ?: "APPROVED").uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (captain?.kycStatus == "approved") SpeedoSuccess else Color(0xFFFF9800)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Driver Profile Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SpeedoWhite,
                border = BorderStroke(1.dp, SpeedoCardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "CAPTAIN DETAILS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoOrange)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Phone", style = MaterialTheme.typography.bodyMedium, color = SpeedoTextSecondary)
                        Text(captain?.phone ?: "+91 98765 43210", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = SpeedoCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Vehicle Registration", style = MaterialTheme.typography.bodyMedium, color = SpeedoTextSecondary)
                        Text(captain?.vehicleNumber ?: "KA-01-EQ-9876", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = SpeedoCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Account Status", style = MaterialTheme.typography.bodyMedium, color = SpeedoTextSecondary)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (uiState.deletionRequest?.status == "pending") Color(0xFFFFF3E0) else SpeedoSuccess.copy(alpha = 0.12f)
                        ) {
                            Text(
                                if (uiState.deletionRequest?.status == "pending") "Deletion Pending (24h)" else "Verified Partner",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (uiState.deletionRequest?.status == "pending") Color(0xFFE65100) else SpeedoSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. Log Out Button
            SpeedoPrimaryButton(
                text = "Log Out",
                leadingIcon = Icons.Default.ExitToApp,
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpeedoSurfaceVariant,
                    contentColor = SpeedoTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Delete Captain Profile & Account Button
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SpeedoError.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SpeedoError)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = SpeedoError, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delete Captain Profile & Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SpeedoError
                )
            }
        }
    }

    // Confirmation Dialog for Requesting Account Deletion (24-Hour Review Notice)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = SpeedoError, modifier = Modifier.size(36.dp)) },
            title = { Text("Request Driver Account Deletion?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "• Deletion review takes 24 hours.\n• Once approved by Speedo Admin, your driver account, vehicle registration, KYC records, and earnings profile will be permanently deleted from the database.\n• You will not be able to accept rides or recover past earnings statements.\n\nPlease state your reason for leaving (optional):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        placeholder = { Text("Reason for deleting driver account...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.requestAccountDeletion(deleteReason)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoError)
                ) {
                    Text("Submit Deletion (24h Review)")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Keep Account")
                }
            }
        )
    }

    // Cancel Deletion Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Deletion Request?") },
            text = { Text("Your captain account will remain active and verified. Admin will not delete your account.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelAccountDeletion()
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange)
                ) {
                    Text("Yes, Keep My Driver Account")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}
