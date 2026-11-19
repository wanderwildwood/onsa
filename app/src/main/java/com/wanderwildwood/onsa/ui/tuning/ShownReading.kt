package com.wanderwildwood.onsa.ui.tuning

import com.wanderwildwood.onsa.notedetection.TuningState
import com.wanderwildwood.onsa.notenames.MusicalNote
import kotlin.math.log2
import kotlin.math.roundToInt

/** What the tuner screen shows, as coarse as it is drawn: the note aimed at, whether it is in
 * tune, and the distance from it in whole cents (null while nothing is heard). Two readings
 * that are equal look the same on the panel. */
data class ShownReading(
    val targetNote: MusicalNote,
    val tuningState: TuningState,
    val cents: Int?
) {
    companion object {
        fun of(
            targetNote: MusicalNote,
            tuningState: TuningState,
            frequency: Float,
            targetFrequency: Float
        ) = ShownReading(
            targetNote,
            tuningState,
            if (tuningState == TuningState.Unknown || frequency <= 0f || targetFrequency <= 0f)
                null
            else
                (1200f * log2(frequency / targetFrequency)).roundToInt()
        )
    }
}

/** Two readings close enough that redrawing one as the other would only flicker: the same note
 * and state, and cents within [ShownReadingGate.CENTS_DEADBAND] of each other. */
fun ShownReading.looksLike(other: ShownReading): Boolean {
    if (targetNote != other.targetNote || tuningState != other.tuningState)
        return false
    val a = cents
    val b = other.cents
    if (a == null || b == null)
        return a == b
    return kotlin.math.abs(a - b) < ShownReadingGate.CENTS_DEADBAND
}

/**
 * The frequencies heard since the last redraw. The detector wobbles a few cents from one
 * reading to the next even on a steady note, so the screen shows the median of the half
 * second instead of whichever reading happened to come last.
 */
class ReadingWindow {
    private val frequencies = ArrayList<Float>()

    fun add(frequency: Float) {
        if (frequency > 0f)
            frequencies.add(frequency)
    }

    fun clear() = frequencies.clear()

    /** The median of what was added since the last [clear], or null if nothing was. */
    fun median(): Float? {
        if (frequencies.isEmpty())
            return null
        val sorted = frequencies.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else 0.5f * (sorted[mid - 1] + sorted[mid])
    }
}

/**
 * Between the pitch detection, which runs many times a second, and the screen.
 *
 * A clock [offer]s a reading every [INTERVAL_MS] (the median of a [ReadingWindow]) and calls
 * [take]; it hands back the reading only when it differs from the one last shown, by [same].
 * So a held note draws nothing more, and a moving one at most twice a second: each redraw on an e-ink panel is a visible flash, and a tuner needs no more to be read.
 */
class ShownReadingGate<T>(
    initial: T,
    private val same: (shown: T, new: T) -> Boolean = { a, b -> a == b }
) {
    private var shown = initial
    private var latest = initial

    fun offer(reading: T) {
        latest = reading
    }

    /** The reading to draw now, or null when the screen already shows it. */
    fun take(): T? {
        if (same(shown, latest))
            return null
        shown = latest
        return latest
    }

    /** Shown at once, outside the clock: for a change the player asked for with a tap. */
    fun showNow(reading: T) {
        latest = reading
        shown = reading
    }

    companion object {
        const val INTERVAL_MS = 500L

        /** Cents the shown reading must move before it is redrawn: less than this is the
         * detector's own wobble, not the string. */
        const val CENTS_DEADBAND = 3
    }
}
