package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.PairingState
import com.example.ui.theme.*

@Composable
fun SecurePairingDialog(
    pairingState: PairingState,
    onConfirmPin: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pairingState is PairingState.Idle) return

    Dialog(
        onDismissRequest = { if (pairingState is PairingState.Success || pairingState is PairingState.Error) onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = BentoWhiteSurface),
            border = BorderStroke(1.dp, BentoBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security Handshake",
                            tint = BentoPurpleAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "SECURE PAIRING HANDSHAKE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = BentoTextDark,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel pairing",
                            tint = BentoTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Render based on state
                when (pairingState) {
                    is PairingState.Discovering -> {
                        DiscoveringView()
                    }
                    is PairingState.PinVerification -> {
                        PinVerificationView(
                            state = pairingState,
                            onConfirmPin = onConfirmPin,
                            onCancel = onCancel
                        )
                    }
                    is PairingState.KeyExchangeActive -> {
                        KeyExchangeView(state = pairingState)
                    }
                    is PairingState.Success -> {
                        PairingSuccessView(state = pairingState, onDismiss = onCancel)
                    }
                    is PairingState.Error -> {
                        PairingErrorView(errorMsg = pairingState.errorMsg, onDismiss = onCancel)
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun DiscoveringView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "discloader")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing)
            ),
            label = "discrot"
        )

        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = BentoPurpleAccent,
                strokeWidth = 6.dp,
                trackColor = BentoGreySurface
            )
            Icon(
                imageVector = Icons.Default.BluetoothSearching,
                contentDescription = null,
                tint = BentoPurpleAccent,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "INITIATING SECURE CHANNEL...",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = BentoTextDark,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Retreiving Bluetooth GATT pairing characteristics and negotiating cryptographic protocol compatibility.",
            style = MaterialTheme.typography.bodyMedium.copy(color = BentoTextSecondary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun PinVerificationView(
    state: PairingState.PinVerification,
    onConfirmPin: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedSpecTab by remember { mutableStateOf("Android & iOS Spec") } // Tab selectors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "USER PASSCODE CONFIRMATION",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BentoTextSecondary,
                letterSpacing = 1.5.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Center stylized PIN code display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BentoGreySurface),
            border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.pinCode.replaceRange(3, 3, " "), // format "123 456"
                    fontSize = 42.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoTextDeep,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = BentoGreenText,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "SHA-256 Key-Derived Secure Factor",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BentoGreenText,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fingerprint verification
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "PEER PUBLIC KEY INTEGRITY HASHES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BentoTextSecondary,
                    letterSpacing = 1.sp
                )
            )

            KeyIdentityBlock(
                title = "Local Android Host (Public Key)",
                hexVal = state.localPublicHex,
                badgeColor = BentoPurpleAccent
            )

            KeyIdentityBlock(
                title = "Remote BLE Peer Link (Public Key)",
                hexVal = state.remotePublicHex,
                badgeColor = BentoAmberText
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Cross-platform developer tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BentoGreySurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Android & iOS Spec", "Swift Code (iOS)", "Kotlin Code (Android)").forEach { tab ->
                val isActive = selectedSpecTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) BentoPurpleAccent else Color.Transparent)
                        .clickable { selectedSpecTab = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.White else BentoTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Code and instructions area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BentoGreySurface)
                .border(1.dp, BentoBorder.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                when (selectedSpecTab) {
                    "Android & iOS Spec" -> {
                        Text(
                            text = "💡 SECURE BROADCAST PAIRING SPECS\n\n" +
                                    "This handshake uses Elliptic Curve Diffie-Hellman Key Exchange (ECDH) over Curve25519 standard coordinates.\n" +
                                    "• Both platforms generate an ephemeral EC key pair locally.\n" +
                                    "• Passkey PIN is computed dynamically via a deterministic SHA-256 hash of both public keys, making MITM (Man-in-the-Middle) attacks mathematically impossible.\n" +
                                    "• Confirming the PIN launches Diffie-Hellman scalar multiplications to establish symmetric AES session keys.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = BentoTextSecondary,
                                lineHeight = 16.sp
                            )
                        )
                    }
                    "Swift Code (iOS)" -> {
                        Text(
                            text = "// Apple iOS Central (CoreBluetooth + CryptoKit)\n" +
                                    "import CoreBluetooth\n" +
                                    "import CryptoKit\n\n" +
                                    "let privateKey = Curve25519.Signing.PrivateKey()\n" +
                                    "let localPublicKey = privateKey.publicKey\n\n" +
                                    "// Send public key representation via write characteristic\n" +
                                    "peripheral.writeValue(localPublicKey.rawRepresentation,\n" +
                                    "  for: keyExchangeChar,\n" +
                                    "  type: .withResponse)",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = BentoTextSecondary,
                                lineHeight = 14.sp
                            )
                        )
                    }
                    "Kotlin Code (Android)" -> {
                        Text(
                            text = "// Android BLE Peripheral (BluetoothGatt + JCE)\n" +
                                    "import android.bluetooth.BluetoothGatt\n" +
                                    "import java.security.KeyPairGenerator\n\n" +
                                    "val kpg = KeyPairGenerator.getInstance(\"EC\")\n" +
                                    "kpg.initialize(256)\n" +
                                    "val myKeys = kpg.generateKeyPair()\n\n" +
                                    "// Write Kotlin characteristic byte payload\n" +
                                    "gattChar.value = myKeys.public.encoded\n" +
                                    "gatt.writeCharacteristic(gattChar)",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = BentoTextSecondary,
                                lineHeight = 14.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Text(
                    text = "Reject & Abort",
                    color = BentoTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onConfirmPin,
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Confirm Match",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun KeyExchangeView(state: PairingState.KeyExchangeActive) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "exchange_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.EaseInOutSine),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "scalepulse"
        )

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(BentoPurpleBg)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SyncAlt,
                contentDescription = null,
                tint = BentoPurpleAccent,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "DIFFIE-HELLMAN KEY EXCHANGE",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = BentoTextDark,
                letterSpacing = 0.8.sp
            )
        )
        Text(
            text = "Performing scalar key multiplication matrices...",
            style = MaterialTheme.typography.bodySmall.copy(
                color = BentoTextSecondary,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Cryptographic Processing Logs Console
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CryptoTermRow("LOCAL_PUB_KEY", state.localPublicKey, BentoPurpleBg)
                CryptoTermRow("REMOTE_PUB_KEY", state.remotePublicKey, BentoAmberContainer)
                CryptoTermRow("SHARED_SECRET", state.sharedSecret, BentoGreenContainer)
                CryptoTermRow("AES_SESSION", state.derivedAesKey, BentoBlueContainer)
                CryptoTermRow("HASH_SIGNATURE", state.fingerPrint, BentoPinkContainer)
            }
        }
    }
}

@Composable
fun CryptoTermRow(label: String, value: String, highlightBg: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = Color(0xFF76E8A3),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PairingSuccessView(
    state: PairingState.Success,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(BentoGreenContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = BentoGreenText,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "TRUST BOND ESTABLISHED",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = BentoGreenText,
                letterSpacing = 1.sp
            )
        )
        Text(
            text = "Symmetric Session keys exchanged successfully.",
            style = MaterialTheme.typography.bodyMedium.copy(color = BentoTextSecondary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BentoGreenContainer.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, BentoGreenText.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Linked Node:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoGreenText)
                    Text(state.deviceName, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = BentoTextDark)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ID Signature:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoGreenText)
                    Text(state.deviceId, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = BentoTextDark)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Active Session Key:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoGreenText)
                    Text(
                        text = "0x" + state.sessionKey.take(16) + "...",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BentoTextDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Close & Monitor Telemetry",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PairingErrorView(
    errorMsg: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GppBad,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SECURE HANDSHAKE REJECTED",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMsg,
            style = MaterialTheme.typography.bodyMedium.copy(color = BentoTextSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Acknowledge & Dismiss",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun KeyIdentityBlock(
    title: String,
    hexVal: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BentoGreySurface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BentoTextSecondary)
            Text(hexVal, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = BentoTextDark, overflow = TextOverflow.Ellipsis, maxLines = 1)
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )
    }
}
