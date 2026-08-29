package com.speedo.core.maps

import android.content.Context
import com.google.gson.JsonParser
import com.speedo.core.model.RoutePoint
import com.speedo.core.model.RouteResponse
import com.speedo.core.network.NetworkResult
import com.speedo.core.network.RetrofitClient
import com.speedo.core.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL

object RouteHelper {

    suspend fun fetchRoute(
        context: Context,
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        isCaptain: Boolean = false
    ): NetworkResult<RouteResponse> = withContext(Dispatchers.IO) {
        // 1. Try Ola Maps Directions API using user's Ola Maps API Key
        try {
            val urlStr = "https://api.olamaps.io/routing/v1/directions?origin=$originLat,$originLng&destination=$destLat,$destLng&api_key=${Constants.OLA_MAPS_API_KEY}"
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Content-Type", "application/json")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JsonParser.parseString(responseText).asJsonObject
                val routes = root.getAsJsonArray("routes")
                if (routes != null && routes.size() > 0) {
                    val firstRoute = routes[0].asJsonObject
                    val legs = firstRoute.getAsJsonArray("legs")
                    var totalDistMeters = 0.0
                    var totalDurationSec = 0.0

                    legs?.forEach { legElem ->
                        val leg = legElem.asJsonObject
                        totalDistMeters += leg.get("distance")?.asDouble ?: 0.0
                        totalDurationSec += leg.get("duration")?.asDouble ?: 0.0
                    }

                    val overviewPolyline = firstRoute.getAsJsonObject("overview_polyline")?.get("points")?.asString
                    val finalPoints = if (!overviewPolyline.isNullOrBlank()) {
                        decodePolyline(overviewPolyline)
                    } else {
                        emptyList()
                    }

                    if (finalPoints.isNotEmpty()) {
                        val distKm = if (totalDistMeters > 0) totalDistMeters / 1000.0 else DistanceUtils.calculateDistanceKm(originLat, originLng, destLat, destLng)
                        val durationMins = if (totalDurationSec > 0) Math.max(1, (totalDurationSec / 60.0).toInt()) else DistanceUtils.calculateEtaMinutes(distKm)
                        return@withContext NetworkResult.Success(
                            RouteResponse(
                                distanceKm = Math.round(distKm * 10.0) / 10.0,
                                durationMins = durationMins,
                                coordinates = finalPoints,
                                summary = "Ola Maps Road Route"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("RouteHelper", "Ola Maps directions fallback: ${e.message}")
        }

        // 2. Try Backend calculate route
        try {
            val api = RetrofitClient.getService(context)
            val body = mapOf(
                "origin_lat" to originLat,
                "origin_lng" to originLng,
                "dest_lat" to destLat,
                "dest_lng" to destLng
            )
            val res = if (isCaptain) {
                api.calculateCaptainRoute(body)
            } else {
                api.calculateRiderRoute(body)
            }

            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                return@withContext NetworkResult.Success(res.body()!!.data!!)
            }
        } catch (e: Exception) {
            android.util.Log.w("RouteHelper", "Backend route calculation fallback: ${e.message}")
        }

        // 3. Spline Curved Route Fallback
        val fallbackPoints = generateSplineGeoPoints(
            GeoPoint(originLat, originLng),
            GeoPoint(destLat, destLng)
        )
        val distKm = DistanceUtils.calculateDistanceKm(originLat, originLng, destLat, destLng) * 1.25
        val routeResponse = RouteResponse(
            distanceKm = Math.round(distKm * 10.0) / 10.0,
            durationMins = Math.max(2, (distKm / 20 * 60).toInt()),
            coordinates = fallbackPoints.map { RoutePoint(it.latitude, it.longitude) },
            summary = "Direct arterial route"
        )
        NetworkResult.Success(routeResponse)
    }

    fun toGeoPoints(points: List<RoutePoint>): List<GeoPoint> {
        return points.map { GeoPoint(it.lat, it.lng) }
    }

    fun generateSplineGeoPoints(origin: GeoPoint, dest: GeoPoint, segments: Int = 24): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        val midLat = (origin.latitude + dest.latitude) / 2
        val midLng = (origin.longitude + dest.longitude) / 2
        val dLat = dest.latitude - origin.latitude
        val dLng = dest.longitude - origin.longitude

        val offsetLat = -dLng * 0.12
        val offsetLng = dLat * 0.12
        val ctrlLat = midLat + offsetLat
        val ctrlLng = midLng + offsetLng

        for (i in 0..segments) {
            val t = i.toDouble() / segments
            val lat = (1 - t) * (1 - t) * origin.latitude + 2 * (1 - t) * t * ctrlLat + t * t * dest.latitude
            val lng = (1 - t) * (1 - t) * origin.longitude + 2 * (1 - t) * t * ctrlLng + t * t * dest.longitude
            points.add(GeoPoint(lat, lng))
        }
        return points
    }

    /**
     * Decodes an encoded path string into a sequence of RoutePoints (standard Encoded Polyline Algorithm)
     */
    fun decodePolyline(encoded: String): List<RoutePoint> {
        val poly = mutableListOf<RoutePoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val pLat = lat.toDouble() / 1E5
            val pLng = lng.toDouble() / 1E5
            poly.add(RoutePoint(pLat, pLng))
        }
        return poly
    }
}
