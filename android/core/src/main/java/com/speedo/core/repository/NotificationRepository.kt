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
    private val appVersionRepo: AppVersionRepository = AppVersionRepository.getInstance(context)

    val cachedNotificationsFlow: Flow<List<NotificationItem>> = notifDao.getAllNotifications().map { list ->
        val (currentCode, currentName) = appVersionRepo.getInstalledVersionInfo()
        list.map { it.toDomainModel() }.filter {
            !it.isOldUpdateFor(currentCode, isUpdateAvailable = false, currentVersionName = currentName)
        }
    }

    val unreadCountFlow: Flow<Int> = notifDao.getUnreadCountFlow()

    private suspend fun pruneOldUpdateNotifications(currentCode: Int, currentName: String) {
        try {
            val allCached = notifDao.getAllCachedList()
            allCached.forEach { entity ->
                val domain = entity.toDomainModel()
                if (domain.isOldUpdateFor(currentCode, isUpdateAvailable = false, currentVersionName = currentName)) {
                    notifDao.removeNotification(entity.id)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun syncRiderNotifications(): NetworkResult<List<NotificationItem>> = withContext(Dispatchers.IO) {
        try {
            val (currentCode, currentName) = appVersionRepo.getInstalledVersionInfo()
            val res = api.getRiderNotifications(currentCode)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val notifs = res.body()!!.data!!
                val freshNotifs = notifs.filter {
                    !it.isOldUpdateFor(currentCode, isUpdateAvailable = false, currentVersionName = currentName)
                }
                notifDao.insertNotifications(freshNotifs.map { NotificationEntity.fromDomainModel(it) })
                pruneOldUpdateNotifications(currentCode, currentName)
                val unreadCount = notifDao.getUnreadCount()
                BadgeHelper.updateUnreadCount(unreadCount)
                NetworkResult.Success(freshNotifs)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to sync notifications")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun syncCaptainNotifications(): NetworkResult<List<NotificationItem>> = withContext(Dispatchers.IO) {
        try {
            val (currentCode, currentName) = appVersionRepo.getInstalledVersionInfo()
            val res = api.getCaptainNotifications(currentCode)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val notifs = res.body()!!.data!!
                val freshNotifs = notifs.filter {
                    !it.isOldUpdateFor(currentCode, isUpdateAvailable = false, currentVersionName = currentName)
                }
                notifDao.insertNotifications(freshNotifs.map { NotificationEntity.fromDomainModel(it) })
                pruneOldUpdateNotifications(currentCode, currentName)
                val unreadCount = notifDao.getUnreadCount()
                BadgeHelper.updateUnreadCount(unreadCount)
                NetworkResult.Success(freshNotifs)
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
            notifDao.removeNotification(id)
            val unreadCount = notifDao.getUnreadCount()
            BadgeHelper.updateUnreadCount(unreadCount)
            api.markRiderNotificationRead(id)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun markCaptainRead(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            notifDao.markAsRead(id)
            notifDao.removeNotification(id)
            val unreadCount = notifDao.getUnreadCount()
            BadgeHelper.updateUnreadCount(unreadCount)
            api.markCaptainNotificationRead(id)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }
}
