package com.speedo.rider.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.speedo.core.maps.DistanceUtils
import com.speedo.core.maps.LocationHelper
import com.speedo.core.maps.RouteHelper
import com.speedo.core.model.*
import com.speedo.core.network.NetworkResult
import com.speedo.core.repository.AuthRepository
import com.speedo.core.repository.ChatRepository
import com.speedo.core.repository.NotificationRepository
import com.speedo.core.repository.RiderRepository
import com.speedo.core.socket.SpeedoSocketManager
import com.speedo.core.utils.BadgeHelper
import com.speedo.core.utils.Constants
import com.speedo.core.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

import com.speedo.core.maps.LocationSearchHelper

data class RiderUiState(
    val isLoggedIn: Boolean = false,
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val currentUserEmail: String? = null,
    val currentLocation: LocationPoint = LocationPoint(0.0, 0.0),
    val pickupAddress: String = "Locating your pickup...",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val dropAddress: String = "",
    val dropLat: Double = 0.0,
    val dropLng: Double = 0.0,
    val selectedVehicleType: String = "bike",
    val nearbyCaptains: List<Captain> = emptyList(),
    val fareEstimates: FareEstimatesResponse? = null,
    val activeRide: Ride? = null,
    val isBookingRide: Boolean = false,
    val isAuthLoading: Boolean = false,
    val isLoadingFares: Boolean = false,
    val roadPolyline: List<GeoPoint> = emptyList(),
    val driverPolyline: List<GeoPoint> = emptyList(),
    val roadDurationMins: Int? = null,
    val roadSummary: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class RiderViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepo = AuthRepository(application)
    private val riderRepo = RiderRepository(application)
    private val notifRepo = NotificationRepository(application)
    private val chatRepo = ChatRepository(application)
    private val locationHelper = LocationHelper(application)

    private val _uiState = MutableStateFlow(RiderUiState())
    val uiState: StateFlow<RiderUiState> = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    val cachedRides = riderRepo.cachedRidesFlow
    val cachedNotifications = notifRepo.cachedNotificationsFlow
    val unreadCount = notifRepo.unreadCountFlow

    private var activeRidePollingJob: Job? = null
    private var nearbyPollingJob: Job? = null

    init {
        checkAuthStatus()
        fetchCurrentLocation()
        observeSocketEvents()
    }

    fun checkAuthStatus() {
        val loggedIn = authRepo.tokenManager.isLoggedIn() && authRepo.tokenManager.getUserRole() == "rider"
        _uiState.value = _uiState.value.copy(
            isLoggedIn = loggedIn,
            currentUserId = authRepo.tokenManager.getUserId(),
            currentUserName = authRepo.tokenManager.getUserName(),
            currentUserEmail = authRepo.tokenManager.getUserEmail()
        )
        if (loggedIn) {
            viewModelScope.launch(Dispatchers.IO) {
                SpeedoSocketManager.getInstance(getApplication()).connect()
                startNearbyCaptainsPolling()
                checkActiveRide()
                syncHistory()
                syncNotifications()
            }
        }
    }

    fun loadChatMessages(rideId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val res = chatRepo.getMessages(rideId)) {
                is NetworkResult.Success -> {
                    _chatMessages.value = res.data
                }
                else -> {}
            }
        }
    }

    fun sendChatMessage(text: String, type: String = "text") {
        val rideId = _uiState.value.activeRide?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (val res = chatRepo.sendMessage(rideId, text, type)) {
                is NetworkResult.Success -> {
                    val current = _chatMessages.value
                    if (current.none { it.id == res.data.id }) {
                        _chatMessages.value = current + res.data
                    }
                }
                else -> {}
            }
        }
    }

    private fun observeSocketEvents() {
        val socketManager = SpeedoSocketManager.getInstance(getApplication())

        // 1. Sub-second live GPS movement of Captain on Map
        viewModelScope.launch {
            socketManager.liveCaptainLocationFlow.collect { loc ->
                val currentRide = _uiState.value.activeRide
                if (currentRide != null) {
                    val driverPts = listOf(
                        GeoPoint(loc.lat, loc.lng),
                        GeoPoint(currentRide.pickupLat, currentRide.pickupLng)
                    )
                    _uiState.value = _uiState.value.copy(
                        activeRide = currentRide.copy(
                            liveCaptainLat = loc.lat,
                            liveCaptainLng = loc.lng,
                            liveCaptainBearing = loc.bearing.toDouble()
                        ),
                        driverPolyline = driverPts
                    )
                }
            }
        }

        // 2. Real-time Status Transitions
        viewModelScope.launch {
            socketManager.liveRideStatusFlow.collect { update ->
                val currentRide = _uiState.value.activeRide
                if (currentRide != null && (update.rideId.isEmpty() || update.rideId == currentRide.id)) {
                    when (update.status) {
                        "accepted" -> {
                            _uiState.value = _uiState.value.copy(
                                activeRide = currentRide.copy(
                                    status = "accepted",
                                    captainName = update.captainName ?: currentRide.captainName,
                                    vehicleNumber = update.vehicleNumber ?: currentRide.vehicleNumber
                                ),
                                successMessage = "Captain on the way! 🛵"
                            )
                            NotificationHelper.showNotification(
                                getApplication(),
                                "Captain on the way! 🛵",
                                "${update.captainName ?: "Captain"} is driving to your pickup location."
                            )
                        }
                        "arrived" -> {
                            _uiState.value = _uiState.value.copy(
                                activeRide = currentRide.copy(status = "arrived"),
                                successMessage = "Captain has arrived at pickup!"
                            )
                            NotificationHelper.showNotification(
                                getApplication(),
                                "Captain has arrived! 📍",
                                "Share your PIN (${currentRide.otp}) with captain to start the ride."
                            )
                        }
                        "ongoing" -> {
                            _uiState.value = _uiState.value.copy(
                                activeRide = currentRide.copy(status = "ongoing"),
                                successMessage = "Trip in progress. Have a safe journey!"
                            )
                        }
                        "completed" -> {
                            _uiState.value = _uiState.value.copy(
                                activeRide = currentRide.copy(status = "completed"),
                                successMessage = "Trip completed! 🎉"
                            )
                            syncHistory()
                        }
                        "cancelled" -> {
                            _uiState.value = _uiState.value.copy(
                                activeRide = null,
                                errorMessage = "Ride was cancelled."
                            )
                            syncHistory()
                        }
                    }
                }
            }
        }

        // 3. In-App Real-Time Chat messages
        viewModelScope.launch {
            chatRepo.liveChatFlow.collect { msg ->
                val current = _chatMessages.value
                if (current.none { it.id == msg.id }) {
                    _chatMessages.value = current + msg
                    if (msg.senderRole == "captain") {
                        NotificationHelper.showNotification(
                            getApplication(),
                            "Message from Captain 🛵",
                            msg.messageText
                        )
                    }
                }
            }
        }

        // 4. Real-time Geofenced Surge Zone Updates
        viewModelScope.launch {
            socketManager.liveSurgeUpdateFlow.collect {
                // If user is currently looking at fare estimates, recalculate fares with new surge
                if (_uiState.value.isLoggedIn && _uiState.value.dropAddress.isNotBlank() && _uiState.value.activeRide == null) {
                    calculateFares()
                }
            }
        }

        // 5. Real-time City-Wide Broadcasts & Promo Vouchers
        viewModelScope.launch {
            socketManager.liveBroadcastFlow.collect { bcast ->
                if (bcast.targetAudience == "all" || bcast.targetAudience == "riders") {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "📢 ${bcast.title}: ${bcast.message}"
                    )
                    NotificationHelper.showNotification(
                        getApplication(),
                        "📢 " + bcast.title,
                        bcast.message + (if (!bcast.couponCode.isNullOrBlank()) " • Code: ${bcast.couponCode}" else "")
                    )
                }
            }
        }
    }

    fun login(email: String, pass: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAuthLoading = true, errorMessage = null)
            when (val res = authRepo.loginRider(email.trim(), pass)) {
                is NetworkResult.Success -> {
                    checkAuthStatus()
                    _uiState.value = _uiState.value.copy(isAuthLoading = false)
                    onComplete(true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isAuthLoading = false, errorMessage = res.message)
                    onComplete(false)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isAuthLoading = false)
                    onComplete(false)
                }
            }
        }
    }

    fun register(name: String, email: String, pass: String, phone: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAuthLoading = true, errorMessage = null)
            when (val res = authRepo.registerRider(name.trim(), email.trim(), pass, phone.trim())) {
                is NetworkResult.Success -> {
                    checkAuthStatus()
                    _uiState.value = _uiState.value.copy(isAuthLoading = false)
                    onComplete(true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isAuthLoading = false, errorMessage = res.message)
                    onComplete(false)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isAuthLoading = false)
                    onComplete(false)
                }
            }
        }
    }

    fun logout() {
        activeRidePollingJob?.cancel()
        nearbyPollingJob?.cancel()
        authRepo.logout()
        _uiState.value = RiderUiState(isLoggedIn = false)
    }

    fun fetchCurrentLocation() {
        locationHelper.getCurrentLiveLocation(
            onSuccess = { loc ->
                viewModelScope.launch {
                    val realAddress = LocationSearchHelper.reverseGeocode(getApplication(), loc.lat, loc.lng)
                    _uiState.value = _uiState.value.copy(
                        currentLocation = loc,
                        pickupLat = loc.lat,
                        pickupLng = loc.lng,
                        pickupAddress = realAddress
                    )
                    if (_uiState.value.isLoggedIn && _uiState.value.dropAddress.isNotBlank()) {
                        calculateFares()
                    }
                }
            },
            onFailure = {
                // If GPS is not ready yet, keep safe default
                _uiState.value = _uiState.value.copy(
                    pickupAddress = "Current Location"
                )
            }
        )
    }

    fun updatePickupLocation(address: String, lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(pickupAddress = address, pickupLat = lat, pickupLng = lng)
        if (_uiState.value.isLoggedIn && _uiState.value.dropAddress.isNotBlank()) {
            calculateFares()
        }
    }

    fun updateDropLocation(address: String, lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(dropAddress = address, dropLat = lat, dropLng = lng)
        if (_uiState.value.isLoggedIn && address.isNotBlank() && lat != 0.0) {
            calculateFares()
        }
    }

    fun clearDropLocation() {
        _uiState.value = _uiState.value.copy(
            dropAddress = "",
            dropLat = 0.0,
            dropLng = 0.0,
            fareEstimates = null,
            roadPolyline = emptyList(),
            isLoadingFares = false
        )
    }

    fun setPinDropLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                dropLat = lat,
                dropLng = lng,
                isLoadingFares = true
            )
            val geocoded = LocationSearchHelper.reverseGeocode(getApplication(), lat, lng)
            _uiState.value = _uiState.value.copy(
                dropAddress = geocoded
            )
            calculateFares()
        }
    }

    fun selectVehicleType(type: String) {
        _uiState.value = _uiState.value.copy(selectedVehicleType = type)
    }

    fun calculateFares() {
        if (!_uiState.value.isLoggedIn) return
        val pLat = _uiState.value.pickupLat
        val pLng = _uiState.value.pickupLng
        val dLat = _uiState.value.dropLat
        val dLng = _uiState.value.dropLng

        if (dLat == 0.0 || dLng == 0.0 || _uiState.value.dropAddress.isBlank()) {
            _uiState.value = _uiState.value.copy(
                fareEstimates = null,
                roadPolyline = emptyList(),
                isLoadingFares = false
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoadingFares = true)

            // 1. Fetch live road-snapped polyline & traffic ETA via OSRM
            val routeRes = RouteHelper.fetchRoute(getApplication(), pLat, pLng, dLat, dLng)
            if (routeRes is NetworkResult.Success) {
                val geoPoints = RouteHelper.toGeoPoints(routeRes.data.coordinates)
                _uiState.value = _uiState.value.copy(
                    roadPolyline = geoPoints,
                    roadDurationMins = routeRes.data.durationMins,
                    roadSummary = routeRes.data.summary
                )
            }

            // 2. Fetch Fare Estimates based on road distance
            val res = riderRepo.estimateFares(pLat, pLng, dLat, dLng)
            if (res is NetworkResult.Success) {
                _uiState.value = _uiState.value.copy(fareEstimates = res.data, isLoadingFares = false)
            } else {
                _uiState.value = _uiState.value.copy(isLoadingFares = false)
            }
        }
    }

    fun startNearbyCaptainsPolling() {
        nearbyPollingJob?.cancel()
        nearbyPollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (_uiState.value.isLoggedIn && _uiState.value.activeRide == null) {
                    val res = riderRepo.getNearbyCaptains(
                        _uiState.value.pickupLat,
                        _uiState.value.pickupLng,
                        radiusKm = 6.0,
                        vehicleType = _uiState.value.selectedVehicleType
                    )
                    if (res is NetworkResult.Success) {
                        _uiState.value = _uiState.value.copy(nearbyCaptains = res.data)
                    }
                }
                delay(Constants.RIDER_TRACKING_POLL_INTERVAL_MS)
            }
        }
    }

    fun bookRide(onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isBookingRide = true, errorMessage = null)
            val pAddr = _uiState.value.pickupAddress.ifBlank { "Indiranagar 100ft Road, Bangalore" }
            val dAddr = _uiState.value.dropAddress.ifBlank { "Koramangala 5th Block, Bangalore" }
            val pLat = _uiState.value.pickupLat
            val pLng = _uiState.value.pickupLng
            val dLat = _uiState.value.dropLat
            val dLng = _uiState.value.dropLng
            val vType = _uiState.value.selectedVehicleType.lowercase()

            val res = riderRepo.requestRide(
                pickupAddress = pAddr,
                pickupLat = pLat,
                pickupLng = pLng,
                dropAddress = dAddr,
                dropLat = dLat,
                dropLng = dLng,
                vehicleType = vType
            )

            when (res) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        activeRide = res.data,
                        isBookingRide = false,
                        successMessage = "Ride requested! Searching for nearby captains..."
                    )
                    startActiveRidePolling()
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                }
                is NetworkResult.Error -> {
                    // Check if an active ride already exists on backend
                    val activeRes = riderRepo.getActiveRide()
                    if (activeRes is NetworkResult.Success && activeRes.data != null) {
                        _uiState.value = _uiState.value.copy(
                            activeRide = activeRes.data,
                            isBookingRide = false
                        )
                        startActiveRidePolling()
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isBookingRide = false,
                            errorMessage = res.message
                        )
                    }
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isBookingRide = false)
                }
            }
        }
    }

    fun checkActiveRide() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = riderRepo.getActiveRide()
            if (res is NetworkResult.Success && res.data != null) {
                _uiState.value = _uiState.value.copy(activeRide = res.data)
                startActiveRidePolling()
            }
        }
    }

    private fun startActiveRidePolling() {
        activeRidePollingJob?.cancel()
        activeRidePollingJob = viewModelScope.launch(Dispatchers.IO) {
            var lastStatus = _uiState.value.activeRide?.status

            while (isActive) {
                delay(Constants.RIDER_TRACKING_POLL_INTERVAL_MS)
                val res = riderRepo.getActiveRide()

                if (res is NetworkResult.Success) {
                    val updatedRide = res.data
                    _uiState.value = _uiState.value.copy(activeRide = updatedRide)

                    if (updatedRide != null) {
                        if (updatedRide.status != lastStatus) {
                            when (updatedRide.status) {
                                "accepted" -> NotificationHelper.showNotification(
                                    getApplication(),
                                    "Captain on the way! 🛵",
                                    "${updatedRide.captainName ?: "Captain"} (${updatedRide.vehicleNumber ?: ""}) has accepted your ride."
                                )
                                "arrived" -> NotificationHelper.showNotification(
                                    getApplication(),
                                    "Captain has arrived! 📍",
                                    "Your captain is at pickup. Share OTP ${updatedRide.otp} to start."
                                )
                                "ongoing" -> NotificationHelper.showNotification(
                                    getApplication(),
                                    "Ride Started 🚀",
                                    "Have a safe trip to ${updatedRide.dropAddress}!"
                                )
                                "completed" -> {
                                    NotificationHelper.showNotification(
                                        getApplication(),
                                        "Ride Completed 🎉",
                                        "Trip completed! Fare: ₹${updatedRide.fare}. Please pay your captain."
                                    )
                                    syncHistory()
                                    break
                                }
                            }
                            lastStatus = updatedRide.status
                        }
                    } else {
                        syncHistory()
                        break
                    }
                }
            }
        }
    }

    fun cancelRide(rideId: String, reason: String = "Cancelled by rider", onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = riderRepo.cancelRide(rideId, reason)
            if (res is NetworkResult.Success) {
                _uiState.value = _uiState.value.copy(activeRide = null, successMessage = "Ride cancelled.")
                activeRidePollingJob?.cancel()
                syncHistory()
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } else if (res is NetworkResult.Error) {
                _uiState.value = _uiState.value.copy(errorMessage = res.message)
            }
        }
    }

    fun syncHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            riderRepo.syncRideHistory()
        }
    }

    fun syncNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            notifRepo.syncRiderNotifications()
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            notifRepo.markRiderRead(id)
        }
    }

    fun triggerSosEmergency(rideId: String?, lat: Double, lng: Double, address: String?, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // 1. Emit instant socket event
            SpeedoSocketManager.getInstance(getApplication()).emitSosTrigger(rideId, lat, lng, address)
            // 2. Call REST API backup
            val adminRepo = com.speedo.core.repository.AdminRepository(getApplication())
            adminRepo.triggerSosAlert(com.speedo.core.model.TriggerSosRequest(rideId, lat, lng, address))
            _uiState.value = _uiState.value.copy(
                successMessage = "🚨 SOS Emergency Broadcasted to Speedo Command Center & Police!"
            )
            onComplete()
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
