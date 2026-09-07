package com.speedo.rider.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.core.components.SpeedoAboutView
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.theme.*
import com.speedo.rider.viewmodel.RiderViewModel

@Composable
fun RiderProfileScreen(
    viewModel: RiderViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedMainTab by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkDeletionStatus()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(title = if (selectedMainTab == 0) "Profile" else "About Speedo")
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
            // Main Tabs: Profile vs About & Legal
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SpeedoSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val mainTabs = listOf("My Profile", "About & Legal")
                    mainTabs.forEachIndexed { index, title ->
                        val isSelected = selectedMainTab == index
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SpeedoOrange else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedMainTab = index }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (index == 0) Icons.Default.Person else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isSelected) SpeedoWhite else SpeedoTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SpeedoWhite else SpeedoTextSecondary,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (selectedMainTab == 1) {
                SpeedoAboutView(isCaptain = false)
            } else {
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
                            text = "Deletion review period takes 24 hours. Your request is currently under review by Speedo Admin. Once approved, your account and data will be permanently deleted from the database.",
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

            // 2. Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SpeedoOrange),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.currentUserName?.take(1)?.uppercase() ?: "R",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SpeedoWhite
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = uiState.currentUserName ?: "Speedo Rider",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = uiState.currentUserEmail ?: "rider@speedo.com",
                style = MaterialTheme.typography.bodyMedium,
                color = SpeedoTextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Account Details Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SpeedoWhite,
                border = BorderStroke(1.dp, SpeedoCardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ACCOUNT DETAILS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoOrange)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Role", style = MaterialTheme.typography.bodyMedium, color = SpeedoTextSecondary)
                        Text("Speedo Rider", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = SpeedoCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Account Status", style = MaterialTheme.typography.bodyMedium, color = SpeedoTextSecondary)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (uiState.deletionRequest?.status == "pending") Color(0xFFFFF3E0) else SpeedoSuccess.copy(alpha = 0.12f)
                        ) {
                            Text(
                                if (uiState.deletionRequest?.status == "pending") "Deletion Pending (24h)" else "Active",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (uiState.deletionRequest?.status == "pending") Color(0xFFE65100) else SpeedoSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = SpeedoCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("App Version", style = MaterialTheme.typography.bodyMedium, color = SpeedoTextSecondary)
                        Text("v2.0.0 (Production)", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. Log Out Button
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

            // 5. Delete Profile & Account Button
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
                    text = "Delete Profile & Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SpeedoError
                )
            }
        }
    }
}



    // Confirmation Dialog for Requesting Account Deletion (24-Hour Review Notice)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = SpeedoError, modifier = Modifier.size(36.dp)) },
            title = { Text("Request Account Deletion?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "• Deletion review takes 24 hours.\n• Once approved by Speedo Admin, your profile and personal details will be permanently removed from the realtime database.\n• You will lose access to ride history, wallet balances, and saved addresses.\n\nPlease tell us why you are leaving (optional):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        placeholder = { Text("Reason for account deletion...") },
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
            text = { Text("Your account will remain active and will not be deleted by the Admin.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelAccountDeletion()
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange)
                ) {
                    Text("Yes, Keep My Account")
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
