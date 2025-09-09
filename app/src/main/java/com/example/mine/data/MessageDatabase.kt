package com.example.mine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Database(
    entities = [
        SessionEntity::class,
        DeviceKey::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MessageDatabase : RoomDatabase() {
    
    abstract fun sessionDao(): SessionDao
    abstract fun deviceKeyDao(): DeviceKeyDao
    
    companion object {
        @Volatile
        private var INSTANCE: MessageDatabase? = null
        
        fun getDatabase(context: Context): MessageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MessageDatabase::class.java,
                    "message_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// DAO interfaces with implementations
@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity)
    
    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): SessionEntity?
    
    @Query("SELECT * FROM sessions")
    suspend fun getAllSessions(): List<SessionEntity>
    
    @Delete
    suspend fun deleteSession(session: SessionEntity)
    
    @Query("DELETE FROM sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}

@Dao
interface DeviceKeyDao {
    @Insert
    suspend fun insertDeviceKey(deviceKey: DeviceKey)
    
    @Query("SELECT * FROM device_keys WHERE deviceId = :deviceId")
    suspend fun getDeviceKey(deviceId: String): DeviceKey?
    
    @Query("SELECT * FROM device_keys")
    suspend fun getAllDeviceKeys(): List<DeviceKey>
    
    @Delete
    suspend fun deleteDeviceKey(deviceKey: DeviceKey)
    
    @Query("DELETE FROM device_keys WHERE deviceId = :deviceId")
    suspend fun deleteDeviceKey(deviceId: String)
}

// Type converters for Room
class Converters {
    // Add any type converters if needed for complex data types
}
