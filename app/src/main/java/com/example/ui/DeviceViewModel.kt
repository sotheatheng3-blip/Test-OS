package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.DeviceEntity
import com.example.data.model.LogEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random
import java.security.MessageDigest
import com.example.ui.SecurePairingEngine.Companion.toHexString

// Represents a real/simulated scanned device
data class ScannedDevice(
    val id: String,
    val name: String,
    val type: String, // "Bluetooth", "WiFi"
    val address: String, // MAC or IP Address
    val rssi: Int, // Signal strength in dBm
    val extraInfo: String = "" // e.g. GATT services or WiFi band
)

// Represents OTA firmware update states
sealed class OtaState {
    object Idle : OtaState()
    data class Progress(val msg: String, val progress: Float) : OtaState()
    data class Success(val version: String) : OtaState()
    data class Error(val error: String) : OtaState()
}

enum class ToastType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

data class ToastMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val message: String,
    val type: ToastType = ToastType.INFO,
    val durationMs: Long = 3500L
)

// Telemetry information
data class LiveTelemetry(
    val temperature: Float = 34.2f,
    val cpuLoad: Float = 15.0f,
    val sramUsage: Int = 42, // KB of 128KB
    val inputVoltage: Float = 5.02f,
    val rssi: Int = -58,
    val relayState1: Boolean = false,
    val relayState2: Boolean = false,
    val ledBrightness: Float = 0.5f, // PWM
    val storageTotalGb: Int = 128,
    val storageUsedGb: Float = 64.5f,
    val ramTotalGb: Int = 16,
    val ramUsedGb: Float = 10.4f,
    val eSimEnabled: Boolean = false,
    val eSimCarrier: String = "Global Core Network",
    val eSimIccid: String = "",
    val eSimSignalStrength: Int = -120, // Offline by default
    val eSimStatus: String = "Inactive", // "Inactive", "Provisioning", "Connecting", "Connected", "Error"
    val eSimDataUsedMb: Float = 0.0f
)

class DeviceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val deviceDao = db.deviceDao()
    private val logDao = db.logDao()

    // Saved devices and global logs from Room
    val savedDevices: StateFlow<List<DeviceEntity>> = deviceDao.getAllDevicesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalLogs: StateFlow<List<LogEntity>> = logDao.getAllLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Scanned results
    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Currently connected / Active communication state
    private val _activeDevice = MutableStateFlow<DeviceEntity?>(null)
    val activeDevice: StateFlow<DeviceEntity?> = _activeDevice.asStateFlow()

    private val _connectionState = MutableStateFlow("Disconnected") // "Disconnected", "Connecting", "Connected"
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    // Live sensory data
    private val _telemetry = MutableStateFlow(LiveTelemetry())
    val telemetry: StateFlow<LiveTelemetry> = _telemetry.asStateFlow()

    // Current live logs for active connected device
    private val _deviceLogs = MutableStateFlow<List<LogEntity>>(emptyList())
    val deviceLogs: StateFlow<List<LogEntity>> = _deviceLogs.asStateFlow()

    // OTA Update Status
    private val _otaState = MutableStateFlow<OtaState>(OtaState.Idle)
    val otaState: StateFlow<OtaState> = _otaState.asStateFlow()

    // Toast Notification SharedFlow
    private val _toastFlow = MutableSharedFlow<ToastMessage>(extraBufferCapacity = 5)
    val toastFlow = _toastFlow.asSharedFlow()

    fun showToast(message: String, type: ToastType = ToastType.INFO) {
        viewModelScope.launch {
            _toastFlow.emit(ToastMessage(message = message, type = type))
        }
    }

    // Secure Pairing Status
    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    // Global System Animation state
    private val _windowAnimationScale = MutableStateFlow(0.0f) // 0.0f = no delay (instant)
    val windowAnimationScale: StateFlow<Float> = _windowAnimationScale.asStateFlow()

    private val _transitionAnimationScale = MutableStateFlow(0.0f)
    val transitionAnimationScale: StateFlow<Float> = _transitionAnimationScale.asStateFlow()

    private val _animatorDurationScale = MutableStateFlow(0.0f)
    val animatorDurationScale: StateFlow<Float> = _animatorDurationScale.asStateFlow()

    fun setWindowAnimationScale(scale: Float) {
        _windowAnimationScale.value = scale
        val cur = _activeDevice.value
        if (cur != null) {
            log(cur.id, "DEBUG", "Config changed: Window Animation Scale set to ${scale}x.")
        }
    }

    fun setTransitionAnimationScale(scale: Float) {
        _transitionAnimationScale.value = scale
        val cur = _activeDevice.value
        if (cur != null) {
            log(cur.id, "DEBUG", "Config changed: Transition Animation Scale set to ${scale}x.")
        }
    }

    fun setAnimatorDurationScale(scale: Float) {
        _animatorDurationScale.value = scale
        val cur = _activeDevice.value
        if (cur != null) {
            log(cur.id, "DEBUG", "Config changed: Animator Duration Scale set to ${scale}x.")
        }
    }

    fun applySmoothOptimalPerformancePreset() {
        _windowAnimationScale.value = 0.0f
        _transitionAnimationScale.value = 0.0f
        _animatorDurationScale.value = 0.0f
        val cur = _activeDevice.value
        if (cur != null) {
            log(cur.id, "INFO", "Calibrated optimal performance preset: 0.0x Window Animation, 0.0x Transition Animation, 0.0x Animator Duration (Zero Delay).")
        }
    }

    fun setStorageTotalGb(total: Int) {
        _telemetry.update {
            val used = it.storageUsedGb.coerceAtMost(total.toFloat())
            it.copy(storageTotalGb = total, storageUsedGb = used)
        }
        val cur = _activeDevice.value
        if (cur != null) {
            log(cur.id, "INFO", "Hardware storage partition limit re-allocated to ${total}G.")
        }
    }

    fun setRamTotalGb(total: Int) {
        _telemetry.update {
            val used = it.ramUsedGb.coerceAtMost(total.toFloat())
            it.copy(ramTotalGb = total, ramUsedGb = used)
        }
        val cur = _activeDevice.value
        if (cur != null) {
            log(cur.id, "INFO", "System RAM hardware profiles expanded to ${total}G memory.")
        }
    }

    fun toggleESim(enabled: Boolean) {
        val currentDevice = _activeDevice.value ?: return
        if (enabled) {
            _telemetry.update {
                it.copy(
                    eSimEnabled = true,
                    eSimStatus = "Connecting"
                )
            }
            log(currentDevice.id, "INFO", "eSIM: Powering up transceiver and querying local GSMA eUICC card...")
            viewModelScope.launch {
                delay(1200)
                if (_telemetry.value.eSimIccid.isEmpty()) {
                    // Automatically provision default core eSIM or prompt for activation code
                    val randomIccid = "89049032" + (1000000000L + Random.nextLong(9000000000L)).toString()
                    _telemetry.update {
                        it.copy(
                            eSimStatus = "Connected",
                            eSimIccid = randomIccid,
                            eSimCarrier = "Global Core Network",
                            eSimSignalStrength = -82,
                            eSimDataUsedMb = 0.0f
                        )
                    }
                    log(currentDevice.id, "INFO", "eSIM Activated: Provisioned default Global Core Profile.")
                    log(currentDevice.id, "INFO", "eSIM Connection Status: ATTACHED to LTE-M Band 8 (carrier=Global Core Network, iccid=$randomIccid).")
                } else {
                    _telemetry.update {
                        it.copy(
                            eSimStatus = "Connected",
                            eSimSignalStrength = -80
                        )
                    }
                    log(currentDevice.id, "INFO", "eSIM Connected: Established connection successfully with ${_telemetry.value.eSimCarrier}.")
                }
            }
        } else {
            _telemetry.update {
                it.copy(
                    eSimEnabled = false,
                    eSimStatus = "Inactive",
                    eSimSignalStrength = -120
                )
            }
            log(currentDevice.id, "WARN", "eSIM: Transceiver set to Ultra Low Power mode. Cellular radio detached.")
        }
    }

    fun provisionESimProfile(activationCode: String, carrier: String) {
        val currentDevice = _activeDevice.value ?: return
        _telemetry.update {
            it.copy(
                eSimStatus = "Provisioning",
                eSimEnabled = true
            )
        }
        viewModelScope.launch {
            log(currentDevice.id, "INFO", "RSP (Remote SIM Provisioning): Starting SM-DP+ registration...")
            log(currentDevice.id, "INFO", "RSP: Handshake initialized with GSMA-accredited server: custom code $activationCode.")
            delay(1200)
            
            log(currentDevice.id, "INFO", "RSP: Authenticating eUICC profile signature with security keys...")
            delay(1000)
            
            log(currentDevice.id, "INFO", "RSP: Processing ES9+ Profile Download and Installation Payload...")
            delay(1500)
            
            val randomIccid = "89" + (100000000000000000L + Random.nextLong(900000000000000000L)).toString().take(17)
            _telemetry.update {
                it.copy(
                    eSimStatus = "Connected",
                    eSimCarrier = carrier,
                    eSimIccid = randomIccid,
                    eSimSignalStrength = -78,
                    eSimDataUsedMb = 0.0f
                )
            }
            log(currentDevice.id, "INFO", "RSP Success: eSIM Profile fully loaded & enabled. ICCID=$randomIccid, Carrier=$carrier.")
            log(currentDevice.id, "INFO", "eSIM cellular channel established: Attached to local carrier towers.")
        }
    }

    fun runSimulatedSpeedTest() {
        val currentDevice = _activeDevice.value ?: return
        val currentStatus = _telemetry.value.eSimStatus
        if (currentStatus != "Connected") {
            log(currentDevice.id, "ERROR", "LTE-M/NB-IoT Speed Test Failed: eSIM transceiver is offline.")
            return
        }
        
        viewModelScope.launch {
            log(currentDevice.id, "INFO", "Speedtest: Commencing network throughput diagnostic over eSIM channel...")
            log(currentDevice.id, "INFO", "Speedtest: Testing latency / jitter with nearest edge latency point...")
            delay(1000)
            log(currentDevice.id, "INFO", "Speedtest: Ping=14ms, Jitter=1.8ms.")
            
            log(currentDevice.id, "INFO", "Speedtest: Commencing Downlink (DL) burst stream testing...")
            delay(1200)
            val df = String.format("%.2f", 45f + Random.nextFloat() * 15f)
            log(currentDevice.id, "INFO", "Speedtest: Downlink completion. DL Throughput = $df Mbps")
            
            log(currentDevice.id, "INFO", "Speedtest: Commencing Uplink (UL) saturation burst...")
            delay(1200)
            val uf = String.format("%.2f", 12f + Random.nextFloat() * 5f)
            log(currentDevice.id, "INFO", "Speedtest: Uplink completion. UL Throughput = $uf Mbps")
            
            // Add tested data usage to cellular data counter
            _telemetry.update {
                it.copy(eSimDataUsedMb = it.eSimDataUsedMb + 18.5f)
            }
            log(currentDevice.id, "INFO", "Speedtest: Transistor diagnostic test completed. Consumed 18.50 MB cellular quota.")
        }
    }

    private val pairingEngine = SecurePairingEngine()

    private data class PairingSession(
        val device: ScannedDevice,
        val targetPlatform: String,
        val localKeyPair: java.security.KeyPair,
        val remotePublicKey: java.security.PublicKey
    )
    private var currentPairingSession: PairingSession? = null

    private var telemetryJob: Job? = null
    private var scanJob: Job? = null

    // Preset firmware lists
    val availableFirmwares = listOf(
        "v1.0.0 (Legacy Standard)",
        "v1.1.5 (Buffer Polish & Encryption)",
        "v2.0.1-stable (Super-Scheduler + OTA-v2)",
        "v2.1.0-alpha (Experimental Low-Power BLE)"
    )

    init {
        // Pre-populate some demo devices in database if it is empty, to look fully completed immediately
        viewModelScope.launch {
            deviceDao.getAllDevicesFlow().first().let { devices ->
                if (devices.isEmpty()) {
                    val initial = listOf(
                        DeviceEntity(
                            id = "DEV-ESP32-901",
                            name = "ESP32 Smarthome Controller",
                            type = "WiFi",
                            address = "192.168.1.99",
                            status = "Disconnected",
                            firmwareVersion = "v1.0.0",
                            lastConnected = System.currentTimeMillis() - 86400000 * 2,
                            category = "Smart Home",
                            batteryLevel = 100,
                            signalStrength = -50,
                            operationalMode = "Idle"
                        ),
                        DeviceEntity(
                            id = "DEV-BLE-IOT",
                            name = "Precision BLE Accel Sensor",
                            type = "Bluetooth",
                            address = "AC:8D:1F:B5:12:09",
                            status = "Disconnected",
                            firmwareVersion = "v1.1.5",
                            lastConnected = System.currentTimeMillis() - 3600000,
                            category = "Industrial",
                            batteryLevel = 100,
                            signalStrength = -50,
                            operationalMode = "Idle"
                        ),
                        DeviceEntity(
                            id = "DEV-IOS-BRIDGE",
                            name = "iOS Companion Hub",
                            type = "Bluetooth",
                            address = "7F:DE:3A:45:11:BB",
                            status = "Disconnected",
                            firmwareVersion = "v1.0.0",
                            lastConnected = System.currentTimeMillis() - 172800000,
                            category = "iOS Companion",
                            batteryLevel = 100,
                            signalStrength = -50,
                            operationalMode = "Idle"
                        )
                    )
                    initial.forEach { deviceDao.insertDevice(it) }
                    log("SYSTEM", "INFO", "Database pre-populated with default controller templates.")
                }
            }
        }

        // Global telemetry simulator for all connected devices
        viewModelScope.launch {
            while (true) {
                delay(2500)
                val connectedDevices = savedDevices.value.filter { it.status == "Connected" }
                for (dev in connectedDevices) {
                    val currentBattery = if (dev.batteryLevel <= 5) 100 else dev.batteryLevel
                    val newBattery = (currentBattery - Random.nextInt(0, 2)).coerceIn(5, 100)
                    val newSignal = (-85 + Random.nextInt(0, 45))
                    val newMode = if (_activeDevice.value?.id == dev.id) "Diagnostic" else listOf("Active", "Standby", "Processing").random()
                    deviceDao.updateDeviceTelemetry(dev.id, newBattery, newSignal, newMode)
                }
            }
        }
    }

    // Bluetooth / WiFi scanned simulator
    fun startScanning(type: String) {
        if (_isScanning.value) return
        _isScanning.value = true
        _scannedDevices.value = emptyList()

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            log("SYSTEM", "INFO", "Starting $type scanning discovery...")
            delay(1000)

            val templates = if (type == "Bluetooth" || type == "All") {
                listOf(
                    ScannedDevice("DEV-BLE-A8F", "BLE MotorDriver Node", "Bluetooth", "00:1A:7D:C2:54:19", -68, "GATT, Motor Service"),
                    ScannedDevice("DEV-BLE-99F", "Wearable HR-X3", "Bluetooth", "ED:A8:14:1F:30:1F", -81, "GATT, Medical Profile"),
                    ScannedDevice("DEV-BLE-IOT", "Precision BLE Accel Sensor", "Bluetooth", "AC:8D:1F:B5:12:09", -55, "GATT Companion Node"),
                    ScannedDevice("DEV-IOS-BRIDGE", "iOS Companion Hub", "Bluetooth", "7F:DE:3A:45:11:BB", -49, "CoreBluetooth Bridge")
                )
            } else emptyList()

            val wifiTemplates = if (type == "WiFi" || type == "All") {
                listOf(
                    ScannedDevice("DEV-WIFI-302", "Smart-Bulb RGB", "WiFi", "192.168.1.133", -62, "Port 80 (HTTP Server)"),
                    ScannedDevice("DEV-ESP32-901", "ESP32 Smarthome Controller", "WiFi", "192.168.1.99", -45, "Port 80, 5001 (WebSockets)"),
                    ScannedDevice("DEV-WIFI-PLC", "Industrial PLC Relay", "WiFi", "192.168.1.205", -75, "Port 502 (Modbus-TCP)")
                )
            } else emptyList()

            val pool = (templates + wifiTemplates).shuffled()
            
            // Add items one-by-one with staggered timing to feel highly realistic!
            for (device in pool) {
                if (!_isScanning.value) break
                delay(Random.nextLong(600, 1500))
                _scannedDevices.update { it + device }
                log("SCANNER", "DEBUG", "Discovered node: ${device.name} [Type: ${device.type}, RSSI: ${device.rssi}dBm]")
            }
            _isScanning.value = false
            log("SCANNER", "INFO", "Device discovery finished. Found ${_scannedDevices.value.size} devices.")
        }
    }

    fun stopScanning() {
        _isScanning.value = false
        scanJob?.cancel()
        log("SCANNER", "INFO", "Discovery scanning cancelled.")
    }

    // Save a discovered device to Room DB
    fun saveDevice(scanned: ScannedDevice, category: String = "General") {
        viewModelScope.launch {
            val dbMatch = deviceDao.getDeviceById(scanned.id)
            val updated = DeviceEntity(
                id = scanned.id,
                name = scanned.name,
                type = scanned.type,
                address = scanned.address,
                status = dbMatch?.status ?: "Disconnected",
                firmwareVersion = dbMatch?.firmwareVersion ?: "v1.0.0",
                lastConnected = dbMatch?.lastConnected ?: 0L,
                category = category
            )
            deviceDao.insertDevice(updated)
            log(scanned.id, "INFO", "Registered device ${scanned.name} in hardware inventory.")
        }
    }

    // Add a new device manually through UI dialog
    fun addCustomDevice(name: String, type: String, address: String, category: String) {
        viewModelScope.launch {
            val id = "DEV-CUSTOM-${Random.nextInt(100, 999)}"
            val device = DeviceEntity(
                id = id,
                name = name,
                type = type,
                address = address,
                status = "Disconnected",
                firmwareVersion = "v1.0.0",
                lastConnected = 0L,
                category = category
            )
            deviceDao.insertDevice(device)
            log(id, "INFO", "Manually inserted node: $name ($type: $address)")
        }
    }

    // Initiates the multi-stage secure Bluetooth pairing sequence
    fun startSecurePairing(device: ScannedDevice, targetPlatform: String) {
        _pairingState.value = PairingState.Discovering
        viewModelScope.launch {
            log(device.id, "INFO", "Securing BLE connection to remote $targetPlatform...")
            log(device.id, "DEBUG", "BLE Phase 1: Locating secure pairing characteristics...")
            delay(1200)

            try {
                // Generate real local key pair and simulated remote public key
                val localKeyPair = pairingEngine.generateKeyPair()
                val remoteKeyPair = pairingEngine.generateKeyPair()

                val localPubBytes = localKeyPair.public.encoded
                val remotePubBytes = remoteKeyPair.public.encoded

                val localPubHex = localPubBytes.toHexString().take(48) + "..."
                val remotePubHex = remotePubBytes.toHexString().take(48) + "..."

                // Generate Passkey PIN from keys
                val pinCode = pairingEngine.generatePasskey(localPubBytes, remotePubBytes)

                log(device.id, "INFO", "GATT Callback: Passkey Pin generated. Require comparison validation.")
                
                _pairingState.value = PairingState.PinVerification(
                    pinCode = pinCode,
                    localPublicHex = localPubHex,
                    remotePublicHex = remotePubHex
                )

                // Store keys temporarily in a pairing session container
                currentPairingSession = PairingSession(
                    device = device,
                    targetPlatform = targetPlatform,
                    localKeyPair = localKeyPair,
                    remotePublicKey = remoteKeyPair.public
                )

            } catch (e: Exception) {
                _pairingState.value = PairingState.Error("Cryptographic engine failed: ${e.localizedMessage}")
                log(device.id, "ERROR", "Secure pairing failed during key initialization.")
            }
        }
    }

    // Confirms Pin verification and runs Elliptic Curve Diffie-Hellman Key Exchange
    fun confirmPairingPin() {
        val session = currentPairingSession ?: return
        val device = session.device

        viewModelScope.launch {
            _pairingState.value = PairingState.KeyExchangeActive(
                localPublicKey = session.localKeyPair.public.encoded.toHexString().take(32) + "...",
                remotePublicKey = session.remotePublicKey.encoded.toHexString().take(32) + "...",
                sharedSecret = "Computing secret vector...",
                derivedAesKey = "Deriving symmetric cipher...",
                fingerPrint = "Computing hash..."
            )
            log(device.id, "INFO", "User confirmed Bluetooth Passcode pin matches.")
            log(device.id, "DEBUG", "BLE Phase 2: Starting ECDH public key exchange.")
            delay(1500)

            try {
                // Calculate actual shared secret and symmetric key
                val rawSecret = pairingEngine.calculateSharedSecret(
                    session.localKeyPair.private,
                    session.remotePublicKey
                )
                val aesKey = pairingEngine.deriveAesSessionKey(rawSecret)

                val secretHex = rawSecret.toHexString().take(32) + "..."
                val aesHex = aesKey.toHexString()
                val fingerprint = MessageDigest.getInstance("MD5")
                    .digest(aesKey).toHexString().take(16)

                _pairingState.value = PairingState.KeyExchangeActive(
                    localPublicKey = session.localKeyPair.public.encoded.toHexString().take(32) + "...",
                    remotePublicKey = session.remotePublicKey.encoded.toHexString().take(32) + "...",
                    sharedSecret = secretHex,
                    derivedAesKey = aesHex.take(24) + "...",
                    fingerPrint = fingerprint
                )

                log(device.id, "INFO", "ECDH Shared Secret determined successfully!")
                log(device.id, "DEBUG", "Symmetric AES-256 session key Derived: 0x${aesHex.take(16)}[...]")
                delay(1800)

                // Complete bonding and save to DB
                saveDevice(device, "Bonded Link")
                
                // Immediately connect to established node to show dynamic live telemetry immediately!
                val dbMatch = deviceDao.getDeviceById(device.id) ?: DeviceEntity(
                    id = device.id,
                    name = device.name,
                    type = device.type,
                    address = device.address,
                    status = "Disconnected",
                    firmwareVersion = "v1.0.0",
                    lastConnected = 0L,
                    category = "Bonded Link"
                )
                connectToDevice(dbMatch)

                _pairingState.value = PairingState.Success(
                    deviceId = device.id,
                    deviceName = device.name,
                    sessionKey = aesHex.take(32)
                )

                log(device.id, "INFO", "Established secured cryptographically authenticated link!")

            } catch (e: Exception) {
                _pairingState.value = PairingState.Error("Key exchange failed: ${e.localizedMessage}")
                log(device.id, "ERROR", "Secure pairing failed during ECDH handshaking.")
            }
        }
    }

    // Aborts pairing
    fun cancelPairing() {
        _pairingState.value = PairingState.Idle
        currentPairingSession = null
        log("SCANNER", "INFO", "Bluetooth pairing request rejected by user.")
    }

    // Connect to a saved hardware device
    fun connectToDevice(device: DeviceEntity) {
        if (_activeDevice.value?.id == device.id && _connectionState.value == "Connected") return

        _activeDevice.value = device
        _connectionState.value = "Connecting"
        _deviceLogs.value = emptyList()

        viewModelScope.launch {
            log(device.id, "INFO", "Initiating handshake over ${device.type} at ${device.address}...")
            
            // Connect timeline
            delay(1200)
            if (_connectionState.value == "Connecting" && _activeDevice.value?.id == device.id) {
                _connectionState.value = "Connected"
                deviceDao.updateDeviceStatus(device.id, "Connected", System.currentTimeMillis())
                _activeDevice.update { it?.copy(status = "Connected", lastConnected = System.currentTimeMillis()) }

                log(device.id, "INFO", "Handshake secured! Initializing telemetry buffers...")
                log(device.id, "DEBUG", "Reading hardware capabilities (GATT services / WebSockets endpoints)")
                log(device.id, "INFO", "Dynamic streaming enabled. Firmware version: ${device.firmwareVersion}")

                // Start live telemetry mock loops
                startTelemetryStream(device.id)
            }
        }
    }

    fun disconnectDevice(deviceId: String) {
        viewModelScope.launch {
            deviceDao.updateDeviceStatus(deviceId, "Disconnected", System.currentTimeMillis())
            log(deviceId, "INFO", "Channel successfully disconnected by user.")
            
            if (_activeDevice.value?.id == deviceId) {
                telemetryJob?.cancel()
                _connectionState.value = "Disconnected"
                _activeDevice.value = null
                _telemetry.value = LiveTelemetry()
                _otaState.value = OtaState.Idle
            }
        }
    }

    // Simulation command for interactive drop connection testing
    fun simulateUnexpectedDisconnect() {
        val currentDevice = _activeDevice.value ?: return
        viewModelScope.launch {
            log(currentDevice.id, "ERROR", "CRITICAL: Suddenly lost link connection handshake carrier!")
            deviceDao.updateDeviceStatus(currentDevice.id, "Disconnected", System.currentTimeMillis())
            telemetryJob?.cancel()
            _connectionState.value = "Disconnected"
            _activeDevice.value = null
            _telemetry.value = LiveTelemetry()
            _otaState.value = OtaState.Idle
            showToast("Connection to ${currentDevice.name} dropped unexpectedly!", ToastType.ERROR)
        }
    }

    // Disconnect active connection
    fun disconnectActive() {
        val current = _activeDevice.value ?: return
        disconnectDevice(current.id)
    }

    // Delete a device from Room database
    fun deleteSavedDevice(device: DeviceEntity) {
        viewModelScope.launch {
            if (_activeDevice.value?.id == device.id) {
                disconnectActive()
            }
            deviceDao.deleteDevice(device)
            log("SYSTEM", "INFO", "Removed device ${device.name} from catalog database.")
        }
    }

    // Clear saved device serial logs
    fun clearDeviceLogs(deviceId: String) {
        viewModelScope.launch {
            logDao.clearLogsForDevice(deviceId)
            if (_activeDevice.value?.id == deviceId) {
                _deviceLogs.value = emptyList()
            }
        }
    }

    // Toggle hardware simulated GPIO values
    fun toggleRelay(index: Int, active: Boolean) {
        val currentDevice = _activeDevice.value ?: return
        _telemetry.update {
            if (index == 1) it.copy(relayState1 = active) else it.copy(relayState2 = active)
        }
        val port = if (index == 1) "GPIO 12 (RELAY_A)" else "GPIO 13 (RELAY_B)"
        val valStr = if (active) "HIGH / CLOSED" else "LOW / OPENED"
        
        viewModelScope.launch {
            log(currentDevice.id, "DEBUG", "Command OUT -> Write $port pin to $valStr")
            delay(150)
            log(currentDevice.id, "INFO", "Telemetry ACK -> Pin state confirmed: $port is $valStr")
            
            // Slightly fluctuate temperature and voltage upon relay toggle to simulate electric load!
            _telemetry.update {
                val tempDiff = if (active) 1.8f else -1.2f
                val voltDiff = if (active) -0.06f else 0.04f
                it.copy(
                    temperature = (it.temperature + tempDiff).coerceIn(30.0f, 65.0f),
                    inputVoltage = (it.inputVoltage + voltDiff).coerceIn(4.5f, 5.3f),
                    cpuLoad = (it.cpuLoad + Random.nextFloat() * 10f).coerceIn(0f, 100f)
                )
            }
        }
    }

    // PWM brightness change
    fun setLedBrightness(brightness: Float) {
        val currentDevice = _activeDevice.value ?: return
        _telemetry.update { it.copy(ledBrightness = brightness) }

        viewModelScope.launch {
            val dutyPercentage = (brightness * 100).toInt()
            log(currentDevice.id, "DEBUG", "PWM command -> Write GPIO 14 (LEDPWM) dutycycle to $dutyPercentage%")
        }
    }

    // Simulated telemetry telemetry data loop representing real-time monitoring
    private fun startTelemetryStream(deviceId: String) {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            var tempBase = 32.5f
            var cpuBase = 12.0f
            while (true) {
                delay(1500)
                
                // Keep states moving naturally and slightly randomize
                val currentRelayA = _telemetry.value.relayState1
                val currentRelayB = _telemetry.value.relayState2
                val currentPwm = _telemetry.value.ledBrightness

                // Relays and LED brightness elevate temperature to simulate real heat dissapation!
                val heatLoad = (if (currentRelayA) 2.5f else 0.0f) + (if (currentRelayB) 1.8f else 0.0f) + (currentPwm * 3.4f)
                val targetTemp = 31.0f + heatLoad + Random.nextFloat() * 3.5f
                
                // Glide towards target temperature
                tempBase = tempBase * 0.8f + targetTemp * 0.2f
                
                val currentCpu = (cpuBase + Random.nextFloat() * 12f + (currentPwm * 15f)).coerceIn(5f, 95f)
                val randomRssi = -50 - Random.nextInt(15)
                val sramFree = 128 - (32 + (currentPwm * 10).toInt() + (if (currentRelayA) 4 else 0) + Random.nextInt(5))

                val currentStorageTotal = _telemetry.value.storageTotalGb
                val currentRamTotal = _telemetry.value.ramTotalGb
                
                // Simulate periodic file read/write or log saving which alters storage utilization slightly!
                val storageOffset = if (Random.nextBoolean()) Random.nextFloat() * 0.25f else -Random.nextFloat() * 0.15f
                val newStorageUsed = (_telemetry.value.storageUsedGb + storageOffset)
                    .coerceIn(10.0f, currentStorageTotal.toFloat() * 0.95f)
                
                // Simulate RAM overhead scaling with CPU load and LED controller PWM
                val baseRam = currentRamTotal * 0.35f // 35% basic background services
                val loadRam = (currentCpu / 100f) * (currentRamTotal * 0.3f) 
                val pwmRam = currentPwm * (currentRamTotal * 0.15f)
                val newRamUsed = (baseRam + loadRam + pwmRam + Random.nextFloat() * 0.25f)
                    .coerceIn(1.5f, currentRamTotal.toFloat() * 0.98f)

                val currentEsim = _telemetry.value
                val newEsimSignal = if (currentEsim.eSimEnabled && currentEsim.eSimStatus == "Connected") {
                    (-75 - Random.nextInt(15)).coerceIn(-115, -60)
                } else {
                    -120
                }
                val esimDataOffset = if (currentEsim.eSimEnabled && currentEsim.eSimStatus == "Connected") {
                    0.02f + Random.nextFloat() * 0.08f
                } else {
                    0.0f
                }

                _telemetry.update {
                    it.copy(
                        temperature = String.format("%.1f", tempBase).toFloat(),
                        cpuLoad = String.format("%.1f", currentCpu).toFloat(),
                        sramUsage = sramFree,
                        rssi = randomRssi,
                        inputVoltage = String.format("%.2f", 4.98f + Random.nextFloat() * 0.1f).toFloat(),
                        storageUsedGb = String.format("%.2f", newStorageUsed).toFloat(),
                        ramUsedGb = String.format("%.2f", newRamUsed).toFloat(),
                        eSimSignalStrength = newEsimSignal,
                        eSimDataUsedMb = if (currentEsim.eSimEnabled && currentEsim.eSimStatus == "Connected") {
                            String.format("%.2f", currentEsim.eSimDataUsedMb + esimDataOffset).toFloat()
                        } else {
                            currentEsim.eSimDataUsedMb
                        }
                    )
                }

                // Append serial logs occasionally to represent device status monitoring
                if (Random.nextInt(4) == 0) {
                    val statusMsg = when (Random.nextInt(5)) {
                        0 -> "Dynamic loop: System heap matches expected levels. free_sram=${sramFree}KB"
                        1 -> "Sensors report: Temp=${String.format("%.1f", tempBase)}°C, Voltage=${_telemetry.value.inputVoltage}V"
                        2 -> "Network link ping response packet: RTT=18ms"
                        3 -> "Power monitor: Input current normal. Current drawn=120mA"
                        else -> "Hardware Watchdog cleared."
                    }
                    log(deviceId, "DEBUG", statusMsg)
                }
            }
        }
    }

    // Real-time Automated OTA Firmware Update
    fun runFirmwareUpdate(targetVersion: String) {
        val currentDevice = _activeDevice.value ?: return
        if (_otaState.value is OtaState.Progress) return

        // Cancel standard telemetry simulation to simulate bootloader lockout!
        telemetryJob?.cancel()
        _otaState.value = OtaState.Progress("Initiating bootloader update protocol...", 0.0f)

        viewModelScope.launch {
            log(currentDevice.id, "WARN", "User initiated Firmware OTA to $targetVersion. Telemetry paused.")
            log(currentDevice.id, "INFO", "Rebooting controller into dynamic safe OTA bootloader...")
            delay(1500)

            _otaState.value = OtaState.Progress("Contacting cloud firmware repository standard file...", 0.1f)
            delay(1200)

            _otaState.value = OtaState.Progress("Downloading signature payload & checking integrity...", 0.2f)
            log(currentDevice.id, "INFO", "File signature verify: SHA256 matches repository fingerprint.")
            delay(1000)

            _otaState.value = OtaState.Progress("Preparing device partitions for flashing...", 0.3f)
            log(currentDevice.id, "WARN", "OTA-SPIFFS partitions resized successfully.")
            delay(800)

            // Dynamic progress count loops
            val steps = listOf(
                "Uploading binary chunk 1 of 8 (128KB)..." to 0.4f,
                "Uploading binary chunk 3 of 8 (384KB)..." to 0.5f,
                "Uploading binary chunk 5 of 8 (640KB)..." to 0.7f,
                "Uploading binary chunk 7 of 8 (896KB)..." to 0.85f,
                "Verifying written flash block configurations..." to 0.95f
            )

            for ((msg, progress) in steps) {
                _otaState.value = OtaState.Progress(msg, progress)
                val chunkNum = (progress * 100).toInt()
                log(currentDevice.id, "DEBUG", "BOOTLOADER: Flash written: $chunkNum% complete.")
                delay(900)
            }

            _otaState.value = OtaState.Progress("Soft-resetting and securing secure handshake...", 0.99f)
            log(currentDevice.id, "WARN", "OTA Success. Booting system. Reset vector: 0x40080400")
            delay(1500)

            // Update local fields & Room DB
            val shortVer = targetVersion.substringBefore(" ")
            deviceDao.updateDeviceFirmware(currentDevice.id, shortVer)
            _activeDevice.update { it?.copy(firmwareVersion = shortVer) }

            _otaState.value = OtaState.Success(shortVer)
            log(currentDevice.id, "INFO", "System reboot completed! Running Firmware version: $shortVer")
            log(currentDevice.id, "INFO", "Firmware integrity verified. Resuming live telemetry stream.")
            showToast("Firmware updated to v$shortVer successfully!", ToastType.SUCCESS)

            // Restore telemetry loops
            startTelemetryStream(currentDevice.id)
            _otaState.value = OtaState.Idle
        }
    }

    // Local helper to append and database log
    fun log(deviceId: String, level: String, message: String) {
        viewModelScope.launch {
            val logEntry = LogEntity(
                deviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                level = level,
                message = message
            )
            logDao.insertLog(logEntry)

            // If the log is for our currently active device (or global setup log), append locally:
            if (deviceId == _activeDevice.value?.id || deviceId == "SYSTEM" || deviceId == "SCANNER") {
                _deviceLogs.update { (listOf(logEntry) + it).take(500) }
            }
        }
    }
}
