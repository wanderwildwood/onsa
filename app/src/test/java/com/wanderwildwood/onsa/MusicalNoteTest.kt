package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.notenames.BaseNote
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.notenames.NoteModifier
import com.wanderwildwood.onsa.notenames.NoteNames
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicalNoteTest {

    @Test
    fun testMusicalScaleIndexing() {
        val notes = arrayOf(
            MusicalNote(BaseNote.C, NoteModifier.None),
            MusicalNote(BaseNote.C, NoteModifier.Sharp),
            MusicalNote(BaseNote.D, NoteModifier.None),
            MusicalNote(BaseNote.D, NoteModifier.Sharp),
            MusicalNote(BaseNote.E, NoteModifier.None),
            MusicalNote(BaseNote.F, NoteModifier.None),
            MusicalNote(BaseNote.F, NoteModifier.Sharp),
            MusicalNote(BaseNote.G, NoteModifier.None),
            MusicalNote(BaseNote.G, NoteModifier.Sharp),
            MusicalNote(BaseNote.A, NoteModifier.None),
            MusicalNote(BaseNote.A, NoteModifier.Sharp),
            MusicalNote(BaseNote.B, NoteModifier.None),
        )

        val scale = NoteNames(
            notes,
            MusicalNote(BaseNote.A, NoteModifier.None, 4)
        )

        // The index is the place in the note array; the octave is ignored.
        assertEquals(8, scale.getNoteIndex(MusicalNote(BaseNote.G, NoteModifier.Sharp, 4)))
        assertEquals(9, scale.getNoteIndex(MusicalNote(BaseNote.A, NoteModifier.None, 4)))
        assertEquals(10, scale.getNoteIndex(MusicalNote(BaseNote.A, NoteModifier.Sharp, 4)))
        assertEquals(11, scale.getNoteIndex(MusicalNote(BaseNote.B, NoteModifier.None, 4)))
        assertEquals(0, scale.getNoteIndex(MusicalNote(BaseNote.C, NoteModifier.None, 5)))
        assertEquals(1, scale.getNoteIndex(MusicalNote(BaseNote.C, NoteModifier.Sharp, 5)))
        assertEquals(1, scale.getNoteIndex(MusicalNote(BaseNote.C, NoteModifier.Sharp, 6)))

        for (someIndex in notes.indices) {
            assertEquals(someIndex, scale.getNoteIndex(scale[someIndex]))
        }
    }
}