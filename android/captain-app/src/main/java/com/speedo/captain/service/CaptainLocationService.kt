package com.speedo.captain.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.speedo.captain.ui.CaptainMainActivity
import com.speedo.core.network.RetrofitClient
import com.speedo.core.socket.SpeedoSocketManager
import com.speedo.core.storage.TokenManager
import com.speedo.core.utils.Constants
import kotlinx.coroutines.*

class CaptainLocationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isTracking = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    pushLocationToBackend(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_SERVICE) {
            stopTracking()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!isTracking) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
                startTracking()
            } catch (e: Exception) {
                android.util.Log.e("CaptainLocationService", "Failed to start foreground service", e)
            }
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        isTracking = true
        SpeedoSocketManager.getInstance(this).connect()

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.CAPTAIN_LOCATION_PUSH_INTERVAL_MS
        ).setMinUpdateIntervalMillis(Constants.CAPTAIN_LOCATION_PUSH_INTERVAL_MS / 2)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Permission missing
        }
    }

    private fun stopTracking() {
        isTracking = false
        SpeedoSocketManager.getInstance(this).disconnect()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun pushLocationToBackend(location: Location) {
        val tokenManager = TokenManager(this)
        if (!tokenManager.isLoggedIn()) return

        // 1. Sub-second WebSocket push
        SpeedoSocketManager.getInstance(this).emitCaptainLocation(
            lat = location.latitude,
            lng = location.longitude,
            bearing = location.bearing,
            speed = location.speed,
            isOnline = true
        )

        // 2. HTTP persistence fallback
        serviceScope.launch {
            try {
                val api = RetrofitClient.getService(this@CaptainLocationService)
                api.updateCaptainLocation(
                    mapOf(
                        "lat" to location.latitude,
                        "lng" to location.longitude,
                        "bearing" to location.bearing.toDouble(),
                        "speed" to location.speed.toDouble()
                    )
                )
            } catch (e: Exception) {
                // Background push retry next interval
            }
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, CaptainMainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.CHANNEL_LOCATION_SERVICE)
            .setContentTitle("Speedo Captain Online 🛵")
            .setContentText("Broadcasting live location. You are receiving ride requests.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_LOCATION_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_LOCATION_SERVICE"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            try {
                val intent = Intent(context, CaptainLocationService::class.java).apply {
                    action = ACTION_START_SERVICE
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("CaptainLocationService", "Failed to start location service", e)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, CaptainLocationService::class.java).apply {
                    action = ACTION_STOP_SERVICE
                }
                context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.e("CaptainLocationService", "Failed to stop location service", e)
            }
        }
    }
}
