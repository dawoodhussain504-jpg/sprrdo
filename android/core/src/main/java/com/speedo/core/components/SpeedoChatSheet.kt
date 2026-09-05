package com.speedo.core.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.core.model.ChatMessage
import com.speedo.core.theme.*
import kotlinx.coroutines.launch

@Composable
fun SpeedoChatSheet(
    rideId: String,
    currentUserId: String,
    currentUserRole: String, // "rider" or "captain"
    peerName: String,
    peerSubtitle: String?,
    peerPhone: String?,
    messages: List<ChatMessage>,
    onSendMessage: (text: String, type: String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val quickChips = remember(currentUserRole) {
        if (currentUserRole == "rider") {
            listOf(
                "I'm at the main gate 📍",
                "Please bring helmet 🪖",
                "Coming down in 1 min ⏳",
                "Where are you? 🔍",
                "Stuck in lift / stairs 🚶"
            )
        } else {
            listOf(
                "I have arrived at pickup 🛵",
                "Where are you? 📍",
                "Stuck at signal, 2 mins away 🚦",
                "Please come to main road 🛣️",
                "Reached your gate 🚪"
            )
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = SpeedoWhite,
        shadowElevation = 24.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Header with Peer Profile & Call Action
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SpeedoWhite,
                shadowElevation = 4.dp,
                border = BorderStroke(0.5.dp, SpeedoCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (currentUserRole == "rider") Color(0xFFFFF9C4) else Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentUserRole == "rider") Icons.Default.DirectionsBike else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (currentUserRole == "rider") Color(0xFFF57F17) else Color(0xFF00C853),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = peerName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SpeedoTextPrimary
                                )
                            )
                            if (!peerSubtitle.isNullOrBlank()) {
                                Text(
                                    text = peerSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoTextSecondary
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!peerPhone.isNullOrBlank()) {
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$peerPhone"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9))
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF00C853))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = SpeedoTextSecondary)
                        }
                    }
                }
            }

            // 2. Chat Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(SpeedoSurfaceVariant)
            ) {
                if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = SpeedoTextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No messages yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = SpeedoTextSecondary)
                        )
                        Text(
                            text = "Tap a quick chip below or type a message to start chatting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpeedoTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isOutgoing = msg.senderId == currentUserId || msg.senderRole == currentUserRole
                            ChatBubbleItem(message = msg, isOutgoing = isOutgoing)
                        }
                    }
                }
            }

            // 3. Quick Action Chips Carousel
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpeedoWhite)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickChips) { chip ->
                    Surface(
                        modifier = Modifier.clickable {
                            onSendMessage(chip, "quick_chip")
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF1F8E9),
                        border = BorderStroke(1.dp, Color(0xFF81C784))
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Divider(color = SpeedoCardBorder)

            // 4. Message Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpeedoWhite)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type message...", color = SpeedoTextSecondary) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SpeedoSurfaceVariant,
                        unfocusedContainerColor = SpeedoSurfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim(), "text")
                            inputText = ""
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim(), "text")
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) Color(0xFF00C853) else SpeedoSurfaceVariant)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) SpeedoWhite else SpeedoTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    isOutgoing: Boolean
) {
    val bubbleColor = if (isOutgoing) Color(0xFF00C853) else SpeedoWhite
    val textColor = if (isOutgoing) SpeedoWhite else SpeedoTextPrimary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOutgoing) 16.dp else 4.dp,
                bottomEnd = if (isOutgoing) 4.dp else 16.dp
            ),
            color = bubbleColor,
            shadowElevation = 2.dp,
            border = if (!isOutgoing) BorderStroke(1.dp, SpeedoCardBorder) else null
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.messageText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (message.messageType == "quick_chip") FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Now",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = if (isOutgoing) SpeedoWhite.copy(alpha = 0.8f) else SpeedoTextSecondary
                        )
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = null,
                            tint = SpeedoWhite,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
