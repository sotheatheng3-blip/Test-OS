package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY lastConnected DESC")
    fun getAllDevicesFlow(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :deviceId")
    suspend fun getDeviceById(deviceId: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Query("UPDATE devices SET status = :status, lastConnected = :lastConnected WHERE id = :deviceId")
    suspend fun updateDeviceStatus(deviceId: String, status: String, lastConnected: Long)

    @Query("UPDATE devices SET batteryLevel = :batteryLevel, signalStrength = :signalStrength, operationalMode = :operationalMode WHERE id = :deviceId")
    suspend fun updateDeviceTelemetry(deviceId: String, batteryLevel: Int, signalStrength: Int, operationalMode: String)

    @Query("UPDATE devices SET firmwareVersion = :version WHERE id = :deviceId")
    suspend fun updateDeviceFirmware(deviceId: String, version: String)

    @Delete
    suspend fun deleteDevice(device: DeviceEntity)
}
