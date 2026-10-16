package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.ui.plot.TickLevelVisibleRanges
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class PitchLabelThinningTest {
    // Equal temperament from C0, A4 = 440 Hz, with the same coarse-to-fine steps as the plot.
    private val c0 = 440f * 2f.pow(-57f / 12f)
    private fun note(i: Int) = c0 * 2f.pow(i / 12f)
    private val steps = listOf(12, 6, 3, 2, 1)
    private val levels = TickLevelVisibleRanges(
        steps.map { step -> FloatArray(108 / step) { note(it * step) } }.toImmutableList()
    )
    private val a4 = 57
    private val d2 = 26

    @Test
    fun zoomedOnOneNoteEveryNoteIsLabelled() {
        // The usual view: a semitone and a half either side of A4, labels a third of a semitone.
        val label = (note(a4 + 1) - note(a4)) / 3f
        val range = levels.getTicksRange(note(a4) - 40f, note(a4) + 40f, 3, label)
        assertEquals(steps.size - 1, range.level)
    }

    @Test
    fun stretchedFromD2toA4TheNamesThinOut() {
        // A guitar played against an A4 target: about 370 Hz in a short plot, so a label is
        // taller than the semitones around D2 and the plot falls back to fewer notes.
        val label = 20f
        val range = levels.getTicksRange(note(d2), note(a4), 3, label)
        val step = steps[range.level]
        assert(step > 1) { "every note still labelled" }
        for (i in range.indexBegin + 1 until range.indexEnd) {
            val gap = levels.getTickValue(range.level, i) - levels.getTickValue(range.level, i - 1)
            assert(gap >= label) { "labels $i overlap: $gap Hz apart" }
        }
    }

    @Test
    fun theWidestStepIsUsedWhenNothingElseFits() {
        val range = levels.getTicksRange(note(0), note(100), 3, 1000f)
        assertEquals(0, range.level)
    }
}
