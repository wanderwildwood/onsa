/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.wanderwildwood.onsa.instruments

import android.content.Context
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.notenames.BaseNote
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.notenames.NoteModifier
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions2
import com.wanderwildwood.onsa.ui.notes.asAnnotatedString
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/** Instrument.
 * @param name Name as string (for user defined instruments). For predefined instruments, the
 *   name resource should be used to provide translations. However, in this case, still set a
 *   unique name here, since this will serve as a unique identifier to recognize the instrument
 *   later on again. So, name AND nameResource is set -> predefined instrument. Only name set
 *   -> user defined instrument.
 * @param nameResource Name resource id for predefined instruments or null for user defined
 *   instruments.
 * @param strings Strings, i.e. the notes of the instrument or empty array for using a
 *   chromatic scale.
 * @param icon Instrument icon.
 * @param stableId Stable id to identify instrument in a list.
 * @param isChromatic Defines if a instrument is defined via strings or it is chromatic. In latter
 *   case, strings should be empty.
 * @param group The instrument this is a tuning of, or null for one that stands on its own in
 *   the list. Absent from settings and files written before tunings were grouped, which
 *   therefore read back as standing on their own.
 * @param tuningNameResource For a predefined tuning whose name, shown under its instrument,
 *   should not simply be its full name with the instrument's name taken off the front - the
 *   standard guitar tuning, which is called "6-string guitar" everywhere else.
 */
@Serializable
@Parcelize
@Immutable
data class Instrument(
    private val name: String,
    @StringRes private val nameResource: Int?,
    val strings: Array<MusicalNote>,
    val icon: InstrumentIcon,
    val stableId: Long,
    val isChromatic: Boolean = false,
    val group: InstrumentGroup? = null,
    @StringRes private val tuningNameResource: Int? = null
) : Parcelable {
    /** Tell if an instrument is a predefined instrument. */
    fun isPredefined() = nameResource != null

    /** Get instrument name as string.
     * @param context Context is only needed, if the instrument name is a string resource.
     * @return Name of instrument.
     */
    fun getNameString(context: Context?): String {
        return when {
            nameResource != null && context != null -> context.getString(nameResource)
            else -> name
        }
    }

    /**
     * The name to show among the other tunings of the same instrument, where the instrument
     * is already the page's title: "Open G" rather than "Banjo, open G". A name that does not
     * begin with its instrument's name is shown whole.
     */
    fun getTuningNameString(context: Context): String {
        if (tuningNameResource != null) return context.getString(tuningNameResource)
        val full = getNameString(context)
        val instrumentName = group?.let { context.getString(it.nameResource) } ?: return full
        // The one built-in tuning of an instrument named after it - Violin among the violin's
        // tunings - is that instrument's standard tuning.
        if (isPredefined() && full == instrumentName) return context.getString(R.string.tuning_standard)
        val prefix = "$instrumentName, "
        return if (full.startsWith(prefix, ignoreCase = true) && full.length > prefix.length)
            full.substring(prefix.length).replaceFirstChar { it.titlecase() }
        else
            full
    }

    /** Get readable representation of all strings (e.g. "A#4 - C5 - G5")
     * @param context Context for obtaining string resources.
     * @param notePrintOptions How to print notes.
     */
    fun getStringsString(
        context: Context,
        notePrintOptions: NotePrintOptions2,
        fontSize: TextUnit,
        fontWeight: FontWeight? = null
        ): AnnotatedString {
        return if (isChromatic) {
            buildAnnotatedString { append(context.getString(R.string.chromatic)) }
        } else {
            buildAnnotatedString {
//            Log.v("Tuner", "Instrument.getStringsString: printOption=$printOption, preferFlat=$preferFlat")
                if (strings.isNotEmpty())
                    append(strings[0].asAnnotatedString(
                        notePrintOptions,
                        fontSize,
                        fontWeight,
                        withOctave = true,
                        resources = context.resources
                    ))

                for (i in 1 until strings.size) {
                    append(" - ")
                    append(strings[i].asAnnotatedString(
                        notePrintOptions,
                        fontSize,
                        fontWeight,
                        withOctave = true,
                        resources = context.resources
                    ))
                }
            }
        }
    }

    /** Whether this is the list row standing for all of an instrument's tunings. */
    val isGroupRow: Boolean
        get() = stableId in (GROUP_ROW_ID_BASE - 99)..GROUP_ROW_ID_BASE

    companion object {
        const val NO_STABLE_ID = Long.MAX_VALUE

        /**
         * Ids for instrument rows, far below the predefined tunings' small negative ids and
         * the reader's positive ones, so that none can collide.
         */
        private const val GROUP_ROW_ID_BASE = Long.MIN_VALUE + 1000

        /**
         * The row in the instrument list that stands for every tuning of [group]. It is only
         * ever a row: tapping it opens the instrument's page, so it is never selected,
         * tuned to or saved.
         */
        fun groupRow(group: InstrumentGroup) = Instrument(
            name = group.name,
            nameResource = group.nameResource,
            strings = arrayOf(),
            icon = group.icon,
            stableId = GROUP_ROW_ID_BASE - group.ordinal,
            group = group
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Instrument

        if (name != other.name) return false
        if (nameResource != other.nameResource) return false
        if (!strings.contentEquals(other.strings)) return false
        if (icon != other.icon) return false
        if (stableId != other.stableId) return false
        if (isChromatic != other.isChromatic) return false
        if (group != other.group) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (nameResource ?: 0)
        result = 31 * result + strings.contentHashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + stableId.hashCode()
        result = 31 * result + isChromatic.hashCode()
        result = 31 * result + (group?.hashCode() ?: 0)
        return result
    }
}

val instrumentChromatic = Instrument(
    name = "Chromatic",
    nameResource = R.string.chromatic,
    strings = arrayOf(),
    icon = InstrumentIcon.piano,
    stableId = -1, // this should be set by a id generator
    isChromatic = true
)

val instrumentDatabase = createInstrumentDatabase()

private fun createInstrumentDatabase(): ArrayList<Instrument> {
    val instruments = ArrayList<Instrument>()
    instruments.add(
        instrumentChromatic.copy(stableId = -1 - instruments.size.toLong())
    )
    instruments.add(
        Instrument(
            name = "6-string guitar",
            nameResource = R.string.guitar_eadgbe,
            strings = arrayOf(
                MusicalNote(BaseNote.E, NoteModifier.None, 2),
                MusicalNote(BaseNote.A, NoteModifier.None, 2),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.B, NoteModifier.None, 3),
                MusicalNote(BaseNote.E, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.guitar,
            group = InstrumentGroup.guitar,
            tuningNameResource = R.string.tuning_standard,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "4-string bass",
            nameResource = R.string.bass_eadg,
            strings = arrayOf(
                MusicalNote(BaseNote.E, NoteModifier.None, 1),
                MusicalNote(BaseNote.A, NoteModifier.None, 1),
                MusicalNote(BaseNote.D, NoteModifier.None, 2),
                MusicalNote(BaseNote.G, NoteModifier.None, 2),
            ),
            icon = InstrumentIcon.bass,
            group = InstrumentGroup.bass,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "5-string bass",
            nameResource = R.string.bass_beadg,
            strings = arrayOf(
                MusicalNote(BaseNote.B, NoteModifier.None, 0),
                MusicalNote(BaseNote.E, NoteModifier.None, 1),
                MusicalNote(BaseNote.A, NoteModifier.None, 1),
                MusicalNote(BaseNote.D, NoteModifier.None, 2),
                MusicalNote(BaseNote.G, NoteModifier.None, 2),
            ),
            icon = InstrumentIcon.bass,
            group = InstrumentGroup.bass,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Ukulele",
            nameResource = R.string.ukulele_gcea,
            strings = arrayOf(
                MusicalNote(BaseNote.G, NoteModifier.None, 4),
                MusicalNote(BaseNote.C, NoteModifier.None, 4),
                MusicalNote(BaseNote.E, NoteModifier.None, 4),
                MusicalNote(BaseNote.A, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.ukulele,
            group = InstrumentGroup.ukulele,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Violin",
            nameResource = R.string.violin_gdae,
            strings = arrayOf(
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4),
                MusicalNote(BaseNote.A, NoteModifier.None, 4),
                MusicalNote(BaseNote.E, NoteModifier.None, 5),
            ),
            icon = InstrumentIcon.violin,
            group = InstrumentGroup.violin,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Viola",
            nameResource = R.string.viola_cgda,
            strings = arrayOf(
                MusicalNote(BaseNote.C, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4),
                MusicalNote(BaseNote.A, NoteModifier.None, 4),
            ),
            icon = InstrumentIcon.violin,
            group = InstrumentGroup.viola,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Cello",
            nameResource = R.string.cello_cgda,
            strings = arrayOf(
                MusicalNote(BaseNote.C, NoteModifier.None, 2),
                MusicalNote(BaseNote.G, NoteModifier.None, 2),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.A, NoteModifier.None, 3),
            ),
            icon = InstrumentIcon.cello,
            group = InstrumentGroup.cello,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Double bass",
            nameResource = R.string.double_bass_eadg,
            strings = arrayOf(
                MusicalNote(BaseNote.E, NoteModifier.None, 1),
                MusicalNote(BaseNote.A, NoteModifier.None, 1),
                MusicalNote(BaseNote.D, NoteModifier.None, 2),
                MusicalNote(BaseNote.G, NoteModifier.None, 2),
            ),
            icon = InstrumentIcon.double_bass,
            group = InstrumentGroup.double_bass,
            stableId = -1 - instruments.size.toLong()
        )
    )
    // 5-string banjo, after carefulcarpenter's fork (github.com/heavencanthandle/onsa).
    // Appended after the others so that no existing stable id moves. Strings run fifth to
    // first, so the short fifth string, the high drone, comes first and sits beside the low
    // fourth -- the order a banjo player reads them in. Each tuning is its own row, the way
    // the two basses are. A sharp or flat is spelled as the default scale spells it, sharp
    // first with its flat as the enharmonic, because a selected string is looked up by exact
    // spelling: written plainly as B flat, G minor's second string would find no note.
    instruments.add(
        Instrument(
            name = "Banjo, open G",
            nameResource = R.string.banjo_open_g,
            strings = arrayOf(
                MusicalNote(BaseNote.G, NoteModifier.None, 4),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.B, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.banjo,
            group = InstrumentGroup.banjo,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Banjo, G minor",
            nameResource = R.string.banjo_g_minor,
            strings = arrayOf(
                MusicalNote(BaseNote.G, NoteModifier.None, 4),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.A, NoteModifier.Sharp, 3,
                    enharmonicBase = BaseNote.B, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(BaseNote.D, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.banjo,
            group = InstrumentGroup.banjo,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Banjo, sawmill",
            nameResource = R.string.banjo_sawmill,
            strings = arrayOf(
                MusicalNote(BaseNote.G, NoteModifier.None, 4),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.C, NoteModifier.None, 4),
                MusicalNote(BaseNote.D, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.banjo,
            group = InstrumentGroup.banjo,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Banjo, double C",
            nameResource = R.string.banjo_double_c,
            strings = arrayOf(
                MusicalNote(BaseNote.G, NoteModifier.None, 4),
                MusicalNote(BaseNote.C, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.C, NoteModifier.None, 4),
                MusicalNote(BaseNote.D, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.banjo,
            group = InstrumentGroup.banjo,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Banjo, open C",
            nameResource = R.string.banjo_open_c,
            strings = arrayOf(
                MusicalNote(BaseNote.G, NoteModifier.None, 4),
                MusicalNote(BaseNote.C, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.C, NoteModifier.None, 4),
                MusicalNote(BaseNote.E, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.banjo,
            group = InstrumentGroup.banjo,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Banjo, drop C",
            nameResource = R.string.banjo_drop_c,
            strings = arrayOf(
                MusicalNote(BaseNote.G, NoteModifier.None, 4),
                MusicalNote(BaseNote.C, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.B, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.banjo,
            group = InstrumentGroup.banjo,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Banjo, C minor",
            nameResource = R.string.banjo_c_minor,
            strings = arrayOf(
                MusicalNote(BaseNote.G, NoteModifier.None, 4),
                MusicalNote(BaseNote.C, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.C, NoteModifier.None, 4),
                MusicalNote(BaseNote.D, NoteModifier.Sharp, 4,
                    enharmonicBase = BaseNote.E, enharmonicModifier = NoteModifier.Flat)
            ),
            icon = InstrumentIcon.banjo,
            group = InstrumentGroup.banjo,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Banjo, open D",
            nameResource = R.string.banjo_open_d,
            strings = arrayOf(
                MusicalNote(BaseNote.F, NoteModifier.Sharp, 4,
                    enharmonicBase = BaseNote.G, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.F, NoteModifier.Sharp, 3,
                    enharmonicBase = BaseNote.G, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(BaseNote.A, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.banjo,
            group = InstrumentGroup.banjo,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Banjo, D minor",
            nameResource = R.string.banjo_d_minor,
            strings = arrayOf(
                MusicalNote(BaseNote.F, NoteModifier.None, 4),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.F, NoteModifier.None, 3),
                MusicalNote(BaseNote.A, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.banjo,
            group = InstrumentGroup.banjo,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Banjo, double D",
            nameResource = R.string.banjo_double_d,
            strings = arrayOf(
                MusicalNote(BaseNote.A, NoteModifier.None, 4),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.A, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4),
                MusicalNote(BaseNote.E, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.banjo,
            group = InstrumentGroup.banjo,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Banjo, open A",
            nameResource = R.string.banjo_open_a,
            strings = arrayOf(
                MusicalNote(BaseNote.A, NoteModifier.None, 4),
                MusicalNote(BaseNote.E, NoteModifier.None, 3),
                MusicalNote(BaseNote.A, NoteModifier.None, 3),
                MusicalNote(BaseNote.C, NoteModifier.Sharp, 4,
                    enharmonicBase = BaseNote.D, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(BaseNote.E, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.banjo,
            group = InstrumentGroup.banjo,
            stableId = -1 - instruments.size.toLong()
        )
    )
    // Guitar's other tunings. Appended after the banjo for the same reason the banjo was
    // appended after the rest: so that no existing stable id moves. They list under the one
    // Guitar row with the standard tuning, lowest string first like it.
    instruments.add(
        Instrument(
            name = "Guitar, drop D",
            nameResource = R.string.guitar_drop_d,
            strings = arrayOf(
                MusicalNote(BaseNote.D, NoteModifier.None, 2),
                MusicalNote(BaseNote.A, NoteModifier.None, 2),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.B, NoteModifier.None, 3),
                MusicalNote(BaseNote.E, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.guitar,
            group = InstrumentGroup.guitar,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Guitar, D standard",
            nameResource = R.string.guitar_d_standard,
            strings = arrayOf(
                MusicalNote(BaseNote.D, NoteModifier.None, 2),
                MusicalNote(BaseNote.G, NoteModifier.None, 2),
                MusicalNote(BaseNote.C, NoteModifier.None, 3),
                MusicalNote(BaseNote.F, NoteModifier.None, 3),
                MusicalNote(BaseNote.A, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.guitar,
            group = InstrumentGroup.guitar,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Guitar, half-step down",
            nameResource = R.string.guitar_half_step_down,
            strings = arrayOf(
                MusicalNote(BaseNote.D, NoteModifier.Sharp, 2,
                    enharmonicBase = BaseNote.E, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(BaseNote.G, NoteModifier.Sharp, 2,
                    enharmonicBase = BaseNote.A, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(BaseNote.C, NoteModifier.Sharp, 3,
                    enharmonicBase = BaseNote.D, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(BaseNote.F, NoteModifier.Sharp, 3,
                    enharmonicBase = BaseNote.G, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(BaseNote.A, NoteModifier.Sharp, 3,
                    enharmonicBase = BaseNote.B, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(BaseNote.D, NoteModifier.Sharp, 4,
                    enharmonicBase = BaseNote.E, enharmonicModifier = NoteModifier.Flat)
            ),
            icon = InstrumentIcon.guitar,
            group = InstrumentGroup.guitar,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Guitar, DADGAD",
            nameResource = R.string.guitar_dadgad,
            strings = arrayOf(
                MusicalNote(BaseNote.D, NoteModifier.None, 2),
                MusicalNote(BaseNote.A, NoteModifier.None, 2),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.A, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.guitar,
            group = InstrumentGroup.guitar,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Guitar, open D",
            nameResource = R.string.guitar_open_d,
            strings = arrayOf(
                MusicalNote(BaseNote.D, NoteModifier.None, 2),
                MusicalNote(BaseNote.A, NoteModifier.None, 2),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.F, NoteModifier.Sharp, 3,
                    enharmonicBase = BaseNote.G, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(BaseNote.A, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.guitar,
            group = InstrumentGroup.guitar,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Guitar, open G",
            nameResource = R.string.guitar_open_g,
            strings = arrayOf(
                MusicalNote(BaseNote.D, NoteModifier.None, 2),
                MusicalNote(BaseNote.G, NoteModifier.None, 2),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.B, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.guitar,
            group = InstrumentGroup.guitar,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = "Guitar, open E",
            nameResource = R.string.guitar_open_e,
            strings = arrayOf(
                MusicalNote(BaseNote.E, NoteModifier.None, 2),
                MusicalNote(BaseNote.B, NoteModifier.None, 2),
                MusicalNote(BaseNote.E, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.Sharp, 3,
                    enharmonicBase = BaseNote.A, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(BaseNote.B, NoteModifier.None, 3),
                MusicalNote(BaseNote.E, NoteModifier.None, 4)
            ),
            icon = InstrumentIcon.guitar,
            group = InstrumentGroup.guitar,
            stableId = -1 - instruments.size.toLong()
        )
    )
//    instruments.add(
//        Instrument(
//            name = "Test instrument",
//            nameResource = null,
//            strings = arrayOf(
//                MusicalNote(BaseNote.A, NoteModifier.None, 4),
//                MusicalNote(BaseNote.A, NoteModifier.Sharp, 4, BaseNote.B, NoteModifier.Flat),
//                MusicalNote(BaseNote.B, NoteModifier.None, 4),
//                MusicalNote(BaseNote.A, NoteModifier.None, 4),
//                MusicalNote(BaseNote.B, NoteModifier.None, 4),
//                MusicalNote(BaseNote.B, NoteModifier.None, 4),
//                MusicalNote(BaseNote.C, NoteModifier.None, 5),
//                MusicalNote(BaseNote.C, NoteModifier.Sharp, 5, BaseNote.D, NoteModifier.Flat),
//                MusicalNote(BaseNote.D, NoteModifier.None, 5),
//                MusicalNote(BaseNote.A, NoteModifier.None, 4)
//            ),
//            iconResource = R.drawable.ic_violin,
//            stableId = -1 - instruments.size.toLong()
//        )
//    )
//    instruments.add(
//        Instrument(
//            name = "Test instrument 2",
//            nameResource = null,
//            strings = arrayOf(
//                MusicalNote(BaseNote.A, NoteModifier.None, 4),
//                MusicalNote(BaseNote.A, NoteModifier.Sharp, 4, BaseNote.B, NoteModifier.Flat),
//                MusicalNote(BaseNote.A, NoteModifier.Sharp, 4, BaseNote.B, NoteModifier.Flat),
//            iconResource = R.drawable.ic_violin,
//            stableId = -1 - instruments.size.toLong()
//        )
//    )
    return instruments
}
