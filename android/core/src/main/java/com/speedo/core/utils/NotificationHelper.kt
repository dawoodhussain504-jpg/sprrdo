package com.speedo.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.speedo.core.R

object NotificationHelper {

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            // 1. Ride Alerts Channel (High priority with sound & vibration)
            val rideChannel = NotificationChannel(
                Constants.CHANNEL_RIDE_ALERTS,
                "Speedo Ride Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for incoming rides, captain arrival, and ride updates"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(defaultSoundUri, audioAttributes)
            }

            // 2. Foreground Location Service Channel
            val locationChannel = NotificationChannel(
                Constants.CHANNEL_LOCATION_SERVICE,
                "Speedo Captain Location Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background GPS location tracking while online"
                setShowBadge(false)
            }

            // 3. KYC Updates Channel
            val kycChannel = NotificationChannel(
                Constants.CHANNEL_KYC_UPDATES,
                "KYC Verification Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Status updates regarding your KYC document submission"
            }

            // 4. App Updates Channel (High priority with sound & vibration)
            val updateChannel = NotificationChannel(
                Constants.CHANNEL_APP_UPDATES,
                "Speedo App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for new app versions and Over-the-Air updates"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(defaultSoundUri, audioAttributes)
            }

            // 5. General Notifications Channel
            val generalChannel = NotificationChannel(
                Constants.CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationManager.createNotificationChannels(
                listOf(rideChannel, locationChannel, kycChannel, updateChannel, generalChannel)
            )
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = Constants.CHANNEL_RIDE_ALERTS,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt(),
        intent: Intent? = null
    ) {
        val pendingIntent = if (intent != null) {
            PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            // Safe fallback if notification permission is missing or blocked on Android 13+
        }
    }

    fun showAppUpdateNotification(
        context: Context,
        title: String,
        message: String,
        updateUrl: String,
        versionName: String? = null
    ) {
        // User requested: NO system notification banner for app updates in Android shade.
        // In-app download & update dialog is enough. Cancel any existing update banners.
        cancelUpdateNotification(context)
    }

    fun showDownloadProgressNotification(
        context: Context,
        progressPercent: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
        // In-app progress indicator is shown directly inside the app dialog.
    }

    fun cancelDownloadProgressNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(Constants.NOTIFICATION_ID_APP_UPDATE_PROGRESS)
        } catch (_: Exception) {}
    }

    fun showUpdateDownloadedNotification(
        context: Context,
        apkFile: java.io.File
    ) {
        // User requested: NO notification banner for app updates. In-app installer launch is enough.
        cancelUpdateNotification(context)
    }

    fun cancelUpdateNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(Constants.NOTIFICATION_ID_APP_UPDATE)
            NotificationManagerCompat.from(context).cancel(Constants.NOTIFICATION_ID_APP_UPDATE_PROGRESS)
        } catch (_: Exception) {}
    }
}
