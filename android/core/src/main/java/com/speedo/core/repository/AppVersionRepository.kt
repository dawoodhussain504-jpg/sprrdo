package com.speedo.core.repository

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.speedo.core.model.AppUpdatePromptState
import com.speedo.core.model.AppVersionConfig
import com.speedo.core.network.NetworkResult
import com.speedo.core.network.RetrofitClient
import com.speedo.core.network.SpeedoApiService
import com.speedo.core.socket.SpeedoSocketManager
import com.speedo.core.utils.NotificationHelper
import kotlinx.coroutines.flow.SharedFlow

class AppVersionRepository(private val context: Context) {
    private val apiService: SpeedoApiService = RetrofitClient.getService(context)
    private val socketManager = SpeedoSocketManager.getInstance(context)

    val liveVersionUpdatedFlow: SharedFlow<AppVersionConfig> = socketManager.appVersionUpdatedFlow

    fun getInstalledVersionInfo(): Pair<Int, String> {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = PackageInfoCompat.getLongVersionCode(pInfo).toInt()
            val versionName = pInfo.versionName ?: "1.0.0"
            Pair(versionCode, versionName)
        } catch (e: Exception) {
            Pair(1, "1.0.0")
        }
    }

    private fun isCurrentVersionAtLeast(currentName: String, latestName: String): Boolean {
        val currentParts = currentName.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latestName.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (c > l) return true
            if (c < l) return false
        }
        return true
    }

    suspend fun checkAppVersion(appId: String): AppUpdatePromptState {
        val (currentCode, currentName) = getInstalledVersionInfo()
        return try {
            val res = apiService.getAppVersion(app = appId, currentVersion = currentCode)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val config = res.body()!!.data!!
                // If user is already on latest version (or newer), do NOT show update dialog under any circumstances!
                val isAlreadyLatest = currentCode >= config.latestVersionCode || isCurrentVersionAtLeast(currentName, config.latestVersionName)
                val hasUpdate = !isAlreadyLatest && config.isActive

                val prefs = context.getSharedPreferences("speedo_update_prefs", Context.MODE_PRIVATE)
                val dismissedVersion = prefs.getInt("dismissed_update_version_code", 0)
                val isDismissed = isAlreadyLatest || (dismissedVersion >= config.latestVersionCode)

                val isForce = hasUpdate && (config.forceUpdate || (currentCode < config.minSupportedVersionCode))
                val isAvailable = hasUpdate

                if (isAvailable && !isAlreadyLatest) {
                    NotificationHelper.showAppUpdateNotification(
                        context = context,
                        title = config.title,
                        message = config.message,
                        updateUrl = config.updateUrl,
                        versionName = config.latestVersionName
                    )
                }

                AppUpdatePromptState(
                    isUpdateAvailable = if (isAlreadyLatest) false else isAvailable,
                    isForceUpdate = if (isAlreadyLatest) false else isForce,
                    config = config,
                    currentVersionCode = currentCode,
                    currentVersionName = currentName,
                    isDismissed = isDismissed
                )
            } else {
                AppUpdatePromptState(
                    isUpdateAvailable = false,
                    isForceUpdate = false,
                    currentVersionCode = currentCode,
                    currentVersionName = currentName
                )
            }
        } catch (e: Exception) {
            AppUpdatePromptState(
                isUpdateAvailable = false,
                isForceUpdate = false,
                currentVersionCode = currentCode,
                currentVersionName = currentName
            )
        }
    }

    suspend fun getAdminAppVersions(): NetworkResult<List<AppVersionConfig>> {
        return try {
            val res = apiService.getAdminAppVersions()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch app versions")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error fetching app versions")
        }
    }

    suspend fun updateAppVersion(appId: String, config: AppVersionConfig): NetworkResult<AppVersionConfig> {
        return try {
            val res = apiService.updateAppVersion(appId, config)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to update app version configuration")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error updating app version")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppVersionRepository? = null

        fun getInstance(context: Context): AppVersionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppVersionRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
