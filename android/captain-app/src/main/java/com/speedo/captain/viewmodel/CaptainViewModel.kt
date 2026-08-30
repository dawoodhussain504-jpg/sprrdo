package com.speedo.captain.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.speedo.captain.service.CaptainLocationService
import com.speedo.core.maps.RouteHelper
import com.speedo.core.model.*
import com.speedo.core.network.NetworkResult
import com.speedo.core.repository.AuthRepository
import com.speedo.core.repository.CaptainRepository
import com.speedo.core.repository.ChatRepository
import com.speedo.core.repository.NotificationRepository
import com.speedo.core.socket.SpeedoSocketManager
import com.speedo.core.utils.BadgeHelper
import com.speedo.core.utils.Constants
import com.speedo.core.utils.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.io.File

data class CaptainUiState(
    val isLoggedIn: Boolean = false,
    val captain: Captain? = null,
    val kycStatus: KycStatusResponse? = null,
    val isOnline: Boolean = false,
    val incomingRequests: List<Ride> = emptyList(),
    val activeRide: Ride? = null,
    val pendingPaymentRide: Ride? = null,
    val isLoading: Boolean = false,
    val isUploadingDoc: Boolean = false,
    val otpInput: String = "",
    val roadPolyline: List<GeoPoint> = emptyList(),
    val maneuvers: List<RouteManeuver> = emptyList(),
    val currentManeuver: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class CaptainViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepo = AuthRepository(application)
    private val captainRepo = CaptainRepository(application)
    private val notifRepo = NotificationRepository(application)
    private val chatRepo = ChatRepository(application)

    private val _uiState = MutableStateFlow(CaptainUiState())
    val uiState: StateFlow<CaptainUiState> = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    val cachedRides = captainRepo.cachedRidesFlow
    val cachedNotifications = notifRepo.cachedNotificationsFlow
    val unreadCount = notifRepo.unreadCountFlow

    private var rideRequestPollingJob: Job? = null
    private var activeRidePollingJob: Job? = null

    init {
        checkAuthStatus()
        observeSocketEvents()
    }

    fun loadRoadRoute(originLat: Double, originLng: Double, destLat: Double, destLng: Double) {
        viewModelScope.launch {
            val res = RouteHelper.fetchRoute(getApplication(), originLat, originLng, destLat, destLng, isCaptain = true)
            if (res is NetworkResult.Success) {
                val geoPoints = RouteHelper.toGeoPoints(res.data.coordinates)
                val topManeuver = res.data.maneuvers.firstOrNull()?.instruction ?: "Head towards destination"
                _uiState.value = _uiState.value.copy(
                    roadPolyline = geoPoints,
                    maneuvers = res.data.maneuvers,
                    currentManeuver = topManeuver
                )
            }
        }
    }

    fun loadChatMessages(rideId: String) {
        viewModelScope.launch {
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
        viewModelScope.launch {
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

        // 1. Instant Incoming Ride Request Flash Alert
        viewModelScope.launch {
            socketManager.incomingRideRequestFlow.collect { ride ->
                val currentActive = _uiState.value.activeRide
                val isBusyWithRide = currentActive != null && currentActive.status in listOf("accepted", "arrived", "ongoing")
                
                // Strict Vehicle Type Matching (e.g. Auto -> Auto, Bike -> Bike, Cab -> Cab)
                val captVehicle = _uiState.value.captain?.vehicleType ?: "bike"
                val isVehicleMatch = ride.vehicleType.equals(captVehicle, ignoreCase = true)

                if (!isBusyWithRide && isVehicleMatch) {
                    val currentList = _uiState.value.incomingRequests
                    if (currentList.none { it.id == ride.id }) {
                        _uiState.value = _uiState.value.copy(
                            incomingRequests = listOf(ride) + currentList
                        )
                        NotificationHelper.showNotification(
                            getApplication(),
                            "🚨 New ${ride.vehicleType.uppercase()} Request! (₹${ride.fare.toInt()})",
                            "Pickup: ${ride.pickupAddress} • ${ride.distanceKm} km trip"
                        )
                    }
                }
            }
        }

        // 2. Real-time Ride Cancellation Updates
        viewModelScope.launch {
            socketManager.liveRideStatusFlow.collect { update ->
                val currentRide = _uiState.value.activeRide
                if (currentRide != null && (update.rideId.isEmpty() || update.rideId == currentRide.id)) {
                    if (update.status == "cancelled") {
                        _uiState.value = _uiState.value.copy(
                            activeRide = null,
                            errorMessage = "Ride was cancelled by the passenger."
                        )
                        syncHistory()
                    }
                }
            }
        }

        // 3. In-App Real-Time Chat Messages from Rider
        viewModelScope.launch {
            chatRepo.liveChatFlow.collect { msg ->
                val current = _chatMessages.value
                if (current.none { it.id == msg.id }) {
                    _chatMessages.value = current + msg
                    if (msg.senderRole == "rider") {
                        NotificationHelper.showNotification(
                            getApplication(),
                            "💬 Message from Passenger",
                            msg.messageText
                        )
                    }
                }
            }
        }

        // 4. Live KYC Status Sync from Admin Verification / Instant AI Approval
        viewModelScope.launch {
            socketManager.liveKycStatusFlow.collect { map ->
                val status = map["status"] ?: return@collect
                val remarks = map["admin_remarks"]
                Log.i("CaptainViewModel", "⚡ Realtime KYC status updated to $status")
                fetchKycStatus()
                fetchProfile()
                if (status == "approved") {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "🎉 KYC Approved! You can now switch ONLINE."
                    )
                    NotificationHelper.showNotification(
                        getApplication(),
                        "🎉 Speedo KYC Approved!",
                        "Your documents are verified! You can now switch ONLINE and accept rides."
                    )
                } else if (status == "rejected") {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "KYC Rejected: ${remarks ?: "Please re-upload clear documents"}"
                    )
                }
            }
        }

        // 5. Live City Broadcasts & Incentive Alerts
        viewModelScope.launch {
            socketManager.liveBroadcastFlow.collect { bcast ->
                if (bcast.targetAudience == "all" || bcast.targetAudience == "captains") {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "📢 ${bcast.title}: ${bcast.message}"
                    )
                    NotificationHelper.showNotification(
                        getApplication(),
                        "📢 " + bcast.title,
                        bcast.message + (if (bcast.bonusAmount > 0) " (+₹${bcast.bonusAmount.toInt()} Bonus)" else "")
                    )
                }
            }
        }
    }

    fun checkAuthStatus() {
        val loggedIn = authRepo.tokenManager.isLoggedIn() && authRepo.tokenManager.getUserRole() == "captain"
        _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)

        if (loggedIn) {
            SpeedoSocketManager.getInstance(getApplication()).connect()
            fetchProfile()
            fetchKycStatus()
            checkActiveRide()
            syncHistory()
            syncNotifications()
        }
    }

    fun login(email: String, pass: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = authRepo.loginCaptain(email.trim(), pass)) {
                is NetworkResult.Success -> {
                    checkAuthStatus()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onComplete(true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                    onComplete(false)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onComplete(false)
                }
            }
        }
    }

    fun register(
        name: String,
        email: String,
        pass: String,
        phone: String,
        vehicleType: String,
        vehicleNumber: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = authRepo.registerCaptain(name.trim(), email.trim(), pass, phone.trim(), vehicleType.trim(), vehicleNumber.trim())) {
                is NetworkResult.Success -> {
                    checkAuthStatus()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onComplete(true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                    onComplete(false)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onComplete(false)
                }
            }
        }
    }

    fun logout() {
        if (_uiState.value.isOnline) {
            toggleOnline(false)
        }
        rideRequestPollingJob?.cancel()
        activeRidePollingJob?.cancel()
        authRepo.logout()
        _uiState.value = CaptainUiState(isLoggedIn = false)
    }

    fun fetchProfile() {
        viewModelScope.launch {
            when (val res = captainRepo.getProfile()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        captain = res.data,
                        isOnline = res.data.isOnline
                    )
                    if (res.data.isOnline) {
                        CaptainLocationService.startService(getApplication())
                        startIncomingRideRequestsPolling()
                    }
                }
                else -> {}
            }
        }
    }

    fun fetchKycStatus() {
        viewModelScope.launch {
            when (val res = captainRepo.getKycStatus()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(kycStatus = res.data)
                }
                else -> {}
            }
        }
    }

    fun uploadKycDocument(documentType: String, file: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingDoc = true, errorMessage = null)
            when (val res = captainRepo.uploadKycDocument(documentType, file)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isUploadingDoc = false,
                        successMessage = "Uploaded $documentType successfully!"
                    )
                    fetchKycStatus()
                    fetchProfile()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isUploadingDoc = false, errorMessage = res.message)
                }
                else -> {}
            }
        }
    }

    fun toggleOnline(targetOnline: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Check if KYC is approved
            val kycApproved = _uiState.value.kycStatus?.isApproved == true || _uiState.value.captain?.kycStatus == "approved"
            if (targetOnline && !kycApproved) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Cannot go ONLINE. Your KYC status is not approved yet."
                )
                return@launch
            }

            when (val res = captainRepo.toggleOnline(targetOnline)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(isOnline = targetOnline, isLoading = false)
                    if (targetOnline) {
                        CaptainLocationService.startService(getApplication())
                        startIncomingRideRequestsPolling()
                    } else {
                        CaptainLocationService.stopService(getApplication())
                        rideRequestPollingJob?.cancel()
                        _uiState.value = _uiState.value.copy(incomingRequests = emptyList())
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                else -> {}
            }
        }
    }

    fun startIncomingRideRequestsPolling() {
        rideRequestPollingJob?.cancel()
        rideRequestPollingJob = viewModelScope.launch {
            var lastRequestCount = 0

            while (isActive) {
                val currentActive = _uiState.value.activeRide
                val isBusyWithRide = currentActive != null && currentActive.status in listOf("accepted", "arrived", "ongoing")
                if (!isBusyWithRide) {
                    val res = captainRepo.getIncomingRideRequests()
                    if (res is NetworkResult.Success) {
                        val serverRequests = res.data
                        val currentList = _uiState.value.incomingRequests
                        val captVehicle = _uiState.value.captain?.vehicleType ?: "bike"
                        val merged = (serverRequests + currentList)
                            .distinctBy { it.id }
                            .filter { it.status == "requested" && it.vehicleType.equals(captVehicle, ignoreCase = true) }

                        _uiState.value = _uiState.value.copy(incomingRequests = merged)

                        // Trigger loud local notification when new requests appear
                        if (merged.isNotEmpty() && merged.size > lastRequestCount) {
                            val req = merged.first()
                            NotificationHelper.showNotification(
                                getApplication(),
                                "🚨 New Ride Request! (₹${req.fare.toInt()})",
                                "Pickup: ${req.pickupAddress} • ${req.distanceKm} km trip"
                            )
                        }
                        lastRequestCount = merged.size
                    }
                }
                delay(Constants.CAPTAIN_RIDE_REQUEST_POLL_INTERVAL_MS)
            }
        }
    }

    fun acceptRide(rideId: String, onAccepted: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = captainRepo.acceptRide(rideId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        activeRide = res.data,
                        incomingRequests = emptyList(),
                        isLoading = false,
                        successMessage = "Ride Accepted! Navigate to rider pickup."
                    )
                    startActiveRidePolling()
                    onAccepted()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                else -> {}
            }
        }
    }

    fun checkActiveRide() {
        viewModelScope.launch {
            val res = captainRepo.getActiveRide()
            if (res is NetworkResult.Success) {
                val ride = res.data
                if (ride != null && ride.status in listOf("accepted", "arrived", "ongoing")) {
                    _uiState.value = _uiState.value.copy(activeRide = ride)
                    startActiveRidePolling()
                } else {
                    _uiState.value = _uiState.value.copy(activeRide = null)
                }
            }
        }
    }

    private fun startActiveRidePolling() {
        activeRidePollingJob?.cancel()
        activeRidePollingJob = viewModelScope.launch {
            while (isActive) {
                delay(Constants.CAPTAIN_RIDE_REQUEST_POLL_INTERVAL_MS)
                val res = captainRepo.getActiveRide()
                if (res is NetworkResult.Success) {
                    val updated = res.data
                    _uiState.value = _uiState.value.copy(activeRide = updated)
                    if (updated == null) {
                        syncHistory()
                        fetchProfile()
                        break
                    }
                }
            }
        }
    }

    fun updateStatus(status: String, otp: String? = null, onComplete: () -> Unit = {}) {
        val rideId = _uiState.value.activeRide?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = captainRepo.updateRideStatus(rideId, status, otp)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Status updated to ${status.uppercase()}"
                    )
                    if (status == "completed") {
                        _uiState.value = _uiState.value.copy(activeRide = null)
                        syncHistory()
                        fetchProfile()
                    } else {
                        checkActiveRide()
                    }
                    onComplete()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                else -> {}
            }
        }
    }

    fun updateRideStatus(rideId: String, status: String, onComplete: () -> Unit = {}) {
        updateStatus(status, null, onComplete)
    }

    fun startRideWithOtp(rideId: String, otp: String, onComplete: (Boolean) -> Unit = {}) {
        val rId = _uiState.value.activeRide?.id ?: rideId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = captainRepo.updateRideStatus(rId, "ongoing", otp)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "OTP Verified! Trip started."
                    )
                    checkActiveRide()
                    onComplete(true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                    onComplete(false)
                }
                else -> {}
            }
        }
    }

    fun initiatePayment(ride: Ride) {
        _uiState.value = _uiState.value.copy(pendingPaymentRide = ride)
    }

    fun confirmPaymentAndFinishRide(rideId: String, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = captainRepo.updateRideStatus(rideId, "completed")) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeRide = null,
                        pendingPaymentRide = null,
                        successMessage = "Payment confirmed and ride completed successfully!"
                    )
                    syncHistory()
                    fetchProfile()
                    onFinished()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeRide = null,
                        pendingPaymentRide = null,
                        errorMessage = res.message
                    )
                    syncHistory()
                    fetchProfile()
                    onFinished()
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeRide = null,
                        pendingPaymentRide = null
                    )
                    onFinished()
                }
            }
        }
    }

    fun completeRide(rideId: String, onComplete: () -> Unit = {}) {
        confirmPaymentAndFinishRide(rideId, onComplete)
    }

    fun rejectRide(rideId: String) {
        _uiState.value = _uiState.value.copy(
            incomingRequests = _uiState.value.incomingRequests.filter { it.id != rideId }
        )
    }

    fun setOtpInput(otp: String) {
        _uiState.value = _uiState.value.copy(otpInput = otp)
    }

    fun syncHistory() {
        viewModelScope.launch {
            captainRepo.syncRideHistory()
        }
    }

    fun syncNotifications() {
        viewModelScope.launch {
            notifRepo.syncCaptainNotifications()
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            notifRepo.markCaptainRead(id)
        }
    }

    fun triggerSosEmergency(rideId: String? = null, lat: Double = 0.0, lng: Double = 0.0, address: String? = null, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val r = _uiState.value.activeRide
            val rId = rideId ?: r?.id
            val finalLat = if (lat != 0.0) lat else (r?.pickupLat ?: 12.9716)
            val finalLng = if (lng != 0.0) lng else (r?.pickupLng ?: 77.5946)
            val finalAddress = address ?: r?.pickupAddress ?: "Live GPS Location"

            // 1. Emit instant socket event
            SpeedoSocketManager.getInstance(getApplication()).emitSosTrigger(rId, finalLat, finalLng, finalAddress)
            // 2. Call REST API backup
            val adminRepo = com.speedo.core.repository.AdminRepository(getApplication())
            adminRepo.triggerSosAlert(com.speedo.core.model.TriggerSosRequest(rId, finalLat, finalLng, finalAddress))
            
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
