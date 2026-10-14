package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.instruments.Instrument
import com.wanderwildwood.onsa.instruments.InstrumentGroup
import com.wanderwildwood.onsa.instruments.InstrumentIO
import com.wanderwildwood.onsa.instruments.InstrumentIcon
import com.wanderwildwood.onsa.instruments.instrumentDatabase
import com.wanderwildwood.onsa.musicalscale.MusicalScale2
import com.wanderwildwood.onsa.notenames.BaseNote
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.notenames.NoteModifier
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentGroupsTest {
    // Equal temperament, A4 = 440 Hz. Lowest string first, as the guitar is listed.
    private val d2 = 73.42f; private val dSharp2 = 77.78f; private val e2 = 82.41f
    private val g2 = 98.00f; private val gSharp2 = 103.83f; private val a2 = 110.00f
    private val b2 = 123.47f; private val c3 = 130.81f; private val cSharp3 = 138.59f
    private val d3 = 146.83f; private val e3 = 164.81f; private val f3 = 174.61f
    private val fSharp3 = 185.00f; private val g3 = 196.00f; private val gSharp3 = 207.65f
    private val a3 = 220.00f; private val aSharp3 = 233.08f; private val b3 = 246.94f
    private val d4 = 293.66f; private val dSharp4 = 311.13f; private val e4 = 329.63f

    private val guitar = mapOf(
        "6-string guitar" to listOf(e2, a2, d3, g3, b3, e4),
        "Guitar, drop D" to listOf(d2, a2, d3, g3, b3, e4),
        "Guitar, D standard" to listOf(d2, g2, c3, f3, a3, d4),
        "Guitar, half-step down" to listOf(dSharp2, gSharp2, cSharp3, fSharp3, aSharp3, dSharp4),
        "Guitar, DADGAD" to listOf(d2, a2, d3, g3, a3, d4),
        "Guitar, open D" to listOf(d2, a2, d3, fSharp3, a3, d4),
        "Guitar, open G" to listOf(d2, g2, d3, g3, b3, d4),
        "Guitar, open E" to listOf(e2, b2, e3, gSharp3, b3, e4),
    )

    @Test
    fun guitarTuningsResolveToTheRightPitches() {
        val scale = MusicalScale2.createTestEdo12()
        val guitars = instrumentDatabase.filter { it.group == InstrumentGroup.guitar }
        assertEquals(guitar.keys.toList(), guitars.map { it.getNameString(null) })
        for (tuning in guitars) {
            val name = tuning.getNameString(null)
            assertEquals(name, InstrumentIcon.guitar, tuning.icon)
            tuning.strings.forEachIndexed { i, note ->
                val index = scale.getNoteIndex2(note)
                assertNotEquals("$name string $i: $note is not in the scale", Int.MAX_VALUE, index)
                assertEquals("$name string $i", guitar.getValue(name)[i], scale.getNoteFrequency(index), 0.02f)
            }
        }
    }

    @Test
    fun everyBuiltInTuningButChromaticHasItsInstrument() {
        for (instrument in instrumentDatabase) {
            val name = instrument.getNameString(null)
            if (instrument.isChromatic) assertNull(name, instrument.group)
            else assertTrue("$name has no instrument", instrument.group != null)
        }
        assertEquals(2, instrumentDatabase.count { it.group == InstrumentGroup.bass })
        assertEquals(11, instrumentDatabase.count { it.group == InstrumentGroup.banjo })
    }

    @Test
    fun noExistingStableIdMoved() {
        // The guitar's new tunings come after the banjo, which came after the first nine.
        assertEquals(-1L, instrumentDatabase[0].stableId)
        assertEquals("Banjo, open A", instrumentDatabase[19].getNameString(null))
        assertEquals(-20L, instrumentDatabase[19].stableId)
        assertEquals("Guitar, drop D", instrumentDatabase[20].getNameString(null))
        assertEquals(instrumentDatabase.size, instrumentDatabase.map { it.stableId }.toSet().size)
        assertEquals(instrumentDatabase.size, instrumentDatabase.map { it.getNameString(null) }.toSet().size)
    }

    @Test
    fun instrumentRowsNeverCollideWithTunings() {
        for (group in InstrumentGroup.entries) {
            val row = Instrument.groupRow(group)
            assertTrue(row.isGroupRow)
            assertEquals(group, row.group)
            assertFalse(instrumentDatabase.any { it.stableId == row.stableId })
        }
        assertFalse(instrumentDatabase.any { it.isGroupRow })
        assertFalse(sample(42L, null).isGroupRow)
    }

    @Test
    fun aFileKeepsEachTuningsInstrument() {
        val written = InstrumentIO.instrumentsListToString(
            null, listOf(sample(5L, InstrumentGroup.banjo), sample(6L, null))
        )
        val read = InstrumentIO.stringToInstruments(written).instruments
        assertEquals(listOf(InstrumentGroup.banjo, null), read.map { it.group })
    }

    @Test
    fun aFileFromBeforeGroupingReadsAsStandingAlone() {
        // What 0.1.17 wrote is exactly this, without the Group line.
        val written = InstrumentIO.instrumentsListToString(null, listOf(sample(5L, InstrumentGroup.banjo)))
        assertTrue(written.contains("Group=banjo"))
        val old = written.lines().filterNot { it.startsWith("Group=") }.joinToString("\n")
        val read = InstrumentIO.stringToInstruments(old).instruments
        assertEquals(1, read.size)
        assertNull(read[0].group)
    }

    @Test
    fun settingsSavedBeforeGroupingStillRead() {
        // A custom instrument as 0.1.17 saved it: no group field at all.
        val saved = Json.encodeToString(sample(7L, null))
        assertFalse(saved.contains("group"))
        assertNull(Json.decodeFromString<Instrument>(saved).group)
        val grouped = Json.decodeFromString<Instrument>(Json.encodeToString(sample(8L, InstrumentGroup.guitar)))
        assertEquals(InstrumentGroup.guitar, grouped.group)
    }

    private fun sample(id: Long, group: InstrumentGroup?) = Instrument(
        name = "Mine",
        nameResource = null,
        strings = arrayOf(
            MusicalNote(BaseNote.G, NoteModifier.None, 4),
            MusicalNote(BaseNote.D, NoteModifier.None, 3)
        ),
        icon = InstrumentIcon.banjo,
        stableId = id,
        group = group
    )
}
