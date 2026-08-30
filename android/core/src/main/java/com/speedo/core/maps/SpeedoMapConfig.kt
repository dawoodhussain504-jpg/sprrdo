package com.speedo.core.maps

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.io.File

object SpeedoMapConfig {
    // Ultra fast, CDN-accelerated raster tiles with complete road networks & landmarks
    val CARTO_VOYAGER = XYTileSource(
        "CartoVoyager",
        0, 20, 256, ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
        )
    )

    val OSM_STANDARD = XYTileSource(
        "OpenStreetMap",
        0, 19, 256, ".png",
        arrayOf(
            "https://tile.openstreetmap.org/"
        )
    )

    fun init(context: Context) {
        try {
            val basePath = File(context.cacheDir, "osmdroid")
            val tileCache = File(basePath, "tiles")
            if (!basePath.exists()) basePath.mkdirs()
            if (!tileCache.exists()) tileCache.mkdirs()

            val config = Configuration.getInstance()
            config.osmdroidBasePath = basePath
            config.osmdroidTileCache = tileCache
            config.userAgentValue = "SpeedoRideHailing/2.0 (${context.packageName}; Android ${android.os.Build.VERSION.RELEASE})"
            config.load(context, context.getSharedPreferences("speedo_osmdroid", Context.MODE_PRIVATE))
            config.isMapViewHardwareAccelerated = true
            config.expirationExtendedDuration = 1000L * 60 * 60 * 24 * 7 // 7 days cache
            config.tileFileSystemCacheMaxBytes = 100L * 1024 * 1024 // 100 MB
            config.tileFileSystemCacheTrimBytes = 80L * 1024 * 1024 // 80 MB
        } catch (e: Exception) {
            android.util.Log.e("SpeedoMapConfig", "Failed to configure Osmdroid storage cache", e)
        }
    }
}
