package com.speedo.rider

import android.app.Application
import com.speedo.core.maps.SpeedoMapConfig
import com.speedo.core.utils.NotificationHelper
import com.speedo.core.work.WorkScheduler

class RiderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize osmdroid internal cache, User-Agent, and tile storage configuration safely
        SpeedoMapConfig.init(this)

        NotificationHelper.createNotificationChannels(this)
        WorkScheduler.schedulePeriodicNotificationPolling(this)
    }
}
