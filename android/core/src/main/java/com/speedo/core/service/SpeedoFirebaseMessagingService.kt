package com.speedo.core.service

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.speedo.core.network.RetrofitClient
import com.speedo.core.storage.TokenManager
import com.speedo.core.utils.Constants
import com.speedo.core.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class SpeedoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token generated: $token")

        // 1. Cache token in TokenManager
        val tokenManager = TokenManager.getInstance(applicationContext)
        tokenManager.saveFcmToken(token)

        // 2. Upload to Speedo backend if user is authenticated
        syncTokenWithBackend(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Push notification received from: ${remoteMessage.from}")

        // Ensure notification channels exist
        NotificationHelper.createNotificationChannels(applicationContext)

        // Extract Title & Message
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Speedo Update"

        val message = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "You have a new update from Speedo."

        val type = remoteMessage.data["type"] ?: "general"
        val channelId = remoteMessage.data["channelId"] ?: when {
            type.startsWith("ride_") -> Constants.CHANNEL_RIDE_ALERTS
            type.startsWith("kyc_") -> Constants.CHANNEL_KYC_UPDATES
            type == "app_update" -> Constants.CHANNEL_APP_UPDATES
            else -> Constants.CHANNEL_GENERAL
        }

        // Show heads-up notification with sound & vibration in the system tray
        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            message = message,
            channelId = channelId
        )
    }

    companion object {
        private const val TAG = "SpeedoFCM"

        fun syncTokenWithBackend(context: Context, token: String? = null) {
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope.launch {
                try {
                    val tokenManager = TokenManager.getInstance(context)
                    val activeToken = token ?: tokenManager.getFcmToken()
                    val userId = tokenManager.getUserId()
                    val role = tokenManager.getUserRole() ?: "rider"

                    if (!activeToken.isNullOrEmpty() && !userId.isNullOrEmpty()) {
                        val api = RetrofitClient.getService(context)
                        val body = mapOf(
                            "token" to activeToken,
                            "userId" to userId,
                            "role" to role,
                            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                            "osVersion" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                        )
                        api.registerFcmToken(body)
                        Log.d(TAG, "Successfully synced FCM token with backend for user: $userId ($role)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "FCM token background sync skipped: ${e.message}")
                }
            }
        }
    }
}
