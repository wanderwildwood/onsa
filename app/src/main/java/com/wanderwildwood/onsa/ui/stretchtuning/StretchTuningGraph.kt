package com.wanderwildwood.onsa.ui.stretchtuning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.misc.GetTextFromString
import com.wanderwildwood.onsa.stretchtuning.StretchTuning
import com.wanderwildwood.onsa.temperaments.centsToFrequency
import com.wanderwildwood.onsa.temperaments.ratioToCents
import com.wanderwildwood.onsa.ui.plot.Anchor
import com.wanderwildwood.onsa.ui.plot.GestureBasedViewPort
import com.wanderwildwood.onsa.ui.plot.HorizontalLinesPositions
import com.wanderwildwood.onsa.ui.plot.LineCoordinates
import com.wanderwildwood.onsa.ui.plot.Plot
import com.wanderwildwood.onsa.ui.plot.PlotWindowOutline
import com.wanderwildwood.onsa.ui.plot.PointShape
import com.wanderwildwood.onsa.ui.plot.TickLevelDeltaBased
import com.wanderwildwood.onsa.ui.plot.VerticalLinesPositions
import com.wanderwildwood.onsa.ui.plot.rememberTextLabelHeight
import com.wanderwildwood.onsa.ui.plot.rememberTextLabelWidth
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun StretchTuningGraph(
    stretchTuningData: StretchTuning,
    modifier: Modifier = Modifier,
    gestureBasedViewPort: GestureBasedViewPort = remember { GestureBasedViewPort() },
    selectedKey: Int = -1,
    // line properties
    lineWidth: Dp = 2.dp,
    lineColor: Color = MaterialTheme.colorScheme.onSurface,
    // tick properties
    tickLineWidth: Dp = 1.dp,
    tickLineColor: Color = MaterialTheme.colorScheme.outline,
    tickLabelStyle: TextStyle = MaterialTheme.typography.labelMedium,
    tickLabelColor: Color = MaterialTheme.colorScheme.onSurface,
    // outline
    plotWindowOutline: PlotWindowOutline = PlotWindowOutline(),
    // frequencies
    referenceFrequency: Float = 440f
) {
    val viewPort = remember(stretchTuningData, referenceFrequency) {
        val marginRelative = 0.04f
        val fmin = when(stretchTuningData.size) {
            0 -> 0.5 * referenceFrequency
            1 -> 0.5 * stretchTuningData.unstretchedFrequencies[0]
            else -> stretchTuningData.unstretchedFrequencies.min()
        }
        val fmax = when(stretchTuningData.size) {
            0 -> 2.0 * referenceFrequency
            1 -> 2.0 * stretchTuningData.unstretchedFrequencies[0]
            else -> stretchTuningData.unstretchedFrequencies.max()
        }
        val fminCents = ratioToCents(fmin / referenceFrequency.toDouble()).toFloat()
        val fmaxCents = ratioToCents(fmax / referenceFrequency.toDouble()).toFloat()
        val fCentsMargin = (fmaxCents - fminCents) * marginRelative
        val cminInit = when(stretchTuningData.size) {
            0 -> -12.0
            1 -> min(-5.0, stretchTuningData.stretchInCents[0])
            else -> stretchTuningData.stretchInCents.min()
        }
        val cmaxInit = when(stretchTuningData.size) {
            0 -> 12.0
            1 -> max(5.0, stretchTuningData.stretchInCents[0])
            else -> stretchTuningData.stretchInCents.max()
        }
        val cmin = if ((cminInit - cmaxInit).absoluteValue < 0.5) cminInit - 1 else cminInit
        val cmax = if ((cminInit - cmaxInit).absoluteValue < 0.5) cmaxInit + 1 else cmaxInit
        val cMargin = (cmax - cmin) * marginRelative
        Rect(
            left = fminCents - fCentsMargin,
            top = (cmax + cMargin).toFloat(),
            right = fmaxCents + fCentsMargin,
            bottom = (cmin - cMargin).toFloat()
        )
    }

    val viewPortGestureLimits = remember(stretchTuningData, referenceFrequency) {
        val marginRelative = 2
        val fmax = when(stretchTuningData.size) {
            0 -> 2.0 * referenceFrequency
            1 -> 2.0 * stretchTuningData.unstretchedFrequencies[0]
            else -> stretchTuningData.unstretchedFrequencies.max()
        }
        val fmaxCents = ratioToCents(fmax / referenceFrequency.toDouble()).toFloat()
        val cminInit = when(stretchTuningData.size) {
            0 -> -12.0
            1 -> min(-5.0, stretchTuningData.stretchInCents[0])
            else -> stretchTuningData.stretchInCents.min()
        }
        val cmaxInit = when(stretchTuningData.size) {
            0 -> 12.0
            1 -> max(5.0, stretchTuningData.stretchInCents[0])
            else -> stretchTuningData.stretchInCents.max()
        }
        val cmin = if ((cminInit - cmaxInit).absoluteValue < 0.5) cminInit - 1 else cminInit
        val cmax = if ((cminInit - cmaxInit).absoluteValue < 0.5) cmaxInit + 1 else cmaxInit

        val cMargin = (cmax - cmin) * marginRelative
        Rect(
            left = ratioToCents(1.0 / referenceFrequency.toDouble()).toFloat(),
            top = (cmax + cMargin).toFloat(),
            right = 4 * fmaxCents,
            bottom = (cmax - cMargin).toFloat(),
        )
    }

    val maxLabelWidth = rememberTextLabelWidth(
        stringResource(R.string.cent, 500),
        style = tickLabelStyle
    )
    val maxLabelWidthDp = with(LocalDensity.current) { maxLabelWidth.toDp() + 2.dp}

    val maxLabelHeight = rememberTextLabelHeight(tickLabelStyle)
    val maxLabelHeightDp = with(LocalDensity.current) { maxLabelHeight.toDp() + 2.dp}

    val scope = rememberCoroutineScope()

    Plot(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                scope.launch { gestureBasedViewPort.finish() }
            },
        viewPort = viewPort,
        viewPortGestureLimits = viewPortGestureLimits,
        gestureBasedViewPort = gestureBasedViewPort,
        plotWindowPadding = DpRect(
            left = 0.dp,
            top = 0.dp,
            right = maxLabelWidthDp,
            bottom = maxLabelHeightDp
        ),
        plotWindowOutline = plotWindowOutline,
    ) {
        val horizontalLinePositions = remember {
            HorizontalLinesPositions.create(floatArrayOf(0f))
        }
        val verticalLinePositions = remember {
            VerticalLinesPositions.create(floatArrayOf(0f))
        }
        val line = remember(stretchTuningData, referenceFrequency) {
            LineCoordinates.create(
                stretchTuningData.size,
                { ratioToCents(stretchTuningData.unstretchedFrequencies[it] / referenceFrequency.toDouble()).toFloat() },
                { stretchTuningData.stretchInCents[it].toFloat() }
            )
        }
        val highlightedPoint = remember(selectedKey, stretchTuningData, referenceFrequency) {
            val index = stretchTuningData.keys.indexOfFirst { it == selectedKey }
            if (index < 0) {
                null
            } else {
                Offset(
                    ratioToCents(stretchTuningData.unstretchedFrequencies[index] / referenceFrequency.toDouble()).toFloat(),
                    stretchTuningData.stretchInCents[index].toFloat()
                )
            }
        }
        val tickLevelX = remember {
            TickLevelDeltaBased(600f, 1200f, 2400f)
        }

        val tickLevelY = remember {
            TickLevelDeltaBased(1f, 2f, 3f, 5f, 10f, 20f, 50f, 100f, 200f, 500f)
        }
        XTicks(
            tickLevelX,
            maxLabelWidth = rememberTextLabelWidth(
                stringResource(R.string.hertz, 10000f),
                tickLabelStyle
            ),
            verticalLabelPosition = 0f,
            anchor = Anchor.North,
            lineWidth = tickLineWidth,
            lineColor = tickLineColor,
            clipLabelToPlotWindow = false
        ) { tickModifier, _, _, c ->
            TextMMD(
                stringResource(R.string.hertz, centsToFrequency(c.toDouble(), referenceFrequency.toDouble()).toFloat()),
                modifier = tickModifier.padding(horizontal = 2.dp),
                style = tickLabelStyle,
                color = tickLabelColor
            )
        }
        YTicks(
            tickLevelY,
            maxLabelHeight = maxLabelHeight,
            horizontalLabelPosition = 1f,
            anchor = Anchor.West,
            lineWidth = tickLineWidth,
            lineColor = tickLineColor,
            clipLabelToPlotWindow = false
        ) { tickModifier, _, _, c ->
            TextMMD(
                stringResource(R.string.cent, c.roundToInt()),
                modifier = tickModifier.padding(horizontal = 2.dp),
                style = tickLabelStyle,
                color = tickLabelColor
            )
        }
        HorizontalLines(
            positions = horizontalLinePositions,
            lineWidth = 2.dp
        )
        VerticalLines(
            positions = verticalLinePositions,
            lineWidth = 2.dp
        )
        Line(
            data = line,
            lineColor = lineColor,
            lineWidth = lineWidth
        )
        Points(
            data = line,
            shape = PointShape.circle(size = 6.dp),
            maxPointSize = 3.dp // radius is relevant here
        )
        if (highlightedPoint != null) {
            Point(
                position = highlightedPoint,
                shape = PointShape.circle(size = 12.dp, color = MaterialTheme.colorScheme.error)
            )
        }
    }
}

@Preview(widthDp = 400, heightDp = 300, showBackground = true)
@Composable
private fun StretchTuningGraphPreview() {
    TunerTheme {
        Surface {
            val stretchTuningData = remember {StretchTuning(
                GetTextFromString("Test"),
                GetTextFromString("Test stretch description"),
                doubleArrayOf(100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0, 900.0, 1200.0),
                doubleArrayOf( 97.0, 199.0, 297.0, 400.0, 502.0, 606.0, 708.0, 807.0, 913.0, 1220.0)
            )}
            StretchTuningGraph(
                stretchTuningData = stretchTuningData,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                selectedKey = 2
            )
        }
    }
}