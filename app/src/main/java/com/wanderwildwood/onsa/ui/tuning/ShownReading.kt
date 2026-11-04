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

/**
 * Between the pitch detection, which runs many times a second, and the screen.
 *
 * Every reading is [offer]ed, and only the latest is kept. A clock calls [take] every
 * [INTERVAL_MS]; it hands back the latest reading only when it differs from the one last
 * shown. So a held note draws nothing more, and a moving one at most twice a second: each
 * redraw on an e-ink panel is a visible flash, and a tuner needs no more to be read.
 */
class ShownReadingGate<T>(initial: T) {
    private var shown = initial
    private var latest = initial

    fun offer(reading: T) {
        latest = reading
    }

    /** The reading to draw now, or null when the screen already shows it. */
    fun take(): T? {
        if (latest == shown)
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
    }
}
