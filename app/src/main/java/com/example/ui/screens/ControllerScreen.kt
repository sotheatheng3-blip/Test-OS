package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DeviceViewModel
import com.example.ui.components.RadialGauge
import com.example.ui.components.TelemetryWaveGraph
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun ControllerScreen(
    viewModel: DeviceViewModel,
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDevice by viewModel.activeDevice.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val deviceLogs by viewModel.deviceLogs.collectAsState()

    val windowScale by viewModel.windowAnimationScale.collectAsState()
    val transitionScale by viewModel.transitionAnimationScale.collectAsState()
    val animatorDurationScale by viewModel.animatorDurationScale.collectAsState()

    // Tracking the temperature values for historical micro graph (telemetry tracker)
    val tempHistory = remember { mutableStateListOf<Float>() }
    var showEsimProvisionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(telemetry.temperature) {
        if (activeDevice != null) {
            tempHistory.add(telemetry.temperature)
            if (tempHistory.size > 20) {
                tempHistory.removeAt(0)
            }
        } else {
            tempHistory.clear()
        }
    }

    if (activeDevice == null || connectionState == "Disconnected") {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BentoBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(BentoGreySurface)
                        .background(BentoPurpleBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = BentoTextDeep,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "NO CONTROLLER ACTIVE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = BentoTextDark,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You must select and connect to a device before viewing real-time sensory telemetry or dispatching commands.",
                    color = BentoTextSecondary,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onNavigateToScan,
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Radar, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Connection Scanner",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(BentoBg)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Header Active Device Specs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                    border = BorderStroke(1.dp, BentoBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BentoGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BentoGreenText)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeDevice!!.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BentoTextDark
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activeDevice!!.type} • ${activeDevice!!.address}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BentoTextSecondary,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BentoPurpleBg)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${telemetry.rssi} dBm",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (telemetry.rssi >= -65) BentoGreenText else BentoAmberText
                                )
                            )
                        }
                    }
                }
            }

            // Realtime Dials / Gauges Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                        border = BorderStroke(1.dp, BentoBorder),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RadialGauge(
                                value = telemetry.temperature,
                                max = 80f,
                                title = "Temp",
                                unit = "°C",
                                gaugeColor = if (telemetry.temperature > 50f) BentoAmberText else BentoPurpleAccent
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                        border = BorderStroke(1.dp, BentoBorder),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RadialGauge(
                                value = telemetry.cpuLoad,
                                max = 100f,
                                title = "MCU",
                                unit = "%",
                                gaugeColor = BentoPurpleAccent
                            )
                        }
                    }
                }
            }

            // Historical telemetries wave graph (Spark line)
            if (tempHistory.size > 1) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                        border = BorderStroke(1.dp, BentoBorder),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TEMPERATURE TRACKER TIMELINE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BentoTextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Text(
                                    text = "Peak: ${String.format("%.1f", tempHistory.maxOrNull() ?: 0f)}°C",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BentoTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            TelemetryWaveGraph(
                                values = tempHistory.toList(),
                                max = 80f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp),
                                lineColor = BentoPurpleAccent
                            )
                        }
                    }
                }
            }

            // Power Metrics Info Row
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PowerStatBlock(
                            title = "Input voltage",
                            value = "${telemetry.inputVoltage} V",
                            icon = Icons.Default.Bolt,
                            color = BentoAmberText,
                            badgeBg = BentoAmberContainer,
                            modifier = Modifier.weight(1f)
                        )
                        PowerStatBlock(
                            title = "SRAM Memory",
                            value = "${telemetry.sramUsage}/128K",
                            icon = Icons.Default.Memory,
                            color = BentoPurpleAccent,
                            badgeBg = BentoPurpleBg,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PowerStatBlock(
                            title = "SSD Storage",
                            value = "${telemetry.storageUsedGb}G / ${telemetry.storageTotalGb}G",
                            icon = Icons.Default.Storage,
                            color = Color(0xFF00B8D4),
                            badgeBg = Color(0xFFE0F2F1),
                            modifier = Modifier.weight(1f)
                        )
                        PowerStatBlock(
                            title = "System RAM",
                            value = "${telemetry.ramUsedGb}G / ${telemetry.ramTotalGb}G",
                            icon = Icons.Default.Dns,
                            color = BentoTextDeep,
                            badgeBg = BentoBlueContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // SYSTEM ANIMATION & RESPONSIVENESS CALIBRATION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                    border = BorderStroke(1.dp, BentoBorder),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BentoPurpleBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = BentoPurpleAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Calibrate Link Performance",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BentoTextDark,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Set animation and transition latency values",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BentoTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                        // Sliders & Details
                        // 1. Window animation scale
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Window Animation Scale",
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextDark,
                                    fontSize = 13.sp
                                )
                                val scaleLabel = when (windowScale) {
                                    0.0f -> "0.0x (Instant / No Delay)"
                                    0.5f -> "0.5x (Fast)"
                                    1.0f -> "1.0x (Normal)"
                                    2.0f -> "2.0x (Slow)"
                                    5.0f -> "5.0x (Very Slow)"
                                    else -> "${windowScale}x"
                                }
                                Text(
                                    text = scaleLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (windowScale == 0.0f) BentoGreenText else BentoPurpleAccent
                                )
                            }
                            Slider(
                                value = windowScale,
                                onValueChange = { viewModel.setWindowAnimationScale(it) },
                                valueRange = 0f..5f,
                                steps = 9, // 0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0
                                colors = SliderDefaults.colors(
                                    thumbColor = BentoPurpleAccent,
                                    activeTrackColor = BentoPurpleAccent,
                                    inactiveTrackColor = BentoGreySurface
                                )
                            )
                        }

                        // 2. Transition animation scale
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Transition Animation Scale",
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextDark,
                                    fontSize = 13.sp
                                )
                                val scaleLabel = when (transitionScale) {
                                    0.0f -> "0.0x (Instant / No Delay)"
                                    0.5f -> "0.5x (Fast)"
                                    1.0f -> "1.0x (Normal)"
                                    2.0f -> "2.0x (Slow)"
                                    5.0f -> "5.0x (Very Slow)"
                                    else -> "${transitionScale}x"
                                }
                                Text(
                                    text = scaleLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (transitionScale == 0.0f) BentoGreenText else BentoPurpleAccent
                                )
                            }
                            Slider(
                                value = transitionScale,
                                onValueChange = { viewModel.setTransitionAnimationScale(it) },
                                valueRange = 0f..5f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = BentoPurpleAccent,
                                    activeTrackColor = BentoPurpleAccent,
                                    inactiveTrackColor = BentoGreySurface
                                )
                            )
                        }

                        // 3. Animator duration scale
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Animator Duration Scale",
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextDark,
                                    fontSize = 13.sp
                                )
                                val scaleLabel = when (animatorDurationScale) {
                                    0.0f -> "0.0x (Instant / No Delay)"
                                    0.5f -> "0.5x (Fast)"
                                    1.0f -> "1.0x (Normal)"
                                    2.0f -> "2.0x (Slow)"
                                    5.0f -> "5.0x (Very Slow)"
                                    else -> "${animatorDurationScale}x"
                                }
                                Text(
                                    text = scaleLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (animatorDurationScale == 0.0f) BentoGreenText else BentoPurpleAccent
                                )
                            }
                            Slider(
                                value = animatorDurationScale,
                                onValueChange = { viewModel.setAnimatorDurationScale(it) },
                                valueRange = 0f..5f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = BentoPurpleAccent,
                                    activeTrackColor = BentoPurpleAccent,
                                    inactiveTrackColor = BentoGreySurface
                                )
                            )
                        }

                        HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                        // Optimal Zero-lag calibrator button
                        Button(
                            onClick = { viewModel.applySmoothOptimalPerformancePreset() },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleAccent),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CALIBRATE OPTIMAL ZERO-DELAY PRESET",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Dynamic Speed graphics
                        InteractiveSpeedVisualizer(
                            windowScale = windowScale,
                            transitionScale = transitionScale,
                            animatorScale = animatorDurationScale,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // HARDWARE PROVISIONING & STORAGE/RAM ALLOCATION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                    border = BorderStroke(1.dp, BentoBorder),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFE0F7FA)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeveloperBoard,
                                        contentDescription = null,
                                        tint = Color(0xFF00B8D4),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Hardware Profile Provisioning",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BentoTextDark,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Calibrate active memory allocation sectors",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BentoTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                        // Dynamic Sector Visualizer Canvas
                        HardwarePartitionVisualizer(
                            storageTotal = telemetry.storageTotalGb,
                            storageUsed = telemetry.storageUsedGb,
                            ramTotal = telemetry.ramTotalGb,
                            ramUsed = telemetry.ramUsedGb,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                        // 1. Storage provisioning slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Provision Active Cold Storage",
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextDark,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${telemetry.storageTotalGb} GB (Max 128G)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00B8D4)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = telemetry.storageTotalGb.toFloat(),
                                onValueChange = { viewModel.setStorageTotalGb(it.toInt()) },
                                valueRange = 32f..128f,
                                steps = 3, // 32, 64, 96, 128
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00B8D4),
                                    activeTrackColor = Color(0xFF00B8D4),
                                    inactiveTrackColor = BentoGreySurface
                                )
                            )
                        }

                        // 2. RAM capacity configuration slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Allocate System RAM Buffer",
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextDark,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${telemetry.ramTotalGb} GB (Max 16G)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BentoPurpleAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = telemetry.ramTotalGb.toFloat(),
                                onValueChange = { viewModel.setRamTotalGb(it.toInt()) },
                                valueRange = 2f..16f,
                                steps = 6, // 2, 4, 6, 8, 10, 12, 14, 16
                                colors = SliderDefaults.colors(
                                    thumbColor = BentoPurpleAccent,
                                    activeTrackColor = BentoPurpleAccent,
                                    inactiveTrackColor = BentoGreySurface
                                )
                            )
                        }
                    }
                }
            }

            // eSIM CELLULAR PROVISIONING SECTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                    border = BorderStroke(1.dp, BentoBorder),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CellTower,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "eSIM Cellular Profile Manager",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BentoTextDark,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Over-The-Air cellular carrier subscription settings",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BentoTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            
                            // eSIM Toggle Switch
                            Switch(
                                checked = telemetry.eSimEnabled,
                                onCheckedChange = { viewModel.toggleESim(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF2E7D32),
                                    uncheckedThumbColor = BentoTextSecondary,
                                    uncheckedTrackColor = BentoGreySurface
                                )
                            )
                        }

                        HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                        if (telemetry.eSimEnabled) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Real-time connection sub-state panel
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BentoGreySurface)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "TRANSCEIVER STATUS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BentoTextSecondary
                                        )
                                        Text(
                                            text = telemetry.eSimStatus.uppercase(),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = when(telemetry.eSimStatus) {
                                                "Connected" -> Color(0xFF2E7D32)
                                                "Provisioning" -> BentoPurpleAccent
                                                "Connecting" -> Color(0xFF0288D1)
                                                else -> BentoTextDark
                                            },
                                            fontSize = 13.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "CARRIER NETWORK",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BentoTextSecondary
                                        )
                                        Text(
                                            text = if (telemetry.eSimIccid.isEmpty()) "UNPROVISIONED" else telemetry.eSimCarrier,
                                            fontWeight = FontWeight.Bold,
                                            color = BentoTextDark,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                if (telemetry.eSimStatus == "Connected" || telemetry.eSimStatus == "Connecting" || telemetry.eSimStatus == "Provisioning") {
                                    // Visual Live Signal Indicator Bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF13131A))
                                            .border(1.dp, BentoBorder.copy(alpha = 0.3f))
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Signal bars Canvas
                                        Box(
                                            modifier = Modifier.size(50.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val barCount = 5
                                                val space = 3.dp.toPx()
                                                val totalWidth = size.width
                                                val barWidth = (totalWidth - (barCount - 1) * space) / barCount
                                                
                                                // Map signal dBm to visible bars
                                                val activeBars = when {
                                                    telemetry.eSimSignalStrength > -75 -> 5
                                                    telemetry.eSimSignalStrength > -85 -> 4
                                                    telemetry.eSimSignalStrength > -95 -> 3
                                                    telemetry.eSimSignalStrength > -105 -> 2
                                                    telemetry.eSimSignalStrength > -115 -> 1
                                                    else -> 0
                                                }
                                                
                                                for (i in 0 until barCount) {
                                                    val fraction = (i + 1).toFloat() / barCount
                                                    val barHeight = size.height * fraction
                                                    val x = i * (barWidth + space)
                                                    val y = size.height - barHeight
                                                    
                                                    drawRect(
                                                        color = if (i < activeBars) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.15f),
                                                        topLeft = Offset(x, y),
                                                        size = Size(barWidth, barHeight)
                                                    )
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "CELL NETWORK SIGNAL",
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                val description = when {
                                                    telemetry.eSimSignalStrength > -80 -> "Excellent"
                                                    telemetry.eSimSignalStrength > -90 -> "Good"
                                                    telemetry.eSimSignalStrength > -100 -> "Fair"
                                                    telemetry.eSimSignalStrength > -110 -> "Weak"
                                                    else -> "Searching..."
                                                }
                                                Text(
                                                    text = description.uppercase(),
                                                    color = if (telemetry.eSimSignalStrength > -90) Color(0xFF4CAF50) else Color(0xFFFFB300),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                            Text(
                                                text = if (telemetry.eSimStatus == "Connected") "${telemetry.eSimSignalStrength} dBm" else "CALIBRATING...",
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Active Band: 5G NR n78 (Sub-6 GHz DL)",
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    // ICCID and Cellular Quota Metrics Card
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = BentoGreySurface),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "ICCID REGISTER",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = BentoTextSecondary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (telemetry.eSimIccid.isEmpty()) "ID: NONE LOADED" else telemetry.eSimIccid.chunked(4).joinToString(" "),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BentoTextDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Secure eUICC Profile Active",
                                                    fontSize = 8.sp,
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = BentoGreySurface),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "LIVE CELLULAR DATA",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = BentoTextSecondary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${telemetry.eSimDataUsedMb} MB",
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Black,
                                                    color = BentoTextDeep
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = "Quota: 5000.0 MB Limit",
                                                    fontSize = 8.sp,
                                                    color = BentoTextSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Provision/Replace eSIM profile button
                                    Button(
                                        onClick = { showEsimProvisionDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleAccent),
                                        modifier = Modifier.weight(1.5f),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (telemetry.eSimIccid.isEmpty()) "Provision eSIM Profile" else "Replace eSIM Profile",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Run diagnostic speed test button
                                    if (telemetry.eSimStatus == "Connected") {
                                        Button(
                                            onClick = { viewModel.runSimulatedSpeedTest() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Speed,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Speed Test",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Offline notice
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsCell,
                                    contentDescription = null,
                                    tint = BentoTextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Transceiver power down. Toggle eSIM switch to attach cellular link.",
                                    color = BentoTextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Digital Controllers & Custom GPIO Pins toggles
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                    border = BorderStroke(1.dp, BentoBorder),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "GPIO PIN REGISTER DIRECTIVES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BentoTextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )

                        HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                        // Relay 1 Control
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Relay Terminal A", fontWeight = FontWeight.ExtraBold, color = BentoTextDark)
                                Text("Routing: GPIO 12 (RELAY_A)", style = MaterialTheme.typography.bodySmall, color = BentoTextSecondary)
                            }
                            Switch(
                                checked = telemetry.relayState1,
                                onCheckedChange = { active -> viewModel.toggleRelay(1, active) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BentoPurpleAccent,
                                    uncheckedThumbColor = BentoTextSecondary,
                                    uncheckedTrackColor = BentoGreySurface
                                )
                            )
                        }

                        // Relay 2 Control
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Relay Terminal B", fontWeight = FontWeight.ExtraBold, color = BentoTextDark)
                                Text("Routing: GPIO 13 (RELAY_B)", style = MaterialTheme.typography.bodySmall, color = BentoTextSecondary)
                            }
                            Switch(
                                checked = telemetry.relayState2,
                                onCheckedChange = { active -> viewModel.toggleRelay(2, active) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BentoPurpleAccent,
                                    uncheckedThumbColor = BentoTextSecondary,
                                    uncheckedTrackColor = BentoGreySurface
                                )
                            )
                        }

                        HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                        // PWM LED control slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Pulse LED Dimming Intensity", fontWeight = FontWeight.ExtraBold, color = BentoTextDark)
                                    Text("Routing: GPIO 14 (PWM_LED_A)", style = MaterialTheme.typography.bodySmall, color = BentoTextSecondary)
                                }
                                Text(
                                    text = "${(telemetry.ledBrightness * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BentoPurpleAccent
                                    )
                                )
                            }
                            Slider(
                                value = telemetry.ledBrightness,
                                onValueChange = { duty -> viewModel.setLedBrightness(duty) },
                                colors = SliderDefaults.colors(
                                    thumbColor = BentoPurpleAccent,
                                    activeTrackColor = BentoPurpleAccent,
                                    inactiveTrackColor = BentoGreySurface
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            // Realtime Device Logs Shell Terminal Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONTROLLER SERIAL OUTPUT (TX/RX)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BentoTextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    TextButton(
                        onClick = { viewModel.clearDeviceLogs(activeDevice!!.id) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Clear Terminal", 
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            if (deviceLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BentoGreySurface),
                        border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Terminal empty. Awaiting hardware callbacks...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BentoTextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            } else {
                items(deviceLogs) { logObj ->
                    TerminalLogRow(logObj = logObj)
                }
            }
        }
    }

    if (showEsimProvisionDialog) {
        var carrierInput by remember { mutableStateOf("T-Mobile IoT") }
        var codeInput by remember { mutableStateOf("LPA:1\$rsp.global-esim.com\$IQ_7392_E2") }
        var isSimulatingScan by remember { mutableStateOf(false) }
        var scanProgress by remember { mutableStateOf(0f) }
        
        // Simulating QR code scanning overlay
        LaunchedEffect(isSimulatingScan) {
            if (isSimulatingScan) {
                scanProgress = 0f
                while (scanProgress < 1f) {
                    delay(150)
                    scanProgress += 0.1f
                }
                isSimulatingScan = false
                // Auto fill premium details
                carrierInput = listOf("T-Mobile IoT", "Orange IoT", "Truphone eSIM", "Verizon IoT").random()
                val hexToken = (1000..9999).random()
                codeInput = "LPA:1\$rsp.iota-carrier.com\$PROV_${hexToken}_SECURE"
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isSimulatingScan) showEsimProvisionDialog = false },
            containerColor = BentoWhiteSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = BentoPurpleAccent
                    )
                    Text(
                        text = "Remote SIM Provisioning",
                        color = BentoTextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Configure over-the-air subscription profile using GSMA SM-DP+ (Subscription Manager Data Preparation) network configurations.",
                        color = BentoTextSecondary,
                        fontSize = 11.sp
                    )
                    
                    if (isSimulatingScan) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BentoGreySurface, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "SIMULATING CAMERA QR DISCOVERY",
                                fontWeight = FontWeight.Bold,
                                color = BentoPurpleAccent,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { scanProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = BentoPurpleAccent,
                                trackColor = BentoPurpleBg
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Decoding RSP payload: ${(scanProgress * 100).toInt()}%",
                                fontSize = 10.sp,
                                color = BentoTextSecondary
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Carrier select dropdown or text
                            OutlinedTextField(
                                value = carrierInput,
                                onValueChange = { carrierInput = it },
                                label = { Text("Selected Carrier Network") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BentoPurpleAccent,
                                    focusedLabelColor = BentoPurpleAccent,
                                    unfocusedBorderColor = BentoBorder,
                                    unfocusedLabelColor = BentoTextSecondary
                                )
                            )

                            OutlinedTextField(
                                value = codeInput,
                                onValueChange = { codeInput = it },
                                label = { Text("SM-DP+ Activation Code") },
                                placeholder = { Text("e.g. LPA:1\$rsp.carrier.com\$code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BentoPurpleAccent,
                                    focusedLabelColor = BentoPurpleAccent,
                                    unfocusedBorderColor = BentoBorder,
                                    unfocusedLabelColor = BentoTextSecondary
                                )
                            )

                            Button(
                                onClick = { isSimulatingScan = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = BentoGreySurface),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = BentoTextDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scan Simulated Carrier QR Code",
                                    color = BentoTextDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.provisionESimProfile(codeInput, carrierInput)
                        showEsimProvisionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleAccent),
                    enabled = !isSimulatingScan && carrierInput.isNotBlank() && codeInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Provision Profile", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEsimProvisionDialog = false },
                    enabled = !isSimulatingScan
                ) {
                    Text("Cancel", color = BentoTextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun PowerStatBlock(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    badgeBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
        border = BorderStroke(1.dp, BentoBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BentoTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = BentoTextDark,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

@Composable
fun TerminalLogRow(logObj: com.example.data.model.LogEntity) {
    val levelColor = when (logObj.level) {
        "INFO" -> BentoGreenText
        "DEBUG" -> BentoPurpleAccent
        "WARN" -> BentoAmberText
        else -> MaterialTheme.colorScheme.error
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BentoGreySurface)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = ">>>",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = levelColor
                )
            )
            Text(
                text = logObj.message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = BentoTextDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InteractiveSpeedVisualizer(
    windowScale: Float,
    transitionScale: Float,
    animatorScale: Float,
    modifier: Modifier = Modifier
) {
    // A target position that moves left/right or rotates
    var isAActive by remember { mutableStateOf(true) }
    
    // Auto toggle target to show constant dynamic motion if user isn't clicking
    LaunchedEffect(windowScale, transitionScale, animatorScale) {
        while (true) {
            val pauseTime = if (animatorScale == 0f) 1800L else (1800L * animatorScale).toLong().coerceIn(100L, 10000L)
            delay(pauseTime)
            isAActive = !isAActive
        }
    }

    // Determine duration and delay based on scales
    // 0.0x scale means 0 ms / No Delay!
    val duration = (500 * animatorScale).toInt()
    val delay = (100 * transitionScale).toInt()

    val animatedProgress by animateFloatAsState(
        targetValue = if (isAActive) 1f else 0f,
        animationSpec = if (animatorScale == 0f && transitionScale == 0f) {
            snap()
        } else {
            tween(
                durationMillis = maxOf(1, duration),
                delayMillis = delay,
                easing = FastOutSlowInEasing
            )
        },
        label = "visualizer_progress"
    )

    // Window scale simulates general scale container bouncing, simulating window launch lag!
    val windowAnimationProgress by animateFloatAsState(
        targetValue = if (windowScale == 0f) 1f else if (isAActive) 1.05f else 0.95f,
        animationSpec = if (windowScale == 0f) {
            snap()
        } else {
            tween(
                durationMillis = maxOf(1, (600 * windowScale).toInt()),
                easing = EaseOutBack
            )
        },
        label = "window_bounce"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BentoGreySurface)
            .border(1.dp, BentoBorder.copy(alpha = 0.5f))
            .padding(14.dp)
            .clickable { isAActive = !isAActive },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GRAPHICAL LATENCY RESPONSE ENGINE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BentoTextSecondary,
                    letterSpacing = 1.sp
                )
            )
            // Pulse Badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (animatorScale == 0f) BentoGreenContainer else BentoPinkContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (animatorScale == 0f) "ZERO LAG LINK" else "SIMULATED DELAY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (animatorScale == 0f) BentoGreenText else MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Let's render the canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E24))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height
                val center = Offset(width / 2, height / 2 - 4.dp.toPx())
                
                // Draw a radar sweeping arc representing dynamic scanning and transmission speed
                val radius = (height / 2 - 14.dp.toPx() ) * windowAnimationProgress
                
                // Static track
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Render dynamic rotating vectors
                val targetAngle = animatedProgress * 360f
                val angleRad = Math.toRadians(targetAngle.toDouble())
                val markerX = center.x + radius * cos(angleRad).toFloat()
                val markerY = center.y + radius * sin(angleRad).toFloat()

                // Glow radial guide lines
                drawLine(
                    color = BentoPurpleAccent.copy(alpha = 0.2f),
                    start = center,
                    end = Offset(markerX, markerY),
                    strokeWidth = 2.dp.toPx()
                )

                // Particle dot representing transmitting packets
                drawCircle(
                    color = if (animatorScale == 0f) Color(0xFF4CAF50) else BentoPurpleAccent,
                    radius = 8.dp.toPx(),
                    center = Offset(markerX, markerY)
                )

                // Extra pulsing halos to represent "animation duration scale" visually
                drawCircle(
                    color = if (animatorScale == 0f) Color(0xFF4CAF50).copy(alpha = 0.15f) else BentoPurpleAccent.copy(alpha = 0.15f),
                    radius = 8.dp.toPx() + (12.dp.toPx() * (1f - animatedProgress)),
                    center = Offset(markerX, markerY)
                )

                // Draw speed bars indicating simulated response time in milliseconds
                val responseTimeValue = if (animatorScale == 0f && transitionScale == 0f) 5f else (150 * animatorScale + 50 * transitionScale + 12).toFloat()
                
                // Label showing simulated latency
                val barWidth = 140.dp.toPx()
                val barHeight = 8.dp.toPx()
                val barLeft = center.x - barWidth / 2
                val barTop = height - 12.dp.toPx()
                
                // BG bar
                drawRect(
                    color = Color.White.copy(alpha = 0.1f),
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidth, barHeight)
                )
                
                // Active bar colored by lag intensity
                val lagRatio = (responseTimeValue / 850f).coerceIn(0f, 1f)
                val barColor = if (lagRatio < 0.15f) Color(0xFF4CAF50) else if (lagRatio < 0.5f) Color(0xFFFFB300) else Color(0xFFF44336)
                drawRect(
                    color = barColor,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidth * (if (animatorScale == 0f) 0.05f else lagRatio), barHeight)
                )
            }

            // Superpose overlay text for Latency measurements cleanly with Text Composable
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "BLE-HANDSHAKE TX-BUFFER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (animatorScale == 0f && transitionScale == 0f) "PING: 5ms [RTB]" else "PING: ${(150 * animatorScale + 50 * transitionScale + 12).toInt()}ms [LAGGED]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (animatorScale == 0f && transitionScale == 0f) Color(0xFF76E8A3) else Color(0xFFFF7E7E),
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isAActive) "CHANNEL DIRECTIVES: HIGH_BANDWIDTH_BURST" else "CHANNEL DIRECTIVES: BUFFER_TX_AWAIT_ACK",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "⚡ TAP ANYWHERE ON UNIT TO DISPATCH SIG-PULSE TARGETS MANUALLY.",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                color = BentoTextSecondary,
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HardwarePartitionVisualizer(
    storageTotal: Int,
    storageUsed: Float,
    ramTotal: Int,
    ramUsed: Float,
    modifier: Modifier = Modifier
) {
    // We animate values over changes
    val animatedStorageUsed by animateFloatAsState(targetValue = storageUsed, label = "storage_used")
    val animatedRamUsed by animateFloatAsState(targetValue = ramUsed, label = "ram_used")
    
    // Pulse animation for simulated reading/writing or cache sync activity
    val infiniteTransition = rememberInfiniteTransition(label = "sector_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF13131A))
            .border(1.dp, BentoBorder.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REAL-TIME SECTOR MAP & PARTITIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BentoPurpleAccent.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "STATUS: ACTIVE SYNC",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoPurpleAccent
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerRadius = size.height / 2 - 12.dp.toPx()
                val innerRadius = outerRadius - 16.dp.toPx()
                
                // Draw sector segments for RAM
                val ramRatio = (animatedRamUsed / ramTotal.toFloat()).coerceIn(0f, 1f)
                val activeRamSweep = ramRatio * 180f
                
                // RAM Half-Circle (top half: 180 to 360 degrees)
                // Draw background track for RAM
                drawArc(
                    color = Color.White.copy(alpha = 0.05f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 10.dp.toPx())
                )
                // Draw active RAM
                drawArc(
                    color = BentoPurpleAccent,
                    startAngle = 180f,
                    sweepAngle = activeRamSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 10.dp.toPx())
                )

                // Draw sector segments for Storage (bottom half: 0 to 180 degrees)
                val storageRatio = (animatedStorageUsed / storageTotal.toFloat()).coerceIn(0f, 1f)
                val activeStorageSweep = storageRatio * 180f

                // Storage background track
                drawArc(
                    color = Color.White.copy(alpha = 0.05f),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 10.dp.toPx())
                )
                // Draw active Storage
                drawArc(
                    color = Color(0xFF00E5FF),
                    startAngle = 0f,
                    sweepAngle = activeStorageSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 10.dp.toPx())
                )

                // Draw central rotating sync indicator & core stats
                drawCircle(
                    color = Color.White.copy(alpha = 0.02f),
                    radius = innerRadius - 4.dp.toPx(),
                    center = center
                )

                // Dynamic radar scanning vectors of partitions
                val angleRad = Math.toRadians(rotateAngle.toDouble())
                val rayX = center.x + innerRadius * cos(angleRad).toFloat()
                val rayY = center.y + innerRadius * sin(angleRad).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = pulseAlpha * 0.25f),
                    start = center,
                    end = Offset(rayX, rayY),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Text specs center aligned
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "MEM ALLOC",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", animatedRamUsed)}G / ${ramTotal}G",
                    color = BentoPurpleAccent,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "STORAGE MAP",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", animatedStorageUsed)}G / ${storageTotal}G",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Legends
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BentoPurpleAccent))
                Text("RAM (Max 16G)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00E5FF)))
                Text("Storage (Max 128G)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
