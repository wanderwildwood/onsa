package com.wanderwildwood.onsa.temperaments

import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.misc.GetText
import com.wanderwildwood.onsa.misc.GetTextFromResIdWithIntArg
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.notenames.NoteNames2
import com.wanderwildwood.onsa.notenames.NoteNamesEDOGenerator
import kotlinx.serialization.Serializable

/** Equal division temperaments.
 * @param stableId Unique id.
 * @param notesPerOctave Number of notes per octave.
 */
@Serializable
data class Temperament3EDO(
    override val stableId: Long,
    val notesPerOctave: Int,
) : Temperament3 {
    override val name: GetText
        get() = GetTextFromResIdWithIntArg(R.string.equal_temperament_x, notesPerOctave)
    override val abbreviation: GetText
        get() = GetTextFromResIdWithIntArg(R.string.equal_temperament_x_abbr, notesPerOctave)
    override val description: GetText
        get() = GetTextFromResIdWithIntArg(R.string.equal_temperament_x_desc, notesPerOctave)
    override val size: Int
        get() = notesPerOctave

    override fun cents() = DoubleArray(notesPerOctave + 1) {
        it * 1200.0 / notesPerOctave.toDouble()
    }

    override fun chainOfFifths(): ChainOfFifths? {
        return if (notesPerOctave == 12) {
            ChainOfFifths(
                Array(notesPerOctave - 1) {
                    FifthModification(pythagoreanComma = RationalNumber(-1, 12))
                },
                rootIndex = 0
            )
        } else {
            null
        }
    }
    override fun equalOctaveDivision(): Int = notesPerOctave
    override fun rationalNumbers(): Array<RationalNumber>? = null
    override fun possibleRootNotes(): Array<MusicalNote>
            = NoteNamesEDOGenerator.possibleRootNotes(notesPerOctave)
    override fun noteNames(rootNote: MusicalNote?): NoteNames2
            = NoteNamesEDOGenerator.getNoteNames(notesPerOctave, rootNote)!!
}
