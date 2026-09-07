package com.speedo.core.maps

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.io.File

object SpeedoMapConfig {
    // 100% FREE, ULTRA-RELIABLE, ZERO WATERMARK, HIGH-CONTRAST TILE SOURCES

    // 1. Carto Voyager - Clean, high-definition road network, building footprints, zero watermark (Lightning-Fast Global CDN)
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

    // 2. OpenStreetMap Standard (Worldwide Free Public Mapnik)
    val OSM_STANDARD = TileSourceFactory.MAPNIK

    // Default primary tile source: Carto Voyager (High contrast, vibrant streets, ultra-reliable tile loading)
    val DEFAULT_TILE_SOURCE = CARTO_VOYAGER

    fun init(context: Context) {
        try {
            val basePath = File(context.cacheDir, "osmdroid")
            val tileCache = File(basePath, "tiles")
            if (!basePath.exists()) basePath.mkdirs()
            if (!tileCache.exists()) tileCache.mkdirs()

            // Purge any corrupted 103-byte blank tiles from previous OpenStreetMapHOT/FR cache
            val corruptedHotFolder = File(tileCache, "OpenStreetMapHOT")
            if (corruptedHotFolder.exists()) {
                corruptedHotFolder.deleteRecursively()
            }
            val corruptedFrFolder = File(tileCache, "OpenStreetMapFR")
            if (corruptedFrFolder.exists()) {
                corruptedFrFolder.deleteRecursively()
            }

            val config = Configuration.getInstance()
            config.osmdroidBasePath = basePath
            config.osmdroidTileCache = tileCache
            config.userAgentValue = "Mozilla/5.0 (Linux; Android ${android.os.Build.VERSION.RELEASE}; Mobile) SpeedoApp/${context.packageName}"
            config.load(context, context.getSharedPreferences("speedo_osmdroid_v3", Context.MODE_PRIVATE))
            config.isMapViewHardwareAccelerated = true
            config.expirationExtendedDuration = 1000L * 60 * 60 * 24 * 7 // 7 days cache
            config.tileFileSystemCacheMaxBytes = 250L * 1024 * 1024 // 250 MB
            config.tileFileSystemCacheTrimBytes = 200L * 1024 * 1024 // 200 MB
        } catch (e: Exception) {
            android.util.Log.e("SpeedoMapConfig", "Failed to configure Osmdroid storage cache", e)
        }
    }
}
