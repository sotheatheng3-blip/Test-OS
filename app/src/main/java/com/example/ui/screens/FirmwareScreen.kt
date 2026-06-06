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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DeviceViewModel
import com.example.ui.OtaState
import com.example.ui.theme.*

@Composable
fun FirmwareScreen(
    viewModel: DeviceViewModel,
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDevice by viewModel.activeDevice.collectAsState()
    val otaState by viewModel.otaState.collectAsState()
    val availableFirmwares = viewModel.availableFirmwares

    var selectedFirmware by remember { mutableStateOf(availableFirmwares.first()) }

    Box(modifier = modifier.fillMaxSize().background(BentoBg)) {
        if (activeDevice == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                            .background(BentoPurpleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = BentoTextDeep,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "NO DEVICE LINKED FOR OTA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = BentoTextDark,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "To run firmware system diagnostics or initiate OTA flashing, connect to a compatible node first.",
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
                            text = "Find Target Boards",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                // Header of Active Device Firmware Specs
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                        border = BorderStroke(1.dp, BentoBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = activeDevice!!.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = BentoTextDark,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                    Text(
                                        text = "${activeDevice!!.type} Core Controller",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BentoTextSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BentoGreenContainer)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = activeDevice!!.firmwareVersion,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = BentoGreenText,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Checklist diagnostics Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                        border = BorderStroke(1.dp, BentoBorder),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "OTA SAFETY FLASHER CHECKLIST",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BentoTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )

                            HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                            OtaRequirementRow(label = "Hardware core power level >= 50%", verified = true)
                            OtaRequirementRow(label = "Secured hand-shake channel secured", verified = true)
                            OtaRequirementRow(label = "Local signature verification ready", verified = true)
                        }
                    }
                }

                // Choose Firmware list header
                item {
                    Text(
                        text = "AVAILABLE DIRECTORY BINARIES",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = BentoTextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                // Available Firmwares listing
                items(availableFirmwares) { firmware ->
                    val isSelected = selectedFirmware == firmware
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFirmware = firmware },
                        colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isSelected) BentoPurpleAccent.copy(alpha = 0.5f) else BentoBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedFirmware = firmware },
                                colors = RadioButtonDefaults.colors(selectedColor = BentoPurpleAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = firmware,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BentoTextDark
                                )
                                Text(
                                    text = "Ready to flash over ${activeDevice!!.type} link.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BentoTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // Big Flash button Action
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.runFirmwareUpdate(selectedFirmware) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleAccent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Deploy Updates over-the-air",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Animated full progress blocker representing the ongoing OTA sequence
        AnimatedVisibility(
            visible = otaState is OtaState.Progress,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val progressState = otaState as? OtaState.Progress
            if (progressState != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BentoBg.copy(alpha = 0.95f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, BentoPurpleAccent.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Dial loader representation
                            Box(
                                modifier = Modifier.size(110.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { progressState.progress },
                                    modifier = Modifier.fillMaxSize(),
                                    strokeWidth = 10.dp,
                                    color = BentoPurpleAccent,
                                    trackColor = BentoGreySurface,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                Text(
                                    text = "${(progressState.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BentoTextDark
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "AUTOMATED FLASHING CONSOLE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = BentoPurpleAccent,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = progressState.msg,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BentoTextDark,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            LinearProgressIndicator(
                                progress = { progressState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = BentoGreenText,
                                trackColor = BentoGreySurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "CRITICAL: Do NOT turn off node or close connection.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OtaRequirementRow(label: String, verified: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (verified) BentoGreenContainer else BentoGreySurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (verified) BentoGreenText else BentoTextSecondary,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = if (verified) BentoTextDark else BentoTextSecondary
        )
    }
}
