package com.speedo.core.repository

import android.content.Context
import com.speedo.core.model.Captain
import com.speedo.core.model.DashboardStats
import com.speedo.core.model.LiveMapResponse
import com.speedo.core.model.Ride
import com.speedo.core.network.NetworkResult
import com.speedo.core.network.RetrofitClient
import com.speedo.core.network.SpeedoApiService

class AdminRepository(context: Context) {
    private val api: SpeedoApiService = RetrofitClient.getService(context)

    suspend fun getDashboardStats(): NetworkResult<DashboardStats> {
        return try {
            val res = api.getAdminDashboard()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch stats")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getKycQueue(): NetworkResult<List<Captain>> {
        return try {
            val res = api.getAdminKycQueue()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch KYC queue")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun reviewKyc(captainId: String, status: String, remarks: String?): NetworkResult<Boolean> {
        return try {
            val body = mapOf("status" to status, "admin_remarks" to (remarks ?: ""))
            val res = api.reviewCaptainKyc(captainId, body)
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to review KYC")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getLiveMap(): NetworkResult<LiveMapResponse> {
        return try {
            val res = api.getAdminLiveMap()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch live map")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getRides(status: String? = null, vehicleType: String? = null): NetworkResult<List<Ride>> {
        return try {
            val res = api.getAdminRides(status, vehicleType)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch rides")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getUsersManagement(role: String = "all"): NetworkResult<com.speedo.core.model.UsersManagementResponse> {
        return try {
            val res = api.getAdminUsers(role)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch users")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun toggleUserStatus(role: String, id: String, isActive: Boolean): NetworkResult<Boolean> {
        return try {
            val res = api.toggleUserActiveStatus(role, id, mapOf("is_active" to isActive))
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to toggle status")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }
}
