package com.speedo.core.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("error") val error: String? = null
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: User? = null,
    @SerializedName("captain") val captain: Captain? = null,
    @SerializedName("admin") val admin: Admin? = null
)

data class UnreadCountResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int
)
