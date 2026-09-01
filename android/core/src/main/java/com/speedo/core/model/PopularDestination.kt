package com.speedo.core.model

import com.google.gson.annotations.SerializedName
import com.speedo.core.maps.DistanceUtils

data class PopularDestination(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("full_address", alternate = ["fullAddress", "address"]) val fullAddress: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("category") val category: String,
    @SerializedName("image_url", alternate = ["imageUrl"]) val imageUrl: String,
    @SerializedName("badge") val badge: String = "Popular",
    @SerializedName("distance_km", alternate = ["distanceKm"]) val distanceKm: Double? = null,
    @SerializedName("is_active", alternate = ["isActive"]) val isActive: Boolean = true,
    @SerializedName("sort_order", alternate = ["sortOrder"]) val sortOrder: Int = 0
)

object PopularDestinationsData {
    val ALL_DESTINATIONS = listOf(
        PopularDestination(
            id = "blr_airport",
            title = "Kempegowda Int'l Airport (BLR)",
            subtitle = "Devanahalli, Terminal 1 & 2",
            fullAddress = "Kempegowda International Airport, Devanahalli, Bangalore, Karnataka",
            lat = 13.1986,
            lng = 77.7066,
            category = "AIRPORT",
            imageUrl = "https://images.unsplash.com/photo-1542296332-2e4473faf563?w=600&auto=format&fit=crop&q=80",
            badge = "✈️ Airport Terminal",
            sortOrder = 0
        ),
        PopularDestination(
            id = "mg_road",
            title = "MG Road & Church Street",
            subtitle = "Central Business District & Metro",
            fullAddress = "MG Road Metro Station, Shivaji Nagar, Bangalore, Karnataka",
            lat = 12.9756,
            lng = 77.6066,
            category = "METRO",
            imageUrl = "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=600&auto=format&fit=crop&q=80",
            badge = "🚇 Direct Metro",
            sortOrder = 1
        ),
        PopularDestination(
            id = "koramangala",
            title = "Koramangala 5th Block",
            subtitle = "Cafes, Breweries & Startup Hub",
            fullAddress = "Koramangala 5th Block, Jyoti Nivas College Road, Bangalore",
            lat = 12.9352,
            lng = 77.6245,
            category = "CAFE",
            imageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=600&auto=format&fit=crop&q=80",
            badge = "☕ Food & Nightlife",
            sortOrder = 2
        ),
        PopularDestination(
            id = "phoenix_mall",
            title = "Phoenix Marketcity & VR",
            subtitle = "Whitefield Main Road, Mahadevapura",
            fullAddress = "Phoenix Marketcity, Whitefield Main Road, Bangalore",
            lat = 12.9959,
            lng = 77.6963,
            category = "MALL",
            imageUrl = "https://images.unsplash.com/photo-1567449303078-57ad995bd301?w=600&auto=format&fit=crop&q=80",
            badge = "🛍️ Shopping & Cinema",
            sortOrder = 3
        ),
        PopularDestination(
            id = "itpl_whitefield",
            title = "ITPL Tech Park Whitefield",
            subtitle = "Main Gate, Export Promotion Zone",
            fullAddress = "International Tech Park Bangalore (ITPL), Whitefield, Bangalore",
            lat = 12.9850,
            lng = 77.7289,
            category = "TECH PARK",
            imageUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=600&auto=format&fit=crop&q=80",
            badge = "💼 IT Hub",
            sortOrder = 4
        ),
        PopularDestination(
            id = "indiranagar",
            title = "Indiranagar 100ft Road",
            subtitle = "Dining, Boutique & Lifestyle",
            fullAddress = "100 Feet Road, HAL 2nd Stage, Indiranagar, Bangalore",
            lat = 12.9716,
            lng = 77.6412,
            category = "DINING",
            imageUrl = "https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=600&auto=format&fit=crop&q=80",
            badge = "🎉 Trending Spot",
            sortOrder = 5
        ),
        PopularDestination(
            id = "cubbon_park",
            title = "Cubbon Park & Vidhana Soudha",
            subtitle = "Kasturba Road & Heritage Zone",
            fullAddress = "Cubbon Park, Kasturba Road, Ambedkar Veedhi, Bangalore",
            lat = 12.9763,
            lng = 77.5929,
            category = "PARK",
            imageUrl = "https://images.unsplash.com/photo-1519331379826-f10be5486c6f?w=600&auto=format&fit=crop&q=80",
            badge = "🌳 Sightseeing",
            sortOrder = 6
        ),
        PopularDestination(
            id = "ksr_railway",
            title = "KSR Bengaluru City Junction",
            subtitle = "Majestic Central Railway & Bus Terminus",
            fullAddress = "KSR Bengaluru City Railway Station, Majestic, Bangalore",
            lat = 12.9781,
            lng = 77.5694,
            category = "TRANSIT",
            imageUrl = "https://images.unsplash.com/photo-1474487548417-781cb71495f3?w=600&auto=format&fit=crop&q=80",
            badge = "🚆 Trains & Metro",
            sortOrder = 7
        )
    )

    fun getDestinationsWithDistance(userLat: Double?, userLng: Double?): List<PopularDestination> {
        return calculateDistances(ALL_DESTINATIONS, userLat, userLng)
    }

    fun calculateDistances(destinations: List<PopularDestination>, userLat: Double?, userLng: Double?): List<PopularDestination> {
        if (userLat == null || userLng == null || userLat == 0.0 || userLng == 0.0) {
            return destinations
        }
        return destinations.map { dest ->
            val dist = DistanceUtils.calculateDistanceKm(userLat, userLng, dest.lat, dest.lng)
            dest.copy(distanceKm = dist)
        }
    }
}
