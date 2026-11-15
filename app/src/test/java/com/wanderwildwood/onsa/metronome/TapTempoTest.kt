package com.wanderwildwood.onsa.metronome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TapTempoTest {
    @Test
    fun twoTapsGiveATempo() {
        val tap = TapTempo()
        assertNull(tap.tap(10_000))
        assertEquals(120, tap.tap(10_500))
    }

    @Test
    fun aPauseOfMoreThanTwoSecondsStartsAfresh() {
        val tap = TapTempo()
        tap.tap(10_000)
        tap.tap(10_500)
        assertNull(tap.tap(13_000))
    }

    @Test
    fun tapsBeforeAResetAreNeverCountedWithTapsAfterIt() {
        // Taps, then the tempo set in the dialog, then one more tap within two seconds: that
        // tap alone must not set a tempo out of the earlier ones.
        val tap = TapTempo()
        tap.tap(10_000)
        tap.tap(10_400)
        tap.reset()
        assertNull(tap.tap(11_000))
        assertEquals(60, tap.tap(12_000))
    }
}
