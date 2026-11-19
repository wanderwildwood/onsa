package com.wanderwildwood.onsa.ui.tuning

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.notedetection.TuningState
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.ui.notes.Note
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions2
import kotlin.math.abs

/** How far the cents scale reaches each side of the center. */
private const val SCALE_CENTS = 50

/**
 * The reading in large type, for a phone on a music stand: the note aimed at, how many cents
 * off it is and which way to turn, and a scale from -50 to +50 cents with a dot where the
 * pitch sits. The graph below shows how the pitch got there; this says what to do now.
 */
@Composable
fun TuningReadout(
    targetNote: MusicalNote,
    cents: Int?,
    tuningState: TuningState,
    toleranceInCents: Int,
    notePrintOptions: NotePrintOptions2,
    modifier: Modifier = Modifier,
    scaleHeight: Dp = 36.dp
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val faint = MaterialTheme.colorScheme.outline
    val heard = cents != null && tuningState != TuningState.Unknown

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Note(
                targetNote,
                notePrintOptions = notePrintOptions,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = if (heard) ink else faint
            )
            TextMMD(
                text = readoutLine(cents, tuningState),
                fontSize = 22.sp,
                fontWeight = if (tuningState == TuningState.InTune) FontWeight.Bold else null,
                color = if (heard) ink else faint
            )
        }
        CentsScale(
            cents = if (heard) cents else null,
            toleranceInCents = toleranceInCents,
            ink = ink,
            faint = faint,
            modifier = Modifier
                .fillMaxWidth()
                .height(scaleHeight)
                .padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun readoutLine(cents: Int?, tuningState: TuningState): String = when {
    cents == null || tuningState == TuningState.Unknown -> stringResource(R.string.readout_listening)
    tuningState == TuningState.InTune -> stringResource(R.string.readout_in_tune)
    cents < 0 -> stringResource(R.string.readout_flat, abs(cents))
    else -> stringResource(R.string.readout_sharp, cents)
}

/** A row of ticks from -50 to +50 cents, the tolerance as a band around the center line, and
 * a dot for the pitch; past the end of the scale the dot becomes an arrow at that end. */
@Composable
private fun CentsScale(
    cents: Int?,
    toleranceInCents: Int,
    ink: Color,
    faint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val midY = size.height / 2
        val left = size.height / 2
        val right = size.width - size.height / 2
        fun x(c: Float) = left + (c + SCALE_CENTS) / (2f * SCALE_CENTS) * (right - left)

        val band = toleranceInCents.coerceIn(0, SCALE_CENTS).toFloat()
        if (band > 0f) {
            drawRect(
                faint.copy(alpha = 0.35f),
                topLeft = Offset(x(-band), 0f),
                size = Size(x(band) - x(-band), size.height)
            )
        }
        drawLine(ink, Offset(left, midY), Offset(right, midY), strokeWidth = 2.dp.toPx())
        for (c in -SCALE_CENTS..SCALE_CENTS step 10) {
            val tall = when {
                c == 0 -> size.height / 2
                c % 25 == 0 -> size.height / 3
                else -> size.height / 6
            }
            drawLine(
                ink, Offset(x(c.toFloat()), midY - tall), Offset(x(c.toFloat()), midY + tall),
                strokeWidth = if (c == 0) 3.dp.toPx() else 1.5.dp.toPx()
            )
        }
        for (c in listOf(-25, 25)) {
            drawLine(
                ink, Offset(x(c.toFloat()), midY - size.height / 3),
                Offset(x(c.toFloat()), midY + size.height / 3), strokeWidth = 1.5.dp.toPx()
            )
        }

        if (cents == null)
            return@Canvas
        val r = size.height * 0.28f
        if (abs(cents) <= SCALE_CENTS) {
            drawCircle(ink, radius = r, center = Offset(x(cents.toFloat()), midY))
        } else {
            val tipX = if (cents < 0) left - r * 0.6f else right + r * 0.6f
            val backX = if (cents < 0) tipX + 2 * r else tipX - 2 * r
            val arrow = Path().apply {
                moveTo(tipX, midY)
                lineTo(backX, midY - r)
                lineTo(backX, midY + r)
                close()
            }
            drawPath(arrow, ink)
        }
    }
}
