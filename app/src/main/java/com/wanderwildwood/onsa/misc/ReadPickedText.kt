package com.wanderwildwood.onsa.misc

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Largest file Load from file reads; instruments, temperaments and stretch tunings are far smaller. */
const val MAX_PICKED_TEXT_BYTES = 1_000_000

/** Reads a picked text file whole.
 * @return The text, or null when the file cannot be opened or read (a provider that throws,
 *   a document gone or no longer allowed) or is larger than [maxBytes].
 */
fun readPickedText(context: Context, uri: Uri, maxBytes: Int = MAX_PICKED_TEXT_BYTES): String? {
    return try {
        context.contentResolver?.openInputStream(uri)?.use { readCappedText(it, maxBytes) }
    } catch (e: Exception) {
        null
    }
}

/** Reads [stream] as UTF-8 text, or returns null as soon as it holds more than [maxBytes]. */
fun readCappedText(stream: InputStream, maxBytes: Int): String? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val n = stream.read(buffer)
        if (n < 0)
            break
        if (out.size() + n > maxBytes)
            return null
        out.write(buffer, 0, n)
    }
    return out.toString(Charsets.UTF_8.name())
}
