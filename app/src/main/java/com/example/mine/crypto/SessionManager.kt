package com.example.mine.crypto

import android.util.Log
import java.security.KeyPair
import java.security.PublicKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class SessionManager(private val cryptoManager: CryptoManager) {

    companion object {
        private const val TAG = "SessionManager"
        private const val MAX_SESSION_AGE_HOURS = 24
        private const val MAX_MESSAGES_PER_SESSION = 1_000_000
        private const val REPLAY_WINDOW_SIZE = 64
    }

    private val sessions = ConcurrentHashMap<Int, Session>()
    private val sessionCounter = AtomicInteger(Random.nextInt())
    private val deviceId = Random.nextInt()

    // ---- Session lifecycle ---------------------------------------------------

    fun createSession(peerPublicKey: PublicKey): Session {
        val sessionId = sessionCounter.incrementAndGet()
        val session = Session(
            id = sessionId,
            peerPublicKey = peerPublicKey,
            createdAt = System.currentTimeMillis(),
            sendCounter = AtomicInteger(0),
            receiveCounter = AtomicInteger(0),
            replayWindow = ReplayWindow(REPLAY_WINDOW_SIZE)
        )
        sessions[sessionId] = session
        Log.d(TAG, "Created session $sessionId")
        return session
    }

    fun getOrCreateSession(peerPublicKey: PublicKey): Session {
        val existing = sessions.values.find { it.peerPublicKey == peerPublicKey && !it.isExpired() }
        return existing ?: createSession(peerPublicKey)
    }

    fun establishSessionKeys(
        session: Session,
        ourKeyPair: KeyPair,
        peerPublicKey: PublicKey
    ): Boolean = try {
        val shared = cryptoManager.computeSharedSecret(ourKeyPair.private, peerPublicKey)
        val salt = generateHandshakeSalt(session, ourKeyPair, peerPublicKey)
        val keys = cryptoManager.deriveSessionKeys(shared, salt)
        session.txKey = keys.kTx
        session.rxKey = keys.kRx
        session.isEstablished = true
        cryptoManager.secureWipe(shared)
        Log.d(TAG, "Session keys established for ${session.id}")
        true
    } catch (e: Exception) {
        Log.e(TAG, "establishSessionKeys failed", e)
        false
    }

    // ---- Encrypt (simplified payload path) ----------------------------------

    fun encryptMessage(session: Session, message: String, destinationId: Int): ByteArray? {
        if (!session.isEstablished) {
            Log.e(TAG, "Session ${session.id} not established")
            return null
        }
        return try {
            val raw = message.toByteArray()
            val compressed = cryptoManager.compressData(raw)
            val isCompressed = compressed.size < raw.size
            val payload = if (isCompressed) {
                Payload(ContentType.TEXT, true, raw.size, compressed)
            } else {
                Payload(ContentType.TEXT, false, raw.size, raw)
            }
            encryptPayload(session, payload, destinationId, isCompressed)
        } catch (e: Exception) {
            Log.e(TAG, "encryptMessage failed", e)
            null
        }
    }

    fun encryptMessageSecure(session: Session, message: String, destinationId: Int): ByteArray? {
        if (!session.isEstablished) {
            Log.e(TAG, "Session ${session.id} not established")
            return null
        }
        return try {
            require(message.isNotEmpty()) { "Message cannot be empty" }

            val raw = message.toByteArray()
            val compressed = cryptoManager.compressData(raw)
            val isCompressed = compressed.size < raw.size
            val payload = if (isCompressed) {
                Payload(ContentType.TEXT, true, raw.size, compressed)
            } else {
                Payload(ContentType.TEXT, false, raw.size, raw)
            }

            val sequence = session.sendCounter.incrementAndGet()
            val nonce = cryptoManager.generateSecureNonce()
            val aad = createSimpleAAD(session, destinationId, isCompressed)
            val txKey = session.txKey ?: error("Session not established")

            val result = cryptoManager.encryptWithAAD(
                txKey,
                payload.toByteArray(),
                aad,
                nonce
            )
            Log.d(TAG, "Encrypted (secure nonce) seq=$sequence sid=${session.id}")
            result.ciphertext
        } catch (e: Exception) {
            Log.e(TAG, "encryptMessageSecure failed: ${e.message}", e)
            null
        }
    }

    // ---- Decrypt -------------------------------------------------------------

    fun decryptMessage(session: Session, frame: Frame): String? {
        if (!session.isEstablished) return null
        return try {
            if (!session.replayWindow.checkAndAdd(frame.sequence)) {
                Log.w(TAG, "Replay detected seq=${frame.sequence}")
                return null
            }
            val rxKey = session.rxKey ?: error("Session not established")
            val aad = frame.getAAD()
            val plain = cryptoManager.decryptWithAAD(rxKey, frame.ciphertext, aad, frame.nonce)
                ?: return null
            val payload = Payload.fromByteArray(plain) ?: return null
            val finalBytes = if (payload.isCompressed) {
                cryptoManager.decompressData(payload.data, payload.originalSize)
            } else payload.data
            String(finalBytes)
        } catch (e: Exception) {
            Log.e(TAG, "decryptMessage(frame) failed", e)
            null
        }
    }

    fun decryptMessage(session: Session, encryptedData: ByteArray): String? {
        if (!session.isEstablished) return null
        return try {
            require(encryptedData.isNotEmpty()) { "Encrypted data cannot be empty" }

            // Legacy parse
            Frame.fromByteArray(encryptedData)?.let { return decryptMessage(session, it) }

            // Simplified: empty AAD + deterministic nonce by receive counter
            val rxKey = session.rxKey ?: error("Session not established")
            val sequence = session.receiveCounter.incrementAndGet()
            val nonce = cryptoManager.generateNonce(session.id, deviceId, sequence)
            val plain = cryptoManager.decryptWithAAD(rxKey, encryptedData, ByteArray(0), nonce)
                ?: return null

            Payload.fromByteArray(plain)?.let { pl ->
                val bytes = if (pl.isCompressed)
                    cryptoManager.decompressData(pl.data, pl.originalSize)
                else pl.data
                return String(bytes)
            }

            String(plain)
        } catch (e: Exception) {
            Log.e(TAG, "decryptMessage(bytes) failed: ${e.message}", e)
            null
        }
    }

    // ---- Helpers -------------------------------------------------------------

    private fun encryptPayload(
        session: Session,
        payload: Payload,
        destinationId: Int,
        isCompressed: Boolean
    ): ByteArray {
        val sequence = session.sendCounter.incrementAndGet()
        val nonce = cryptoManager.generateNonce(session.id, deviceId, sequence)
        val aad = createSimpleAAD(session, destinationId, isCompressed)
        val txKey = session.txKey ?: error("Session not established")
        val result = cryptoManager.encryptWithAAD(txKey, payload.toByteArray(), aad, nonce)
        return result.ciphertext
    }

    private fun createAAD(session: Session, destinationId: Int, sequence: Int, isCompressed: Boolean): ByteArray {
        val buf = java.nio.ByteBuffer.allocate(20).order(java.nio.ByteOrder.BIG_ENDIAN)
        buf.put(1) // version
        buf.put(FrameType.DATA.value)
        buf.put(if (isCompressed) FrameFlags.COMPRESSED.value else 0)
        buf.put(Frame.FRAME_HEADER_SIZE.toByte())
        buf.putInt(deviceId)
        buf.putInt(destinationId)
        buf.putInt(session.id)
        buf.putInt(sequence)
        buf.put(32) // TTL
        return buf.array()
    }

    private fun createSimpleAAD(session: Session, destinationId: Int, isCompressed: Boolean): ByteArray {
        val buf = java.nio.ByteBuffer.allocate(12).order(java.nio.ByteOrder.BIG_ENDIAN)
        buf.putInt(session.id)
        buf.putInt(destinationId)
        buf.putInt(if (isCompressed) 1 else 0)
        return buf.array()
    }

    private fun generateHandshakeSalt(
        session: Session,
        ourKeyPair: KeyPair,
        peerPublicKey: PublicKey
    ): ByteArray {
        val a = ourKeyPair.public.encoded
        val b = peerPublicKey.encoded
        val buf = java.nio.ByteBuffer.allocate(8 + 4 + a.size + b.size)
        buf.putLong(session.createdAt)
        buf.putInt(session.id)
        buf.put(a)
        buf.put(b)
        return buf.array()
    }

    fun needsRekey(session: Session): Boolean {
        return session.sendCounter.get() > MAX_MESSAGES_PER_SESSION ||
                System.currentTimeMillis() - session.createdAt > MAX_SESSION_AGE_HOURS * 60L * 60L * 1000L
    }

    fun cleanupExpiredSessions() {
        val expired = sessions.values.filter { it.isExpired() }
        expired.forEach { s ->
            sessions.remove(s.id)
            s.txKey?.let { cryptoManager.secureWipe(it) }
            s.rxKey?.let { cryptoManager.secureWipe(it) }
            Log.d(TAG, "Cleaned expired session ${s.id}")
        }
    }

    fun getSession(sessionId: Int): Session? = sessions[sessionId]
    fun getActiveSessions(): List<Session> = sessions.values.filter { !it.isExpired() }
}

// ---- Session + helpers ------------------------------------------------------

data class Session(
    val id: Int,
    val peerPublicKey: PublicKey,
    val createdAt: Long,
    val sendCounter: AtomicInteger,
    val receiveCounter: AtomicInteger,
    val replayWindow: ReplayWindow,
    var txKey: ByteArray? = null,
    var rxKey: ByteArray? = null,
    var isEstablished: Boolean = false
) {
    fun isExpired(): Boolean =
        System.currentTimeMillis() - createdAt > 24 * 60 * 60 * 1000 // 24h

    override fun equals(other: Any?): Boolean =
        other is Session && other.id == id

    override fun hashCode(): Int = id
}

class ReplayWindow(private val windowSize: Int) {
    private val received = mutableSetOf<Int>()
    private var highest = -1

    fun checkAndAdd(sequence: Int): Boolean {
        if (sequence <= highest - windowSize) return false
        if (!received.add(sequence)) return false
        if (sequence > highest) highest = sequence
        received.removeIf { it <= highest - windowSize }
        return true
    }
}

enum class ContentType(val value: Byte) { TEXT(1), BINARY(2), FILE(3), CONTROL(4) }

data class Payload(
    val contentType: ContentType,
    val isCompressed: Boolean,
    val originalSize: Int,
    val data: ByteArray
) {
    fun toByteArray(): ByteArray {
        val buf = java.nio.ByteBuffer.allocate(4 + data.size)
        buf.put(contentType.value)
        buf.put(if (isCompressed) 1 else 0)
        buf.putShort(originalSize.toShort())
        buf.put(data)
        return buf.array()
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): Payload? = try {
            val buf = java.nio.ByteBuffer.wrap(bytes)
            val ctVal = buf.get()
            val ct = ContentType.values().firstOrNull { it.value == ctVal } ?: return null
            val compressed = buf.get() == 1.toByte()
            val original = buf.short.toInt()
            val payloadData = ByteArray(bytes.size - 4)
            buf.get(payloadData)
            Payload(ct, compressed, original, payloadData)
        } catch (_: Exception) {
            null
        }
    }
}
