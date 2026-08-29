package com.speedo.core.model

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    @SerializedName("id") val id: String,
    @SerializedName("rideId") val rideId: String,
    @SerializedName("senderId") val senderId: String,
    @SerializedName("senderRole") val senderRole: String, // "rider" or "captain"
    @SerializedName("recipientId") val recipientId: String,
    @SerializedName("recipientRole") val recipientRole: String,
    @SerializedName("messageText") val messageText: String,
    @SerializedName("messageType") val messageType: String = "text", // "text", "quick_chip", "voice_note"
    @SerializedName("audioUrl") val audioUrl: String? = null,
    @SerializedName("isRead") val isRead: Boolean = false,
    @SerializedName("createdAt") val createdAt: String? = null
)
