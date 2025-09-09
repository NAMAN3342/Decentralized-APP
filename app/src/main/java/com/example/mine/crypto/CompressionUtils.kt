package com.example.mine.crypto

import java.util.zip.Deflater
import java.util.zip.Inflater

object CompressionUtils {

    fun compress(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val deflater = Deflater(Deflater.BEST_SPEED, /*nowrap*/true)
        deflater.setInput(data)
        deflater.finish()

        val out = ByteArray(data.size + data.size / 3 + 64)
        var total = 0
        while (!deflater.finished()) {
            total += deflater.deflate(out, total, out.size - total)
        }
        deflater.end()
        return out.copyOf(total)
    }

    fun decompress(data: ByteArray, expectedSize: Int): ByteArray {
        if (data.isEmpty()) return data
        val inflater = Inflater(/*nowrap*/true)
        inflater.setInput(data)

        val out = ByteArray(expectedSize)
        var total = 0
        while (!inflater.finished() && total < expectedSize) {
            total += inflater.inflate(out, total, out.size - total)
            if (inflater.needsInput()) break
        }
        inflater.end()
        return if (total == expectedSize) out else out.copyOf(total)
    }
}
