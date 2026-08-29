package com.speedo.rider

import android.app.Application
import android.content.Context
import com.speedo.core.utils.NotificationHelper
import com.speedo.core.work.WorkScheduler
import org.osmdroid.config.Configuration

class RiderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize osmdroid tile & cache configuration safely before any view loads
        Configuration.getInstance().load(this, getSharedPreferences("speedo_osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        NotificationHelper.createNotificationChannels(this)
        WorkScheduler.schedulePeriodicNotificationPolling(this)
    }
}
