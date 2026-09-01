package com.speedo.core.repository

import android.content.Context
import android.util.Log
import com.speedo.core.model.PopularDestination
import com.speedo.core.model.PopularDestinationsData
import com.speedo.core.network.NetworkResult
import com.speedo.core.network.RetrofitClient
import com.speedo.core.socket.SpeedoSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PopularDestinationRepository private constructor(private val context: Context) {

    private val api = RetrofitClient.getService(context)
    private val socketManager = SpeedoSocketManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _destinationsFlow = MutableStateFlow<List<PopularDestination>>(PopularDestinationsData.ALL_DESTINATIONS)
    val destinationsFlow: StateFlow<List<PopularDestination>> = _destinationsFlow.asStateFlow()

    init {
        // Initial fetch from backend
        refreshDestinations()

        // Observe real-time WebSocket updates when Admin adds, edits, or deletes a destination
        scope.launch {
            socketManager.liveDestinationsUpdatedFlow.collect {
                Log.d("PopularDestRepo", "⚡ Real-time WebSocket destinations update received, refreshing list!")
                refreshDestinations()
            }
        }
    }

    fun refreshDestinations() {
        scope.launch {
            try {
                val res = api.getPopularDestinations()
                if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                    val list = res.body()!!.data!!
                    if (list.isNotEmpty()) {
                        _destinationsFlow.value = list
                    }
                }
            } catch (e: Exception) {
                Log.w("PopularDestRepo", "Failed to fetch popular destinations from API, using fallback: ${e.message}")
            }
        }
    }

    suspend fun getAdminDestinations(): NetworkResult<List<PopularDestination>> {
        return try {
            val res = api.getAdminPopularDestinations()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val list = res.body()!!.data!!
                _destinationsFlow.value = list.filter { it.isActive }
                NetworkResult.Success(list)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to load destinations")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun createDestination(body: Map<String, Any>): NetworkResult<PopularDestination> {
        return try {
            val res = api.createPopularDestination(body)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                refreshDestinations()
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to create destination")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun updateDestination(id: String, body: Map<String, Any>): NetworkResult<PopularDestination> {
        return try {
            val res = api.updatePopularDestination(id, body)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                refreshDestinations()
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to update destination")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun deleteDestination(id: String): NetworkResult<Boolean> {
        return try {
            val res = api.deletePopularDestination(id)
            if (res.isSuccessful && res.body()?.success == true) {
                refreshDestinations()
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to delete destination")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PopularDestinationRepository? = null

        fun getInstance(context: Context): PopularDestinationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PopularDestinationRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
