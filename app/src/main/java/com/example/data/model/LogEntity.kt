package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: String,
    val timestamp: Long,
    val level: String, // "INFO", "DEBUG", "WARN", "ERROR"
    val message: String
)
