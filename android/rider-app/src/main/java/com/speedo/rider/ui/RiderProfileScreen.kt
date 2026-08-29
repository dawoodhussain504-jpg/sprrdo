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
import com.speedo.core.components.SpeedoOutlinedButton
import com.speedo.core.components.SpeedoPrimaryButton
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

            // Account & App Details Card
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
                        Text("Status", style = MaterialTheme.typography.bodyMedium, color = SpeedoTextSecondary)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SpeedoSuccess.copy(alpha = 0.12f)
                        ) {
                            Text(
                                "Active",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SpeedoSuccess,
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

            Spacer(modifier = Modifier.height(30.dp))

            SpeedoPrimaryButton(
                text = "Log Out",
                leadingIcon = Icons.Default.ExitToApp,
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
}
