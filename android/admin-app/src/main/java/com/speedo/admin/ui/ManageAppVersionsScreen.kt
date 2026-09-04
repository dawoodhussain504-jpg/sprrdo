package com.speedo.admin.ui

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
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.model.AppVersionConfig
import com.speedo.core.theme.*

@Composable
fun ManageAppVersionsScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedAppTab by remember { mutableStateOf("rider") } // "rider", "captain", "admin"
    var showConfirmPublishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchAppVersions()
    }

    // Find current config for selected app or default
    val currentConfig = uiState.appVersions.find { it.appId.equals(selectedAppTab, ignoreCase = true) }
        ?: AppVersionConfig(
            appId = selectedAppTab,
            appName = when (selectedAppTab) {
                "captain" -> "Speedo Captain"
                "admin" -> "Speedo Admin"
                else -> "Speedo Rider"
            }
        )

    var versionCodeInput by remember(currentConfig) { mutableStateOf(currentConfig.latestVersionCode.toString()) }
    var versionNameInput by remember(currentConfig) { mutableStateOf(currentConfig.latestVersionName) }
    var minVersionCodeInput by remember(currentConfig) { mutableStateOf(currentConfig.minSupportedVersionCode.toString()) }
    var forceUpdateInput by remember(currentConfig) { mutableStateOf(currentConfig.forceUpdate) }
    var titleInput by remember(currentConfig) { mutableStateOf(currentConfig.title) }
    var messageInput by remember(currentConfig) { mutableStateOf(currentConfig.message) }
    var releaseNotesInput by remember(currentConfig) { mutableStateOf(currentConfig.releaseNotes ?: "") }
    var updateUrlInput by remember(currentConfig) { mutableStateOf(currentConfig.updateUrl) }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "App Version & OTA Updates",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.fetchAppVersions() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SpeedoTextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Real-Time OTA Broadcast Info Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SpeedoOrange.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, SpeedoOrange.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SpeedoOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Real-Time Over-the-Air Version Engine",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoOrange)
                        )
                        Text(
                            text = "Publish updates or toggle force-update. Active apps on rider/captain phones receive real-time alerts instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpeedoTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Tabs (Rider, Captain, Admin)
            TabRow(
                selectedTabIndex = when (selectedAppTab) {
                    "captain" -> 1
                    "admin" -> 2
                    else -> 0
                },
                containerColor = SpeedoSurfaceVariant,
                contentColor = SpeedoOrange,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedAppTab == "rider",
                    onClick = { selectedAppTab = "rider" },
                    text = { Text("Rider App", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedAppTab == "captain",
                    onClick = { selectedAppTab = "captain" },
                    text = { Text("Captain App", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ElectricRickshaw, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedAppTab == "admin",
                    onClick = { selectedAppTab = "admin" },
                    text = { Text("Admin App", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Live Status Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SpeedoWhite,
                border = BorderStroke(1.dp, SpeedoCardBorder),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE PRODUCTION STATUS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = SpeedoOrange)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentConfig.forceUpdate) SpeedoError.copy(alpha = 0.12f) else SpeedoSuccess.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (currentConfig.forceUpdate) "MANDATORY (FORCED)" else "FLEXIBLE (OPTIONAL)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (currentConfig.forceUpdate) SpeedoError else SpeedoSuccess
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Latest Version", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                            Text(
                                "v${currentConfig.latestVersionName} (Build ${currentConfig.latestVersionCode})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Min Supported Build", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                            Text(
                                "Build ${currentConfig.minSupportedVersionCode}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SpeedoCardBorder.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Current Dialog Title: ${currentConfig.title}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Store / Download URL: ${currentConfig.updateUrl}",
                        style = MaterialTheme.typography.labelSmall.copy(color = SpeedoTextTertiary),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Configuration Form Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SpeedoWhite,
                border = BorderStroke(1.dp, SpeedoCardBorder),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "CONFIGURE & PUBLISH OTA UPDATE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = SpeedoOrange)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Version Code & Name in Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = versionCodeInput,
                            onValueChange = { versionCodeInput = it },
                            label = { Text("Latest Build Code") },
                            placeholder = { Text("e.g. 2") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = versionNameInput,
                            onValueChange = { versionNameInput = it },
                            label = { Text("Latest Version Name") },
                            placeholder = { Text("e.g. 1.0.1") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Min Supported Version Code
                    OutlinedTextField(
                        value = minVersionCodeInput,
                        onValueChange = { minVersionCodeInput = it },
                        label = { Text("Minimum Supported Build Code") },
                        placeholder = { Text("e.g. 1") },
                        supportingText = { Text("Users on build codes strictly less than this will be FORCE-BLOCKED until updated.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Force Update Switch Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (forceUpdateInput) SpeedoError.copy(alpha = 0.08f) else SpeedoSurfaceVariant,
                        border = BorderStroke(1.dp, if (forceUpdateInput) SpeedoError.copy(alpha = 0.3f) else SpeedoCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Force Mandatory Update",
                                    fontWeight = FontWeight.Bold,
                                    color = if (forceUpdateInput) SpeedoError else SpeedoTextPrimary
                                )
                                Text(
                                    text = if (forceUpdateInput) "All users MUST update immediately before accessing any app screen." else "Users can dismiss the dialog and update later.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoTextSecondary
                                )
                            }
                            Switch(
                                checked = forceUpdateInput,
                                onCheckedChange = { forceUpdateInput = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SpeedoWhite,
                                    checkedTrackColor = SpeedoError
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dialog Title
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Update Dialog Title") },
                        placeholder = { Text("New Speedo Update Available 🚀") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Message
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text("Prompt Description / Message") },
                        placeholder = { Text("Please update to enjoy the latest features...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Release Notes
                    OutlinedTextField(
                        value = releaseNotesInput,
                        onValueChange = { releaseNotesInput = it },
                        label = { Text("Release Notes / Changelog (Optional)") },
                        placeholder = { Text("• Feature 1\n• Feature 2\n• Bug fixes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Update / Play Store URL
                    OutlinedTextField(
                        value = updateUrlInput,
                        onValueChange = { updateUrlInput = it },
                        label = { Text("Download / Store URL") },
                        placeholder = { Text("https://play.google.com/store/apps/details?id=...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Button
                    SpeedoPrimaryButton(
                        text = if (uiState.isSubmittingAction) "PUBLISHING..." else "PUBLISH UPDATE TO ALL USERS 🚀",
                        enabled = !uiState.isSubmittingAction && versionCodeInput.isNotBlank(),
                        onClick = { showConfirmPublishDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Confirmation Alert
    if (showConfirmPublishDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmPublishDialog = false },
            icon = { Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(36.dp)) },
            title = { Text("Publish Version Update?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will update the backend database for ${currentConfig.appName} (Build ${versionCodeInput.trim()}), insert an announcement into the user notification center, and immediately broadcast a high-priority system push notification to all installed apps with browser download and install links.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmPublishDialog = false
                        val updated = currentConfig.copy(
                            latestVersionCode = versionCodeInput.trim().toIntOrNull() ?: currentConfig.latestVersionCode,
                            latestVersionName = versionNameInput.trim().ifEmpty { currentConfig.latestVersionName },
                            minSupportedVersionCode = minVersionCodeInput.trim().toIntOrNull() ?: currentConfig.minSupportedVersionCode,
                            forceUpdate = forceUpdateInput,
                            title = titleInput.trim().ifEmpty { currentConfig.title },
                            message = messageInput.trim().ifEmpty { currentConfig.message },
                            releaseNotes = releaseNotesInput.trim().ifEmpty { null },
                            updateUrl = updateUrlInput.trim().ifEmpty { currentConfig.updateUrl }
                        )
                        viewModel.updateAppVersionConfig(selectedAppTab, updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange)
                ) {
                    Text("Yes, Publish Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmPublishDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
