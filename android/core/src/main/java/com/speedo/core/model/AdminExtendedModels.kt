package com.speedo.core.model

import com.google.gson.annotations.SerializedName

// -------------------------------------------------------------
// 1. AI DOCUMENT OCR & INSTANT KYC MODELS
// -------------------------------------------------------------
data class DocumentTraceSummary(
    @SerializedName("documentType") val documentType: String,
    @SerializedName("title") val title: String,
    @SerializedName("filename") val filename: String = "",
    @SerializedName("extractedTextSnippet") val extractedTextSnippet: String = "",
    @SerializedName("detectedPatterns") val detectedPatterns: List<String> = emptyList(),
    @SerializedName("confidence") val confidence: Double = 90.0
)

data class KycAiScanResult(
    @SerializedName("captainId") val captainId: String,
    @SerializedName("captainName") val captainName: String,
    @SerializedName("registeredVehicle") val registeredVehicle: String,
    @SerializedName("vehicleType") val vehicleType: String,
    @SerializedName("dlNumber") val dlNumber: String,
    @SerializedName("rcNumber") val rcNumber: String,
    @SerializedName("aadhaarMasked") val aadhaarMasked: String,
    @SerializedName("extractedName") val extractedName: String? = null,
    @SerializedName("extractedUpiId") val extractedUpiId: String? = null,
    @SerializedName("expiryDate") val expiryDate: String,
    @SerializedName("nameMatchConfidence") val nameMatchConfidence: Double,
    @SerializedName("vehicleMatchConfidence") val vehicleMatchConfidence: Double,
    @SerializedName("faceMatchConfidence") val faceMatchConfidence: Double,
    @SerializedName("overallScore") val overallScore: Int,
    @SerializedName("isAutoApprovedEligible") val isAutoApprovedEligible: Boolean,
    @SerializedName("rawTraces") val rawTraces: List<DocumentTraceSummary> = emptyList(),
    @SerializedName("verifiedAt") val verifiedAt: String
)

data class InstantApproveKycRequest(
    @SerializedName("admin_remarks") val adminRemarks: String? = null
)

// -------------------------------------------------------------
// 2. GEOFENCED SURGE ZONE MODELS
// -------------------------------------------------------------
data class SurgeZone(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("zone_type") val zoneType: String = "custom",
    @SerializedName("center_lat") val centerLat: Double,
    @SerializedName("center_lng") val centerLng: Double,
    @SerializedName("radius_km") val radiusKm: Double = 3.0,
    @SerializedName("surge_multiplier") val surgeMultiplier: Double = 1.3,
    @SerializedName("base_fare_multiplier") val baseFareMultiplier: Double = 1.25,
    @SerializedName("per_km_multiplier") val perKmMultiplier: Double = 1.25,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("created_at") val createdAt: String? = null
)

data class CreateSurgeZoneRequest(
    @SerializedName("name") val name: String,
    @SerializedName("zone_type") val zoneType: String = "custom",
    @SerializedName("center_lat") val centerLat: Double,
    @SerializedName("center_lng") val centerLng: Double,
    @SerializedName("radius_km") val radiusKm: Double = 3.0,
    @SerializedName("surge_multiplier") val surgeMultiplier: Double = 1.3,
    @SerializedName("base_fare_multiplier") val baseFareMultiplier: Double = 1.25,
    @SerializedName("per_km_multiplier") val perKmMultiplier: Double = 1.25
)

data class UpdateSurgeZoneRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("surge_multiplier") val surgeMultiplier: Double? = null,
    @SerializedName("base_fare_multiplier") val baseFareMultiplier: Double? = null,
    @SerializedName("per_km_multiplier") val perKmMultiplier: Double? = null,
    @SerializedName("radius_km") val radiusKm: Double? = null,
    @SerializedName("is_active") val isActive: Boolean? = null
)

// -------------------------------------------------------------
// 3. LIVE SOS EMERGENCY MODELS
// -------------------------------------------------------------
data class SosAlert(
    @SerializedName("id") val id: String,
    @SerializedName("ride_id") val rideId: String? = null,
    @SerializedName("triggered_by") val triggeredBy: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_phone") val userPhone: String,
    @SerializedName("captain_id") val captainId: String? = null,
    @SerializedName("captain_name") val captainName: String? = null,
    @SerializedName("captain_phone") val captainPhone: String? = null,
    @SerializedName("vehicle_number") val vehicleNumber: String? = null,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("address") val address: String? = null,
    @SerializedName("status") val status: String = "active",
    @SerializedName("admin_notes") val adminNotes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("resolved_at") val resolvedAt: String? = null
)

data class SosAlertsResponse(
    @SerializedName("data") val alerts: List<SosAlert>,
    @SerializedName("active_count") val activeCount: Int
)

data class TriggerSosRequest(
    @SerializedName("ride_id") val rideId: String? = null,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("address") val address: String? = null
)

data class ResolveSosRequest(
    @SerializedName("status") val status: String,
    @SerializedName("admin_notes") val adminNotes: String? = null
)

// -------------------------------------------------------------
// 4. TARGETED CITY-WIDE BROADCAST MODELS
// -------------------------------------------------------------
data class BroadcastAnnouncement(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("target_audience") val targetAudience: String = "all",
    @SerializedName("target_city") val targetCity: String = "All Cities",
    @SerializedName("coupon_code") val couponCode: String? = null,
    @SerializedName("discount_percent") val discountPercent: Double = 0.0,
    @SerializedName("bonus_amount") val bonusAmount: Double = 0.0,
    @SerializedName("total_recipients") val totalRecipients: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null
)

data class SendBroadcastRequest(
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("target_audience") val targetAudience: String = "all",
    @SerializedName("target_city") val targetCity: String = "All Cities",
    @SerializedName("coupon_code") val couponCode: String? = null,
    @SerializedName("discount_percent") val discountPercent: Double = 0.0,
    @SerializedName("bonus_amount") val bonusAmount: Double = 0.0
)
