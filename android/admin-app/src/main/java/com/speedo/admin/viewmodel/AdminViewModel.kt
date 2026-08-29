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
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepo = AuthRepository(application)
    private val adminRepo = AdminRepository(application)
    private val notifRepo = NotificationRepository(application)

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private var liveMapPollingJob: Job? = null
    private var dashboardPollingJob: Job? = null

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        val loggedIn = authRepo.tokenManager.isLoggedIn() && authRepo.tokenManager.getUserRole() == "admin"
        _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)

        if (loggedIn) {
            startDashboardPolling()
            fetchKycQueue()
            startLiveMapPolling()
            fetchRides()
            fetchUsers()
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
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = adminRepo.reviewKyc(captainId, status, remarks)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Captain KYC marked as ${status.uppercase()}"
                    )
                    fetchKycQueue()
                    onComplete()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
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
