package com.speedo.core.repository

import android.content.Context
import com.speedo.core.database.KycDao
import com.speedo.core.database.KycStatusEntity
import com.speedo.core.database.RideDao
import com.speedo.core.database.RideEntity
import com.speedo.core.database.SpeedoDatabase
import com.speedo.core.model.Captain
import com.speedo.core.model.KycStatusResponse
import com.speedo.core.model.Ride
import com.speedo.core.network.NetworkResult
import com.speedo.core.network.RetrofitClient
import com.speedo.core.network.SpeedoApiService
import com.speedo.core.storage.TokenManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CaptainRepository(context: Context) {
    private val api: SpeedoApiService = RetrofitClient.getService(context)
    private val rideDao: RideDao = SpeedoDatabase.getDatabase(context).rideDao()
    private val kycDao: KycDao = SpeedoDatabase.getDatabase(context).kycDao()
    private val tokenManager = TokenManager(context)

    val cachedRidesFlow: Flow<List<Ride>> = rideDao.getAllRides().map { list ->
        list.map { it.toDomainModel() }
    }

    suspend fun getProfile(): NetworkResult<Captain> {
        return try {
            val res = api.getCaptainProfile()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to get captain profile")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun toggleOnline(isOnline: Boolean): NetworkResult<Boolean> {
        return try {
            val res = api.toggleCaptainOnline(mapOf("is_online" to isOnline))
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(isOnline)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to update online status")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun updateLocation(lat: Double, lng: Double, bearing: Float = 0f, speed: Float = 0f): NetworkResult<Boolean> {
        return try {
            val res = api.updateCaptainLocation(
                mapOf("lat" to lat, "lng" to lng, "bearing" to bearing.toDouble(), "speed" to speed.toDouble())
            )
            if (res.isSuccessful) NetworkResult.Success(true)
            else NetworkResult.Error("Location push failed")
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun uploadKycDocument(documentType: String, file: File): NetworkResult<Boolean> {
        return try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("document", file.name, requestFile)
            val typeBody = documentType.toRequestBody("text/plain".toMediaTypeOrNull())

            val res = api.uploadKycDocument(body, typeBody)
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Upload failed")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getKycStatus(): NetworkResult<KycStatusResponse> {
        return try {
            val res = api.getCaptainKycStatus()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val data = res.body()!!.data!!
                tokenManager.getUserId()?.let { captainId ->
                    kycDao.insertKycStatus(
                        KycStatusEntity(
                            captainId = captainId,
                            kycStatus = data.kycStatus,
                            adminRemarks = data.adminRemarks,
                            paymentQrUrl = data.paymentQrUrl,
                            isApproved = data.isApproved,
                            isComplete = data.isComplete
                        )
                    )
                }
                NetworkResult.Success(data)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to get KYC status")
            }
        } catch (e: Exception) {
            // Check offline cached KYC
            val captainId = tokenManager.getUserId()
            if (captainId != null) {
                val cached = kycDao.getKycStatus(captainId)
                if (cached != null) {
                    return NetworkResult.Success(
                        KycStatusResponse(
                            kycStatus = cached.kycStatus,
                            adminRemarks = cached.adminRemarks,
                            paymentQrUrl = cached.paymentQrUrl,
                            documents = emptyList(),
                            isComplete = cached.isComplete,
                            isApproved = cached.isApproved
                        )
                    )
                }
            }
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getIncomingRideRequests(): NetworkResult<List<Ride>> {
        return try {
            val res = api.getIncomingRideRequests()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch ride requests")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun acceptRide(rideId: String): NetworkResult<Ride> {
        return try {
            val res = api.acceptRide(rideId)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val ride = res.body()!!.data!!
                rideDao.insertRide(RideEntity.fromDomainModel(ride))
                NetworkResult.Success(ride)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to accept ride")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getActiveRide(): NetworkResult<Ride?> {
        return try {
            val res = api.getCaptainActiveRide()
            if (res.isSuccessful && res.body()?.success == true) {
                val ride = res.body()?.data
                if (ride != null) {
                    rideDao.insertRide(RideEntity.fromDomainModel(ride))
                }
                NetworkResult.Success(ride)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to get active ride")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun updateRideStatus(rideId: String, status: String, otp: String? = null): NetworkResult<Boolean> {
        return try {
            val body = mutableMapOf("status" to status)
            if (otp != null) body["otp"] = otp

            val res = api.updateRideStatus(rideId, body)
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to update status")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun syncRideHistory(): NetworkResult<List<Ride>> {
        return try {
            val res = api.getCaptainRideHistory()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val rides = res.body()!!.data!!
                rideDao.insertRides(rides.map { RideEntity.fromDomainModel(it) })
                NetworkResult.Success(rides)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch ride history")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun requestAccountDeletion(reason: String): NetworkResult<com.speedo.core.model.AccountDeletionRequest> {
        return try {
            val res = api.requestCaptainAccountDeletion(com.speedo.core.model.CreateDeletionRequestBody(reason))
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to submit deletion request")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getAccountDeletionStatus(): NetworkResult<com.speedo.core.model.AccountDeletionRequest?> {
        return try {
            val res = api.getCaptainDeletionStatus()
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(res.body()?.data)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to get deletion status")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun cancelAccountDeletion(): NetworkResult<Boolean> {
        return try {
            val res = api.cancelCaptainAccountDeletion()
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to cancel deletion request")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

}