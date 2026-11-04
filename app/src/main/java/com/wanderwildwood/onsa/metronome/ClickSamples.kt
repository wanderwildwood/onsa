package com.wanderwildwood.onsa.metronome

import android.content.Context
import androidx.annotation.RawRes
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Reads a 16-bit PCM mono WAV from res/raw into floats from -1 to 1. Only the sounds bundled
 * with the app pass through here, so anything else is refused rather than guessed at.
 */
internal fun readClick(context: Context, @RawRes raw: Int): FloatArray {
    val bytes = context.resources.openRawResource(raw).use { it.readBytes() }
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    require(bytes.size > 12 && String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WAVE")
    var pos = 12
    var channels = 0
    var bits = 0
    while (pos + 8 <= bytes.size) {
        val id = String(bytes, pos, 4)
        val size = buf.getInt(pos + 4)
        val body = pos + 8
        when (id) {
            "fmt " -> {
                channels = buf.getShort(body + 2).toInt()
                bits = buf.getShort(body + 14).toInt()
            }
            "data" -> {
                require(channels == 1 && bits == 16)
                val n = minOf(size, bytes.size - body) / 2
                return FloatArray(n) { i -> buf.getShort(body + 2 * i) / 32768f }
            }
        }
        pos = body + size + (size and 1)
    }
    error("no data chunk")
}
