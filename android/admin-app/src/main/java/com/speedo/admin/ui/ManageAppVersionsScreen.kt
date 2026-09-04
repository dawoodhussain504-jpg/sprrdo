package com.speedo.admin.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.model.AppVersionConfig
import com.speedo.core.theme.*

private fun computeNextSequentialCode(currentCode: Int): Int = (currentCode + 1).coerceAtLeast(1)

private fun computeNextVersionName(currentName: String, bumpType: String = "patch"): String {
    val clean = currentName.trim().removePrefix("v").ifEmpty { "1.0.0" }
    val parts = clean.split(".").mapNotNull { it.toIntOrNull() }.toMutableList()
    while (parts.size < 3) {
        parts.add(0)
    }
    when (bumpType.lowercase()) {
        "major" -> {
            parts[0] += 1
            parts[1] = 0
            parts[2] = 0
        }
        "minor" -> {
            parts[1] += 1
            parts[2] = 0
        }
        else -> { // "patch"
            parts[2] += 1
        }
    }
    return parts.joinToString(".")
}

@OptIn(ExperimentalLayoutApi::class)
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

    val nextSequentialCode = remember(currentConfig) {
        computeNextSequentialCode(currentConfig.latestVersionCode)
    }
    val defaultVersionName = remember(currentConfig) {
        computeNextVersionName(currentConfig.latestVersionName, "patch")
    }
    val defaultUpdateUrl = remember(currentConfig, selectedAppTab) {
        if (currentConfig.updateUrl.isNotBlank() && !currentConfig.updateUrl.contains("play.google.com")) {
            currentConfig.updateUrl
        } else {
            "https://web-production-5d826.up.railway.app/downloads/speedo-${selectedAppTab}.apk"
        }
    }
    val defaultTitle = remember(currentConfig) {
        "Speedo ${currentConfig.appName} Update Available 🚀"
    }
    val defaultMessage = remember(currentConfig, defaultVersionName, nextSequentialCode) {
        "Version $defaultVersionName (Build $nextSequentialCode) is now available. Update now to enjoy latest performance improvements."
    }

    // Auto-filled inputs based on next sequential calculation
    var versionCodeInput by remember(currentConfig) { mutableStateOf(nextSequentialCode.toString()) }
    var versionNameInput by remember(currentConfig) { mutableStateOf(defaultVersionName) }
    var minVersionCodeInput by remember(currentConfig) { mutableStateOf(currentConfig.latestVersionCode.toString()) }
    var forceUpdateInput by remember(currentConfig) { mutableStateOf(currentConfig.forceUpdate) }
    var titleInput by remember(currentConfig) { mutableStateOf(defaultTitle) }
    var messageInput by remember(currentConfig) { mutableStateOf(defaultMessage) }
    var releaseNotesInput by remember(currentConfig) { mutableStateOf(currentConfig.releaseNotes ?: "• Performance enhancements & bug fixes\n• Faster ride matching & live updates") }
    var updateUrlInput by remember(currentConfig) { mutableStateOf(defaultUpdateUrl) }

    // Sequential Validation
    val parsedCode = versionCodeInput.trim().toIntOrNull()
    val isSequentialValid = parsedCode != null && parsedCode > currentConfig.latestVersionCode
    val isExactNext = parsedCode == nextSequentialCode
    val isDowngradeOrDuplicate = parsedCode != null && parsedCode <= currentConfig.latestVersionCode

    val parsedMinCode = minVersionCodeInput.trim().toIntOrNull() ?: 1
    val isMinCodeValid = parsedCode == null || parsedMinCode <= parsedCode

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
                            text = "Sequential Over-the-Air Update Engine",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoOrange)
                        )
                        Text(
                            text = "Versions are published strictly sequentially. User apps automatically receive clickable update badges and prompts.",
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
                            text = "CURRENT LIVE PRODUCTION STATUS",
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
                            Text("Current Live Version", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                            Text(
                                "v${currentConfig.latestVersionName} (Build #${currentConfig.latestVersionCode})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Min Supported Build", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                            Text(
                                "Build #${currentConfig.minSupportedVersionCode}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = SpeedoCardBorder.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Next Expected Sequential Build: #${nextSequentialCode}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = SpeedoOrange)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Download URL: ${currentConfig.updateUrl}",
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONFIGURE SEQUENTIAL OTA UPDATE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = SpeedoOrange)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SpeedoOrange.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "AUTO-FILLED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoOrange),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Auto-Fill Chips
                    Text(
                        text = "Quick Version Presets (Auto-Increment):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = SpeedoTextSecondary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                versionCodeInput = nextSequentialCode.toString()
                                versionNameInput = computeNextVersionName(currentConfig.latestVersionName, "patch")
                                titleInput = "Speedo ${currentConfig.appName} Update Available 🚀"
                                messageInput = "Version $versionNameInput (Build $versionCodeInput) is now available. Update now to enjoy latest performance improvements."
                            },
                            label = { Text("+1 Patch (v${computeNextVersionName(currentConfig.latestVersionName, "patch")})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )

                        SuggestionChip(
                            onClick = {
                                versionCodeInput = nextSequentialCode.toString()
                                versionNameInput = computeNextVersionName(currentConfig.latestVersionName, "minor")
                                titleInput = "Speedo ${currentConfig.appName} Update Available 🚀"
                                messageInput = "Version $versionNameInput (Build $versionCodeInput) is now available. Update now to enjoy latest performance improvements."
                            },
                            label = { Text("+1 Minor (v${computeNextVersionName(currentConfig.latestVersionName, "minor")})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )

                        SuggestionChip(
                            onClick = {
                                versionCodeInput = nextSequentialCode.toString()
                                versionNameInput = computeNextVersionName(currentConfig.latestVersionName, "major")
                                titleInput = "Speedo ${currentConfig.appName} Major Upgrade 🚀"
                                messageInput = "Version $versionNameInput (Build $versionCodeInput) is now available. Update now to enjoy latest features."
                            },
                            label = { Text("+1 Major (v${computeNextVersionName(currentConfig.latestVersionName, "major")})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )

                        SuggestionChip(
                            onClick = {
                                versionCodeInput = nextSequentialCode.toString()
                                versionNameInput = defaultVersionName
                                minVersionCodeInput = currentConfig.latestVersionCode.toString()
                                updateUrlInput = defaultUpdateUrl
                                titleInput = defaultTitle
                                messageInput = defaultMessage
                            },
                            label = { Text("⚡ Reset Sequential", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SpeedoOrange) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Version Code & Name in Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = versionCodeInput,
                            onValueChange = { versionCodeInput = it },
                            label = { Text("Build Code (Serial)") },
                            placeholder = { Text("e.g. $nextSequentialCode") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            isError = isDowngradeOrDuplicate || parsedCode == null
                        )
                        OutlinedTextField(
                            value = versionNameInput,
                            onValueChange = { versionNameInput = it },
                            label = { Text("Version Name") },
                            placeholder = { Text("e.g. $defaultVersionName") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sequential Status Banner
                    when {
                        isDowngradeOrDuplicate -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SpeedoError.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, SpeedoError.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = SpeedoError, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Non-Sequential: Current live build is #${currentConfig.latestVersionCode}. You must enter a strictly higher sequential build (expected: #$nextSequentialCode).",
                                        style = MaterialTheme.typography.labelSmall.copy(color = SpeedoError, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                        isExactNext -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SpeedoSuccess.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, SpeedoSuccess.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpeedoSuccess, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "✓ Valid Sequential Build: #$parsedCode (Next after live #${currentConfig.latestVersionCode})",
                                        style = MaterialTheme.typography.labelSmall.copy(color = SpeedoSuccess, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                        parsedCode != null && parsedCode > nextSequentialCode -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SpeedoOrange.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, SpeedoOrange.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Notice: Skipping build #${nextSequentialCode} to #$parsedCode. (Sequential minimum is #${nextSequentialCode})",
                                        style = MaterialTheme.typography.labelSmall.copy(color = SpeedoOrange, fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Min Supported Version Code
                    OutlinedTextField(
                        value = minVersionCodeInput,
                        onValueChange = { minVersionCodeInput = it },
                        label = { Text("Minimum Supported Build Code") },
                        placeholder = { Text("e.g. ${currentConfig.latestVersionCode}") },
                        supportingText = {
                            Text(
                                if (!isMinCodeValid) "Error: Minimum build cannot be greater than latest build (#$parsedCode)"
                                else "Users on build codes strictly less than this will be FORCE-BLOCKED until updated."
                            )
                        },
                        isError = !isMinCodeValid,
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
                                    text = if (forceUpdateInput) "All users MUST update immediately before accessing any app screen." else "Users can dismiss dialog and update later.",
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
                        placeholder = { Text("https://web-production-5d826.up.railway.app/downloads/speedo-rider.apk") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Button
                    SpeedoPrimaryButton(
                        text = if (uiState.isSubmittingAction) "PUBLISHING..." else "PUBLISH UPDATE TO ALL USERS 🚀",
                        enabled = !uiState.isSubmittingAction && isSequentialValid && isMinCodeValid && versionNameInput.isNotBlank(),
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
            title = { Text("Publish Sequential Version Update?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will update ${currentConfig.appName} to sequential build #${versionCodeInput.trim()} (v${versionNameInput.trim()}). It will automatically notify all installed apps and provide direct browser download & installation.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmPublishDialog = false
                        val updated = currentConfig.copy(
                            latestVersionCode = versionCodeInput.trim().toIntOrNull() ?: nextSequentialCode,
                            latestVersionName = versionNameInput.trim().ifEmpty { defaultVersionName },
                            minSupportedVersionCode = minVersionCodeInput.trim().toIntOrNull() ?: currentConfig.latestVersionCode,
                            forceUpdate = forceUpdateInput,
                            title = titleInput.trim().ifEmpty { defaultTitle },
                            message = messageInput.trim().ifEmpty { defaultMessage },
                            releaseNotes = releaseNotesInput.trim().ifEmpty { null },
                            updateUrl = updateUrlInput.trim().ifEmpty { defaultUpdateUrl }
                        )
                        viewModel.updateAppVersionConfig(selectedAppTab, updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange)
                ) {
                    Text("Yes, Publish Sequentially", fontWeight = FontWeight.Bold)
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
