package com.speedo.core.model

import com.google.gson.annotations.SerializedName

data class SupportTicket(
    @SerializedName("id") val id: String = "",
    @SerializedName("userId", alternate = ["user_id", "ticketId", "ticket_id"]) val userId: String = "",
    @SerializedName("userRole", alternate = ["user_role", "role"]) val userRole: String = "rider", // "rider", "captain"
    @SerializedName("userName", alternate = ["user_name", "name"]) val userName: String? = null,
    @SerializedName("userPhone", alternate = ["user_phone", "phone"]) val userPhone: String? = null,
    @SerializedName("rideId", alternate = ["ride_id"]) val rideId: String? = null,
    @SerializedName("subject") val subject: String = "",
    @SerializedName("category") val category: String = "general", // "payment_fare", "ride_issue", "safety", "account_kyc", "app_feedback", "general"
    @SerializedName("status") val status: String = "open", // "open", "in_progress", "resolved", "closed"
    @SerializedName("priority") val priority: String = "normal", // "normal", "high", "urgent"
    @SerializedName("createdAt", alternate = ["created_at"]) val createdAt: String? = null,
    @SerializedName("updatedAt", alternate = ["updated_at"]) val updatedAt: String? = null
)

data class SupportMessage(
    @SerializedName("id") val id: String = "",
    @SerializedName("ticketId", alternate = ["ticket_id"]) val ticketId: String = "",
    @SerializedName("senderId", alternate = ["sender_id"]) val senderId: String = "",
    @SerializedName("senderRole", alternate = ["sender_role"]) val senderRole: String = "speedo_support", // "rider", "captain", "admin", "speedo_support"
    @SerializedName("senderName", alternate = ["sender_name"]) val senderName: String = "Speedo Support",
    @SerializedName("messageText", alternate = ["message_text", "text", "message"]) val messageText: String = "",
    @SerializedName("createdAt", alternate = ["created_at"]) val createdAt: String? = null
)

data class CreateTicketResponse(
    @SerializedName("ticketId", alternate = ["ticket_id", "id"]) val ticketId: String = "",
    @SerializedName("subject") val subject: String = "",
    @SerializedName("category") val category: String = "general",
    @SerializedName("status") val status: String = "open",
    @SerializedName("priority") val priority: String = "normal",
    @SerializedName("autoReply", alternate = ["auto_reply", "message"]) val autoReply: String? = null
)
