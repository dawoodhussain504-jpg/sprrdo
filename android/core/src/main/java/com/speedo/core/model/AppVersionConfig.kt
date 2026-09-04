package com.speedo.core.model

import com.google.gson.annotations.SerializedName

data class AppVersionConfig(
    @SerializedName("appId") val appId: String = "rider",
    @SerializedName("appName") val appName: String = "Speedo",
    @SerializedName("latestVersionCode") val latestVersionCode: Int = 1,
    @SerializedName("latestVersionName") val latestVersionName: String = "1.0.0",
    @SerializedName("minSupportedVersionCode") val minSupportedVersionCode: Int = 1,
    @SerializedName("forceUpdate") val forceUpdate: Boolean = false,
    @SerializedName("title") val title: String = "Update Available 🚀",
    @SerializedName("message") val message: String = "A new update is available with new features and improvements.",
    @SerializedName("releaseNotes") val releaseNotes: String? = null,
    @SerializedName("updateUrl") val updateUrl: String = "https://play.google.com/store/apps/details?id=com.speedo",
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("isUpdateAvailable") val isUpdateAvailable: Boolean = false,
    @SerializedName("isForceUpdate") val isForceUpdate: Boolean = false
)

data class AppUpdatePromptState(
    val isUpdateAvailable: Boolean = false,
    val isForceUpdate: Boolean = false,
    val config: AppVersionConfig? = null,
    val currentVersionCode: Int = 1,
    val currentVersionName: String = "1.0.0",
    val isDismissed: Boolean = false
)
