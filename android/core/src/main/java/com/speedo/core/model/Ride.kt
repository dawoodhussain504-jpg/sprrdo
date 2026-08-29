package com.speedo.core.model

import com.google.gson.annotations.SerializedName

data class Ride(
    @SerializedName("id") val id: String,
    @SerializedName("rider_id") val riderId: String,
    @SerializedName("captain_id") val captainId: String? = null,
    @SerializedName("pickup_address") val pickupAddress: String,
    @SerializedName("pickup_lat") val pickupLat: Double,
    @SerializedName("pickup_lng") val pickupLng: Double,
    @SerializedName("drop_address") val dropAddress: String,
    @SerializedName("drop_lat") val dropLat: Double,
    @SerializedName("drop_lng") val dropLng: Double,
    @SerializedName("vehicle_type") val vehicleType: String,
    @SerializedName("fare") val fare: Double,
    @SerializedName("distance_km") val distanceKm: Double,
    @SerializedName("status") val status: String, // "requested", "accepted", "arrived", "ongoing", "completed", "cancelled"
    @SerializedName("otp") val otp: String,
    @SerializedName("captain_lat") val captainLat: Double? = null,
    @SerializedName("captain_lng") val captainLng: Double? = null,
    @SerializedName("captain_heading") val captainHeading: Double? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("cancelled_by") val cancelledBy: String? = null,
    @SerializedName("cancellation_reason") val cancellationReason: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    // Joined Captain details
    @SerializedName("captain_name") val captainName: String? = null,
    @SerializedName("captain_phone") val captainPhone: String? = null,
    @SerializedName("vehicle_number") val vehicleNumber: String? = null,
    @SerializedName("captain_rating") val captainRating: Double? = null,
    @SerializedName("captain_avatar_url") val captainAvatarUrl: String? = null,
    @SerializedName("captain_qr_url") val captainQrUrl: String? = null,
    @SerializedName("live_captain_lat") val liveCaptainLat: Double? = null,
    @SerializedName("live_captain_lng") val liveCaptainLng: Double? = null,
    @SerializedName("live_captain_bearing") val liveCaptainBearing: Double? = null,
    @SerializedName("captain_distance_km") val captainDistanceKm: Double? = null,
    @SerializedName("eta_minutes") val etaMinutes: Int? = null,
    // Joined Rider details
    @SerializedName("rider_name") val riderName: String? = null,
    @SerializedName("rider_phone") val riderPhone: String? = null,
    @SerializedName("rider_avatar_url") val riderAvatarUrl: String? = null
)

data class FareEstimatesResponse(
    @SerializedName("distance_km") val distanceKm: Double,
    @SerializedName("estimates") val estimates: Map<String, FareOption>
)

data class FareOption(
    @SerializedName("vehicleType") val vehicleType: String,
    @SerializedName("baseFare") val baseFare: Double,
    @SerializedName("perKmRate") val perKmRate: Double,
    @SerializedName("totalFare") val totalFare: Double,
    @SerializedName("distanceKm") val distanceKm: Double,
    @SerializedName("estimatedTimeMin") val estimatedTimeMin: Int
)

enum class VehicleCategory(val key: String, val displayName: String, val description: String) {
    BIKE("bike", "Speedo Bike", "Fastest for solo travel & heavy traffic"),
    AUTO("auto", "Speedo Auto", "Affordable & comfortable 3-seater"),
    CAB("cab", "Speedo Cab", "AC ride with premium comfort")
}

data class RideRequestBody(
    @SerializedName("pickup_address") val pickupAddress: String,
    @SerializedName("pickup_lat") val pickupLat: Double,
    @SerializedName("pickup_lng") val pickupLng: Double,
    @SerializedName("drop_address") val dropAddress: String,
    @SerializedName("drop_lat") val dropLat: Double,
    @SerializedName("drop_lng") val dropLng: Double,
    @SerializedName("vehicle_type") val vehicleType: String
)

