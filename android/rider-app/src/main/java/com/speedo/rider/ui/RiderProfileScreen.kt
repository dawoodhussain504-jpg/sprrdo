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
import androidx.compose.material.icons.automirrored.filled.*
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
import com.speedo.core.components.SpeedoOutlinedButton
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.components.SpeedoSupportChatSheet
import com.speedo.core.components.SpeedoTextField
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.theme.*
import com.speedo.core.utils.Constants
import com.speedo.rider.viewmodel.RiderViewModel

@Composable
fun RiderProfileScreen(
    viewModel: RiderViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var customApiUrl by remember { mutableStateOf(Constants.getBaseUrl(context)) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showSupportSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SpeedoTopBar(title = "Profile")
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
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SpeedoOrangeContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = SpeedoOrange,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = uiState.currentUserName ?: "Speedo Rider",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = uiState.currentUserEmail ?: "rider@speedo.com",
                style = MaterialTheme.typography.bodyMedium,
                color = SpeedoTextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 24/7 Support & Help Desk card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSupportSheet = true },
                shape = RoundedCornerShape(14.dp),
                color = SpeedoWhite,
                border = BorderStroke(1.dp, Color(0xFF90CAF9)),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = Color(0xFF1565C0),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Speedo 24/7 Support Desk",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SpeedoTextPrimary
                        )
                        Text(
                            text = "Raise complaints, fare issues, or safety queries",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpeedoTextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = SpeedoTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings options
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SpeedoWhite,
                border = BorderStroke(1.dp, SpeedoCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NETWORK & SERVER",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoOrange)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "API Base URL: ${Constants.getBaseUrl(context)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SpeedoTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SpeedoOutlinedButton(
                        text = "Change API URL",
                        onClick = { showUrlDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            SpeedoPrimaryButton(
                text = "Log Out",
                leadingIcon = Icons.Default.Logout,
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpeedoError,
                    contentColor = SpeedoWhite
                )
            )
        }
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Configure API Server URL") },
            text = {
                Column {
                    Text(
                        text = "Enter the host backend URL (e.g. Railway URL or Local IP):",
                        style = MaterialTheme.typography.bodySmall,
                        color = SpeedoTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SpeedoTextField(
                        value = customApiUrl,
                        onValueChange = { customApiUrl = it },
                        label = "API Base URL"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Constants.setCustomBaseUrl(context, customApiUrl)
                        showUrlDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange)
                ) {
                    Text("Save & Apply", color = SpeedoWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Cancel", color = SpeedoTextPrimary)
                }
            }
        )
    }

    if (showSupportSheet) {
        SpeedoSupportChatSheet(
            userRole = "rider",
            onDismiss = { showSupportSheet = false }
        )
    }
}
