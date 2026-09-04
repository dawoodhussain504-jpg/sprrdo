package com.speedo.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.model.AccountDeletionRequest
import com.speedo.core.theme.*

@Composable
fun AccountDeletionRequestsScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val requests = uiState.deletionRequests

    var selectedTab by remember { mutableStateOf("Pending") }
    var requestToApprove by remember { mutableStateOf<AccountDeletionRequest?>(null) }
    var requestToReject by remember { mutableStateOf<AccountDeletionRequest?>(null) }
    var adminNotesInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchDeletionRequests()
    }

    val filterOptions = listOf("Pending", "Approved", "Rejected", "All")

    val filteredList = remember(requests, selectedTab) {
        requests.filter { req ->
            when (selectedTab) {
                "Pending" -> req.status == "pending"
                "Approved" -> req.status == "approved"
                "Rejected" -> req.status == "rejected"
                else -> true
            }
        }
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "Account Deletion Center",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.fetchDeletionRequests() }) {
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
        ) {
            // Real-Time Purge Notice Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFEBEE),
                border = BorderStroke(1.dp, Color(0xFFFFCDD2))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SecurityUpdateWarning,
                        contentDescription = null,
                        tint = SpeedoError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "24-Hour Review Window • Approved deletions permanently purge user and driver records from realtime database",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SpeedoError,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Filter Tabs
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { tab ->
                    val isSelected = selectedTab == tab
                    val count = when (tab) {
                        "Pending" -> requests.count { it.status == "pending" }
                        "Approved" -> requests.count { it.status == "approved" }
                        "Rejected" -> requests.count { it.status == "rejected" }
                        else -> requests.size
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                text = "$tab ($count)",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (tab == "Pending") SpeedoError else SpeedoOrange,
                            selectedLabelColor = SpeedoWhite
                        )
                    )
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = SpeedoSuccess,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No $selectedTab Deletion Requests",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SpeedoTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredList, key = { it.id }) { req ->
                        DeletionRequestCard(
                            request = req,
                            onApprove = {
                                adminNotesInput = "Approved and account permanently purged from database."
                                requestToApprove = req
                            },
                            onReject = {
                                adminNotesInput = "Deletion request reviewed and rejected."
                                requestToReject = req
                            }
                        )
                    }
                }
            }
        }
    }

    // Approve & Purge Confirmation Dialog
    requestToApprove?.let { req ->
        AlertDialog(
            onDismissRequest = { requestToApprove = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = SpeedoError, modifier = Modifier.size(36.dp)) },
            title = { Text("Approve & Purge Account?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Are you sure you want to permanently delete the account of ${req.name} (${req.userRole.uppercase()})?\n\n⚠️ WARNING: This will permanently DELETE their profile, login credentials, and vehicle/location data from the realtime backend database. The client app will be instantly force-logged out.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = adminNotesInput,
                        onValueChange = { adminNotesInput = it },
                        label = { Text("Admin Audit Remarks") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.approveDeletionRequest(req.id, adminNotesInput)
                        requestToApprove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoError)
                ) {
                    Text("Permanently Delete Account")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { requestToApprove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reject Dialog
    requestToReject?.let { req ->
        AlertDialog(
            onDismissRequest = { requestToReject = null },
            title = { Text("Reject Deletion Request?") },
            text = {
                Column {
                    Text("The account will remain active and will not be removed from the database.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = adminNotesInput,
                        onValueChange = { adminNotesInput = it },
                        label = { Text("Rejection Reason / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectDeletionRequest(req.id, adminNotesInput)
                        requestToReject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange)
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { requestToReject = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DeletionRequestCard(
    request: AccountDeletionRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SpeedoWhite,
        border = BorderStroke(
            1.dp,
            if (request.status == "pending") Color(0xFFFFCDD2) else SpeedoCardBorder
        ),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (request.userRole == "captain") Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                    ) {
                        Text(
                            text = request.userRole.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (request.userRole == "captain") Color(0xFF2E7D32) else Color(0xFF1565C0)
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = request.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (request.status) {
                        "pending" -> Color(0xFFFFF3E0)
                        "approved" -> Color(0xFFFFEBEE)
                        "rejected" -> Color(0xFFE8F5E9)
                        else -> SpeedoSurfaceVariant
                    }
                ) {
                    Text(
                        text = when (request.status) {
                            "pending" -> "PENDING (24h)"
                            "approved" -> "PURGED & DELETED"
                            "rejected" -> "REJECTED"
                            else -> request.status.uppercase()
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = when (request.status) {
                                "pending" -> Color(0xFFE65100)
                                "approved" -> SpeedoError
                                "rejected" -> SpeedoSuccess
                                else -> SpeedoTextSecondary
                            },
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contact Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = SpeedoTextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(request.phone, style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.Email, contentDescription = null, tint = SpeedoTextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(request.email, style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stated Reason
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SpeedoSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Reason for deletion:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoTextSecondary))
                    Text(
                        text = request.reason ?: "No reason provided by user.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Schedule / Grace Period Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFB78103), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "24-Hour Review Window Active • Requested: ${request.requestedAt?.take(16)?.replace("T", " ") ?: "Recently"}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFFB78103), fontWeight = FontWeight.Medium)
                )
            }

            // Action Buttons (Only for Pending requests)
            if (request.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SpeedoCardBorder)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, SpeedoTextSecondary)
                    ) {
                        Text("Reject Request", color = SpeedoTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoError)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve & Purge", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
