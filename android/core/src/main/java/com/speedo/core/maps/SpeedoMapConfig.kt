package com.speedo.core.maps

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.io.File

object SpeedoMapConfig {
    // 100% FREE, ULTRA-RELIABLE, ZERO WATERMARK LEAFLET / OPENSTREETMAP STANDARD TILES

    // Official Leaflet / OpenStreetMap Standard Tile Layer (Zoom 0-19)
    val LEAFLET_STANDARD_TILES = XYTileSource(
        "LeafletStandard",
        0, 19, 256, ".png",
        arrayOf(
            "https://a.tile.openstreetmap.org/",
            "https://b.tile.openstreetmap.org/",
            "https://c.tile.openstreetmap.org/"
        )
    )

    // OpenStreetMap Mapnik Fallback
    val OSM_STANDARD = TileSourceFactory.MAPNIK

    // Default primary tile source: Leaflet Standard
    val DEFAULT_TILE_SOURCE = LEAFLET_STANDARD_TILES

    fun init(context: Context) {
        try {
            val basePath = File(context.cacheDir, "osmdroid")
            val tileCache = File(basePath, "tiles")
            if (!basePath.exists()) basePath.mkdirs()
            if (!tileCache.exists()) tileCache.mkdirs()

            // Purge legacy caches (CartoVoyager, OpenStreetMapHOT, OpenStreetMapFR)
            val legacyFolders = listOf("CartoVoyager", "OpenStreetMapHOT", "OpenStreetMapFR")
            for (folderName in legacyFolders) {
                val folder = File(tileCache, folderName)
                if (folder.exists()) {
                    folder.deleteRecursively()
                }
            }

            val config = Configuration.getInstance()
            config.osmdroidBasePath = basePath
            config.osmdroidTileCache = tileCache
            config.userAgentValue = "SpeedoRideHailing/1.0.14 (Linux; Android ${android.os.Build.VERSION.RELEASE}; com.speedo)"
            config.load(context, context.getSharedPreferences("speedo_osmdroid_v4", Context.MODE_PRIVATE))
            config.isMapViewHardwareAccelerated = true
            config.expirationExtendedDuration = 1000L * 60 * 60 * 24 * 7 // 7 days cache
            config.tileFileSystemCacheMaxBytes = 250L * 1024 * 1024 // 250 MB
            config.tileFileSystemCacheTrimBytes = 200L * 1024 * 1024 // 200 MB
        } catch (e: Exception) {
            android.util.Log.e("SpeedoMapConfig", "Failed to configure Osmdroid storage cache", e)
        }
    }

    private var googleMapsInitialized = false

    fun initGoogleMaps(context: Context): Boolean {
        if (googleMapsInitialized) return true
        return try {
            val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            if (availability.isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                com.google.android.gms.maps.MapsInitializer.initialize(context)
                // Verify that BitmapDescriptorFactory is functional without throwing NPE
                com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker()
                googleMapsInitialized = true
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            android.util.Log.w("SpeedoMapConfig", "Google Maps SDK unavailable on this device: ${e.message}")
            googleMapsInitialized = false
            false
        }
    }

    fun isGoogleMapsReady(context: Context): Boolean {
        if (googleMapsInitialized) return true
        return initGoogleMaps(context)
    }
}
