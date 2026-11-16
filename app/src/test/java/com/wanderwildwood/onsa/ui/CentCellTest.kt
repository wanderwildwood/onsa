package com.wanderwildwood.onsa.ui

import com.wanderwildwood.onsa.notenames.BaseNote
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.notenames.NoteModifier
import com.wanderwildwood.onsa.ui.notes.centCellWidth
import com.wanderwildwood.onsa.ui.notes.headingSigns
import com.wanderwildwood.onsa.ui.notes.modifierSigns
import org.junit.Assert.assertEquals
import org.junit.Test

class CentCellTest {
    @Test
    fun signsOfAModifier() {
        assertEquals(0, modifierSigns(NoteModifier.None))
        assertEquals(1, modifierSigns(NoteModifier.Sharp))
        assertEquals(1, modifierSigns(NoteModifier.NaturalUp))
        assertEquals(2, modifierSigns(NoteModifier.FlatFlat))
        assertEquals(4, modifierSigns(NoteModifier.SharpDownDownDown))
    }

    @Test
    fun twelveNotesKeepTheirWidth() {
        val dSharp = MusicalNote(BaseNote.D, NoteModifier.Sharp, enharmonicBase = BaseNote.E, enharmonicModifier = NoteModifier.Flat)
        assertEquals(4, headingSigns(dSharp))
        assertEquals(48f, centCellWidth(listOf(MusicalNote(BaseNote.C, NoteModifier.None), dSharp)))
    }

    @Test
    fun arrowsWidenTheCell() {
        val upUpC = MusicalNote(BaseNote.C, NoteModifier.NaturalUpUp, enharmonicBase = BaseNote.D, enharmonicModifier = NoteModifier.FlatUp)
        assertEquals(6, headingSigns(upUpC))
        assertEquals(68f, centCellWidth(listOf(upUpC)))
        assertEquals(48f, centCellWidth(emptyList()))
    }
}
