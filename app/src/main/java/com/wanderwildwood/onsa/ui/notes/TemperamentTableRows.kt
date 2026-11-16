package com.wanderwildwood.onsa.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.temperaments.Temperament3
import kotlin.math.roundToInt

/*
 * The cents table and the circle of fifths as rows that wrap to the width of a dialog, each row
 * its own item of a paged list. As one long sideways-scrolling row each, the cents table showed
 * four notes of twenty with no sign of the rest, and the circle of fifths was one tall item that
 * went whole to the next page, leaving a blank band where it would not fit.
 */

/** Width one note of the cents table takes ("1088 ¢", "D♯/E♭"). */
private val CentCell = 48.dp

/** Width one fifth (its note and the arrow to the next) takes in the circle of fifths. */
private val FifthCell = 84.dp

/** Side margin of the rows, as the old tables had. */
private val RowMargin = 16.dp

/** Side margin of the cents rows: less, so four notes fit beside the page bar. */
private val CentMargin = 8.dp

/** What the page bar takes from the rows when the dialog has more than one page. */
private val BarAllowance = 36.dp

/** The rows of the cents (and ratio) table of [temperament], laid out for [width]. */
fun centAndRatioRows(
    temperament: Temperament3,
    rootNote: MusicalNote?,
    notePrintOptions: NotePrintOptions2,
    width: Dp,
): List<@Composable () -> Unit> {
    val names = temperament.noteNames(rootNote)
    val cell = maxOf(CentCell.value, centCellWidth(names.notes.toList()))
    val perRow = cellsPerRow((width - CentMargin * 2 - BarAllowance).value, cell)
    val ranges = rowRanges(temperament.size + 1, perRow)
    return ranges.mapIndexed { row, range ->
        @Composable {
            CentAndRatioRow(temperament, rootNote, notePrintOptions, range, perRow, first = row == 0)
        }
    }
}

@Composable
private fun CentAndRatioRow(
    temperament: Temperament3,
    rootNote: MusicalNote?,
    notePrintOptions: NotePrintOptions2,
    range: IntRange,
    perRow: Int,
    first: Boolean,
) {
    val cents = remember(temperament) { temperament.cents() }
    val ratios = remember(temperament) { temperament.rationalNumbers() }
    val noteNames = remember(temperament, rootNote) { temperament.noteNames(rootNote) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CentMargin)
            .padding(top = if (first) 0.dp else 12.dp),
    ) {
        for (i in range) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NoteWithEnharmonic(
                    noteNames[i % noteNames.size],
                    modifier = Modifier.height(40.dp),
                    notePrintOptions = notePrintOptions,
                    withOctave = false,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Rule()
                TextMMD(
                    stringResource(id = R.string.cent_nosign, cents[i].roundToInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                if (ratios != null) {
                    Fraction(
                        ratios[i].numerator,
                        ratios[i].denominator,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Rule()
            }
        }
        // The last row keeps the columns of the rows above it.
        repeat(perRow - (range.last - range.first + 1)) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun Rule() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

/** The rows of the circle of fifths [fifths] (see [chainOfFifths]), laid out for [width]. */
fun circleOfFifthRows(
    fifths: Array<Fifth>,
    notePrintOptions: NotePrintOptions2,
    width: Dp,
): List<@Composable () -> Unit> {
    val perRow = cellsPerRow((width - RowMargin * 2 - BarAllowance).value, FifthCell.value)
    return rowRanges(fifths.size, perRow).map { range ->
        @Composable { CircleOfFifthRow(fifths, range, notePrintOptions) }
    }
}

@Composable
private fun CircleOfFifthRow(
    fifths: Array<Fifth>,
    range: IntRange,
    notePrintOptions: NotePrintOptions2,
) {
    val textMeasurer = rememberTextMeasurer()
    val noteTypography = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    val bottomToBaselineDistance = remember(noteTypography, density) {
        val measureResult = textMeasurer.measure("M", noteTypography)
        with(density) { (measureResult.size.height - measureResult.firstBaseline).toDp() }
    }
    // A FlowRow, so a row wider than it was reckoned wraps instead of being cut off.
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RowMargin)
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        for (i in range) {
            val fifth = fifths[i]
            // A note and the arrow from it stay together if the row has to wrap.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                NoteWithEnharmonic(
                    fifth.startNote,
                    Modifier.graphicsLayer { alpha = if (fifth.drawNoteLight) 0.5f else 1f },
                    notePrintOptions = notePrintOptions,
                    withOctave = false,
                    fontWeight = if (fifth.isRoot) FontWeight.ExtraBold else FontWeight.Normal,
                    style = noteTypography,
                )
                fifth.modification?.let { modification ->
                    FifthJumpOverArrow(
                        fifthModification = modification,
                        style = MaterialTheme.typography.labelSmall,
                        arrowHeight = noteTypography.fontSize / 5 * 3,
                        modifier = Modifier
                            .padding(bottom = bottomToBaselineDistance)
                            .graphicsLayer { alpha = if (fifth.drawModificationLight) 0.5f else 1f },
                    )
                }
            }
        }
    }
}
