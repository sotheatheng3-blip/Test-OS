package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.model.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM device_logs WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 500")
    fun getLogsForDeviceFlow(deviceId: String): Flow<List<LogEntity>>

    @Query("SELECT * FROM device_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogsFlow(): Flow<List<LogEntity>>

    @Insert
    suspend fun insertLog(log: LogEntity)

    @Query("DELETE FROM device_logs WHERE deviceId = :deviceId")
    suspend fun clearLogsForDevice(deviceId: String)

    @Query("DELETE FROM device_logs")
    suspend fun clearAllLogs()
}
