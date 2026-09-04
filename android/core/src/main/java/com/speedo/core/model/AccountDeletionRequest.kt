package com.speedo.core.model

import com.google.gson.annotations.SerializedName

data class AccountDeletionRequest(
    @SerializedName("id") val id: String,
    @SerializedName("user_id", alternate = ["userId"]) val userId: String,
    @SerializedName("user_role", alternate = ["userRole"]) val userRole: String, // "rider", "captain"
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("email") val email: String,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("status") val status: String = "pending", // "pending", "approved", "rejected", "cancelled"
    @SerializedName("requested_at", alternate = ["requestedAt"]) val requestedAt: String? = null,
    @SerializedName("scheduled_deletion_at", alternate = ["scheduledDeletionAt"]) val scheduledDeletionAt: String? = null,
    @SerializedName("reviewed_at", alternate = ["reviewedAt"]) val reviewedAt: String? = null,
    @SerializedName("reviewed_by", alternate = ["reviewedBy"]) val reviewedBy: String? = null,
    @SerializedName("admin_notes", alternate = ["adminNotes"]) val adminNotes: String? = null,
    @SerializedName("created_at", alternate = ["createdAt"]) val createdAt: String? = null
)

data class CreateDeletionRequestBody(
    @SerializedName("reason") val reason: String
)

data class ReviewDeletionRequestBody(
    @SerializedName("admin_notes") val adminNotes: String? = null
)
