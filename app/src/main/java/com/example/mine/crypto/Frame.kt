package com.example.mine.crypto

import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class FrameType(val value: Byte) { DATA(1) }
enum class FrameFlags(val value: Byte) { COMPRESSED(1) }

/**
 * Legacy transport frame (kept for backward compatibility).
 * New path can send raw AES-GCM bytes without this wrapper.
 */
data class Frame(
    val type: Byte,
    val flags: Byte,
    val headerLength: Byte,
    val sourceId: Int,
    val destinationId: Int,
    val sessionId: Int,
    val sequence: Int,
    val ttl: Byte,
    val nonce: ByteArray,       // 12 bytes for GCM
    val ciphertext: ByteArray,  // includes GCM tag at end
    val tag: ByteArray          // kept for legacy (stores IV/nonce)
) {
    fun getAAD(): ByteArray {
        val buf = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)
        buf.put(1) // version
        buf.put(type)
        buf.put(flags)
        buf.put(headerLength)
        buf.putInt(sourceId)
        buf.putInt(destinationId)
        buf.putInt(sessionId)
        buf.putInt(sequence)
        buf.put(ttl)
        return buf.array()
    }

    fun toByteArray(): ByteArray {
        val header = ByteBuffer.allocate(FRAME_HEADER_SIZE + 12 + 2)
            .order(ByteOrder.BIG_ENDIAN)
        header.put(type)
        header.put(flags)
        header.put(headerLength)
        header.putInt(sourceId)
        header.putInt(destinationId)
        header.putInt(sessionId)
        header.putInt(sequence)
        header.put(ttl)
        header.putShort(ciphertext.size.toShort())
        header.put(nonce)
        header.putShort(tag.size.toShort())

        val out = ByteArray(header.position() + tag.size + ciphertext.size)
        System.arraycopy(header.array(), 0, out, 0, header.position())
        System.arraycopy(tag, 0, out, header.position(), tag.size)
        System.arraycopy(ciphertext, 0, out, header.position() + tag.size, ciphertext.size)
        return out
    }

    companion object {
        const val FRAME_HEADER_SIZE: Int = 1 /*type*/ + 1 /*flags*/ + 1 /*hlen*/ +
                4 /*src*/ + 4 /*dst*/ + 4 /*sid*/ + 4 /*seq*/ + 1 /*ttl*/

        fun fromByteArray(bytes: ByteArray): Frame? = try {
            val hdr = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            val type = hdr.get()
            val flags = hdr.get()
            val hlen = hdr.get()
            val src = hdr.int
            val dst = hdr.int
            val sid = hdr.int
            val seq = hdr.int
            val ttl = hdr.get()
            val ctLen = hdr.short.toInt()
            val nonce = ByteArray(12).also { hdr.get(it) }
            val tagLen = hdr.short.toInt()

            val tag = ByteArray(tagLen)
            hdr.get(tag)

            val ct = ByteArray(ctLen)
            hdr.get(ct)

            Frame(
                type = type,
                flags = flags,
                headerLength = hlen,
                sourceId = src,
                destinationId = dst,
                sessionId = sid,
                sequence = seq,
                ttl = ttl,
                nonce = nonce,
                ciphertext = ct,
                tag = tag
            )
        } catch (_: Exception) {
            null
        }
    }
}
