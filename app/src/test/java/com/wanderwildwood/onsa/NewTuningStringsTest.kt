package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.instruments.InstrumentGroup
import com.wanderwildwood.onsa.instruments.instrumentDatabase
import com.wanderwildwood.onsa.instruments.stringsForNewTuning
import com.wanderwildwood.onsa.notenames.BaseNote
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.notenames.NoteModifier
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

/** A new tuning starts with as many strings as its instrument has. */
class NewTuningStringsTest {

    private val a4 = arrayOf(MusicalNote(BaseNote.A, NoteModifier.None, octave = 4))

    @Test
    fun eachInstrumentGivesItsOwnStringCount() {
        val expected = mapOf(
            InstrumentGroup.guitar to 6,
            InstrumentGroup.bass to 4,
            InstrumentGroup.ukulele to 4,
            InstrumentGroup.violin to 4,
            InstrumentGroup.viola to 4,
            InstrumentGroup.cello to 4,
            InstrumentGroup.double_bass to 4,
            InstrumentGroup.banjo to 5,
        )
        assertEquals(InstrumentGroup.entries.toSet(), expected.keys)
        for ((group, count) in expected) {
            assertEquals(group.name, count, stringsForNewTuning(group, a4).size)
        }
    }

    @Test
    fun theStringsAreTheInstrumentsFirstTuning() {
        val openG = instrumentDatabase.first { it.group == InstrumentGroup.banjo }
        assertEquals("Banjo, open G", openG.getNameString(null))
        assertArrayEquals(openG.strings, stringsForNewTuning(InstrumentGroup.banjo, a4))
    }

    @Test
    fun theBuiltInTuningIsNotSharedWithTheNewOne() {
        val openG = instrumentDatabase.first { it.group == InstrumentGroup.banjo }
        assertNotSame(openG.strings, stringsForNewTuning(InstrumentGroup.banjo, a4))
    }

    @Test
    fun noInstrumentKeepsTheFallback() {
        assertArrayEquals(a4, stringsForNewTuning(null, a4))
        assertArrayEquals(a4, stringsForNewTuning(InstrumentGroup.banjo, a4, database = emptyList()))
    }
}
