package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceEntity
import com.example.data.model.LogEntity
import com.example.ui.DeviceViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DeviceViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToControl: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedDevices by viewModel.savedDevices.collectAsState()
    val globalLogs by viewModel.globalLogs.collectAsState()
    val activeDevice by viewModel.activeDevice.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Bento Page Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Control Center",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoTextDark,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = "Active Hub: ${activeDevice?.name ?: "DC-420-X"}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = BentoTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                // Profile Avatar visual matching the user design specification
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BentoPurpleBg)
                        .clickable { showAddDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👤",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bento Grid: Active Hero Cell
        item {
            AnimatedContent(
                targetState = activeDevice,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "heroBentoCell"
            ) { device ->
                if (device != null) {
                    // Connected Active Hero bento cell (2 columns width)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToControl() },
                        colors = CardDefaults.cardColors(containerColor = BentoPurpleBg),
                        border = BorderStroke(1.dp, BentoPurpleAccent.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Status Pill Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(BentoPurpleAccent)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "CONNECTED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                                
                                // BLE / Wifi Icon indicators in visual spec
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (device.type == "Bluetooth") Icons.Default.Bluetooth else Icons.Default.Wifi,
                                            contentDescription = null,
                                            tint = BentoTextDeep,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sensors,
                                            contentDescription = null,
                                            tint = BentoTextDeep,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = device.name,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BentoTextDeep,
                                            fontSize = 24.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Dual-band • ${device.address}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BentoTextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                                
                                Button(
                                    onClick = { viewModel.disconnectActive() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BentoBorder)
                                ) {
                                    Text(
                                        text = "Disconnect",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Ready to connect / Welcome visual card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToScan() },
                        colors = CardDefaults.cardColors(containerColor = BentoPurpleBg),
                        border = BorderStroke(1.dp, BentoPurpleAccent.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = BentoTextDeep)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Connect Hardware",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BentoTextDeep
                                        )
                                    )
                                    Text(
                                        text = "Scan and bind nearby BLE & WiFi microcontrollers.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = BentoTextSecondary)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Scan Link",
                                    tint = BentoTextDeep,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bento Grid: Double Sub-Cells Row (Battery & Latency indices)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Battery Index Bento cell (Col 1)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                    border = BorderStroke(1.dp, BentoBorder),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BentoPinkContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔋", fontSize = 16.sp)
                        }
                        Column {
                            Text(
                                text = "BATTERY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextSecondary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = if (activeDevice != null) "84%" else "--",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BentoTextDark
                                )
                            )
                            Text(
                                text = if (activeDevice != null) "Healthy" else "Unavailable",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (activeDevice != null) BentoGreenText else BentoTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Latency Index Bento cell (Col 2)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
                    border = BorderStroke(1.dp, BentoBorder),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BentoBlueContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📊", fontSize = 16.sp)
                        }
                        Column {
                            Text(
                                text = "LATENCY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextSecondary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = if (activeDevice != null) "12 ms" else "--",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BentoTextDark
                                )
                            )
                            Text(
                                text = if (activeDevice != null) "-64 dBm" else "Offline",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BentoTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section header for Catalog Nodes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HARDWARE CATALOG",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = BentoTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "${savedDevices.size} Controllers",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BentoTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // List of Saved catalog controllers
        if (savedDevices.isEmpty()) {
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
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DeveloperBoard,
                                contentDescription = null,
                                tint = BentoTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Catalog is Empty",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = BentoTextDark,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Scan to register active boards nearby.",
                                style = MaterialTheme.typography.bodySmall.copy(color = BentoTextSecondary),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            items(savedDevices, key = { it.id }) { device ->
                val isSelected = activeDevice?.id == device.id
                DeviceCatalogCard(
                    device = device,
                    isConnected = isSelected && connectionState == "Connected",
                    onClick = {
                        if (isSelected && connectionState == "Connected") {
                            onNavigateToControl()
                        } else {
                            viewModel.connectToDevice(device)
                        }
                    },
                    onDelete = { viewModel.deleteSavedDevice(device) }
                )
            }
        }

        // System Logs Header
        item {
            Text(
                text = "GLOBAL AUDIT LOGS",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = BentoTextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }

        // Global System logs list
        if (globalLogs.isEmpty()) {
            item {
                Text(
                    text = "No audit transactions recorded yet.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BentoTextSecondary),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            items(globalLogs.take(5)) { logObj ->
                LogItemRow(logObj = logObj)
            }
        }
    }

    // Modal Manual Input Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("WiFi") }
        var category by remember { mutableStateOf("Smart Home") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = BentoWhiteSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Register Manual Hardware Board", 
                    color = BentoTextDark, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Device Name") },
                        colors = outlinedTextFieldColorsStyle(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("e.g. 192.168.1.100 or AA:BB:CC...") },
                        label = { Text("Address / IP") },
                        colors = outlinedTextFieldColorsStyle(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text(
                            text = "Transport Medium", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = BentoTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { type = "WiFi" }) {
                                RadioButton(
                                    selected = type == "WiFi", 
                                    onClick = { type = "WiFi" }, 
                                    colors = RadioButtonDefaults.colors(selectedColor = BentoPurpleAccent)
                                )
                                Text("WiFi AP", color = BentoTextDark, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { type = "Bluetooth" }) {
                                RadioButton(
                                    selected = type == "Bluetooth", 
                                    onClick = { type = "Bluetooth" }, 
                                    colors = RadioButtonDefaults.colors(selectedColor = BentoPurpleAccent)
                                )
                                Text("Bluetooth BLE", color = BentoTextDark, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Category", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = BentoTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val categories = listOf("Smart Home", "Industrial", "Robotics", "iOS Companion")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { cat ->
                                val active = category == cat
                                FilterChip(
                                    selected = active,
                                    onClick = { category = cat },
                                    label = { Text(cat, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BentoPurpleBg,
                                        selectedLabelColor = BentoTextDeep,
                                        containerColor = BentoGreySurface,
                                        labelColor = BentoTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank() && address.isNotBlank()) {
                            viewModel.addCustomDevice(name, type, address, category)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Register Node", color = BentoPurpleAccent, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = BentoTextSecondary)
                }
            }
        )
    }
}

@Composable
fun DeviceCatalogCard(
    device: DeviceEntity,
    isConnected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
        border = BorderStroke(1.dp, if (isConnected) BentoPurpleAccent.copy(alpha = 0.5f) else BentoBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Type Icon with pastel background according to type
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) BentoPurpleBg
                        else if (device.type == "Bluetooth") BentoBlueContainer
                        else BentoPinkContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (device.type == "Bluetooth") Icons.Default.Bluetooth else Icons.Default.Wifi,
                    contentDescription = null,
                    tint = if (isConnected) BentoPurpleAccent else BentoTextDark,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoTextDark
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${device.category} • ${device.address}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BentoTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Connected or delete icon
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BentoGreenText)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.bodySmall,
                    color = BentoGreenText,
                    fontWeight = FontWeight.Black
                )
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete device profile",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LogItemRow(logObj: LogEntity) {
    val timeStr = remember(logObj.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(logObj.timestamp))
    }
    val levelColor = when (logObj.level) {
        "INFO" -> BentoGreenText
        "DEBUG" -> BentoTextDeep
        "WARN" -> BentoAmberText
        else -> MaterialTheme.colorScheme.error
    }
    val levelBg = when (logObj.level) {
        "INFO" -> BentoGreenContainer
        "DEBUG" -> BentoBlueContainer
        "WARN" -> BentoAmberContainer
        else -> Color(0xFFFFEBEE)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = timeStr,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = BentoTextSecondary,
                fontWeight = FontWeight.Bold
            )
        )
        // Neat bento pill badge for logs
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(levelBg)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = logObj.level,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = levelColor,
                    fontSize = 10.sp
                )
            )
        }
        Text(
            text = logObj.message,
            style = MaterialTheme.typography.bodySmall.copy(
                color = BentoTextDark,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun outlinedTextFieldColorsStyle() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BentoPurpleAccent,
    unfocusedBorderColor = BentoBorder,
    focusedLabelColor = BentoPurpleAccent,
    unfocusedLabelColor = BentoTextSecondary,
    focusedTextColor = BentoTextDark,
    unfocusedTextColor = BentoTextDark,
    focusedContainerColor = BentoWhiteSurface,
    unfocusedContainerColor = BentoWhiteSurface
)
