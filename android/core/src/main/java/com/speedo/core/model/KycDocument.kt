package com.speedo.core.model

import com.google.gson.annotations.SerializedName

data class KycDocument(
    @SerializedName("id") val id: String? = null,
    @SerializedName("captain_id") val captainId: String? = null,
    @SerializedName("document_type") val documentType: String, // "vehicle_reg", "aadhaar", "selfie", "payment_qr"
    @SerializedName("file_url") val fileUrl: String? = null,
    @SerializedName("status") val status: String = "missing", // "missing", "pending", "approved", "rejected"
    @SerializedName("admin_remarks") val adminRemarks: String? = null,
    @SerializedName("is_uploaded") val isUploaded: Boolean = false
)

data class KycStatusResponse(
    @SerializedName("kyc_status") val kycStatus: String, // "pending", "under_review", "approved", "rejected"
    @SerializedName("admin_remarks") val adminRemarks: String? = null,
    @SerializedName("payment_qr_url") val paymentQrUrl: String? = null,
    @SerializedName("documents") val documents: List<KycDocument>,
    @SerializedName("is_complete") val isComplete: Boolean,
    @SerializedName("is_approved") val isApproved: Boolean
)

data class NotificationItem(
    @SerializedName("id") val id: String,
    @SerializedName("recipient_id") val recipientId: String,
    @SerializedName("recipient_role") val recipientRole: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String,
    @SerializedName("is_read") val isRead: Int = 0,
    @SerializedName("metadata_json") val metadataJson: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    fun isAppUpdateNotification(): Boolean {
        return type.equals("app_update", ignoreCase = true) ||
               title.contains("update", ignoreCase = true) ||
               message.contains("update", ignoreCase = true)
    }

    fun extractUpdateUrl(fallbackAppId: String = "rider"): String {
        return try {
            if (!metadataJson.isNullOrBlank()) {
                val json = org.json.JSONObject(metadataJson)
                val url = json.optString("updateUrl", "")
                if (url.isNotBlank()) return url
            }
            "https://web-production-5d826.up.railway.app/downloads/speedo-$fallbackAppId.apk"
        } catch (e: Exception) {
            "https://web-production-5d826.up.railway.app/downloads/speedo-$fallbackAppId.apk"
        }
    }
}

data class DashboardStats(
    @SerializedName("total_riders") val totalRiders: Int,
    @SerializedName("total_captains") val totalCaptains: Int,
    @SerializedName("online_captains") val onlineCaptains: Int,
    @SerializedName("active_rides") val activeRides: Int,
    @SerializedName("pending_kyc_count") val pendingKycCount: Int,
    @SerializedName("completed_rides") val completedRides: Int,
    @SerializedName("total_revenue") val totalRevenue: Double
)

data class LiveMapResponse(
    @SerializedName("online_captains") val onlineCaptains: List<Captain>,
    @SerializedName("active_rides") val activeRides: List<Ride>
)

data class LocationPoint(
    val lat: Double,
    val lng: Double,
    val bearing: Float = 0f,
    val speed: Float = 0f
)
