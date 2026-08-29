package com.speedo.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RideEntity::class, NotificationEntity::class, KycStatusEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SpeedoDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao
    abstract fun notificationDao(): NotificationDao
    abstract fun kycDao(): KycDao

    companion object {
        @Volatile
        private var INSTANCE: SpeedoDatabase? = null

        fun getDatabase(context: Context): SpeedoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpeedoDatabase::class.java,
                    "speedo_offline.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
