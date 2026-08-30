package com.speedo.core.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("role") val role: String = "rider",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("is_active") val isActive: Int = 1,
    @SerializedName("created_at") val createdAt: String? = null
)

data class Captain(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("vehicle_type") val vehicleType: String, // "bike", "auto", "cab"
    @SerializedName("vehicle_number") val vehicleNumber: String,
    @SerializedName("kyc_status") val kycStatus: String = "pending", // "pending", "under_review", "approved", "rejected"
    @SerializedName("admin_remarks") val adminRemarks: String? = null,
    @SerializedName("is_online") val isOnline: Boolean = false,
    @SerializedName("rating") val rating: Double = 5.0,
    @SerializedName("total_rides") val totalRides: Int = 0,
    @SerializedName("total_earnings") val totalEarnings: Double = 0.0,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("payment_qr_url") val paymentQrUrl: String? = null,
    @SerializedName("documents") val documents: List<KycDocument> = emptyList(),
    @SerializedName("is_active") val isActive: Int = 1,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null,
    @SerializedName("bearing") val bearing: Double? = null,
    @SerializedName("distance_km") val distanceKm: Double? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class Admin(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String = "admin"
)

data class UsersManagementResponse(
    @SerializedName("riders") val riders: List<User> = emptyList(),
    @SerializedName("captains") val captains: List<Captain> = emptyList()
)
