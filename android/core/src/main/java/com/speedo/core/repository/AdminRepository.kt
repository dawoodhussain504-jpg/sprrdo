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

    // --- 1. AI DOCUMENT OCR & INSTANT KYC ---
    suspend fun aiScanKyc(captainId: String): NetworkResult<com.speedo.core.model.KycAiScanResult> {
        return try {
            val res = api.aiScanKycDocuments(captainId)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to perform AI OCR scan")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun instantApproveKyc(captainId: String, remarks: String?): NetworkResult<Boolean> {
        return try {
            val res = api.instantApproveCaptainKyc(captainId, com.speedo.core.model.InstantApproveKycRequest(remarks))
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to instant-approve captain")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    // --- 2. GEOFENCED SURGE ZONES ---
    suspend fun getSurgeZones(): NetworkResult<List<com.speedo.core.model.SurgeZone>> {
        return try {
            val res = api.getSurgeZones()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch surge zones")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun createSurgeZone(req: com.speedo.core.model.CreateSurgeZoneRequest): NetworkResult<com.speedo.core.model.SurgeZone> {
        return try {
            val res = api.createSurgeZone(req)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to create surge zone")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun updateSurgeZone(id: String, req: com.speedo.core.model.UpdateSurgeZoneRequest): NetworkResult<Boolean> {
        return try {
            val res = api.updateSurgeZone(id, req)
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to update surge zone")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun deleteSurgeZone(id: String): NetworkResult<Boolean> {
        return try {
            val res = api.deleteSurgeZone(id)
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to delete surge zone")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    // --- 3. LIVE SOS EMERGENCY COMMAND CENTER ---
    suspend fun getSosAlerts(): NetworkResult<List<com.speedo.core.model.SosAlert>> {
        return try {
            val res = api.getSosAlerts()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch SOS alerts")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun resolveSosAlert(id: String, status: String, notes: String?): NetworkResult<Boolean> {
        return try {
            val res = api.resolveSosAlert(id, com.speedo.core.model.ResolveSosRequest(status, notes))
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to resolve SOS alert")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun triggerSosAlert(req: com.speedo.core.model.TriggerSosRequest): NetworkResult<Boolean> {
        return try {
            val res = api.triggerSosEmergency(req)
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to trigger SOS alert")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    // --- 4. TARGETED CITY-WIDE BROADCASTS ---
    suspend fun sendBroadcast(req: com.speedo.core.model.SendBroadcastRequest): NetworkResult<Boolean> {
        return try {
            val res = api.sendBroadcast(req)
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to send broadcast")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getBroadcasts(): NetworkResult<List<com.speedo.core.model.BroadcastAnnouncement>> {
        return try {
            val res = api.getBroadcasts()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch broadcasts")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }
}
