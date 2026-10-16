package com.speedo.core.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object Constants {
    private const val TAG = "SpeedoConstants"
    const val PREFS_NAME = "speedo_secure_prefs"
    const val KEY_AUTH_TOKEN = "jwt_auth_token"
    const val KEY_USER_ROLE = "auth_user_role"
    const val KEY_USER_ID = "auth_user_id"
    const val KEY_USER_NAME = "auth_user_name"
    const val KEY_USER_EMAIL = "auth_user_email"
    const val KEY_CUSTOM_BASE_URL = "custom_base_url"

    // Default API URL (Render Live Cloud URL)
    const val DEFAULT_BASE_URL = "https://speedo-backend-scfk.onrender.com/api/"

    // GitHub Remote Dynamic Server Config Beacon (Always Free, 100% Uptime, Instant OTA Server Switch)
    const val REMOTE_CONFIG_URL = "https://raw.githubusercontent.com/dawoodhussain504-jpg/sprrdo/main/server-config.json"

    // Google Maps Platform API Key (Official)
    const val GOOGLE_MAPS_API_KEY = "AIzaSyAl4c7ort-C7mVrM-4eZKvqucgWkt03X1E"

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
    const val NOTIFICATION_ID_APP_UPDATE_PROGRESS = 10100

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_CUSTOM_BASE_URL, null)
        
        // Auto-purge dead Railway URL from persistent storage
        if (saved != null && (saved.contains("railway") || saved.contains("up.railway.app"))) {
            Log.w(TAG, "Purging deprecated Railway URL from SharedPreferences: ")
            prefs.edit().remove(KEY_CUSTOM_BASE_URL).apply()
            return DEFAULT_BASE_URL
        }

        return saved ?: DEFAULT_BASE_URL
    }

    fun setCustomBaseUrl(context: Context, url: String) {
        var formatted = url.trim()
        if (!formatted.endsWith("/")) formatted = "/"
        if (!formatted.endsWith("api/")) formatted = "api/"

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_BASE_URL, formatted)
            .apply()
        Log.i(TAG, "Custom Base URL saved: ")
    }

    /**
     * Automatic Zero-Touch Remote Server Discovery & Auto-Sync
     * Fetches GitHub raw server-config.json in background. If server changed, dynamically migrates without user intervention!
     */
    fun syncWithRemoteServerBeacon(context: Context, onServerMigrated: ((String) -> Unit)? = null) {
        thread(isDaemon = true, name = "SpeedoServerDiscovery") {
            try {
                val discoveryUrl = URL("=")
                val conn = (discoveryUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "Speedo-Discovery/1.0")
                }
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val remoteUrl = json.optString("active_server_url", "").trim()
                    if (remoteUrl.isNotBlank()) {
                        var formatted = if (remoteUrl.endsWith("/")) remoteUrl else "/"
                        if (!formatted.endsWith("api/")) formatted = "api/"

                        val current = getBaseUrl(context)
                        if (!current.equals(formatted, ignoreCase = true)) {
                            Log.i(TAG, "🚀 [REMOTE SERVER DISCOVERY] Server migrated from  to ! Auto-updating app...")
                            setCustomBaseUrl(context, formatted)
                            onServerMigrated?.invoke(formatted)
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.d(TAG, "Remote server discovery silent pass: ")
            }
        }
    }
}