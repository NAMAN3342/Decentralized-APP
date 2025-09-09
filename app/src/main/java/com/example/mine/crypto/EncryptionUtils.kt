package com.example.mine.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptionResult(
    val ciphertext: ByteArray,
    /** For AES-GCM this is the nonce/IV used. */
    val iv: ByteArray
)

object EncryptionUtils {
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private val rng = SecureRandom()

    fun randomBytes(len: Int): ByteArray =
        ByteArray(len).also { rng.nextBytes(it) }

    fun encryptAesGcm(
        key: ByteArray,
        plaintext: ByteArray,
        aad: ByteArray = ByteArray(0),
        nonce12: ByteArray
    ): EncryptionResult {
        val secret = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance(AES_GCM)
        val spec = GCMParameterSpec(GCM_TAG_BITS, nonce12)
        cipher.init(Cipher.ENCRYPT_MODE, secret, spec)
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        val ct = cipher.doFinal(plaintext)
        return EncryptionResult(ciphertext = ct, iv = nonce12)
    }

    fun decryptAesGcm(
        key: ByteArray,
        ciphertext: ByteArray,
        aad: ByteArray = ByteArray(0),
        nonce12: ByteArray
    ): ByteArray? {
        return try {
            val secret = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance(AES_GCM)
            val spec = GCMParameterSpec(GCM_TAG_BITS, nonce12)
            cipher.init(Cipher.DECRYPT_MODE, secret, spec)
            if (aad.isNotEmpty()) cipher.updateAAD(aad)
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            null
        }
    }
}

