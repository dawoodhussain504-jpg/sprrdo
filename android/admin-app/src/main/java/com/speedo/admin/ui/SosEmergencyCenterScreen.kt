package com.speedo.admin.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.*
import com.speedo.core.model.SosAlert
import com.speedo.core.theme.*

@Composable
fun SosEmergencyCenterScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val alerts = uiState.sosAlerts
    val activeCount = uiState.activeSosCount
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf("active") }
    var selectedAlertForResolve by remember { mutableStateOf<SosAlert?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchSosAlerts()
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val filteredAlerts = remember(alerts, selectedTab) {
        when (selectedTab) {
            "active" -> alerts.filter { it.status == "active" || it.status == "in_progress" }
            "resolved" -> alerts.filter { it.status == "resolved" || it.status == "false_alarm" }
            else -> alerts
        }
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "SOS Emergency Center",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.fetchSosAlerts() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SpeedoTextPrimary)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (activeCount > 0) {
                Surface(
                    color = SpeedoError,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(SpeedoWhite)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🚨 " + activeCount + " ACTIVE EMERGENCY ALERT" + (if (activeCount > 1) "S" else ""),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SpeedoWhite
                                )
                            )
                            Text(
                                text = "Immediate response and passenger verification required.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpeedoWhite.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "active" to "Active (" + alerts.count { it.status == "active" || it.status == "in_progress" } + ")",
                    "resolved" to "Resolved (" + alerts.count { it.status == "resolved" || it.status == "false_alarm" } + ")",
                    "all" to "All History (" + alerts.size + ")"
                ).forEach { (tabKey, tabLabel) ->
                    val isSelected = selectedTab == tabKey
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) (if (tabKey == "active" && activeCount > 0) SpeedoError else SpeedoOrange) else SpeedoSurfaceVariant,
                        modifier = Modifier.clickable { selectedTab = tabKey }
                    ) {
                        Text(
                            text = tabLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) SpeedoWhite else SpeedoTextPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (filteredAlerts.isEmpty()) {
                SpeedoEmptyView(
                    icon = Icons.Default.Shield,
                    title = "No Emergencies",
                    message = if (selectedTab == "active") "All emergency incidents have been attended to and resolved." else "No incident logs found."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAlerts) { alert ->
                        SosAlertCard(
                            alert = alert,
                            isSubmitting = uiState.isSubmittingAction,
                            onCallPassenger = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + alert.userPhone))
                                context.startActivity(intent)
                            },
                            onCallCaptain = {
                                if (!alert.captainPhone.isNullOrBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + alert.captainPhone))
                                    context.startActivity(intent)
                                }
                            },
                            onDispatchPolice = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                context.startActivity(intent)
                            },
                            onQuickResolve = {
                                viewModel.resolveSosAlert(
                                    id = alert.id,
                                    status = "resolved",
                                    notes = "Quick resolved by Admin: Passenger confirmed safe."
                                )
                            },
                            onResolveCustom = {
                                selectedAlertForResolve = alert
                            }
                        )
                    }
                }
            }
        }
    }

    if (selectedAlertForResolve != null) {
        val alert = selectedAlertForResolve!!
        ResolveSosDialog(
            alert = alert,
            isSubmitting = uiState.isSubmittingAction,
            onDismiss = { selectedAlertForResolve = null },
            onConfirmResolve = { status, notes ->
                viewModel.resolveSosAlert(
                    id = alert.id,
                    status = status,
                    notes = notes
                ) {
                    selectedAlertForResolve = null
                }
            }
        )
    }
}

@Composable
fun SosAlertCard(
    alert: SosAlert,
    isSubmitting: Boolean = false,
    onCallPassenger: () -> Unit,
    onCallCaptain: () -> Unit,
    onDispatchPolice: () -> Unit,
    onQuickResolve: () -> Unit,
    onResolveCustom: () -> Unit
) {
    val isActive = alert.status == "active" || alert.status == "in_progress"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.5.dp, if (isActive) SpeedoError else SpeedoDivider),
        shadowElevation = if (isActive) 6.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isActive) SpeedoError.copy(alpha = 0.15f) else SpeedoSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isActive) SpeedoError else SpeedoTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SOS by " + alert.userName + " (" + alert.triggeredBy.uppercase() + ")",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = alert.userPhone + " • " + (alert.createdAt ?: "Just Now"),
                            style = MaterialTheme.typography.bodySmall,
                            color = SpeedoTextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) SpeedoError else SpeedoSuccess
                ) {
                    Text(
                        text = alert.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = SpeedoWhite
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SpeedoSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (!alert.vehicleNumber.isNullOrBlank()) {
                        Text(
                            text = "Assigned Vehicle: " + alert.vehicleNumber + (if (!alert.captainName.isNullOrBlank()) " (" + alert.captainName + ")" else ""),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "Live Location: " + (alert.address ?: ("GPS: " + alert.lat + ", " + alert.lng)),
                        style = MaterialTheme.typography.bodySmall,
                        color = SpeedoTextSecondary
                    )
                    if (!alert.adminNotes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Resolution Notes: " + alert.adminNotes,
                            style = MaterialTheme.typography.bodySmall.copy(color = SpeedoOrange, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCallPassenger,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call User", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                if (!alert.captainPhone.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onCallCaptain,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call Driver", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Button(
                    onClick = onDispatchPolice,
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoError),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.LocalPolice, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dial 112", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold))
                }
            }

            if (isActive) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onQuickResolve,
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoSuccess),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resolve & Close Incident", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    OutlinedButton(
                        onClick = onResolveCustom,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Action Log", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun ResolveSosDialog(
    alert: SosAlert,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmResolve: (String, String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf("resolved") }
    var notes by remember { mutableStateOf("Passenger called, confirmed safe at location.") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SpeedoWhite,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Resolve SOS Incident",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Incident for " + alert.userName + " (" + alert.userPhone + ")",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpeedoTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Select Incident Outcome:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "resolved" to "Passenger Safe ✅",
                        "false_alarm" to "False Alarm ⚠️",
                        "in_progress" to "Police Dispatched 🚔"
                    ).forEach { (stKey, stLabel) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedStatus == stKey) SpeedoOrange else SpeedoSurfaceVariant,
                            modifier = Modifier.clickable {
                                selectedStatus = stKey
                                if (stKey == "resolved") notes = "Passenger called, confirmed safe at destination."
                                else if (stKey == "false_alarm") notes = "User triggered SOS by mistake, confirmed no emergency."
                                else if (stKey == "in_progress") notes = "Police emergency control (112) dispatched to coordinates."
                            }
                        ) {
                            Text(
                                text = stLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedStatus == stKey) SpeedoWhite else SpeedoTextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Quick Action Presets:", style = MaterialTheme.typography.labelSmall.copy(color = SpeedoTextSecondary))
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Passenger Safe & Destination Reached ✅" to "resolved",
                        "False Alarm / User Accidental Click ⚠️" to "false_alarm",
                        "Police Dispatched & Actively Assisting 🚔" to "in_progress",
                        "Driver Reached Passenger & Assisted 🛵" to "resolved"
                    ).forEach { (presetText, presetStatus) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SpeedoSurfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, SpeedoDivider),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    notes = presetText
                                    selectedStatus = presetStatus
                                }
                        ) {
                            Text(
                                text = presetText,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SpeedoTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Action Log / Resolution Notes",
                    placeholder = "Enter resolution details...",
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val finalNotes = if (notes.isBlank()) "Resolved by Admin" else notes
                            onConfirmResolve(selectedStatus, finalNotes)
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoSuccess),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = SpeedoWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resolve & Close", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}
