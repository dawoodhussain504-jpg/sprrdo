package com.speedo.core.repository

import android.content.Context
import com.speedo.core.database.RideDao
import com.speedo.core.database.RideEntity
import com.speedo.core.database.SpeedoDatabase
import com.speedo.core.model.Captain
import com.speedo.core.model.FareEstimatesResponse
import com.speedo.core.model.Ride
import com.speedo.core.network.NetworkResult
import com.speedo.core.network.RetrofitClient
import com.speedo.core.network.SpeedoApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RiderRepository(context: Context) {
    private val api: SpeedoApiService = RetrofitClient.getService(context)
    private val rideDao: RideDao = SpeedoDatabase.getDatabase(context).rideDao()

    val cachedRidesFlow: Flow<List<Ride>> = rideDao.getAllRides().map { list ->
        list.map { it.toDomainModel() }
    }

    suspend fun getNearbyCaptains(lat: Double, lng: Double, radiusKm: Double = 5.0, vehicleType: String? = null): NetworkResult<List<Captain>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getNearbyCaptains(lat, lng, radiusKm, vehicleType)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch nearby captains")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun estimateFares(pickupLat: Double, pickupLng: Double, dropLat: Double, dropLng: Double): NetworkResult<FareEstimatesResponse> = withContext(Dispatchers.IO) {
        try {
            val res = api.estimateFares(
                mapOf(
                    "pickup_lat" to pickupLat,
                    "pickup_lng" to pickupLng,
                    "drop_lat" to dropLat,
                    "drop_lng" to dropLng
                )
            )
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to calculate fares")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun requestRide(
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        dropAddress: String,
        dropLat: Double,
        dropLng: Double,
        vehicleType: String
    ): NetworkResult<Ride> = withContext(Dispatchers.IO) {
        try {
            val res = api.requestRide(
                com.speedo.core.model.RideRequestBody(
                    pickupAddress = pickupAddress,
                    pickupLat = pickupLat,
                    pickupLng = pickupLng,
                    dropAddress = dropAddress,
                    dropLat = dropLat,
                    dropLng = dropLng,
                    vehicleType = vehicleType
                )
            )
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val ride = res.body()!!.data!!
                rideDao.insertRide(RideEntity.fromDomainModel(ride))
                NetworkResult.Success(ride)
            } else {
                val errMsg = res.body()?.message ?: res.errorBody()?.string() ?: "Failed to request ride"
                NetworkResult.Error(errMsg)
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getActiveRide(): NetworkResult<Ride?> = withContext(Dispatchers.IO) {
        try {
            val res = api.getRiderActiveRide()
            if (res.isSuccessful && res.body()?.success == true) {
                val ride = res.body()?.data
                if (ride != null) {
                    rideDao.insertRide(RideEntity.fromDomainModel(ride))
                }
                NetworkResult.Success(ride)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to fetch active ride")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun cancelRide(rideId: String, reason: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.cancelRide(rideId, mapOf("reason" to reason))
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to cancel ride")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun syncRideHistory(): NetworkResult<List<Ride>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getRiderRideHistory()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val rides = res.body()!!.data!!
                rideDao.insertRides(rides.map { RideEntity.fromDomainModel(it) })
                NetworkResult.Success(rides)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to sync history")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }
}
