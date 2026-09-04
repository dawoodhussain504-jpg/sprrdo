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
    @SerializedName("city") val city: String? = null,
    @SerializedName("district") val district: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("distance_km", alternate = ["distanceKm"]) val distanceKm: Double? = null,
    @SerializedName("is_active", alternate = ["isActive"]) val isActive: Boolean = true,
    @SerializedName("sort_order", alternate = ["sortOrder"]) val sortOrder: Int = 0
)

data class LocationRankedDestinations(
    val destinations: List<PopularDestination>,
    val headerTitle: String,
    val subtitle: String,
    val matchedCity: String? = null
)

object PopularDestinationsData {
    val ALL_DESTINATIONS = listOf(
        // --- Sheikhpura, Bihar (Official Top Places) ---
        PopularDestination(
            id = "dest_sheikhpura_junction",
            title = "Sheikhpura Junction Railway Station",
            subtitle = "Central Railway & Transit Hub",
            fullAddress = "Sheikhpura Railway Station, Station Road, Sheikhpura, Bihar 811105",
            lat = 25.1378,
            lng = 85.8569,
            category = "TRANSIT",
            imageUrl = "https://images.unsplash.com/photo-1474487548417-781cb71495f3?w=600&auto=format&fit=crop&q=80",
            badge = "🚆 Central Railway",
            city = "Sheikhpura",
            district = "Sheikhpura",
            state = "Bihar",
            sortOrder = 1
        ),
        PopularDestination(
            id = "dest_giriyak_hills",
            title = "Giriyak Hills & Buddhist Heritage",
            subtitle = "Ancient Stupa & Scenic Overlook",
            fullAddress = "Giriyak Hill Stupa, Bihar State Highway, near Sheikhpura Border, Bihar",
            lat = 25.0440,
            lng = 85.5290,
            category = "HERITAGE",
            imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=80",
            badge = "🏔️ Historic & Scenic",
            city = "Sheikhpura",
            district = "Sheikhpura",
            state = "Bihar",
            sortOrder = 2
        ),
        PopularDestination(
            id = "dest_tripolia_gate",
            title = "Tripolia Gate & Purani Bazaar",
            subtitle = "Main City Center & Market Complex",
            fullAddress = "Purani Bazaar, Tripolia Gate, Sheikhpura, Bihar 811105",
            lat = 25.1320,
            lng = 85.8480,
            category = "SHOPPING",
            imageUrl = "https://images.unsplash.com/photo-1567449303078-57ad995bd301?w=600&auto=format&fit=crop&q=80",
            badge = "🛍️ City Market",
            city = "Sheikhpura",
            district = "Sheikhpura",
            state = "Bihar",
            sortOrder = 3
        ),
        PopularDestination(
            id = "dest_arghauti_pokhar",
            title = "Arghauti Pokhar Waterfront",
            subtitle = "Scenic Lake & Sunset Promenade",
            fullAddress = "Arghauti Pokhar, Ward No 7, Sheikhpura, Bihar",
            lat = 25.1350,
            lng = 85.8520,
            category = "PARK",
            imageUrl = "https://images.unsplash.com/photo-1519331379826-f10be5486c6f?w=600&auto=format&fit=crop&q=80",
            badge = "🌳 Scenic Lake Spot",
            city = "Sheikhpura",
            district = "Sheikhpura",
            state = "Bihar",
            sortOrder = 4
        ),
        PopularDestination(
            id = "dest_vaidyanath_temple",
            title = "Vaidyanath Temple & Town Hall",
            subtitle = "Heritage Temple & Cultural Center",
            fullAddress = "Vaidyanath Temple Road, Sheikhpura, Bihar",
            lat = 25.1410,
            lng = 85.8610,
            category = "HERITAGE",
            imageUrl = "https://images.unsplash.com/photo-1542296332-2e4473faf563?w=600&auto=format&fit=crop&q=80",
            badge = "🕉️ Spiritual Spot",
            city = "Sheikhpura",
            district = "Sheikhpura",
            state = "Bihar",
            sortOrder = 5
        ),

        // --- Patna, Bihar ---
        PopularDestination(
            id = "dest_patna_golghar",
            title = "Golghar & Gandhi Maidan",
            subtitle = "Historical Landmark & Public Grounds",
            fullAddress = "Ashok Rajpath, near Gandhi Maidan, Patna, Bihar 800001",
            lat = 25.6178,
            lng = 85.1414,
            category = "HERITAGE",
            imageUrl = "https://images.unsplash.com/photo-1542296332-2e4473faf563?w=600&auto=format&fit=crop&q=80",
            badge = "🏛️ Historic Landmark",
            city = "Patna",
            district = "Patna",
            state = "Bihar",
            sortOrder = 10
        ),
        PopularDestination(
            id = "dest_patna_ganga_path",
            title = "Marine Drive (Ganga Pathway)",
            subtitle = "Riverfront Promenade & Cafes",
            fullAddress = "Loknayak Ganga Path, Digha Ghat to Patna Ghat, Patna, Bihar",
            lat = 25.6260,
            lng = 85.1520,
            category = "DINING",
            imageUrl = "https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=600&auto=format&fit=crop&q=80",
            badge = "🌊 Riverfront & Cafes",
            city = "Patna",
            district = "Patna",
            state = "Bihar",
            sortOrder = 11
        ),

        // --- Bangalore, Karnataka ---
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
            city = "Bangalore",
            district = "Bengaluru Urban",
            state = "Karnataka",
            sortOrder = 20
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
            city = "Bangalore",
            district = "Bengaluru Urban",
            state = "Karnataka",
            sortOrder = 21
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
            city = "Bangalore",
            district = "Bengaluru Urban",
            state = "Karnataka",
            sortOrder = 22
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
            city = "Bangalore",
            district = "Bengaluru Urban",
            state = "Karnataka",
            sortOrder = 23
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
            city = "Bangalore",
            district = "Bengaluru Urban",
            state = "Karnataka",
            sortOrder = 24
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
            city = "Bangalore",
            district = "Bengaluru Urban",
            state = "Karnataka",
            sortOrder = 25
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
            city = "Bangalore",
            district = "Bengaluru Urban",
            state = "Karnataka",
            sortOrder = 26
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
            city = "Bangalore",
            district = "Bengaluru Urban",
            state = "Karnataka",
            sortOrder = 27
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

    /**
     * Intelligently filters and ranks destinations based on user's current city, district, state, or GPS location.
     * Generates a tailored headerTitle (e.g. "Top Places in Sheikhpura", "Top Places in Bangalore").
     */
    fun filterAndRankForLocation(
        destinations: List<PopularDestination>,
        userLat: Double?,
        userLng: Double?,
        userCity: String? = null,
        userDistrict: String? = null,
        userState: String? = null,
        userAddress: String? = null
    ): LocationRankedDestinations {
        val withDistance = calculateDistances(destinations, userLat, userLng)

        // Heuristic detection from address string if coordinates or reverse geocode are still loading
        val addr = (userAddress ?: "").lowercase()
        val detectedCity = (userCity?.takeIf { it.isNotBlank() }
            ?: when {
                addr.contains("sheikhpura") -> "Sheikhpura"
                addr.contains("patna") -> "Patna"
                addr.contains("bangalore") || addr.contains("bengaluru") -> "Bangalore"
                addr.contains("delhi") -> "Delhi"
                addr.contains("mumbai") -> "Mumbai"
                addr.contains("kolkata") -> "Kolkata"
                else -> null
            })?.trim()

        val detectedDistrict = (userDistrict?.takeIf { it.isNotBlank() } ?: detectedCity)?.trim()
        val detectedState = (userState?.takeIf { it.isNotBlank() }
            ?: when {
                addr.contains("bihar") || (detectedCity != null && (detectedCity.equals("sheikhpura", true) || detectedCity.equals("patna", true))) -> "Bihar"
                addr.contains("karnataka") || (detectedCity != null && detectedCity.equals("bangalore", true)) -> "Karnataka"
                else -> null
            })?.trim()

        // 1. Check exact City or District matches, OR close proximity (< 35 km)
        val cityMatches = withDistance.filter { d ->
            (detectedCity != null && d.city?.equals(detectedCity, ignoreCase = true) == true) ||
            (detectedDistrict != null && d.district?.equals(detectedDistrict, ignoreCase = true) == true) ||
            (d.distanceKm != null && d.distanceKm <= 35.0)
        }.sortedWith(compareBy({ it.distanceKm ?: 9999.0 }, { it.sortOrder }))

        if (cityMatches.isNotEmpty()) {
            val cityName = detectedCity ?: detectedDistrict ?: cityMatches.firstOrNull { !it.city.isNullOrBlank() }?.city ?: "Your City"
            val remaining = withDistance.filter { d -> cityMatches.none { it.id == d.id } }
            return LocationRankedDestinations(
                destinations = cityMatches + remaining,
                headerTitle = "Top Places in $cityName",
                subtitle = "Popular destinations in $cityName • Tap to set pickup or drop",
                matchedCity = cityName
            )
        }

        // 2. Check State matches OR regional proximity (< 150 km)
        val stateMatches = withDistance.filter { d ->
            (detectedState != null && d.state?.equals(detectedState, ignoreCase = true) == true) ||
            (d.distanceKm != null && d.distanceKm <= 150.0)
        }.sortedWith(compareBy({ it.distanceKm ?: 9999.0 }, { it.sortOrder }))

        if (stateMatches.isNotEmpty()) {
            val stateName = detectedState ?: stateMatches.firstOrNull { !it.state.isNullOrBlank() }?.state ?: "Your Region"
            val remaining = withDistance.filter { d -> stateMatches.none { it.id == d.id } }
            return LocationRankedDestinations(
                destinations = stateMatches + remaining,
                headerTitle = "Top Places in $stateName",
                subtitle = "Popular destinations in $stateName • Tap to set pickup or drop",
                matchedCity = stateName
            )
        }

        // 3. Fallback: Proximity or General Trending
        val closest = withDistance.minByOrNull { it.distanceKm ?: 99999.0 }
        val (title, sub) = if (closest?.distanceKm != null && closest.distanceKm < 80.0) {
            "Top Places near You" to "Nearby popular landmarks (${String.format("%.1f", closest.distanceKm)} km away)"
        } else {
            "Popular Destinations" to "Top trending places • Tap to select destination"
        }

        return LocationRankedDestinations(
            destinations = withDistance.sortedWith(compareBy({ it.distanceKm ?: 9999.0 }, { it.sortOrder })),
            headerTitle = title,
            subtitle = sub,
            matchedCity = null
        )
    }
}
