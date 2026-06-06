package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "Bluetooth", "WiFi"
    val address: String, // MAC or IP address
    val status: String, // "Connected", "Disconnected"
    val firmwareVersion: String,
    val lastConnected: Long,
    val category: String, // "Smart Home", "Industrial", "Robotics", "iOS Companion"
    val supportsOta: Boolean = true
)
