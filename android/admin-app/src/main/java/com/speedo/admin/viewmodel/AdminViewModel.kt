package com.speedo.admin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.speedo.core.model.*
import com.speedo.core.network.NetworkResult
import com.speedo.core.repository.AdminRepository
import com.speedo.core.repository.AuthRepository
import com.speedo.core.repository.NotificationRepository
import com.speedo.core.utils.Constants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoggedIn: Boolean = false,
    val dashboardStats: DashboardStats? = null,
    val kycQueue: List<Captain> = emptyList(),
    val liveMapData: LiveMapResponse? = null,
    val rides: List<Ride> = emptyList(),
    val riders: List<User> = emptyList(),
    val captains: List<Captain> = emptyList(),
    val selectedRideFilter: String = "all",
    val aiScanResults: Map<String, KycAiScanResult> = emptyMap(),
    val isAiScanning: Boolean = false,
    val surgeZones: List<SurgeZone> = emptyList(),
    val sosAlerts: List<SosAlert> = emptyList(),
    val activeSosCount: Int = 0,
    val broadcasts: List<BroadcastAnnouncement> = emptyList(),
    val isSubmittingAction: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepo = AuthRepository(application)
    private val adminRepo = AdminRepository(application)
    private val notifRepo = NotificationRepository(application)
    private val socketManager = com.speedo.core.socket.SpeedoSocketManager.getInstance(application)

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private var liveMapPollingJob: Job? = null
    private var dashboardPollingJob: Job? = null

    init {
        checkAuthStatus()
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        socketManager.connect()
        socketManager.joinAdminSupportRoom()

        viewModelScope.launch {
            socketManager.liveSosAlertFlow.collect { alert ->
                val current = _uiState.value.sosAlerts.toMutableList()
                current.removeAll { it.id == alert.id }
                current.add(0, alert)
                _uiState.value = _uiState.value.copy(
                    sosAlerts = current,
                    activeSosCount = current.count { it.status == "active" || it.status == "in_progress" },
                    successMessage = "🚨 LIVE SOS ALERT from ${alert.userName} (${alert.userPhone})!"
                )
            }
        }

        viewModelScope.launch {
            socketManager.liveSosResolvedFlow.collect { map ->
                val id = map["id"] ?: return@collect
                val st = map["status"] ?: "resolved"
                val notes = map["admin_notes"]
                val updated = _uiState.value.sosAlerts.map {
                    if (it.id == id) it.copy(status = st, adminNotes = notes) else it
                }
                _uiState.value = _uiState.value.copy(
                    sosAlerts = updated,
                    activeSosCount = updated.count { it.status == "active" || it.status == "in_progress" }
                )
            }
        }

        viewModelScope.launch {
            socketManager.liveSurgeUpdateFlow.collect {
                fetchSurgeZones()
            }
        }

        viewModelScope.launch {
            socketManager.liveBroadcastFlow.collect { bcast ->
                val current = _uiState.value.broadcasts.toMutableList()
                current.removeAll { it.id == bcast.id }
                current.add(0, bcast)
                _uiState.value = _uiState.value.copy(broadcasts = current)
            }
        }
    }

    fun checkAuthStatus() {
        val loggedIn = authRepo.tokenManager.isLoggedIn() && authRepo.tokenManager.getUserRole() == "admin"
        _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)

        if (loggedIn) {
            socketManager.connect()
            startDashboardPolling()
            fetchKycQueue()
            startLiveMapPolling()
            fetchRides()
            fetchUsers()
            fetchSurgeZones()
            fetchSosAlerts()
            fetchBroadcasts()
        }
    }

    fun login(email: String, pass: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = authRepo.loginAdmin(email.trim(), pass)) {
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
        liveMapPollingJob?.cancel()
        dashboardPollingJob?.cancel()
        authRepo.logout()
        _uiState.value = AdminUiState(isLoggedIn = false)
    }

    fun startDashboardPolling() {
        dashboardPollingJob?.cancel()
        dashboardPollingJob = viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isLoggedIn) {
                    when (val res = adminRepo.getDashboardStats()) {
                        is NetworkResult.Success -> {
                            _uiState.value = _uiState.value.copy(dashboardStats = res.data)
                        }
                        else -> {}
                    }
                    // Background refresh of SOS alert count
                    when (val sosRes = adminRepo.getSosAlerts()) {
                        is NetworkResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                sosAlerts = sosRes.data,
                                activeSosCount = sosRes.data.count { it.status == "active" || it.status == "in_progress" }
                            )
                        }
                        else -> {}
                    }
                }
                delay(Constants.ADMIN_MAP_POLL_INTERVAL_MS * 2)
            }
        }
    }

    fun fetchKycQueue() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val res = adminRepo.getKycQueue()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(kycQueue = res.data, isLoading = false)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                else -> {}
            }
        }
    }

    fun reviewKyc(captainId: String, status: String, remarks: String?, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingAction = true, errorMessage = null)
            when (val res = adminRepo.reviewKyc(captainId, status, remarks)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingAction = false,
                        successMessage = "Captain KYC marked as ${status.uppercase()}"
                    )
                    fetchKycQueue()
                    onComplete()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSubmittingAction = false, errorMessage = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSubmittingAction = false)
                }
            }
        }
    }

    // --- 1. AI DOCUMENT OCR & INSTANT KYC SCAN ---
    fun runAiKycScan(captainId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiScanning = true, errorMessage = null)
            when (val res = adminRepo.aiScanKyc(captainId)) {
                is NetworkResult.Success -> {
                    val updated = _uiState.value.aiScanResults.toMutableMap()
                    updated[captainId] = res.data
                    _uiState.value = _uiState.value.copy(
                        isAiScanning = false,
                        aiScanResults = updated,
                        successMessage = "AI OCR Scan complete! Match score: ${res.data.overallScore}%"
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isAiScanning = false, errorMessage = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isAiScanning = false)
                }
            }
        }
    }

    fun instantApproveKyc(captainId: String, remarks: String? = null, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingAction = true, errorMessage = null)
            when (val res = adminRepo.instantApproveKyc(captainId, remarks)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingAction = false,
                        successMessage = "Captain instantly approved via AI Verification! 🚀"
                    )
                    fetchKycQueue()
                    onComplete()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSubmittingAction = false, errorMessage = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSubmittingAction = false)
                }
            }
        }
    }

    // --- 2. GEOFENCED SURGE ZONES ---
    fun fetchSurgeZones() {
        viewModelScope.launch {
            when (val res = adminRepo.getSurgeZones()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(surgeZones = res.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> {}
            }
        }
    }

    fun createSurgeZone(
        name: String,
        zoneType: String,
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double,
        surge: Double,
        baseMul: Double,
        perKmMul: Double,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingAction = true, errorMessage = null)
            val req = CreateSurgeZoneRequest(name, zoneType, centerLat, centerLng, radiusKm, surge, baseMul, perKmMul)
            when (val res = adminRepo.createSurgeZone(req)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingAction = false,
                        successMessage = "Surge zone '${name}' created with ${surge}x multiplier!"
                    )
                    fetchSurgeZones()
                    onComplete()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSubmittingAction = false, errorMessage = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSubmittingAction = false)
                }
            }
        }
    }

    fun toggleSurgeZone(id: String, isActive: Boolean) {
        viewModelScope.launch {
            when (val res = adminRepo.updateSurgeZone(id, UpdateSurgeZoneRequest(isActive = isActive))) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Surge zone status updated to ${if (isActive) "Active" else "Paused"}"
                    )
                    fetchSurgeZones()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> {}
            }
        }
    }

    fun deleteSurgeZone(id: String) {
        viewModelScope.launch {
            when (val res = adminRepo.deleteSurgeZone(id)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = "Surge zone removed")
                    fetchSurgeZones()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> {}
            }
        }
    }

    // --- 3. LIVE SOS EMERGENCY COMMAND CENTER ---
    fun fetchSosAlerts() {
        viewModelScope.launch {
            when (val res = adminRepo.getSosAlerts()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        sosAlerts = res.data,
                        activeSosCount = res.data.count { it.status == "active" || it.status == "in_progress" }
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> {}
            }
        }
    }

    fun resolveSosAlert(id: String, status: String, notes: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingAction = true, errorMessage = null)
            when (val res = adminRepo.resolveSosAlert(id, status, notes)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingAction = false,
                        successMessage = "SOS alert marked as ${status.uppercase()}"
                    )
                    fetchSosAlerts()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSubmittingAction = false, errorMessage = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSubmittingAction = false)
                }
            }
        }
    }

    // --- 4. TARGETED CITY-WIDE BROADCASTS ---
    fun sendBroadcast(
        title: String,
        message: String,
        audience: String,
        city: String,
        coupon: String?,
        discount: Double,
        bonus: Double,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingAction = true, errorMessage = null)
            val req = SendBroadcastRequest(title, message, audience, city, coupon, discount, bonus)
            when (val res = adminRepo.sendBroadcast(req)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingAction = false,
                        successMessage = "Broadcast delivered successfully to ${audience.uppercase()}!"
                    )
                    fetchBroadcasts()
                    onComplete()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSubmittingAction = false, errorMessage = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSubmittingAction = false)
                }
            }
        }
    }

    fun fetchBroadcasts() {
        viewModelScope.launch {
            when (val res = adminRepo.getBroadcasts()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(broadcasts = res.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> {}
            }
        }
    }

    fun startLiveMapPolling() {
        liveMapPollingJob?.cancel()
        liveMapPollingJob = viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isLoggedIn) {
                    when (val res = adminRepo.getLiveMap()) {
                        is NetworkResult.Success -> {
                            _uiState.value = _uiState.value.copy(liveMapData = res.data)
                        }
                        else -> {}
                    }
                }
                delay(Constants.ADMIN_MAP_POLL_INTERVAL_MS)
            }
        }
    }

    fun fetchRides(status: String? = null) {
        val filter = status ?: _uiState.value.selectedRideFilter
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedRideFilter = filter)
            when (val res = adminRepo.getRides(if (filter == "all") null else filter)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(rides = res.data)
                }
                else -> {}
            }
        }
    }

    fun fetchUsers() {
        viewModelScope.launch {
            when (val res = adminRepo.getUsersManagement("all")) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        riders = res.data.riders,
                        captains = res.data.captains
                    )
                }
                else -> {}
            }
        }
    }

    fun toggleUserStatus(role: String, id: String, currentActive: Boolean) {
        viewModelScope.launch {
            val target = !currentActive
            when (val res = adminRepo.toggleUserStatus(role, id, target)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "$role status updated to ${if (target) "Active" else "Suspended"}"
                    )
                    fetchUsers()
                    fetchKycQueue()
                }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
