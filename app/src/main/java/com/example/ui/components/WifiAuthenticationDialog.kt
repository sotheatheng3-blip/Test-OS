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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.ScannedDevice
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

sealed class WifiAuthState {
    object Idle : WifiAuthState()
    object Setup : WifiAuthState()
    data class Handshake(
        val ssid: String,
        val protocol: String,
        val phase: Int, // 1 to 4 representing M1, M2, M3, M4
        val statusMsg: String,
        val anonce: String = "",
        val snonce: String = "",
        val pmk: String = "",
        val ptk: String = ""
    ) : WifiAuthState()
    data class Success(
        val ssid: String,
        val protocol: String,
        val pmk: String,
        val ptk: String,
        val cipher: String
    ) : WifiAuthState()
    data class Error(val errorMsg: String) : WifiAuthState()
}

@Composable
fun WifiAuthenticationDialog(
    scannedDevice: ScannedDevice?,
    onAuthenticateComplete: (String, String, String) -> Unit, // (ssid, password, securityType)
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (scannedDevice == null) return

    var authState by remember { mutableStateOf<WifiAuthState>(WifiAuthState.Setup) }
    val scope = rememberCoroutineScope()

    // Setup input fields
    var ssid by remember { mutableStateOf(scannedDevice.name) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedProtocol by remember { mutableStateOf("WPA3-SAE (Personal)") } // WPA2-Personal, WPA3-SAE, WPA2/WPA3 Enterprise
    
    // Enterprise fields
    var enterpriseIdentity by remember { mutableStateOf("supplicant@enterprise.wifi") }
    var scaleAnimationsEnabled by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = { if (authState is WifiAuthState.Success || authState is WifiAuthState.Error) onCancel() },
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
                // Dialog visual header
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
                            imageVector = Icons.Default.WifiLock,
                            contentDescription = "WiFi Secure Authentication",
                            tint = BentoPurpleAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "SECURE WI-FI DECRYPTOR",
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
                            contentDescription = "Cancel connection",
                            tint = BentoTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode controller UI segments
                when (val state = authState) {
                    is WifiAuthState.Setup -> {
                        WifiSetupView(
                            ssid = ssid,
                            onSsidChange = { ssid = it },
                            password = password,
                            onPasswordChange = { password = it },
                            passwordVisible = passwordVisible,
                            onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                            selectedProtocol = selectedProtocol,
                            onProtocolChange = { selectedProtocol = it },
                            enterpriseIdentity = enterpriseIdentity,
                            onEnterpriseIdentityChange = { enterpriseIdentity = it },
                            onCancel = onCancel,
                            onStartAuth = {
                                if (password.length < 8) {
                                    authState = WifiAuthState.Error("Supplied secure key must be at least 8 characters long for WPA Standards.")
                                } else {
                                    scope.launch {
                                        // Trigger 4-way simulated handshake flow
                                        val deviceSsid = ssid
                                        val proto = selectedProtocol
                                        val pass = password
                                        
                                        // Seed deterministic hex string computations
                                        val md = MessageDigest.getInstance("SHA-256")
                                        val pmkBytes = md.digest((pass + deviceSsid).toByteArray())
                                        val pmkHex = pmkBytes.joinToString("") { "%02X".format(it) }
                                        
                                        val anonce = md.digest("anonce_seed_$deviceSsid".toByteArray()).joinToString("") { "%02X".format(it) }
                                        val snonce = md.digest("snonce_seed_$pass".toByteArray()).joinToString("") { "%02X".format(it) }
                                        
                                        val ptkBytes = md.digest((pmkHex + anonce + snonce).toByteArray())
                                        val ptkHex = ptkBytes.joinToString("") { "%02X".format(it) }

                                        // M1 Phase: received Authenticator Nonce (ANonce)
                                        authState = WifiAuthState.Handshake(
                                            ssid = deviceSsid,
                                            protocol = proto,
                                            phase = 1,
                                            statusMsg = "Phase 1/4: Authenticator sends ANonce (AP Challenge Packet).",
                                            anonce = anonce.take(32) + "...",
                                            snonce = "",
                                            pmk = pmkHex.take(24) + "...",
                                            ptk = ""
                                        )
                                        delay(1300)

                                        // M2 Phase: derived Transient Keys, generated SNonce & MIC
                                        authState = WifiAuthState.Handshake(
                                            ssid = deviceSsid,
                                            protocol = proto,
                                            phase = 2,
                                            statusMsg = "Phase 2/4: Local Supplicant computes Pairwise Master Key (PMK) & Pairwise Transient Key (PTK). Dispatches SNonce + HMAC-SHA1 MIC.",
                                            anonce = anonce.take(32) + "...",
                                            snonce = snonce.take(32) + "...",
                                            pmk = pmkHex.take(24) + "...",
                                            ptk = ptkHex.take(24) + "..."
                                        )
                                        delay(1500)

                                        // M3 Phase: AP verified Supplicant MIC, generated GTK (Group Temporal Key) and sent encrypted GTK + MIC
                                        authState = WifiAuthState.Handshake(
                                            ssid = deviceSsid,
                                            protocol = proto,
                                            phase = 3,
                                            statusMsg = "Phase 3/4: AP validates MIC checksum, derives PTK, generates Group Temporal Key (GTK) and transmits GTK under secure wrapper.",
                                            anonce = anonce.take(32) + "...",
                                            snonce = snonce.take(32) + "...",
                                            pmk = pmkHex.take(24) + "...",
                                            ptk = ptkHex.take(24) + "..."
                                        )
                                        delay(1200)

                                        // M4 Phase: Supplicant verified AP's M3 MIC, acked key install
                                        authState = WifiAuthState.Handshake(
                                            ssid = deviceSsid,
                                            protocol = proto,
                                            phase = 4,
                                            statusMsg = "Phase 4/4: Confirming installation of WPA temporal keys into physical hardware wireless MAC drivers.",
                                            anonce = anonce.take(32) + "...",
                                            snonce = snonce.take(32) + "...",
                                            pmk = pmkHex.take(24) + "...",
                                            ptk = ptkHex.take(24) + "..."
                                        )
                                        delay(1000)

                                        // Completed connection
                                        authState = WifiAuthState.Success(
                                            ssid = deviceSsid,
                                            protocol = proto,
                                            pmk = pmkHex,
                                            ptk = ptkHex,
                                            cipher = if (proto.contains("SAE")) "GCMP-256 (AES Galactic)" else "CCMP-128 (AES Block)"
                                        )
                                    }
                                }
                            }
                        )
                    }
                    is WifiAuthState.Handshake -> {
                        WifiHandshakeView(state)
                    }
                    is WifiAuthState.Success -> {
                        WifiSuccessView(
                            state = state,
                            onConfirm = {
                                onAuthenticateComplete(state.ssid, "••••••••", state.protocol)
                            }
                        )
                    }
                    is WifiAuthState.Error -> {
                        WifiErrorView(
                            errorMsg = state.errorMsg,
                            onRetry = {
                                authState = WifiAuthState.Setup
                            }
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun WifiSetupView(
    ssid: String,
    onSsidChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    selectedProtocol: String,
    onProtocolChange: (String) -> Unit,
    enterpriseIdentity: String,
    onEnterpriseIdentityChange: (String) -> Unit,
    onCancel: () -> Unit,
    onStartAuth: () -> Unit
) {
    var expandedProtocolMenu by remember { mutableStateOf(false) }
    val protocols = listOf("WPA2-PSK (Personal)", "WPA3-SAE (Personal)", "WPA2/WPA3 (Enterprise PEAP)")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "WIRELESS NETWORK PARAMETERS",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BentoTextSecondary,
                letterSpacing = 1.2.sp
            )
        )

        // SSID Field
        OutlinedTextField(
            value = ssid,
            onValueChange = onSsidChange,
            label = { Text("Network SSID (Access Point Name)") },
            leadingIcon = { Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, tint = BentoPurpleAccent) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BentoPurpleAccent,
                unfocusedBorderColor = BentoBorder
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Security Protocol Selector
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedProtocol,
                onValueChange = {},
                readOnly = true,
                label = { Text("Authentication Security Suite") },
                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = BentoPurpleAccent) },
                trailingIcon = {
                    IconButton(onClick = { expandedProtocolMenu = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select protocol")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedProtocolMenu = true },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BentoPurpleAccent,
                    unfocusedBorderColor = BentoBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )

            DropdownMenu(
                expanded = expandedProtocolMenu,
                onDismissRequest = { expandedProtocolMenu = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(BentoWhiteSurface)
            ) {
                protocols.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p, fontWeight = FontWeight.SemiBold, color = BentoTextDark) },
                        onClick = {
                            onProtocolChange(p)
                            expandedProtocolMenu = false
                        }
                    )
                }
            }
        }

        // Enterprise Specific Identity Field
        if (selectedProtocol.contains("Enterprise")) {
            OutlinedTextField(
                value = enterpriseIdentity,
                onValueChange = onEnterpriseIdentityChange,
                label = { Text("Enterprise WPA Login Identity (User)") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = BentoPurpleAccent) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BentoPurpleAccent,
                    unfocusedBorderColor = BentoBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Passphrase Shared Secret Password
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(if (selectedProtocol.contains("Enterprise")) "Enterprise Identity Password" else "Secure Shared Passphrase (WPA Key)") },
            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = BentoPurpleAccent) },
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility"
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BentoPurpleAccent,
                unfocusedBorderColor = BentoBorder
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Security Warning Note Box
        Card(
            colors = CardDefaults.cardColors(containerColor = BentoGreySurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = BentoPurpleAccent,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Encrypted Association Mode",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoTextDark
                        )
                    )
                    Text(
                        text = "Your password credentials are encrypted local-only and hashed with PBKDF2. Passphrase is split under WPA standards to derive authentic local hardware keys.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = BentoTextSecondary,
                            lineHeight = 14.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                border = BorderStroke(1.dp, BentoBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = BentoTextSecondary, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onStartAuth,
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleAccent),
                shape = RoundedCornerShape(12.dp),
                enabled = ssid.isNotBlank() && password.isNotBlank()
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Secure Connect", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WifiHandshakeView(state: WifiAuthState.Handshake) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "wifi_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.93f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulsing"
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
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = BentoPurpleAccent,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "WPA 4-WAY CRYPTO HANDSHAKE",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = BentoTextDark,
                letterSpacing = 0.5.sp
            )
        )
        Text(
            text = "EAPOL frame exchange in progress...",
            style = MaterialTheme.typography.bodySmall.copy(
                color = BentoTextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Progress indicators representing 4 steps
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (step in 1..4) {
                val isActiveStep = state.phase >= step
                val stepColor = if (isActiveStep) BentoPurpleAccent else BentoBorder.copy(alpha = 0.4f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(stepColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Detailed live parameters logger card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BentoGreySurface),
            border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "EAPOL CRYPTOGRAPHIC TERMINAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BentoTextSecondary,
                        letterSpacing = 1.sp
                    )
                )

                TermValuePillar(title = "Network SSID", value = state.ssid)
                TermValuePillar(title = "Authentication Protocol", value = state.protocol)
                
                if (state.pmk.isNotBlank()) {
                    TermValuePillar(title = "Pairwise Master Key (PMK) Hash", value = state.pmk)
                }
                
                if (state.anonce.isNotBlank()) {
                    TermValuePillar(title = "AP Authenticator Nonce (ANonce)", value = state.anonce)
                }

                if (state.snonce.isNotBlank()) {
                    TermValuePillar(title = "Client Supplicant Nonce (SNonce)", value = state.snonce)
                }

                if (state.ptk.isNotBlank()) {
                    TermValuePillar(title = "Calculated Pairwise Transient Key (PTK)", value = state.ptk)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed explain text
        Text(
            text = state.statusMsg,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = BentoTextSecondary,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun WifiSuccessView(
    state: WifiAuthState.Success,
    onConfirm: () -> Unit
) {
    var selectedTabSpec by remember { mutableStateOf("Android Kotlin API") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(BentoGreenContainer)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Success",
                tint = BentoGreenText,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SECURE WI-FI SECURED LINK",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = BentoGreenText,
                letterSpacing = 0.5.sp
            )
        )

        Text(
            text = "WPA 4-Way Handshake validation validated successfully.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = BentoTextSecondary,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Cryptographic keys output
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BentoGreySurface),
            border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HARDWARE SECURE VAULT KEYS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BentoTextSecondary,
                        letterSpacing = 1.sp
                    )
                )

                TermValuePillar(title = "WPA Security Protocol", value = state.protocol)
                TermValuePillar(title = "Assigned Session Cipher Suite", value = state.cipher)
                TermValuePillar(title = "WPA PMK (Symmetric Primary Master)", value = state.pmk)
                TermValuePillar(title = "WPA PTK (Ephemeral Derived Transient)", value = state.ptk)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Network Specifiers developer code tab block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BentoGreySurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Android Kotlin API", "Apple Swift API").forEach { t ->
                val isAct = selectedTabSpec == t
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAct) BentoPurpleAccent else Color.Transparent)
                        .clickable { selectedTabSpec = t }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAct) Color.White else BentoTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Render API Spec code snippet
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BentoGreySurface)
                .border(1.dp, BentoBorder.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (selectedTabSpec == "Android Kotlin API") {
                    Text(
                        text = "// Setup WiFi Network Specifier under WPA3 SAE standards\n" +
                                "val specifier = WifiNetworkSpecifier.Builder()\n" +
                                "  .setSsid(\"${state.ssid}\")\n" +
                                "  .setWpa3SaePassphrase(\"••••••••\")\n" +
                                "  .build()\n" +
                                "val request = NetworkRequest.Builder()\n" +
                                "  .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)\n" +
                                "  .setNetworkSpecifier(specifier)\n" +
                                "  .build()\n" +
                                "connectivityManager.requestNetwork(request, networkCallback)",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = BentoTextSecondary,
                            lineHeight = 14.sp
                        )
                    )
                } else {
                    Text(
                        text = "// Apply hotspot network credentials configuration in iOS\n" +
                                "import NetworkExtension\n" +
                                "let config = NEHotspotConfiguration(\n" +
                                "  ssid: \"${state.ssid}\",\n" +
                                "  passphrase: \"••••••••\",\n" +
                                "  isWEP: false\n" +
                                ")\n" +
                                "config.joinOnce = true\n" +
                                "NEHotspotConfigurationManager.shared.apply(config) { error in\n" +
                                "  if let err = error { print(\"WiFi Secure linkage failed: \\(err)\") }\n" +
                                "}",
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

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.WifiProtectedSetup, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Complete Association", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WifiErrorView(
    errorMsg: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(BentoAmberContainer)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = BentoAmberText,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AUTHENTICATION ERROR",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = BentoAmberText,
                letterSpacing = 0.5.sp
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = errorMsg,
            style = MaterialTheme.typography.bodySmall.copy(
                color = BentoTextSecondary,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retry Configuration", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TermValuePillar(title: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 9.sp,
                color = BentoTextSecondary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BentoTextDark
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
