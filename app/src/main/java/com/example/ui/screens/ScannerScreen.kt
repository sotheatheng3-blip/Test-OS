package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DeviceViewModel
import com.example.ui.ScannedDevice
import com.example.ui.components.RadarScannerEffect
import com.example.ui.components.SecurePairingDialog
import com.example.ui.theme.*

@Composable
fun ScannerScreen(
    viewModel: DeviceViewModel,
    modifier: Modifier = Modifier
) {
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val pairingState by viewModel.pairingState.collectAsState()
    var selectedTab by remember { mutableStateOf("All") } // "All", "Bluetooth", "WiFi"

    // Clear and scan when tab changes
    LaunchedEffect(selectedTab) {
        viewModel.stopScanning()
        viewModel.startScanning(selectedTab)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScanning()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick transport tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BentoGreySurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("All", "Bluetooth", "WiFi")
            tabs.forEach { tab ->
                val isActive = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) BentoPurpleBg else Color.Transparent)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tab == "All") "All Channels" else tab,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) BentoTextDeep else BentoTextSecondary
                        )
                    )
                }
            }
        }

        // Concentric Scan Radar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
            border = BorderStroke(1.dp, BentoBorder),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RadarScannerEffect(isScanning = isScanning, radarColor = BentoPurpleAccent)
                    
                    if (!isScanning) {
                        IconButton(
                            onClick = { viewModel.startScanning(selectedTab) },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(BentoPurpleAccent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Restart scanning",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isScanning) "SCANNING ACTIVE" else "SCANNER IDLE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isScanning) BentoPurpleAccent else BentoTextSecondary,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = if (isScanning) "Probing nearby Bluetooth & WiFi nodes..." else "Tap core button to restart survey",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BentoTextSecondary,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Scan Results List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DISCOVERED PEERS",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = BentoTextSecondary,
                    letterSpacing = 1.sp
                )
            )
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = BentoPurpleAccent
                )
            } else {
                Text(
                    text = "${scannedDevices.size} Found",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BentoTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Results Container
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (scannedDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BentoGreySurface),
                        border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.BluetoothDisabled,
                                    contentDescription = null,
                                    tint = BentoTextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Zero nodes detected yet",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = BentoTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Ensure devices are turned on and broadcasting.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = BentoTextSecondary)
                                )
                            }
                        }
                    }
                }
            } else {
                items(scannedDevices) { scanned ->
                    ScannedDeviceRow(
                        device = scanned,
                        onSave = { category ->
                            viewModel.saveDevice(scanned, category)
                        },
                        onSecurePair = { platform ->
                            viewModel.startSecurePairing(scanned, platform)
                        }
                    )
                }
            }
        }

        SecurePairingDialog(
            pairingState = pairingState,
            onConfirmPin = { viewModel.confirmPairingPin() },
            onCancel = { viewModel.cancelPairing() }
        )
    }
}

@Composable
fun ScannedDeviceRow(
    device: ScannedDevice,
    onSave: (String) -> Unit,
    onSecurePair: (String) -> Unit
) {
    var isSaved by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
        border = BorderStroke(1.dp, BentoBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal RSSI Icon Pillar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = if (device.type == "Bluetooth") Icons.Default.Bluetooth else Icons.Default.Wifi,
                    contentDescription = null,
                    tint = if (device.type == "Bluetooth") BentoPurpleAccent else BentoTextDeep,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            device.rssi >= -60 -> BentoGreenText
                            device.rssi >= -75 -> BentoPurpleAccent
                            else -> BentoAmberText
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text specs
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoTextDark
                    )
                )
                Text(
                    text = "${device.type} • ${device.address}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BentoTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (device.extraInfo.isNotBlank()) {
                    Text(
                        text = device.extraInfo,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BentoTextSecondary.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            // Action connect button
            Box {
                Button(
                    onClick = {
                        if (!isSaved) {
                            showCategoryMenu = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaved) BentoPurpleBg else BentoPurpleAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    if (isSaved) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Saved",
                            tint = BentoTextDeep,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = if (device.type == "Bluetooth") "Secure Pair" else "Add Link",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                    }
                }

                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false },
                    modifier = Modifier.background(BentoWhiteSurface)
                ) {
                    if (device.type == "Bluetooth") {
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Phonelink, contentDescription = null, tint = BentoPurpleAccent, modifier = Modifier.size(16.dp))
                                    Text("Pair with iOS Companion", color = BentoTextDark, fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                onSecurePair("iOS Companion Node")
                                showCategoryMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Android, contentDescription = null, tint = BentoPurpleAccent, modifier = Modifier.size(16.dp))
                                    Text("Pair with Android Peer", color = BentoTextDark, fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                onSecurePair("Android Host Core")
                                showCategoryMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = BentoPurpleAccent, modifier = Modifier.size(16.dp))
                                    Text("Pair with IoT Controller", color = BentoTextDark, fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                onSecurePair("ESP32 IoT Node")
                                showCategoryMenu = false
                            }
                        )
                        HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))
                        DropdownMenuItem(
                            text = { Text("Legacy Direct Add (No Key)", color = BentoTextSecondary, fontWeight = FontWeight.Normal) },
                            onClick = {
                                onSave("General Bluetooth")
                                isSaved = true
                                showCategoryMenu = false
                            }
                        )
                    } else {
                        val categories = listOf("Smart Home", "Industrial", "Robotics")
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = BentoTextDark, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    onSave(cat)
                                    isSaved = true
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
