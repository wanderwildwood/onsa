package com.wanderwildwood.onsa.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * A [PagedColumn] for a dialog: as tall as its rows when they fit, [maxHeight] when they do not
 * (then it pages as any list does). A plain PagedColumn fills all the height it is offered, so a
 * dialog with little in it showed a blank band between its last row and its buttons (the key
 * note dialog for Equal temperament 5: about 170 px of nothing above Done).
 *
 * [rows] gets the width the rows have and gives one composable per row. They are measured once,
 * unplaced, to find their height, then shown in the list.
 */
@Composable
fun FitPagedColumn(
    maxHeight: Dp,
    modifier: Modifier = Modifier,
    rows: (width: Dp) -> List<@Composable () -> Unit>,
) {
    SubcomposeLayout(modifier) { constraints ->
        val list = rows(constraints.maxWidth.toDp())
        val loose = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val needed = subcompose(Slot.Probe) {
            Column(Modifier.fillMaxWidth()) { list.forEach { it() } }
        }.sumOf { it.measure(loose).height }
        val height = minOf(needed, maxHeight.roundToPx()).coerceIn(constraints.minHeight, constraints.maxHeight)
        val placeables = subcompose(Slot.List) {
            PagedColumn(Modifier.fillMaxWidth().height(height.toDp())) {
                list.forEach { row -> item { row() } }
            }
        }.map { it.measure(constraints.copy(minHeight = height, maxHeight = height)) }
        val width = placeables.maxOfOrNull { it.width } ?: constraints.minWidth
        layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), height) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}

private enum class Slot { Probe, List }
