package com.speedo.core.repository

import android.content.Context
import com.speedo.core.database.NotificationDao
import com.speedo.core.database.NotificationEntity
import com.speedo.core.database.SpeedoDatabase
import com.speedo.core.model.NotificationItem
import com.speedo.core.network.NetworkResult
import com.speedo.core.network.RetrofitClient
import com.speedo.core.network.SpeedoApiService
import com.speedo.core.utils.BadgeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NotificationRepository(context: Context) {
    private val api: SpeedoApiService = RetrofitClient.getService(context)
    private val notifDao: NotificationDao = SpeedoDatabase.getDatabase(context).notificationDao()

    val cachedNotificationsFlow: Flow<List<NotificationItem>> = notifDao.getAllNotifications().map { list ->
        list.map { it.toDomainModel() }
    }

    val unreadCountFlow: Flow<Int> = notifDao.getUnreadCountFlow()

    suspend fun syncRiderNotifications(): NetworkResult<List<NotificationItem>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getRiderNotifications()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val notifs = res.body()!!.data!!
                notifDao.insertNotifications(notifs.map { NotificationEntity.fromDomainModel(it) })
                val unreadCount = notifs.count { it.isRead == 0 }
                BadgeHelper.updateUnreadCount(unreadCount)
                NetworkResult.Success(notifs)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to sync notifications")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun syncCaptainNotifications(): NetworkResult<List<NotificationItem>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getCaptainNotifications()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val notifs = res.body()!!.data!!
                notifDao.insertNotifications(notifs.map { NotificationEntity.fromDomainModel(it) })
                val unreadCount = notifs.count { it.isRead == 0 }
                BadgeHelper.updateUnreadCount(unreadCount)
                NetworkResult.Success(notifs)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to sync notifications")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun markRiderRead(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            notifDao.markAsRead(id)
            api.markRiderNotificationRead(id)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun markCaptainRead(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            notifDao.markAsRead(id)
            api.markCaptainNotificationRead(id)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }
}
