package com.speedo.core.utils

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.speedo.core.service.SpeedoFirebaseMessagingService
import com.speedo.core.storage.TokenManager

object SpeedoFcmManager {

    private const val TAG = "SpeedoFcmManager"

    /**
     * Initializes FCM device registration and syncs the push token with Speedo backend.
     * Safe across all devices; gracefully handles missing Google Play Services or unconfigured Firebase.
     */
    fun initialize(context: Context) {
        try {
            // 1. Ensure system notification channels exist
            NotificationHelper.createNotificationChannels(context)

            // 2. Fetch current FCM device registration token
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed: ${task.exception?.message}")
                    return@addOnCompleteListener
                }

                val token = task.result
                if (!token.isNullOrEmpty()) {
                    Log.d(TAG, "Current FCM Device Token: $token")
                    val tokenManager = TokenManager.getInstance(context)
                    tokenManager.saveFcmToken(token)

                    // Sync token to backend
                    SpeedoFirebaseMessagingService.syncTokenWithBackend(context, token)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FCM initialization skipped or unavailable: ${e.message}")
        }
    }

    /**
     * Explicitly resyncs token (e.g. called immediately after user logs in)
     */
    fun onUserLoggedIn(context: Context) {
        try {
            val tokenManager = TokenManager.getInstance(context)
            val token = tokenManager.getFcmToken()
            if (!token.isNullOrEmpty()) {
                SpeedoFirebaseMessagingService.syncTokenWithBackend(context, token)
            } else {
                initialize(context)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing FCM token after login: ${e.message}")
        }
    }
}
