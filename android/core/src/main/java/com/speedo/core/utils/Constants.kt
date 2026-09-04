package com.speedo.core.utils

import android.content.Context
import android.content.SharedPreferences

object Constants {
    const val PREFS_NAME = "speedo_secure_prefs"
    const val KEY_AUTH_TOKEN = "jwt_auth_token"
    const val KEY_USER_ROLE = "auth_user_role"
    const val KEY_USER_ID = "auth_user_id"
    const val KEY_USER_NAME = "auth_user_name"
    const val KEY_USER_EMAIL = "auth_user_email"
    const val KEY_CUSTOM_BASE_URL = "custom_base_url"

    // Default API URL (Railway Live Cloud URL)
    const val DEFAULT_BASE_URL = "https://web-production-5d826.up.railway.app/api/"

    // Ola Maps API Key & Service Endpoints
    const val OLA_MAPS_API_KEY = "eyJhbGciOiJIUzI1NiJ9.eyJhIjoiYWNfOTJpbDFzMzIiLCJqdGkiOiIzM2U0ZWE5MyJ9.aBZsvaEcZVSpFU_4jJQpW90xqKJWW41-zct6mLYfVj4"
    const val OLA_MAPS_TILE_URL = "https://api.olamaps.io/tiles/v1/styles/default-light-standard/tiles/"

    // Free Leaflet / OpenStreetMap Standard Tile URL (No watermark, No API key required)
    const val LEAFLET_OSM_TILE_URL = "https://tile.openstreetmap.org/"

    // Polling Intervals in Milliseconds
    const val CAPTAIN_LOCATION_PUSH_INTERVAL_MS = 5000L // 5 seconds
    const val RIDER_TRACKING_POLL_INTERVAL_MS = 4000L   // 4 seconds
    const val CAPTAIN_RIDE_REQUEST_POLL_INTERVAL_MS = 4000L // 4 seconds
    const val NOTIFICATION_POLL_INTERVAL_MS = 12000L    // 12 seconds
    const val ADMIN_MAP_POLL_INTERVAL_MS = 6000L        // 6 seconds

    // Notification Channel IDs
    const val CHANNEL_RIDE_ALERTS = "speedo_ride_alerts"
    const val CHANNEL_LOCATION_SERVICE = "speedo_location_service"
    const val CHANNEL_KYC_UPDATES = "speedo_kyc_updates"
    const val CHANNEL_APP_UPDATES = "speedo_app_updates"
    const val CHANNEL_GENERAL = "speedo_general"

    const val NOTIFICATION_ID_APP_UPDATE = 10099

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setCustomBaseUrl(context: Context, url: String) {
        val formattedUrl = if (url.endsWith("/")) url else "$url/"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_BASE_URL, formattedUrl)
            .apply()
    }
}
