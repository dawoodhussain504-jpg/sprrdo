package com.speedo.core.maps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.speedo.core.model.LocationPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.*

object DistanceUtils {
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val dist = r * c
        return round(dist * 100) / 100
    }

    fun calculateEtaMinutes(distanceKm: Double, averageSpeedKmh: Double = 25.0): Int {
        val safeDist = max(0.2, distanceKm)
        val hours = safeDist / averageSpeedKmh
        return max(1, (hours * 60).roundToInt())
    }

    fun formatDistance(distanceKm: Double): String {
        return if (distanceKm < 1.0) {
            "${(distanceKm * 1000).toInt()} m"
        } else {
            String.format("%.1f km", distanceKm)
        }
    }

    fun formatDuration(minutes: Int): String {
        return if (minutes < 60) {
            "$minutes min"
        } else {
            val hrs = minutes / 60
            val mins = minutes % 60
            "${hrs}h ${mins}m"
        }
    }
}

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLiveLocation(onSuccess: (LocationPoint) -> Unit, onFailure: () -> Unit = {}) {
        try {
            val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { freshLoc: Location? ->
                if (freshLoc != null) {
                    onSuccess(
                        LocationPoint(
                            lat = freshLoc.latitude,
                            lng = freshLoc.longitude,
                            bearing = freshLoc.bearing,
                            speed = freshLoc.speed
                        )
                    )
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            onSuccess(
                                LocationPoint(
                                    lat = lastLoc.latitude,
                                    lng = lastLoc.longitude,
                                    bearing = lastLoc.bearing,
                                    speed = lastLoc.speed
                                )
                            )
                        } else {
                            onFailure()
                        }
                    }.addOnFailureListener { onFailure() }
                }
            }.addOnFailureListener {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                    if (lastLoc != null) {
                        onSuccess(
                            LocationPoint(
                                lat = lastLoc.latitude,
                                lng = lastLoc.longitude,
                                bearing = lastLoc.bearing,
                                speed = lastLoc.speed
                            )
                        )
                    } else {
                        onFailure()
                    }
                }.addOnFailureListener { onFailure() }
            }
        } catch (e: Exception) {
            onFailure()
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastLocation(onSuccess: (LocationPoint) -> Unit, onFailure: () -> Unit = {}) {
        getCurrentLiveLocation(onSuccess, onFailure)
    }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 5000L): Flow<LocationPoint> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(
                        LocationPoint(
                            lat = loc.latitude,
                            lng = loc.longitude,
                            bearing = loc.bearing,
                            speed = loc.speed
                        )
                    )
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}

data class AddressSuggestion(
    val title: String,
    val subtitle: String,
    val fullAddress: String,
    val lat: Double,
    val lng: Double,
    val distanceKm: Double? = null
)

object LocationSearchHelper {
    private const val TAG = "LocationSearchHelper"

    // Comprehensive offline & instant Bangalore + Metro Landmarks
    private val PRESET_LANDMARKS = listOf(
        AddressSuggestion("Indiranagar 100ft Road", "Indiranagar, Bangalore", "Indiranagar 100ft Road, Bangalore, Karnataka", 12.9716, 77.5946),
        AddressSuggestion("Koramangala 5th Block", "Koramangala, Bangalore", "Koramangala 5th Block, Bangalore, Karnataka", 12.9352, 77.6245),
        AddressSuggestion("HSR Layout Sector 1", "HSR Layout, Bangalore", "HSR Layout Sector 1, Bangalore, Karnataka", 12.9121, 77.6446),
        AddressSuggestion("MG Road Metro Station", "MG Road, Bangalore", "MG Road Metro Station, Shivaji Nagar, Bangalore", 12.9756, 77.6066),
        AddressSuggestion("Whitefield ITPL Main Gate", "ITPL, Whitefield, Bangalore", "ITPL Main Gate, Whitefield, Bangalore, Karnataka", 12.9850, 77.7289),
        AddressSuggestion("Electronic City Phase 1", "Electronic City, Bangalore", "Electronic City Phase 1, Bangalore, Karnataka", 12.8399, 77.6770),
        AddressSuggestion("Forum Mall, Koramangala", "Hosur Road, Koramangala", "Forum Mall, Hosur Road, Koramangala, Bangalore", 12.9345, 77.6112),
        AddressSuggestion("Phoenix Marketcity", "Mahadevapura, Whitefield Road", "Phoenix Marketcity, Whitefield Road, Bangalore", 12.9959, 77.6963),
        AddressSuggestion("Orion Mall & World Trade Center", "Rajajinagar, Bangalore", "Orion Mall, Brigade Gateway, Rajajinagar, Bangalore", 13.0112, 77.5550),
        AddressSuggestion("Kempegowda International Airport (BLR)", "Devanahalli, Bangalore", "Kempegowda International Airport, Devanahalli, Bangalore", 13.1986, 77.7066),
        AddressSuggestion("KSR Bengaluru City Railway Station", "Majestic, Bangalore", "KSR Bengaluru Railway Station, Majestic, Bangalore", 12.9781, 77.5696),
        AddressSuggestion("Yeshwanthpur Junction Railway Station", "Yeshwanthpur, Bangalore", "Yeshwanthpur Railway Station, Bangalore", 13.0238, 77.5503),
        AddressSuggestion("Manyata Tech Park", "Nagavara, Hebbal Outer Ring Road", "Manyata Tech Park, Nagavara, Bangalore", 13.0475, 77.6197),
        AddressSuggestion("Bagmane Tech Park", "CV Raman Nagar, Bangalore", "Bagmane Tech Park, CV Raman Nagar, Bangalore", 12.9796, 77.6574),
        AddressSuggestion("Ecospace Business Park", "Bellandur Outer Ring Road", "RMZ Ecospace, Bellandur, Bangalore", 12.9260, 77.6841),
        AddressSuggestion("Brigade Road & Commercial Street", "Central Bangalore", "Brigade Road Shopping Street, Bangalore", 12.9733, 77.6074),
        AddressSuggestion("Jayanagar 4th Block Complex", "Jayanagar, Bangalore", "Jayanagar 4th Block Shopping Complex, Bangalore", 12.9298, 77.5833),
        AddressSuggestion("BTM Layout 2nd Stage", "BTM Layout, Bangalore", "BTM Layout 2nd Stage, Bangalore, Karnataka", 12.9166, 77.6101),
        AddressSuggestion("Marathahalli Bridge", "Marathahalli, Bangalore", "Marathahalli Bridge, Outer Ring Road, Bangalore", 12.9569, 77.7011),
        AddressSuggestion("Silk Board Junction", "Hosur Road / Outer Ring Road", "Central Silk Board Junction, Bangalore", 12.9177, 77.6238),
        AddressSuggestion("Bannerghatta Road (Gopalan Mall)", "Bannerghatta Main Road", "Bannerghatta Main Road, Bangalore", 12.8988, 77.5996),
        AddressSuggestion("Hebbal Flyover", "Bellary Road, Hebbal", "Hebbal Flyover, Bangalore, Karnataka", 13.0358, 77.5970),
        AddressSuggestion("Malleshwaram 8th Cross", "Malleshwaram, Bangalore", "8th Cross Road, Malleshwaram, Bangalore", 13.0031, 77.5701),
        AddressSuggestion("Yelahanka New Town", "Yelahanka, North Bangalore", "Yelahanka New Town, Bangalore, Karnataka", 13.0998, 77.5963),
        AddressSuggestion("Sarjapur Road Wipro Gate", "Sarjapur Road, Bangalore", "Wipro Corporate Office, Sarjapur Road, Bangalore", 12.9103, 77.6847),
        AddressSuggestion("Banashankari 3rd Stage", "Banashankari, Bangalore", "Banashankari 3rd Stage, Bangalore, Karnataka", 12.9255, 77.5468),
        AddressSuggestion("Kalyan Nagar HRBR Layout", "Kalyan Nagar, Bangalore", "HRBR Layout, Kalyan Nagar, Bangalore", 13.0199, 77.6496),
        AddressSuggestion("Cubbon Park Metro Station", "Kasturba Road, Bangalore", "Cubbon Park, Ambedkar Veedhi, Bangalore", 12.9763, 77.5929),
        AddressSuggestion("Lalbagh Botanical Garden", "Mavalli, Bangalore", "Lalbagh Botanical Garden Main Gate, Bangalore", 12.9507, 77.5848),
        AddressSuggestion("Manipal Hospital Old Airport Road", "Kodihalli, HAL Airport Road", "Manipal Hospital, Old Airport Road, Bangalore", 12.9592, 77.6499),
        AddressSuggestion("Apollo Hospitals Bannerghatta", "Bannerghatta Road, Bangalore", "Apollo Hospitals, Bannerghatta Road, Bangalore", 12.8943, 77.5986),
        AddressSuggestion("Narayana Health City", "Bommasandra Industrial Area", "Narayana Institute of Cardiac Sciences, Bangalore", 12.8091, 77.6974)
    )

    suspend fun searchAddress(
        context: Context,
        query: String,
        userLat: Double = 12.9716,
        userLng: Double = 77.5946
    ): List<AddressSuggestion> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return@withContext PRESET_LANDMARKS.take(8).map {
                val dist = DistanceUtils.calculateDistanceKm(userLat, userLng, it.lat, it.lng)
                it.copy(distanceKm = dist)
            }
        }

        val results = mutableListOf<AddressSuggestion>()

        // 1. Try Ola Maps Places Autocomplete API using user's Ola Maps API Key
        try {
            val encoded = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
            val urlStr = "https://api.olamaps.io/places/v1/autocomplete?input=$encoded&location=$userLat,$userLng&api_key=${com.speedo.core.utils.Constants.OLA_MAPS_API_KEY}"
            val url = java.net.URL(urlStr)
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("Content-Type", "application/json")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val root = com.google.gson.JsonParser.parseString(responseText).asJsonObject
                val predictions = root.getAsJsonArray("predictions")

                predictions?.forEach { elem ->
                    val pred = elem.asJsonObject
                    val desc = pred.get("description")?.asString ?: cleanQuery
                    val struct = pred.getAsJsonObject("structured_formatting")
                    val mainText = struct?.get("main_text")?.asString ?: desc.split(",").firstOrNull() ?: cleanQuery
                    val secondaryText = struct?.get("secondary_text")?.asString ?: desc

                    val geom = pred.getAsJsonObject("geometry")?.getAsJsonObject("location")
                    val lat = geom?.get("lat")?.asDouble ?: userLat
                    val lng = geom?.get("lng")?.asDouble ?: userLng
                    val dist = DistanceUtils.calculateDistanceKm(userLat, userLng, lat, lng)

                    results.add(
                        AddressSuggestion(
                            title = mainText,
                            subtitle = secondaryText,
                            fullAddress = desc,
                            lat = lat,
                            lng = lng,
                            distanceKm = dist
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Ola Maps autocomplete fallback: ${e.message}")
        }

        // 2. Try Photon OpenStreetMap Autocomplete API
        if (results.isEmpty()) {
            try {
                val encoded = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
                val urlStr = "https://photon.komoot.io/api/?q=$encoded&lat=$userLat&lon=$userLng&limit=8"
                val url = java.net.URL(urlStr)
                val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                    setRequestProperty("User-Agent", "SpeedoRiderApp/2.0")
                }

                if (connection.responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val root = com.google.gson.JsonParser.parseString(responseText).asJsonObject
                    val features = root.getAsJsonArray("features")

                    features?.forEach { element ->
                        val feature = element.asJsonObject
                        val properties = feature.getAsJsonObject("properties")
                        val geometry = feature.getAsJsonObject("geometry")
                        val coords = geometry.getAsJsonArray("coordinates")

                        val lng = coords[0].asDouble
                        val lat = coords[1].asDouble

                        val name = properties.get("name")?.asString ?: cleanQuery
                        val street = properties.get("street")?.asString
                        val city = properties.get("city")?.asString ?: properties.get("county")?.asString ?: "Bangalore"
                        val state = properties.get("state")?.asString ?: "Karnataka"

                        val subtitle = listOfNotNull(street, city, state).joinToString(", ")
                        val fullAddr = "$name, $subtitle"

                        val dist = DistanceUtils.calculateDistanceKm(userLat, userLng, lat, lng)
                        results.add(
                            AddressSuggestion(
                                title = name,
                                subtitle = subtitle,
                                fullAddress = fullAddr,
                                lat = lat,
                                lng = lng,
                                distanceKm = dist
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Photon OSM search fallback: ${e.message}")
            }
        }

        // 3. Fallback to Android platform Geocoder
        if (results.size < 4 && android.location.Geocoder.isPresent()) {
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(cleanQuery, 6)
                addresses?.forEach { addr ->
                    val title = addr.featureName ?: addr.thoroughfare ?: cleanQuery
                    val subtitle = listOfNotNull(addr.subLocality, addr.locality, addr.adminArea).joinToString(", ")
                    val fullAddr = addr.getAddressLine(0) ?: "$title, $subtitle"
                    val dist = DistanceUtils.calculateDistanceKm(userLat, userLng, addr.latitude, addr.longitude)

                    if (results.none { DistanceUtils.calculateDistanceKm(it.lat, it.lng, addr.latitude, addr.longitude) < 0.1 }) {
                        results.add(
                            AddressSuggestion(
                                title = title,
                                subtitle = subtitle,
                                fullAddress = fullAddr,
                                lat = addr.latitude,
                                lng = addr.longitude,
                                distanceKm = dist
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Geocoder fallback error: ${e.message}")
            }
        }

        // 3. Fallback to matching catalog landmarks
        val matchedCatalog = PRESET_LANDMARKS.filter {
            it.title.contains(cleanQuery, ignoreCase = true) ||
            it.subtitle.contains(cleanQuery, ignoreCase = true)
        }.map {
            val dist = DistanceUtils.calculateDistanceKm(userLat, userLng, it.lat, it.lng)
            it.copy(distanceKm = dist)
        }

        results.addAll(matchedCatalog)

        return@withContext results
            .distinctBy { "${it.title}_${String.format("%.3f", it.lat)}_${String.format("%.3f", it.lng)}" }
            .sortedBy { it.distanceKm ?: Double.MAX_VALUE }
            .take(10)
    }

    /**
     * Reverse geocodes coordinates to a human-readable street/area name using OSM Nominatim and Android Geocoder
     */
    suspend fun reverseGeocode(
        context: Context,
        lat: Double,
        lng: Double
    ): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        // 1. Try Ola Maps Reverse Geocoding with user's Ola Maps API Key
        try {
            val urlStr = "https://api.olamaps.io/places/v1/reverse-geocode?latlng=$lat,$lng&api_key=${com.speedo.core.utils.Constants.OLA_MAPS_API_KEY}"
            val url = java.net.URL(urlStr)
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("Content-Type", "application/json")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val root = com.google.gson.JsonParser.parseString(responseText).asJsonObject
                val results = root.getAsJsonArray("results")
                if (results != null && results.size() > 0) {
                    val firstResult = results[0].asJsonObject
                    val formattedAddress = firstResult.get("formatted_address")?.asString
                    val name = firstResult.get("name")?.asString
                    val chosen = if (!name.isNullOrBlank()) name else formattedAddress
                    if (!chosen.isNullOrBlank()) {
                        return@withContext chosen.split(",").take(2).joinToString(", ")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Ola Maps reverse geocode fallback: ${e.message}")
        }

        // 2. Try Android Native Geocoder
        if (android.location.Geocoder.isPresent()) {
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val feature = addr.featureName
                    val thoroughfare = addr.thoroughfare
                    val subLocality = addr.subLocality ?: addr.locality
                    val line = addr.getAddressLine(0)

                    val name = when {
                        !thoroughfare.isNullOrBlank() && !subLocality.isNullOrBlank() -> "$thoroughfare, $subLocality"
                        !feature.isNullOrBlank() && !subLocality.isNullOrBlank() -> "$feature, $subLocality"
                        !line.isNullOrBlank() -> line.split(",").take(2).joinToString(", ")
                        else -> subLocality ?: "Pinned Location"
                    }
                    return@withContext name
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Geocoder reverse geocode error: ${e.message}")
            }
        }

        // 2. Try OpenStreetMap Nominatim Reverse Geocoding
        try {
            val urlStr = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&zoom=18&addressdetails=1"
            val url = java.net.URL(urlStr)
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("User-Agent", "SpeedoRiderApp/2.0")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val root = com.google.gson.JsonParser.parseString(responseText).asJsonObject
                val displayName = root.get("display_name")?.asString
                val address = root.getAsJsonObject("address")

                val road = address?.get("road")?.asString
                val neighbourhood = address?.get("neighbourhood")?.asString ?: address?.get("suburb")?.asString
                val city = address?.get("city")?.asString ?: address?.get("county")?.asString ?: "Bangalore"

                val formatted = when {
                    !road.isNullOrBlank() && !neighbourhood.isNullOrBlank() -> "$road, $neighbourhood"
                    !road.isNullOrBlank() -> "$road, $city"
                    !neighbourhood.isNullOrBlank() -> "$neighbourhood, $city"
                    !displayName.isNullOrBlank() -> displayName.split(",").take(2).joinToString(", ")
                    else -> "Pinned Location"
                }
                return@withContext formatted
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Nominatim reverse geocode fallback: ${e.message}")
        }

        // 3. Find closest known landmark
        val closest = PRESET_LANDMARKS.minByOrNull {
            DistanceUtils.calculateDistanceKm(lat, lng, it.lat, it.lng)
        }
        if (closest != null) {
            val dist = DistanceUtils.calculateDistanceKm(lat, lng, closest.lat, closest.lng)
            if (dist < 1.0) {
                return@withContext "Near ${closest.title}"
            }
        }

        return@withContext String.format("Pinned Location (%.4f, %.4f)", lat, lng)
    }
}
