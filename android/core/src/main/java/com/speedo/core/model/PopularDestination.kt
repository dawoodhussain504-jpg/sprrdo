package com.speedo.core.model

import com.speedo.core.maps.DistanceUtils

data class PopularDestination(
    val id: String,
    val title: String,
    val subtitle: String,
    val fullAddress: String,
    val lat: Double,
    val lng: Double,
    val category: String, // "AIRPORT", "METRO", "MALL", "TECH PARK", "TRANSIT", "CAFE", "PARK", "DINING"
    val imageUrl: String,
    val badge: String = "Popular",
    val distanceKm: Double? = null
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
            badge = "✈️ Airport Terminal"
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
            badge = "🚇 Direct Metro"
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
            badge = "☕ Food & Nightlife"
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
            badge = "🛍️ Shopping & Cinema"
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
            badge = "💼 IT Hub"
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
            badge = "🎉 Trending Spot"
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
            badge = "🌳 Sightseeing"
        ),
        PopularDestination(
            id = "ksr_railway",
            title = "KSR Bengaluru City Junction",
            subtitle = "Majestic Central Railway & Bus Terminus",
            fullAddress = "KSR Bengaluru Railway Station, Kempegowda Majestic, Bangalore",
            lat = 12.9781,
            lng = 77.5696,
            category = "TRANSIT",
            imageUrl = "https://images.unsplash.com/photo-1474487548417-781cb71495f3?w=600&auto=format&fit=crop&q=80",
            badge = "🚆 Trains & Metro"
        )
    )

    fun getDestinationsWithDistance(userLat: Double, userLng: Double): List<PopularDestination> {
        return ALL_DESTINATIONS.map { dest ->
            val dist = if (userLat != 0.0 && userLng != 0.0) {
                DistanceUtils.calculateDistanceKm(userLat, userLng, dest.lat, dest.lng)
            } else null
            dest.copy(distanceKm = dist)
        }
    }
}
