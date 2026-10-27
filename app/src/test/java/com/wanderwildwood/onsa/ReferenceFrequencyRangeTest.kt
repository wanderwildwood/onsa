package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.ui.preferences.referenceFrequencyRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceFrequencyRangeTest {
    @Test
    fun concertPitchAllows400To480() {
        val range = referenceFrequencyRange(440f)
        assertEquals(400f, range.start, 1e-3f)
        assertEquals(480f, range.endInclusive, 1e-3f)
        assertTrue(440f in range)
        assertTrue(432f in range)
        assertTrue(466f in range)
    }

    @Test
    fun absurdValuesAreOutside() {
        val range = referenceFrequencyRange(440f)
        assertFalse(4400f in range)
        assertFalse(44f in range)
        assertFalse(0f in range)
    }

    @Test
    fun otherNotesScaleWithTheirPitch() {
        // C4 is about 261.63 Hz when A4 is 440 Hz.
        val range = referenceFrequencyRange(261.63f)
        assertTrue(261.63f in range)
        assertFalse(440f in range)
        assertEquals(261.63f * 400f / 440f, range.start, 1e-3f)
    }
}
