package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DeviceEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SecureConnectionDialog(
    device: DeviceEntity?,
    onAuthenticateSuccess: (String) -> Unit, // passes the correct PIN or key
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (device == null) return

    val scope = rememberCoroutineScope()
    var pinValue by remember { mutableStateOf("") }
    var isErrorState by remember { mutableStateOf(false) }
    var isSuccessState by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Support customized PIN per device session!
    var customPinEnabled by remember { mutableStateOf(false) }
    var storedCustomPin by remember { mutableStateOf("1234") } // Default is 1234

    // Simple shaking simulation offset
    var shakeTrigger by remember { mutableStateOf(0) }
    val shakeOffset by animateDpAsState(
        targetValue = if (isErrorState && shakeTrigger > 0) {
            when (shakeTrigger % 3) {
                1 -> (-10).dp
                2 -> 10.dp
                else -> 0.dp
            }
        } else 0.dp,
        animationSpec = tween(100),
        label = "shake"
    )

    // Automatically shake a few times when error starts
    LaunchedEffect(isErrorState) {
        if (isErrorState) {
            errorMessage = "Unauthorized Controller Passkey. Access Denied."
            for (i in 1..6) {
                shakeTrigger = i
                delay(80)
            }
            shakeTrigger = 0
            delay(2000)
            isErrorState = false
            pinValue = ""
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .offset(x = shakeOffset),
            colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
            border = BorderStroke(1.dp, if (isSuccessState) BentoGreenText else if (isErrorState) BentoAmberText else BentoBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Block with Tactical Secure Badge
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
                                .clip(CircleShape)
                                .background(if (isSuccessState) BentoGreenContainer else if (isErrorState) BentoAmberContainer else BentoPurpleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSuccessState) Icons.Default.VerifiedUser else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isSuccessState) BentoGreenText else if (isErrorState) BentoAmberText else BentoPurpleAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "DECRYPT HUB LINK",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = BentoTextDark,
                                    letterSpacing = 1.2.sp
                                )
                            )
                            Text(
                                text = "Secure ${device.type} Encrypted Keypad",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BentoTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel connection",
                            tint = BentoTextSecondary
                        )
                    }
                }

                // Target Node Brief Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BentoGreySurface),
                    border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BentoPurpleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (device.type == "Bluetooth") Icons.Default.Bluetooth else Icons.Default.Wifi,
                                contentDescription = null,
                                tint = BentoPurpleAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextDark
                                )
                            )
                            Text(
                                text = "Address: ${device.address}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = BentoTextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }

                // Custom PIN management toggler
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BentoGreySurface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize connection passcode PIN",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoTextDark
                        )
                    )
                    Switch(
                        checked = customPinEnabled,
                        onCheckedChange = { customPinEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BentoPurpleAccent,
                            uncheckedThumbColor = BentoTextSecondary,
                            uncheckedTrackColor = BentoBorder.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.scale(0.8f) // helper extensions
                    )
                }

                if (customPinEnabled) {
                    // Customizable PIN row
                    OutlinedTextField(
                        value = storedCustomPin,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() } && it.length <= 8) {
                                storedCustomPin = it
                            }
                        },
                        label = { Text("Set Secure Target Association PIN") },
                        placeholder = { Text("Default 1234") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = BentoPurpleAccent) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoPurpleAccent,
                            unfocusedBorderColor = BentoBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // PIN Entry Dots Board
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    val maxLen = if (customPinEnabled) storedCustomPin.length.coerceAtLeast(4) else 4
                    for (i in 0 until maxLen) {
                        val isFilled = pinValue.length > i
                        val dotColor = when {
                            isSuccessState -> BentoGreenText
                            isErrorState -> BentoAmberText
                            isFilled -> BentoPurpleAccent
                            else -> BentoBorder.copy(alpha = 0.5f)
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(
                                    1.dp,
                                    if (isFilled) Color.Transparent else BentoBorder,
                                    CircleShape
                                )
                        )
                    }
                }

                // Error / Success Message
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isErrorState || isSuccessState || pinValue.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = when {
                                isSuccessState -> "Authorization Confirmed! Binding telemetry link..."
                                isErrorState -> errorMessage
                                else -> "Enter authorized key to authenticate link."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSuccessState -> BentoGreenText
                                    isErrorState -> BentoAmberText
                                    else -> BentoTextSecondary
                                }
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Tactile Virtual NumPad Layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BentoGreySurface, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Matrix of Numeric Buttons (1-3, 4-6, 7-9, Backspace-0-Check)
                        val numRows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9")
                        )

                        numRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { num ->
                                    NumPadButton(
                                        text = num,
                                        onClick = {
                                            val limit = if (customPinEnabled) storedCustomPin.length else 4
                                            if (pinValue.length < limit && !isSuccessState && !isErrorState) {
                                                pinValue += num
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Last row: Backspace (Clear/Delete), "0", Verification Check
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Backspace Button
                            IconButton(
                                onClick = {
                                    if (pinValue.isNotEmpty() && !isSuccessState && !isErrorState) {
                                        pinValue = pinValue.dropLast(1)
                                    }
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(BentoWhiteSurface)
                                    .border(1.dp, BentoBorder.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = BentoTextSecondary
                                )
                            }

                            // Number "0"
                            NumPadButton(
                                text = "0",
                                onClick = {
                                    val limit = if (customPinEnabled) storedCustomPin.length else 4
                                    if (pinValue.length < limit && !isSuccessState && !isErrorState) {
                                        pinValue += "0"
                                    }
                                }
                            )

                            // Confirm Enter check button
                            IconButton(
                                onClick = {
                                    if (isSuccessState || isErrorState) return@IconButton
                                    val targetPin = if (customPinEnabled) storedCustomPin else "1234"
                                    if (pinValue == targetPin) {
                                        isSuccessState = true
                                        scope.launch {
                                            delay(1200)
                                            onAuthenticateSuccess(pinValue)
                                        }
                                    } else {
                                        isErrorState = true
                                    }
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(BentoPurpleAccent),
                                enabled = pinValue.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Authenticate Verification Key",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Default passkey tip
                if (!customPinEnabled) {
                    Text(
                        text = "🔐 Hint: Default manufacturer connection PIN is 1234.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = BentoTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun NumPadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(BentoWhiteSurface)
            .border(1.dp, BentoBorder.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = BentoTextDark,
                fontFamily = FontFamily.SansSerif
            )
        )
    }
}

// Extension to scale components as needed
private fun Modifier.scale(scale: Float): Modifier = this.then(Modifier.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout((placeable.width * scale).toInt(), (placeable.height * scale).toInt()) {
        placeable.placeRelative(0, 0)
    }
})
