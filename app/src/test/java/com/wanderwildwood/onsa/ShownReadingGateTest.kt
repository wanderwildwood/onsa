package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.notedetection.TuningState
import com.wanderwildwood.onsa.notenames.BaseNote
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.notenames.NoteModifier
import com.wanderwildwood.onsa.ui.tuning.ShownReading
import com.wanderwildwood.onsa.ui.tuning.ShownReadingGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ShownReadingGateTest {
    private val a4 = MusicalNote(BaseNote.A, NoteModifier.None, 4)
    private val e4 = MusicalNote(BaseNote.E, NoteModifier.None, 4)

    private fun hz(cents: Float) = 440f * 2f.pow(cents / 1200f)
    private fun reading(cents: Float, note: MusicalNote = a4) = ShownReading.of(
        note,
        if (kotlin.math.abs(cents) <= 5f) TuningState.InTune else TuningState.TooHigh,
        hz(cents), 440f
    )

    /** Runs [seconds] of readings at the detector's default rate (44100 / (4096 * 0.25), about
     * 43 a second), with the clock ticking every [ShownReadingGate.INTERVAL_MS], and returns
     * how many times the screen would have been told to redraw. */
    private fun redraws(seconds: Float, centsAt: (Float) -> Float): Int {
        val gate = ShownReadingGate(ShownReading(a4, TuningState.Unknown, null))
        val dt = 4096 * 0.25f / 44100f
        var t = 0f
        var nextTick = ShownReadingGate.INTERVAL_MS / 1000f
        var count = 0
        while (t < seconds) {
            gate.offer(reading(centsAt(t)))
            t += dt
            if (t >= nextTick) {
                if (gate.take() != null) count++
                nextTick += ShownReadingGate.INTERVAL_MS / 1000f
            }
        }
        return count
    }

    @Test fun centsAreRoundedToWholeOnes() {
        assertEquals(3, reading(3.2f).cents)
        assertEquals(reading(3.1f), reading(3.4f))
        assertNull(ShownReading.of(a4, TuningState.Unknown, 440f, 440f).cents)
    }

    @Test fun aHeldNoteDrawsOnceAndThenNothing() {
        // Wobbling by a third of a cent around +2 cents for ten seconds.
        val n = redraws(10f) { t -> 2f + 0.3f * kotlin.math.sin(t * 20f) }
        assertEquals(1, n)
    }

    @Test fun aMovingNoteDrawsAtMostTwiceASecond() {
        // Sliding 30 cents a second, which changes the whole-cent value at every reading.
        val n = redraws(10f) { t -> -150f + 30f * t }
        assertTrue("$n redraws in 10 s", n in 15..20)
    }

    @Test fun nothingNewNothingDrawn() {
        val gate = ShownReadingGate(reading(0f))
        gate.offer(reading(0.2f))
        assertNull(gate.take())
        gate.offer(reading(1f))
        assertEquals(1, gate.take()?.cents)
        assertNull(gate.take())
    }

    @Test fun aNewTargetNoteIsAChange() {
        val gate = ShownReadingGate(reading(0f))
        gate.offer(reading(0f, e4))
        assertEquals(e4, gate.take()?.targetNote)
    }

    @Test fun aTapIsShownAtOnceAndNotAgainOnTheTick() {
        val gate = ShownReadingGate(reading(0f))
        gate.showNow(reading(0f, e4))
        assertNull(gate.take())
    }
}
