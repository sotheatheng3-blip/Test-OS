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
                        color = BentoTextDeep,
                        badgeBg = BentoBlueContainer,
                        modifier = Modifier.weight(1f)
                    )
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
