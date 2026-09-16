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
package com.wanderwildwood.onsa.ui.plot

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/** Plot scope where a plot defines its different elements.
 * @param clipped Defines if the plot scope is clipped or not. This is just for internal use, with
 *   some details which have to be known. E.g. Lines and points are only drawn in the clipped scope,
 *   but ticks can be either drawn in clipped or in unclipped state.
 * @param transformation Transformation rule between screen coordinate system and plot coordinate
 *   system.
 */
class PlotScope(
    private val clipped: Boolean,
    private val transformation: () -> Transformation
) {
    /** Add a line.
     * @param data Line coordinates, normally created by LineCoordinates.create(...).
     * @param lineColor Line color.
     * @param lineWidth Line width.
     */
    @Composable
    fun Line(
        data: LineCoordinates,
        lineColor: Color = Color.Unspecified,
        lineWidth: Dp = 1.dp
    ) {
        if (clipped)
            Line(data, lineColor, lineWidth, transformation)
    }

    /** Add multiple point of same style.
     * @param data Point coordinates, normally created by LineCoordinates.create(...). Note that
     *   even though the function is call Point, we use LineCoordinates here since it provides
     *   the required information.
     * @param shape In the given scope. the shape of the point must be drawn. You can use presets
     *   like `PointShape.circle(...)` or other shapes in `PointShape`.
     * @param maxPointSize Maximum point size measured from actual point position to its maximum
     *   extent in x or y direction. If this value is smaller than the actual point, points closely
     *   outside the viewport will not be drawn anymore even though parts of it would theoretically
     *   should be visible.
     */
    @Composable
    fun Points(
        data: LineCoordinates,
        shape: DrawScope.() -> Unit,
        maxPointSize: Dp = 0.dp
    ) {
        if (clipped)
            Points(data, shape, maxPointSize, transformation)
    }

    /** Add a point.
     * @param position Point position.
     * @param shape In the given scope, the shape of the point must be drawn.
     */
    @Composable
    fun Point(
        position: Offset,
        shape: DrawScope.() -> Unit
    ) {
        if (clipped)
            Point(position, shape, transformation)
    }

    /** Add vertical lines.
     * @param positions Vertical line positions where to add lines. Normally created with
     *   VerticalLinePositions.create()
     * @param color Color of lines.
     * @param lineWidth Line width.
     */
    @Composable
    fun VerticalLines(
        positions: VerticalLinesPositions,
        color: Color = Color.Unspecified,
        lineWidth: Dp = 1.dp
    ) {
        if (clipped)
            VerticalLines(positions, color, lineWidth, transformation)
    }

    /** Add horizontal lines.
     * @param positions Horizontal line positions where to add lines. Normally created with
     *   HorizontalLinePositions.create()
     * @param color Color of lines.
     * @param lineWidth Line width.
     */
    @Composable
    fun HorizontalLines(
        positions: HorizontalLinesPositions,
        color: Color = Color.Unspecified,
        lineWidth: Dp = 1.dp
    ) {
        if (clipped)
            HorizontalLines(positions, color, lineWidth, transformation)
    }

    /** Add horizontal marks (horizontal line with label).
     * @param marks List of horizontal marks.
     * @param sameSizeLabels If true,the labels will have the same size (the one of the largest
     *   mark). If false, each label will be sized individually.
     * @param clipLabelsToWindow If true, the labels will be clipped to the plot window, if false
     *   label can be drawn outside.
     */
    @Composable
    fun HorizontalMarks(
        marks: ImmutableList<HorizontalMark>,
        sameSizeLabels: Boolean = true,
        clipLabelsToWindow: Boolean = true
        ) {
        HorizontalMarks(
            marks = marks,
            clipLabelsToWindow = clipLabelsToWindow,
            sameSizeLabels = sameSizeLabels,
            transformation = transformation,
            clipped = clipped
        )
    }

    /** Add vertical marks (vertical line with label).
     * @param marks List of vertical marks.
     * @param sameSizeLabels If true,the labels will have the same size (the one of the largest
     *   mark). If false, each label will be sized individually.
     * @param clipLabelsToWindow If true, the labels will be clipped to the plot window, if false
     *   label can be drawn outside.
     */
    @Composable
    fun VerticalMarks(
        marks: ImmutableList<VerticalMark>,
        sameSizeLabels: Boolean = true,
        clipLabelsToWindow: Boolean = true
    ) {
        VerticalMarks(
            marks = marks,
            clipLabelsToWindow = clipLabelsToWindow,
            sameSizeLabels = sameSizeLabels,
            transformation = transformation,
            clipped = clipped
        )
    }

    /** Add marks.
     * @param marks List of marks.
     * @param sameSizeLabels If true,the labels will have the same size (the one of the largest
     *   mark). If false, each label will be sized individually.
     * @param clipLabelsToWindow If true, the labels will be clipped to the plot window, if false
     *   label can be drawn outside.
     */
    @Composable
    fun PointMarks(
        marks: ImmutableList<PointMark>,
        sameSizeLabels: Boolean = true,
        clipLabelsToWindow: Boolean = true
    ) {
        PointMarks(
            marks = marks,
            clipLabelsToWindow = clipLabelsToWindow,
            sameSizeLabels = sameSizeLabels,
            transformation = transformation,
            clipped = clipped
        )
    }

    /** Add x-ticks.
     * @param tickLevel Where to draw the ticks. There a different ways to define this, e.g.
     *   by TickLevelDeltaBased or TickLevelExplicitRanges.
     * @param maxLabelWidth Maximum expected label width.
     * @param anchor Label anchor (where to place relative to the position).
     * @param verticalLabelPosition Where to place the label (0f is bottom, 1f is top).
     * @param lineWidth Line width.
     * @param lineColor Line color.
     * @param screenOffset Shift the label by the given amount along the screen.
     * @param maxNumLabels Maximum number of labels to be shown at the same time. This is used
     *   to choose a suitable level from the tickLevel. Depending on the given tick levels, it
     *   might not be possible to fulfill this requirement. Use -1 to autodetect a suitable value.
     * @param clipLabelToPlotWindow True to clip labels to plot window, else false.
     * @param label Composable to draw the label with the following arguments provided:
     *   - modifier -> Modifier which as to be passed to the tick composable.
     *   - level -> The tick level index which is used.
     *   - index -> The index within the given tick level.
     *   - x -> x-value of tick in raw coordinates.
     */
    @Composable
    fun XTicks(
        tickLevel: TickLevel,
        maxLabelWidth: Float,
        anchor: Anchor = Anchor.Center,
        verticalLabelPosition: Float = 0.5f,
        lineWidth: Dp = 1.dp,
        lineColor: Color = MaterialTheme.colorScheme.outline,
        screenOffset: DpOffset = DpOffset.Zero,
        maxNumLabels: Int = -1,
        clipLabelToPlotWindow: Boolean = true,
        label: @Composable ((modifier: Modifier, level: Int, index: Int, x: Float) -> Unit)?
    ) {
        XTicks(
            label = label,
            tickLevel = tickLevel,
            maxLabelWidth = maxLabelWidth,
            anchor = anchor,
            verticalLabelPosition = verticalLabelPosition,
            lineWidth = lineWidth,
            lineColor = lineColor,
            screenOffset = screenOffset,
            maxNumLabels = maxNumLabels,
            clipLabelToPlotWindow = clipLabelToPlotWindow,
            transformation = transformation,
            clipped = clipped
        )
    }

    /** Add y-ticks.
     * @param tickLevel Where to draw the ticks. There a different ways to define this, e.g.
     *   by TickLevelDeltaBased or TickLevelExplicitRanges.
     * @param maxLabelHeight Maximum expected label height.
     * @param anchor Label anchor (where to place relative to the position).
     * @param horizontalLabelPosition Where to place the label (0f is left, 1f is right).
     * @param lineWidth Line width.
     * @param lineColor Line color.
     * @param screenOffset Shift the label by the given amount along the screen.
     * @param maxNumLabels Maximum number of labels to be shown at the same time. This is used
     *   to choose a suitable level from the tickLevel. Depending on the given tick levels, it
     *   might not be possible to fulfill this requirement. Use -1 to autodetect a suitable value.
     * @param clipLabelToPlotWindow True to clip labels to plot window, else false.
     * @param label Composable to draw the label with the following arguments provided:
     *   - modifier -> Modifier which as to be passed to the tick composable.
     *   - level -> The tick level index which is used.
     *   - index -> The index within the given tick level.
     *   - y -> y-value of tick in raw coordinates.
     */
    @Composable
    fun YTicks(
        tickLevel: TickLevel,
        maxLabelHeight: Float,
        anchor: Anchor = Anchor.Center,
        horizontalLabelPosition: Float = 0.5f,
        lineWidth: Dp = 1.dp,
        lineColor: Color = MaterialTheme.colorScheme.outline,
        screenOffset: DpOffset = DpOffset.Zero,
        maxNumLabels: Int = -1,
        clipLabelToPlotWindow: Boolean = true,
        label: @Composable ((modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)?
    ) {
        YTicks(
            label = label,
            tickLevel = tickLevel,
            maxLabelHeight = maxLabelHeight,
            anchor = anchor,
            horizontalLabelPosition = horizontalLabelPosition,
            lineWidth = lineWidth,
            lineColor = lineColor,
            screenOffset = screenOffset,
            maxNumLabels = maxNumLabels,
            clipLabelToPlotWindow = clipLabelToPlotWindow,
            transformation = transformation,
            clipped = clipped
        )
    }
}

/** Create a plot.
 * @param modifier Modifier.
 * @param viewPort This defines the plot limits in raw coordinates (the coordinates, how you also
 *   define points, lines, ...).
 * @param viewPortGestureLimits Limits if dragging the view with gestures is allowed.
 * @param gestureBasedViewPort Viewport defined by gestures. This overrides the viewPort.
 * @param plotWindowPadding Padding outside the plot window. If you draw ticks or marks outside the
 *   window the padding must be large enough to include these ticks or marks.
 * @param plotWindowOutline Definition of outlines which is drawn around the window.
 * @param lockX True to not allow changing the x-limits by gestures.
 * @param lockY True to not allow changing the y-limits by gestures.
 * @param content PlotScope where the content like lines, points, ticks, marks are defined.
 */
@Composable
fun Plot(
    modifier: Modifier,
    viewPort: Rect,
    viewPortGestureLimits: Rect? = null,
    gestureBasedViewPort: GestureBasedViewPort = remember { GestureBasedViewPort() },
    plotWindowPadding: DpRect = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
    plotWindowOutline: PlotWindowOutline = PlotWindowOutline(),
    lockX: Boolean = false,
    lockY: Boolean = false,
    content: @Composable PlotScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.roundToPx() }
        val cornerRadiusPx = with(LocalDensity.current) { plotWindowOutline.cornerRadius.toPx() }
        val density = LocalDensity.current
        val viewPortScreen = remember(density, plotWindowPadding, widthPx, heightPx) {
            with(density) {
                IntRect(
                    plotWindowPadding.left.roundToPx(),
                    plotWindowPadding.top.roundToPx(),
                    widthPx - plotWindowPadding.right.roundToPx(),
                    heightPx - plotWindowPadding.bottom.roundToPx()
                )
            }
        }
        // The viewport used to spring into place. On E Ink a spring is a dozen full repaints
        // of the plot to land where it could have been drawn at once.
        val resolvedViewPortRaw =
            if (gestureBasedViewPort.isActive) gestureBasedViewPort.viewPort else viewPort
        val resolvedLimits = remember(viewPortGestureLimits) {
            if (viewPortGestureLimits == null) {
                null
            } else {
                Rect(
                    min(viewPortGestureLimits.left, viewPortGestureLimits.right),
                    min(viewPortGestureLimits.top, viewPortGestureLimits.bottom),
                    max(viewPortGestureLimits.left, viewPortGestureLimits.right),
                    max(viewPortGestureLimits.top, viewPortGestureLimits.bottom),
                )
            }
        }

        // use updated state here, to avoid having to recreate of the pointerInput modifier
        val transformation by rememberUpdatedState(
            Transformation(
                viewPortScreen,
                if (gestureBasedViewPort.isActive) gestureBasedViewPort.viewPort else resolvedViewPortRaw,
                cornerRadiusPx
            )
        )

        val plotScopeClipped = remember { PlotScope(clipped = true, { transformation }) }
        val plotScopeUnclipped = remember { PlotScope(clipped = false, { transformation }) }
        val clipShape = transformation.rememberClipShape()
        Box(modifier = Modifier
            .fillMaxSize()
            .clip(clipShape)) {
            plotScopeClipped.content()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .createPlotWindowOutline(plotWindowOutline, { transformation.viewPortScreen })
        ) {
            plotScopeUnclipped.content()
        }

        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .dragZoom(
                    gestureBasedViewPort,
                    { resolvedLimits },
                    { transformation },
                    lockX = lockX,
                    lockY = lockY
                )
        )
    }
}

@Preview(widthDp = 250, heightDp = 200, showBackground = true)
@Composable
private fun PlotPreview() {
    TunerTheme {
        val viewPortRaw = remember { Rect(left = -5f, top = 10f, right = 5f, bottom = -10f) }
        val viewPortRawLimits =
            remember { Rect(left = -20f, top = 100f, right = 40f, bottom = -100f) }
        val gestureBasedViewPort = remember { GestureBasedViewPort() }
        val scope = rememberCoroutineScope()
        Column {
            Plot(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { scope.launch { gestureBasedViewPort.finish() } },
                viewPort = viewPortRaw,
                viewPortGestureLimits = viewPortRawLimits,
                gestureBasedViewPort = gestureBasedViewPort,
                plotWindowPadding = DpRect(5.dp, 5.dp, 5.dp, 5.dp),
                plotWindowOutline = PlotWindowOutline(lineWidth = 2.dp)
            ) {
                YTicks(
                    tickLevel = TickLevelExplicitRanges(
                        persistentListOf(
                            floatArrayOf(
                                -24f,
                                -20f,
                                -16f,
                                -12f,
                                -8f,
                                -4f,
                                -0f,
                                4f,
                                8f,
                                12f,
                                16f,
                                20f,
                                24f
                            )
                        )
                    ),
                    maxLabelHeight = rememberTextLabelHeight(),
                    anchor = Anchor.SouthWest,
                    horizontalLabelPosition = 0f,
                    maxNumLabels = 6,
                    clipLabelToPlotWindow = true
                ) { modifier, level, index, y ->
                    TextMMD(
                        "y=$y",
                        modifier = modifier,//.background(MaterialTheme.colorScheme.secondary),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                XTicks(
                    tickLevel = TickLevelExplicitRanges(
                        persistentListOf(
                            floatArrayOf(-3f, 0f, 3f)
                        )
                    ),
                    maxLabelWidth = 20f,// TODO: compute this somehow
                    anchor = Anchor.South,
                    verticalLabelPosition = 0f,
                    maxNumLabels = 6,
                    clipLabelToPlotWindow = true
                ) { modifier, level, index, x ->
                    TextMMD(
                        "x=$x",
                        modifier = modifier,//.background(MaterialTheme.colorScheme.secondary),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val x = remember { floatArrayOf(-4f, -2f, 0f, 2f, 4f) }
                val y = remember { floatArrayOf(1f, -7f, -5f, 0f, 8f) }
                Line(
                    data = LineCoordinates.create(x, y),
                    lineWidth = 5.dp,
                    lineColor = MaterialTheme.colorScheme.primary
                )

                Point(
                    position = Offset(4f, 8f),
                    shape = PointShape.circle(size = 20.dp, MaterialTheme.colorScheme.error)
                )

                HorizontalMarks(
                    marks = persistentListOf(
                        HorizontalMark(
                            position = 1f,
                            settings = HorizontalMark.Settings(
                                lineWidth = 3.dp,
                                labelPosition = 1f,
                                anchor = Anchor.East,
                                lineColor = MaterialTheme.colorScheme.secondary
                            )
                        ) { m ->
                            Surface(m, color = MaterialTheme.colorScheme.secondary) {
                                TextMMD("ABC", modifier = Modifier.padding(horizontal = 2.dp))
                            }
                        },
                    ),
                    clipLabelsToWindow = true,
                    sameSizeLabels = true
                )

                HorizontalMarks(
                    marks = persistentListOf(
                        HorizontalMark(
                            position = 1f,
                            settings = HorizontalMark.Settings(
                                lineWidth = 3.dp,
                                labelPosition = 1f,
                                anchor = Anchor.East,
                                lineColor = MaterialTheme.colorScheme.secondary
                            )
                        ) { m ->
                            Surface(m, color = MaterialTheme.colorScheme.secondary) {
                                TextMMD("ABC", modifier = Modifier.padding(horizontal = 2.dp))
                            }
                        },
                    ),
                    clipLabelsToWindow = true,
                    sameSizeLabels = true
                )

                VerticalMarks(
                    marks = persistentListOf(
                        VerticalMark(
                            position = 0.5f,
                            settings = VerticalMark.Settings(
                                lineWidth = 3.dp,
                                labelPosition = 0.95f,
                                anchor = Anchor.NorthEast,
                                lineColor = MaterialTheme.colorScheme.secondary
                            )
                        ) { m ->
                            Surface(m, color = MaterialTheme.colorScheme.secondary) {
                                TextMMD("ABC", modifier = Modifier.padding(horizontal = 2.dp))
                            }
                        },
                    ),
                    clipLabelsToWindow = true,
                    sameSizeLabels = true
                )

                PointMarks(
                    marks = persistentListOf(
                        PointMark(
                            position = Offset(-3f, -4f),
                            settings = PointMark.Settings(
                                anchor = Anchor.Center,
                            )
                        ) { m ->
                            Surface(m, color = MaterialTheme.colorScheme.secondary) {
                                TextMMD("ABC", modifier = Modifier.padding(horizontal = 2.dp))
                            }
                        },
                    ),
                    clipLabelsToWindow = true,
                    sameSizeLabels = true
                )
            }
        }
    }
}
