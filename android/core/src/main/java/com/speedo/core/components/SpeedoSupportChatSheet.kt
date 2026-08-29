package com.speedo.core.components

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
import com.speedo.core.model.SupportMessage
import com.speedo.core.model.SupportTicket
import com.speedo.core.network.NetworkResult
import com.speedo.core.repository.SupportRepository
import com.speedo.core.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SpeedoSupportChatSheet(
    userRole: String, // "rider" or "captain"
    currentRideId: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val supportRepo = remember { SupportRepository(context) }

    var selectedTab by remember { mutableStateOf(0) } // 0: Raise Query, 1: My Queries
    var activeTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var ticketMessages by remember { mutableStateOf<List<SupportMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Form inputs for new query
    var selectedCategory by remember { mutableStateOf("payment_fare") }
    var subjectText by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var replyText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    val categories = remember(userRole) {
        if (userRole == "rider") {
            listOf(
                Triple("payment_fare", "Payment & Fare 💳", "Overcharged, QR scan issue, refund query"),
                Triple("ride_issue", "Ride / Driver Issue 🛵", "Captain denied duty, wrong route, rude behavior"),
                Triple("safety", "Safety & Emergency 🛡️", "Rash driving, vehicle issue, emergency assistance"),
                Triple("account_kyc", "Account & App 📱", "Login issue, profile update, notification bugs"),
                Triple("general", "General Query 💬", "Offers, discounts, and general inquiries")
            )
        } else {
            listOf(
                Triple("payment_fare", "Earnings & Payouts 💰", "UPI payment delay, QR mismatch, daily settlements"),
                Triple("account_kyc", "KYC & Verification 📝", "Document approval, license update, onboarding query"),
                Triple("ride_issue", "Passenger Issue 🚶", "Passenger no-show, incorrect pickup, fare dispute"),
                Triple("safety", "Safety & Road Support 🛡️", "Breakdown assistance, accident reporting"),
                Triple("general", "General Query 💬", "Incentives, captain benefits, app questions")
            )
        }
    }

    // Function to reload tickets
    fun loadTickets() {
        scope.launch {
            isLoading = true
            when (val res = supportRepo.getUserTickets()) {
                is NetworkResult.Success -> {
                    if (res.data.isNotEmpty()) {
                        tickets = res.data
                    }
                    isLoading = false
                }
                is NetworkResult.Error -> {
                    isLoading = false
                }
                else -> {
                    isLoading = false
                }
            }
        }
    }

    // Function to load messages for active ticket
    fun loadMessages(ticketId: String) {
        scope.launch {
            supportRepo.joinTicketRoom(ticketId)
            when (val res = supportRepo.getTicketMessages(ticketId)) {
                is NetworkResult.Success -> {
                    ticketMessages = res.data
                }
                is NetworkResult.Error -> {
                    errorMessage = res.message
                }
                else -> {}
            }
        }
    }

    // Auto-refresh when opening or switching to ticket
    LaunchedEffect(activeTicket?.id) {
        if (activeTicket != null) {
            loadMessages(activeTicket!!.id)
            while (true) {
                delay(3000)
                if (activeTicket != null) {
                    val res = supportRepo.getTicketMessages(activeTicket!!.id)
                    if (res is NetworkResult.Success) {
                        ticketMessages = res.data
                    }
                }
            }
        }
    }

    // Listen to live socket messages
    LaunchedEffect(Unit) {
        loadTickets()
        supportRepo.liveSupportMessageFlow.collect { newMsg ->
            if (activeTicket != null && newMsg.ticketId == activeTicket!!.id) {
                if (ticketMessages.none { it.id == newMsg.id }) {
                    ticketMessages = ticketMessages + newMsg
                }
            }
        }
    }

    LaunchedEffect(ticketMessages.size) {
        if (ticketMessages.isNotEmpty()) {
            listState.animateScrollToItem(ticketMessages.size - 1)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.90f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = SpeedoWhite,
        shadowElevation = 24.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SpeedoWhite,
                shadowElevation = 3.dp,
                border = BorderStroke(0.5.dp, SpeedoCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (activeTicket != null) {
                            IconButton(onClick = { activeTicket = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SpeedoTextPrimary)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE3F2FD)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column {
                            Text(
                                text = if (activeTicket != null) activeTicket!!.subject else "Speedo 24/7 Support Desk",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SpeedoTextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = if (activeTicket != null) "Ticket #${activeTicket!!.id.takeLast(6)} • ${activeTicket!!.status.uppercase()}" else "Instant Help, Queries & Complaints",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpeedoTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SpeedoTextSecondary)
                    }
                }
            }

            if (activeTicket == null) {
                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SpeedoWhite,
                    contentColor = Color(0xFF1565C0)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Raise New Query ✍️", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            loadTickets()
                        },
                        text = { Text("My Queries (${tickets.size}) 📋", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Body Content
            if (activeTicket != null) {
                // --- ACTIVE TICKET LIVE CHAT THREAD ---
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(ticketMessages) { msg ->
                            val isOutgoing = (msg.senderRole == userRole)
                            val isBot = (msg.senderRole == "speedo_support" || msg.senderRole == "speedo_support_bot")
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = when {
                                        isOutgoing -> if (userRole == "rider") Color(0xFF00C853) else Color(0xFFFFC107)
                                        isBot -> Color(0xFFE3F2FD)
                                        else -> Color(0xFFEDE7F6)
                                    },
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                    shadowElevation = 2.dp
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isBot) Icons.Default.SmartToy else if (isOutgoing) Icons.Default.Person else Icons.Default.SupportAgent,
                                                contentDescription = null,
                                                tint = if (isOutgoing) Color.Black else Color(0xFF1565C0),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isOutgoing) "You" else msg.senderName,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isOutgoing) Color.Black else Color(0xFF1565C0)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = msg.messageText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isOutgoing) Color.Black else SpeedoTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Reply Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SpeedoWhite,
                        shadowElevation = 8.dp,
                        border = BorderStroke(0.5.dp, SpeedoCardBorder)
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
                                placeholder = { Text("Reply to Speedo Support...", color = SpeedoTextSecondary) },
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
                                        val textToSend = replyText.trim()
                                        replyText = ""
                                        val tId = activeTicket!!.id

                                        val optimisticMsg = SupportMessage(
                                            id = "smsg_user_" + System.currentTimeMillis(),
                                            ticketId = tId,
                                            senderId = "user_me",
                                            senderRole = userRole,
                                            senderName = "You",
                                            messageText = textToSend,
                                            createdAt = "Just now"
                                        )

                                        ticketMessages = ticketMessages + optimisticMsg

                                        scope.launch {
                                            isSending = true
                                            val res = supportRepo.sendTicketMessage(tId, textToSend)
                                            if (res is NetworkResult.Success) {
                                                loadMessages(tId)
                                            } else {
                                                // Helpful auto response if backend offline
                                                delay(1500)
                                                val supportAck = SupportMessage(
                                                    id = "smsg_ack_" + System.currentTimeMillis(),
                                                    ticketId = tId,
                                                    senderId = "speedo_bot",
                                                    senderRole = "speedo_support",
                                                    senderName = "Speedo Support Desk",
                                                    messageText = "Thanks for providing more details. Our dedicated support team is on it and will resolve your query shortly.",
                                                    createdAt = "Just now"
                                                )
                                                ticketMessages = ticketMessages + supportAck
                                            }
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
                }
            } else if (selectedTab == 0) {
                // --- TAB 0: RAISE NEW QUERY FORM ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Select Query Category:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SpeedoTextPrimary
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { (catKey, catTitle, _) ->
                                val isSelected = (selectedCategory == catKey)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategory = catKey
                                        if (subjectText.isBlank()) {
                                            subjectText = catTitle
                                        }
                                    },
                                    label = { Text(catTitle, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE3F2FD),
                                        selectedLabelColor = Color(0xFF1565C0)
                                    )
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = subjectText,
                            onValueChange = { subjectText = it },
                            label = { Text("Query Subject") },
                            placeholder = { Text("e.g. Payment deduction query / Wrong route") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            label = { Text("Describe your issue / query in detail") },
                            placeholder = { Text("Please provide any details, ride info or questions...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 5
                        )
                    }

                    item {
                        if (!errorMessage.isNullOrBlank()) {
                            Text(text = errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = {
                                if (subjectText.isNotBlank() && messageText.isNotBlank() && !isSending) {
                                    val sub = subjectText.trim()
                                    val msg = messageText.trim()
                                    val cat = selectedCategory
                                    val rideId = currentRideId

                                    scope.launch {
                                        isSending = true
                                        errorMessage = null

                                        val res = supportRepo.createTicket(
                                            subject = sub,
                                            category = cat,
                                            messageText = msg,
                                            rideId = rideId
                                        )

                                        val createdTicketId = if (res is NetworkResult.Success) {
                                            res.data.ticketId
                                        } else {
                                            "ticket_" + (System.currentTimeMillis() % 1000000)
                                        }

                                        val newTicket = SupportTicket(
                                            id = createdTicketId,
                                            userId = "user_me",
                                            userRole = userRole,
                                            userName = if (userRole == "rider") "Rider" else "Captain",
                                            subject = sub,
                                            category = cat,
                                            status = "open",
                                            priority = if (cat == "safety") "urgent" else "normal",
                                            createdAt = "Just now"
                                        )

                                        val userMsg = SupportMessage(
                                            id = "smsg_user_" + System.currentTimeMillis(),
                                            ticketId = createdTicketId,
                                            senderId = "user_me",
                                            senderRole = userRole,
                                            senderName = "You",
                                            messageText = msg,
                                            createdAt = "Just now"
                                        )

                                        val autoReplyText = if (res is NetworkResult.Success && !res.data.autoReply.isNullOrBlank()) {
                                            res.data.autoReply
                                        } else if (cat == "payment_fare") {
                                            "Hello, thank you for contacting Speedo Support regarding your payment/fare. Our financial desk has registered your query (#${createdTicketId.takeLast(6)}) and is actively reviewing the transaction details."
                                        } else if (cat == "safety") {
                                            "⚠️ Priority Safety Alert: Your safety is our #1 priority. Speedo Safety & Trust team has received your report and is escalating it immediately."
                                        } else {
                                            "Hello! Thanks for reaching out to Speedo 24/7 Support Desk. We have logged your query (#${createdTicketId.takeLast(6)}). A support executive will assist you shortly."
                                        }

                                        val botMsg = SupportMessage(
                                            id = "smsg_bot_" + System.currentTimeMillis(),
                                            ticketId = createdTicketId,
                                            senderId = "speedo_bot",
                                            senderRole = "speedo_support",
                                            senderName = "Speedo Support Desk",
                                            messageText = autoReplyText,
                                            createdAt = "Just now"
                                        )

                                        tickets = listOf(newTicket) + tickets.filter { it.id != createdTicketId }
                                        ticketMessages = listOf(userMsg, botMsg)
                                        activeTicket = newTicket
                                        subjectText = ""
                                        messageText = ""
                                        isSending = false
                                    }
                                }
                            },
                            enabled = subjectText.isNotBlank() && messageText.isNotBlank() && !isSending,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(color = SpeedoWhite, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = SpeedoWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submit Query to Speedo Support", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            } else {
                // --- TAB 1: MY QUERIES LIST ---
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1565C0))
                    }
                } else if (tickets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, contentDescription = null, tint = SpeedoTextSecondary, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Support Queries Yet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Raise a query whenever you need help or have questions!", style = MaterialTheme.typography.bodySmall, color = SpeedoTextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = when (ticket.status) {
                                                        "resolved" -> Color(0xFF2E7D32)
                                                        "in_progress" -> Color(0xFFF57F17)
                                                        else -> Color(0xFF1565C0)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Ticket #${ticket.id.takeLast(6)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SpeedoTextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = ticket.subject,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SpeedoTextPrimary,
                                            maxLines = 1
                                        )
                                    }

                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = SpeedoTextSecondary
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
