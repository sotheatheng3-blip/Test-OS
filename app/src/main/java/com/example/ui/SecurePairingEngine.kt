package com.example.ui

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import javax.crypto.KeyAgreement

sealed class PairingState {
    object Idle : PairingState()
    object Discovering : PairingState()
    data class PinVerification(
        val pinCode: String,
        val localPublicHex: String,
        val remotePublicHex: String
    ) : PairingState()
    data class KeyExchangeActive(
        val localPublicKey: String,
        val remotePublicKey: String,
        val sharedSecret: String,
        val derivedAesKey: String,
        val fingerPrint: String
    ) : PairingState()
    data class Success(
        val deviceId: String,
        val deviceName: String,
        val sessionKey: String
    ) : PairingState()
    data class Error(val errorMsg: String) : PairingState()
}

class SecurePairingEngine {

    companion object {
        fun ByteArray.toHexString(): String {
            return joinToString("") { "%02X".format(it) }
        }
    }

    // Generates a real 256-bit Elliptic Curve KeyPair
    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(256)
        return kpg.generateKeyPair()
    }

    // Performs authentic ECDH key agreement to derive the raw shared secret
    fun calculateSharedSecret(localPrivate: java.security.PrivateKey, remotePublic: java.security.PublicKey): ByteArray {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(localPrivate)
        agreement.doPhase(remotePublic, true)
        return agreement.generateSecret()
    }

    // Derives an AES-256 symmetric cipher key using SHA-256 hash algorithm
    fun deriveAesSessionKey(sharedSecret: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(sharedSecret)
    }

    // Generates a secure, deterministic 6-digit PIN index matching both platforms
    fun generatePasskey(pubKeyA: ByteArray, pubKeyB: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(pubKeyA)
        digest.update(pubKeyB)
        val hash = digest.digest()
        
        var accum = 0
        for (i in 0..3) {
            accum = (accum shl 8) or (hash[i].toInt() and 0xFF)
        }
        val rawPin = Math.abs(accum) % 1000000
        return String.format("%06d", rawPin)
    }
}
