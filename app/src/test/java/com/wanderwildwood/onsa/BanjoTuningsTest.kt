package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.instruments.InstrumentIcon
import com.wanderwildwood.onsa.instruments.instrumentDatabase
import com.wanderwildwood.onsa.musicalscale.MusicalScale2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Every banjo row resolves in the default scale, and to the pitch a banjo player expects. */
class BanjoTuningsTest {
    // Equal temperament, A4 = 440 Hz. Fifth string first.
    private val g4 = 392.00f; private val fSharp4 = 369.99f; private val f4 = 349.23f
    private val a4 = 440.00f; private val e4 = 329.63f; private val eFlat4 = 311.13f
    private val d4 = 293.66f; private val cSharp4 = 277.18f; private val c4 = 261.63f
    private val b3 = 246.94f; private val bFlat3 = 233.08f; private val a3 = 220.00f
    private val g3 = 196.00f; private val fSharp3 = 185.00f; private val f3 = 174.61f
    private val e3 = 164.81f; private val d3 = 146.83f; private val c3 = 130.81f

    private val expected = mapOf(
        "Banjo, open G" to listOf(g4, d3, g3, b3, d4),
        "Banjo, G minor" to listOf(g4, d3, g3, bFlat3, d4),
        "Banjo, sawmill" to listOf(g4, d3, g3, c4, d4),
        "Banjo, double C" to listOf(g4, c3, g3, c4, d4),
        "Banjo, open C" to listOf(g4, c3, g3, c4, e4),
        "Banjo, drop C" to listOf(g4, c3, g3, b3, d4),
        "Banjo, C minor" to listOf(g4, c3, g3, c4, eFlat4),
        "Banjo, open D" to listOf(fSharp4, d3, fSharp3, a3, d4),
        "Banjo, D minor" to listOf(f4, d3, f3, a3, d4),
        "Banjo, double D" to listOf(a4, d3, a3, d4, e4),
        "Banjo, open A" to listOf(a4, e3, a3, cSharp4, e4),
    )

    @Test
    fun banjoTuningsResolveToTheRightPitches() {
        val scale = MusicalScale2.createTestEdo12()
        val banjos = instrumentDatabase.filter { it.icon == InstrumentIcon.banjo }
        assertEquals(expected.keys.toList(), banjos.map { it.getNameString(null) })

        for (banjo in banjos) {
            val name = banjo.getNameString(null)
            val pitches = expected.getValue(name)
            assertEquals(name, 5, banjo.strings.size)
            banjo.strings.forEachIndexed { i, note ->
                // The tuner looks a selected string up exactly, enharmonics included, so a
                // note spelled differently from the scale's own would find nothing.
                val index = scale.getNoteIndex2(note)
                assertNotEquals("$name string ${5 - i}: $note is not in the scale", Int.MAX_VALUE, index)
                assertEquals("$name string ${5 - i}", pitches[i], scale.getNoteFrequency(index), 0.02f)
                assertEquals("$name string ${5 - i}", 1, scale.getMatchingNoteIndices(note).size)
            }
        }
    }

    @Test
    fun existingStableIdsDidNotMove() {
        // The banjo rows come after the nine that were there before them.
        assertEquals("Double bass", instrumentDatabase[8].getNameString(null))
        assertEquals(-9L, instrumentDatabase[8].stableId)
        assertEquals(InstrumentIcon.banjo, instrumentDatabase[9].icon)
    }
}
