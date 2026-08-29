package com.speedo.core.model

import com.google.gson.annotations.SerializedName

data class RoutePoint(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class RouteManeuver(
    @SerializedName("instruction") val instruction: String,
    @SerializedName("distanceMeters") val distanceMeters: Int,
    @SerializedName("durationSeconds") val durationSeconds: Int,
    @SerializedName("modifier") val modifier: String? = "straight",
    @SerializedName("type") val type: String? = "turn",
    @SerializedName("name") val name: String? = ""
)

data class RouteResponse(
    @SerializedName("distanceKm") val distanceKm: Double,
    @SerializedName("durationMins") val durationMins: Int,
    @SerializedName("coordinates") val coordinates: List<RoutePoint>,
    @SerializedName("maneuvers") val maneuvers: List<RouteManeuver> = emptyList(),
    @SerializedName("summary") val summary: String? = null
)
