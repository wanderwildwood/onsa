package com.wanderwildwood.onsa.ui.notes

import com.wanderwildwood.onsa.R
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class OctaveNotation(
    @StringRes val stringResourceId: Int,
    @StringRes val descriptionResId: Int
) {
    /** Just numbers. */
    Scientific(
        R.string.octave_notation_scientific,
        R.string.octave_notation_scientific_description,
    ),
    /** Using primes and commas, and numbers, when primes and commas get too many. */
    Helmholtz(
        R.string.octave_notation_helmholtz,
        R.string.octave_notation_helmholtz_description,
        ),
    /** Using sub- and superscript numbers. */
    HelmholtzNumbered(
        R.string.octave_notation_helmholtz_numbered,
        R.string.octave_notation_helmholtz_numbered_description
    )
}

/** Options for note printing, new variant.
 * @param enharmonicVariant Enharmonic variant, e.g. 0 or 1.
 * @param octaveNotation How to display different octaves.
 *   for higher octaves and capital letters for lower octaves. Also for octaves around 2 and 3, no
 *   octaves numbers are printed but instead , and '.
 * @param notationType Notation type used for printing.
 */
@Serializable
@Stable
data class NotePrintOptions2(
    val enharmonicVariant: Int = 0,
    val octaveNotation: OctaveNotation = OctaveNotation.Scientific,
    val notationType: NotationType = NotationType.Standard
) {
    @Transient
    private val resourceIds = notationType.resourceIds()
    fun resourceId(noteNameStem: NoteNameStem) = resourceIds[noteNameStem]
}

/** Options for note printing.
 * @param useEnharmonic Tells if the enharmonic should be used for printing.
 * @param helmholtzNotation Set this to true, to use Helmholtz notation (this uses small letters
 *   for higher octaves and capital letters for lower octaves. Also for octaves around 2 and 3, no
 *   octaves numbers are printed but instead , and '.
 * @param notationType Notation type used for printing.
 */
@Serializable
@Stable
data class NotePrintOptions(
    val useEnharmonic: Boolean = false,
    val helmholtzNotation: Boolean = false,
    val notationType: NotationType = NotationType.Standard
) {
    @Transient
    private val resourceIds = notationType.resourceIds()
    fun resourceId(noteNameStem: NoteNameStem) = resourceIds[noteNameStem]
}


@Serializable
@Stable
data class NotePrintOptionsOld(
    val sharpFlatPreference: SharpFlatPreference = SharpFlatPreference.None,
    val helmholtzNotation: Boolean = false,
    val notationType: NotationType = NotationType.Standard
) {
    enum class SharpFlatPreference {
        Sharp, Flat, None
    }
}