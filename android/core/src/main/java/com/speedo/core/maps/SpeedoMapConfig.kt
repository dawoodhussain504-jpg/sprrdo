package com.speedo.core.maps

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.io.File

object SpeedoMapConfig {
    // 100% FREE, NO WATERMARK, NO API KEY REQUIRED TILE SERVERS

    // 1. OpenStreetMap Humanitarian (HOT) - Crystal clear roads, transit & building colors, 0 watermark
    val OSM_HOT = XYTileSource(
        "OpenStreetMapHOT",
        0, 19, 256, ".png",
        arrayOf(
            "https://a.tile.openstreetmap.fr/hot/",
            "https://b.tile.openstreetmap.fr/hot/",
            "https://c.tile.openstreetmap.fr/hot/"
        )
    )

    // 2. OpenStreetMap Standard (Worldwide Free Public CDN) - 0 watermark
    val OSM_STANDARD = XYTileSource(
        "OpenStreetMap",
        0, 19, 256, ".png",
        arrayOf(
            "https://a.tile.openstreetmap.org/",
            "https://b.tile.openstreetmap.org/",
            "https://c.tile.openstreetmap.org/"
        )
    )

    // 3. OpenStreetMap France - High definition road network & clear labels - 0 watermark
    val OSM_FRANCE = XYTileSource(
        "OpenStreetMapFR",
        0, 20, 256, ".png",
        arrayOf(
            "https://a.tile.openstreetmap.fr/osmfr/",
            "https://b.tile.openstreetmap.fr/osmfr/",
            "https://c.tile.openstreetmap.fr/osmfr/"
        )
    )

    // Default primary tile source: OSM_HOT (Vibrant, high-contrast, zero watermark)
    val DEFAULT_TILE_SOURCE = OSM_HOT

    fun init(context: Context) {
        try {
            val basePath = File(context.cacheDir, "osmdroid")
            val tileCache = File(basePath, "tiles")
            if (!basePath.exists()) basePath.mkdirs()
            if (!tileCache.exists()) tileCache.mkdirs()

            // Purge old watermarked Carto tiles if present in cache
            val oldCartoFolder = File(tileCache, "CartoVoyager")
            if (oldCartoFolder.exists()) {
                oldCartoFolder.deleteRecursively()
            }

            val config = Configuration.getInstance()
            config.osmdroidBasePath = basePath
            config.osmdroidTileCache = tileCache
            config.userAgentValue = "SpeedoApp-RideHailing/${context.packageName} (Android ${android.os.Build.VERSION.RELEASE}; Linux)"
            config.load(context, context.getSharedPreferences("speedo_osmdroid_v2", Context.MODE_PRIVATE))
            config.isMapViewHardwareAccelerated = true
            config.expirationExtendedDuration = 1000L * 60 * 60 * 24 * 7 // 7 days cache
            config.tileFileSystemCacheMaxBytes = 150L * 1024 * 1024 // 150 MB
            config.tileFileSystemCacheTrimBytes = 120L * 1024 * 1024 // 120 MB
        } catch (e: Exception) {
            android.util.Log.e("SpeedoMapConfig", "Failed to configure Osmdroid storage cache", e)
        }
    }
}
