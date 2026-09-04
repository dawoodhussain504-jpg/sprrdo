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

    fun extractVersionCode(): Int? {
        return try {
            if (!metadataJson.isNullOrBlank()) {
                val json = org.json.JSONObject(metadataJson)
                val code = json.optInt("latestVersionCode", -1)
                if (code > 0) return code
                val vCode = json.optInt("versionCode", -1)
                if (vCode > 0) return vCode
            }
            val buildMatch = Regex("(?:build|code|#)\\s*(\\d+)", RegexOption.IGNORE_CASE).find("$title $message")
            buildMatch?.groupValues?.get(1)?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun extractVersionName(): String? {
        return try {
            if (!metadataJson.isNullOrBlank()) {
                val json = org.json.JSONObject(metadataJson)
                val name = json.optString("latestVersionName", "")
                if (name.isNotBlank()) return name
                val vName = json.optString("versionName", "")
                if (vName.isNotBlank()) return vName
            }
            val vMatch = Regex("v(\\d+(?:\\.\\d+)+)", RegexOption.IGNORE_CASE).find("$title $message")
            vMatch?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun isVersionAtLeast(currentName: String, targetName: String): Boolean {
        val currentParts = currentName.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val targetParts = targetName.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(currentParts.size, targetParts.size)
        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val t = targetParts.getOrElse(i) { 0 }
            if (c > t) return true
            if (c < t) return false
        }
        return true
    }

    fun isOldUpdateFor(currentVersionCode: Int, isUpdateAvailable: Boolean, currentVersionName: String? = null): Boolean {
        if (!isAppUpdateNotification()) return false

        // If the user's app has no pending update available, the user already updated to latest!
        // Therefore all past update notifications are old and must disappear.
        if (!isUpdateAvailable) return true

        // If an update is available, check if this specific notification was for an older version
        val targetCode = extractVersionCode()
        if (targetCode != null && targetCode <= currentVersionCode) {
            return true
        }

        val targetName = extractVersionName()
        if (!targetName.isNullOrBlank() && !currentVersionName.isNullOrBlank()) {
            if (isVersionAtLeast(currentVersionName, targetName)) {
                return true
            }
        }

        return false
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
