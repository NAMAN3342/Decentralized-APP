package com.example.mine.crypto

import android.util.Log
import java.nio.ByteBuffer
import java.security.*
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class SessionKeys(val kTx: ByteArray, val kRx: ByteArray)

class CryptoManager {

    // -------- Key agreement (ECDH/X25519 preferred) --------
    fun computeSharedSecret(ourPrivate: PrivateKey, peerPublic: PublicKey): ByteArray {
        // Try modern X25519 first; fallback to EC if needed.
        return try {
            val ka = KeyAgreement.getInstance("X25519")
            ka.init(ourPrivate)
            ka.doPhase(peerPublic, true)
            ka.generateSecret()
        } catch (_: Exception) {
            val ka = KeyAgreement.getInstance("ECDH")
            ka.init(ourPrivate)
            ka.doPhase(peerPublic, true)
            ka.generateSecret()
        }
    }

    // -------- HKDF-SHA256 --------
    fun deriveSessionKeys(sharedSecret: ByteArray, salt: ByteArray): SessionKeys {
        val prk = hkdfExtract(salt, sharedSecret)
        val okm = hkdfExpand(prk, "mine-session".toByteArray(), 64)
        val kTx = okm.copyOfRange(0, 32)
        val kRx = okm.copyOfRange(32, 64)
        return SessionKeys(kTx, kRx)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray =
        hmacSha256(if (salt.isEmpty()) ByteArray(32) else salt, ikm)

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, len: Int): ByteArray {
        var t = ByteArray(0)
        val out = ByteArray(len)
        var pos = 0
        var counter = 1
        while (pos < len) {
            val macIn = ByteArray(t.size + info.size + 1)
            System.arraycopy(t, 0, macIn, 0, t.size)
            System.arraycopy(info, 0, macIn, t.size, info.size)
            macIn[macIn.lastIndex] = counter.toByte()
            t = hmacSha256(prk, macIn)
            val toCopy = minOf(t.size, len - pos)
            System.arraycopy(t, 0, out, pos, toCopy)
            pos += toCopy
            counter++
        }
        return out
    }

    // -------- AES-GCM wrappers --------
    fun encryptWithAAD(
        key: ByteArray,
        plaintext: ByteArray,
        aad: ByteArray,
        nonce12: ByteArray
    ): EncryptionResult =
        EncryptionUtils.encryptAesGcm(key, plaintext, aad, nonce12)

    fun decryptWithAAD(
        key: ByteArray,
        ciphertext: ByteArray,
        aad: ByteArray,
        nonce12: ByteArray
    ): ByteArray? =
        EncryptionUtils.decryptAesGcm(key, ciphertext, aad, nonce12)

    // -------- Compression --------
    fun compressData(data: ByteArray): ByteArray = CompressionUtils.compress(data)
    fun decompressData(data: ByteArray, expectedSize: Int): ByteArray =
        CompressionUtils.decompress(data, expectedSize)

    // -------- Nonces --------
    fun generateNonce(sessionId: Int, deviceId: Int, sequence: Int): ByteArray {
        // Deterministic 12-byte nonce from (sessionId | deviceId | sequence)
        val buf = ByteBuffer.allocate(12)
        buf.putInt(sessionId)
        buf.putInt(deviceId)
        buf.putInt(sequence)
        return buf.array()
    }

    fun generateSecureNonce(): ByteArray = EncryptionUtils.randomBytes(12)

    // -------- Memory hygiene --------
    fun secureWipe(bytes: ByteArray) {
        for (i in bytes.indices) bytes[i] = 0
    }

    companion object {
        private const val TAG = "CryptoManager"
    }
}
