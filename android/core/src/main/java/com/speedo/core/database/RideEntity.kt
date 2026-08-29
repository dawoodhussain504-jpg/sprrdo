package com.speedo.core.database

import androidx.room.*
import com.speedo.core.model.Ride
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_rides")
data class RideEntity(
    @PrimaryKey val id: String,
    val riderId: String,
    val captainId: String?,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropAddress: String,
    val dropLat: Double,
    val dropLng: Double,
    val vehicleType: String,
    val fare: Double,
    val distanceKm: Double,
    val status: String,
    val otp: String,
    val captainName: String?,
    val captainPhone: String?,
    val vehicleNumber: String?,
    val captainRating: Double?,
    val createdAt: String?
) {
    fun toDomainModel(): Ride = Ride(
        id = id,
        riderId = riderId,
        captainId = captainId,
        pickupAddress = pickupAddress,
        pickupLat = pickupLat,
        pickupLng = pickupLng,
        dropAddress = dropAddress,
        dropLat = dropLat,
        dropLng = dropLng,
        vehicleType = vehicleType,
        fare = fare,
        distanceKm = distanceKm,
        status = status,
        otp = otp,
        captainName = captainName,
        captainPhone = captainPhone,
        vehicleNumber = vehicleNumber,
        captainRating = captainRating,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(ride: Ride): RideEntity = RideEntity(
            id = ride.id,
            riderId = ride.riderId,
            captainId = ride.captainId,
            pickupAddress = ride.pickupAddress,
            pickupLat = ride.pickupLat,
            pickupLng = ride.pickupLng,
            dropAddress = ride.dropAddress,
            dropLat = ride.dropLat,
            dropLng = ride.dropLng,
            vehicleType = ride.vehicleType,
            fare = ride.fare,
            distanceKm = ride.distanceKm,
            status = ride.status,
            otp = ride.otp,
            captainName = ride.captainName,
            captainPhone = ride.captainPhone,
            vehicleNumber = ride.vehicleNumber,
            captainRating = ride.captainRating,
            createdAt = ride.createdAt
        )
    }
}

@Dao
interface RideDao {
    @Query("SELECT * FROM cached_rides ORDER BY createdAt DESC")
    fun getAllRides(): Flow<List<RideEntity>>

    @Query("SELECT * FROM cached_rides WHERE id = :rideId")
    suspend fun getRideById(rideId: String): RideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRides(rides: List<RideEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEntity)

    @Query("DELETE FROM cached_rides")
    suspend fun clearRides()
}
