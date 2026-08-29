package com.speedo.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.*
import com.speedo.core.model.SupportMessage
import com.speedo.core.model.SupportTicket
import com.speedo.core.network.NetworkResult
import com.speedo.core.repository.SupportRepository
import com.speedo.core.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SupportDeskScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val supportRepo = remember { SupportRepository(context) }

    var selectedFilter by remember { mutableStateOf("all") }
    var activeTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var ticketMessages by remember { mutableStateOf<List<SupportMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var statusUpdating by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val defaultMockTickets = remember {
        listOf(
            SupportTicket(
                id = "ticket_demo_101",
                userId = "rider_001",
                userRole = "rider",
                userName = "Rahul Sharma",
                userPhone = "+91 9876543210",
                subject = "Payment charged twice for Whitefield trip",
                category = "payment_fare",
                status = "open",
                priority = "high",
                createdAt = "10 mins ago"
            ),
            SupportTicket(
                id = "ticket_demo_102",
                userId = "captain_002",
                userRole = "captain",
                userName = "Suresh Kumar",
                userPhone = "+91 9123456789",
                subject = "KYC documents re-uploaded for verification",
                category = "account_kyc",
                status = "in_progress",
                priority = "normal",
                createdAt = "25 mins ago"
            ),
            SupportTicket(
                id = "ticket_demo_103",
                userId = "rider_003",
                userRole = "rider",
                userName = "Pooja Verma",
                userPhone = "+91 9988776655",
                subject = "Safety query: Helmet not provided by bike captain",
                category = "safety",
                status = "open",
                priority = "urgent",
                createdAt = "1 hour ago"
            )
        )
    }

    fun loadTickets() {
        scope.launch {
            val statusParam = if (selectedFilter in listOf("open", "in_progress", "resolved")) selectedFilter else null
            val catParam = if (selectedFilter in listOf("payment_fare", "safety")) selectedFilter else null
            when (val res = supportRepo.getAdminTickets(status = statusParam, category = catParam)) {
                is NetworkResult.Success -> {
                    if (res.data.isNotEmpty()) {
                        tickets = res.data
                    } else if (tickets.isEmpty()) {
                        tickets = defaultMockTickets
                    }
                    isLoading = false
                }
                is NetworkResult.Error -> {
                    if (tickets.isEmpty()) {
                        tickets = defaultMockTickets
                    }
                    isLoading = false
                }
                else -> {
                    isLoading = false
                }
            }
        }
    }

    fun loadMessages(ticketId: String) {
        scope.launch {
            supportRepo.joinTicketRoom(ticketId)
            when (val res = supportRepo.getTicketMessages(ticketId)) {
                is NetworkResult.Success -> {
                    ticketMessages = res.data
                }
                else -> {
                    if (ticketMessages.isEmpty()) {
                        ticketMessages = listOf(
                            SupportMessage(
                                id = "msg_init_" + ticketId,
                                ticketId = ticketId,
                                senderId = "user",
                                senderRole = activeTicket?.userRole ?: "rider",
                                senderName = activeTicket?.userName ?: "User",
                                messageText = activeTicket?.subject ?: "Support inquiry details",
                                createdAt = "Just now"
                            ),
                            SupportMessage(
                                id = "msg_bot_" + ticketId,
                                ticketId = ticketId,
                                senderId = "speedo_bot",
                                senderRole = "speedo_support",
                                senderName = "Speedo Support Bot",
                                messageText = "Speedo Helpdesk automated acknowledgment recorded. Admin reviewing.",
                                createdAt = "Just now"
                            )
                        )
                    }
                }
            }
        }
    }

    // Real-time socket updates & background sync
    LaunchedEffect(Unit) {
        supportRepo.joinAdminSupportRoom()
        loadTickets()

        // Real-time new ticket stream
        launch {
            supportRepo.liveSupportTicketFlow.collect { newTicket ->
                tickets = listOf(newTicket) + tickets.filter { it.id != newTicket.id }
            }
        }

        // Real-time new message stream
        launch {
            supportRepo.liveSupportMessageFlow.collect { newMsg ->
                if (activeTicket?.id == newMsg.ticketId) {
                    if (ticketMessages.none { it.id == newMsg.id }) {
                        ticketMessages = ticketMessages + newMsg
                    }
                }
            }
        }

        // Continuous sync every 4 seconds
        while (true) {
            delay(4000)
            loadTickets()
        }
    }

    LaunchedEffect(selectedFilter) {
        loadTickets()
    }

    LaunchedEffect(activeTicket?.id) {
        if (activeTicket != null) {
            loadMessages(activeTicket!!.id)
            while (true) {
                delay(3000)
                if (activeTicket != null) {
                    val res = supportRepo.getTicketMessages(activeTicket!!.id)
                    if (res is NetworkResult.Success && res.data.isNotEmpty()) {
                        ticketMessages = res.data
                    }
                }
            }
        }
    }

    LaunchedEffect(ticketMessages.size) {
        if (ticketMessages.isNotEmpty()) {
            listState.animateScrollToItem(ticketMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = if (activeTicket != null) "Query #${activeTicket!!.id.takeLast(6)}" else "24/7 Support Desk",
                onBackClick = if (activeTicket != null) { { activeTicket = null } } else null,
                onMenuClick = if (activeTicket == null) onMenuClick else null,
                actions = {
                    IconButton(onClick = { loadTickets() }) {
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
                .background(SpeedoBackground)
        ) {
            if (activeTicket != null) {
                val ticket = activeTicket!!
                // --- ACTIVE TICKET DETAIL & CHAT VIEW ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SpeedoWhite,
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
                                    text = ticket.subject,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SpeedoTextPrimary
                                )
                                Text(
                                    text = "${ticket.userName ?: "User"} • ${ticket.userRole.uppercase()} • ${ticket.userPhone ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoTextSecondary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (ticket.status) {
                                    "resolved" -> Color(0xFFE8F5E9)
                                    "in_progress" -> Color(0xFFFFF9C4)
                                    else -> Color(0xFFE3F2FD)
                                }
                            ) {
                                Text(
                                    text = ticket.status.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when (ticket.status) {
                                        "resolved" -> Color(0xFF2E7D32)
                                        "in_progress" -> Color(0xFFF57F17)
                                        else -> Color(0xFF1565C0)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Status Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (ticket.status != "resolved") {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            statusUpdating = true
                                            supportRepo.updateTicketStatus(ticket.id, "resolved")
                                            activeTicket = ticket.copy(status = "resolved")
                                            loadTickets()
                                            statusUpdating = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    enabled = !statusUpdating
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark as Resolved", fontSize = 12.sp)
                                }
                            }
                            if (ticket.status == "resolved") {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            statusUpdating = true
                                            supportRepo.updateTicketStatus(ticket.id, "open")
                                            activeTicket = ticket.copy(status = "open")
                                            loadTickets()
                                            statusUpdating = false
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    enabled = !statusUpdating
                                ) {
                                    Text("Reopen Query", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Chat Messages Thread
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ticketMessages) { msg ->
                        val isAdmin = (msg.senderRole == "admin")
                        val isBot = (msg.senderRole == "speedo_support" || msg.senderRole == "speedo_support_bot")

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isAdmin) Alignment.End else Alignment.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    isAdmin -> Color(0xFF1565C0)
                                    isBot -> Color(0xFFE3F2FD)
                                    else -> SpeedoWhite
                                },
                                border = BorderStroke(1.dp, if (isAdmin) Color.Transparent else Color(0xFFE0E0E0)),
                                shadowElevation = 2.dp
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else if (isBot) Icons.Default.SmartToy else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isAdmin) SpeedoWhite else Color(0xFF1565C0),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isAdmin) "You (Admin)" else msg.senderName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isAdmin) SpeedoWhite else Color(0xFF1565C0)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.messageText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isAdmin) SpeedoWhite else SpeedoTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Admin Reply Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SpeedoWhite,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Reply as Speedo Support Admin...", color = SpeedoTextSecondary) },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SpeedoSurfaceVariant,
                                unfocusedContainerColor = SpeedoSurfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank() && !isSending) {
                                    val text = replyText.trim()
                                    replyText = ""
                                    val adminMsg = SupportMessage(
                                        id = "smsg_admin_" + System.currentTimeMillis(),
                                        ticketId = ticket.id,
                                        senderId = "admin_user",
                                        senderRole = "admin",
                                        senderName = "You (Admin)",
                                        messageText = text,
                                        createdAt = "Just now"
                                    )
                                    ticketMessages = ticketMessages + adminMsg

                                    scope.launch {
                                        isSending = true
                                        supportRepo.sendTicketMessage(ticket.id, text)
                                        isSending = false
                                    }
                                }
                            },
                            enabled = replyText.isNotBlank() && !isSending,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (replyText.isNotBlank()) Color(0xFF1565C0) else SpeedoSurfaceVariant)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (replyText.isNotBlank()) SpeedoWhite else SpeedoTextSecondary
                            )
                        }
                    }
                }
            } else {
                // --- TICKET QUEUE LIST ---
                // Filter Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "all" to "All Queries",
                        "open" to "Open",
                        "in_progress" to "In Progress",
                        "resolved" to "Resolved",
                        "safety" to "Safety / Urgent",
                        "payment_fare" to "Payment & Fare"
                    )
                    items(filters) { (fKey, fLabel) ->
                        val isSelected = (selectedFilter == fKey)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = fKey },
                            label = { Text(fLabel, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE3F2FD),
                                selectedLabelColor = Color(0xFF1565C0)
                            )
                        )
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1565C0))
                    }
                } else if (tickets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("All Caught Up!", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("No pending support tickets matching this filter.", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tickets) { ticket ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SpeedoWhite,
                                border = BorderStroke(1.dp, SpeedoCardBorder),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeTicket = ticket }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (ticket.userRole == "rider") Color(0xFFE8F5E9) else Color(0xFFFFF9C4)
                                            ) {
                                                Text(
                                                    text = if (ticket.userRole == "rider") "RIDER" else "CAPTAIN",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = if (ticket.userRole == "rider") Color(0xFF2E7D32) else Color(0xFFF57F17),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Ticket #${ticket.id.takeLast(6)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SpeedoTextSecondary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (ticket.status) {
                                                "resolved" -> Color(0xFFE8F5E9)
                                                "in_progress" -> Color(0xFFFFF9C4)
                                                else -> Color(0xFFE3F2FD)
                                            }
                                        ) {
                                            Text(
                                                text = ticket.status.uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = when (ticket.status) {
                                                    "resolved" -> Color(0xFF2E7D32)
                                                    "in_progress" -> Color(0xFFF57F17)
                                                    else -> Color(0xFF1565C0)
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = ticket.subject,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SpeedoTextPrimary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "User: ${ticket.userName ?: "User"} • ${ticket.userPhone ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SpeedoTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
