package com.speedo.core.work

import android.content.Context
import androidx.work.*
import com.speedo.core.network.RetrofitClient
import com.speedo.core.storage.TokenManager
import com.speedo.core.utils.BadgeHelper
import com.speedo.core.utils.NotificationHelper
import java.util.concurrent.TimeUnit

class NotificationPollingWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val tokenManager = TokenManager(context)
        if (!tokenManager.isLoggedIn()) {
            return Result.success()
        }

        val role = tokenManager.getUserRole() ?: return Result.success()
        val api = RetrofitClient.getService(context)

        return try {
            val response = when (role) {
                "rider" -> api.getRiderUnreadCount()
                "captain" -> api.getCaptainUnreadCount()
                else -> null
            }

            if (response != null && response.isSuccessful) {
                val count = response.body()?.count ?: 0
                BadgeHelper.updateUnreadCount(count)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

object WorkScheduler {
    private const val NOTIFICATION_WORK_TAG = "speedo_notification_sync"

    fun schedulePeriodicNotificationPolling(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NotificationPollingWorker>(
            15, TimeUnit.MINUTES // Standard WorkManager minimum interval for background battery compliance
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOTIFICATION_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NOTIFICATION_WORK_TAG)
    }
}
