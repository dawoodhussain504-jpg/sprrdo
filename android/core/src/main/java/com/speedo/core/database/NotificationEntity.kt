package com.speedo.core.database

import androidx.room.*
import com.speedo.core.model.NotificationItem
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val recipientId: String,
    val recipientRole: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Int,
    val metadataJson: String?,
    val createdAt: String?
) {
    fun toDomainModel(): NotificationItem = NotificationItem(
        id = id,
        recipientId = recipientId,
        recipientRole = recipientRole,
        title = title,
        message = message,
        type = type,
        isRead = isRead,
        metadataJson = metadataJson,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(item: NotificationItem): NotificationEntity = NotificationEntity(
            id = item.id,
            recipientId = item.recipientId,
            recipientRole = item.recipientRole,
            title = item.title,
            message = item.message,
            type = item.type,
            isRead = item.isRead,
            metadataJson = item.metadataJson,
            createdAt = item.createdAt
        )
    }
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM cached_notifications WHERE isRead = 0 ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM cached_notifications WHERE isRead = 0")
    fun getUnreadCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(items: List<NotificationEntity>)

    @Query("UPDATE cached_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM cached_notifications WHERE isRead = 1 OR id = :id")
    suspend fun removeNotification(id: String)

    @Query("DELETE FROM cached_notifications WHERE isRead = 1")
    suspend fun clearReadNotifications()

    @Query("DELETE FROM cached_notifications")
    suspend fun clearNotifications()
}

@Entity(tableName = "cached_kyc_status")
data class KycStatusEntity(
    @PrimaryKey val captainId: String,
    val kycStatus: String,
    val adminRemarks: String?,
    val paymentQrUrl: String?,
    val isApproved: Boolean,
    val isComplete: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface KycDao {
    @Query("SELECT * FROM cached_kyc_status WHERE captainId = :captainId")
    suspend fun getKycStatus(captainId: String): KycStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKycStatus(status: KycStatusEntity)

    @Query("DELETE FROM cached_kyc_status")
    suspend fun clearKyc()
}
