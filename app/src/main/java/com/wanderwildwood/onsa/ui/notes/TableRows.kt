package com.wanderwildwood.onsa.ui.notes

import kotlin.math.floor

/**
 * How many cells of [cellWidth] fit side by side in [available] (both in the same unit), never
 * fewer than [atLeast]. The key note dialog lays its cents table and circle of fifths out in
 * rows this wide, instead of one long row that scrolled sideways out of sight.
 */
internal fun cellsPerRow(available: Float, cellWidth: Float, atLeast: Int = 2): Int {
    if (cellWidth <= 0f || available.isNaN()) return atLeast
    return floor(available / cellWidth).toInt().coerceAtLeast(atLeast)
}

/** The indices 0 until [count] in runs of [perRow]; the last run may be shorter. */
internal fun rowRanges(count: Int, perRow: Int): List<IntRange> {
    if (count <= 0) return emptyList()
    val step = perRow.coerceAtLeast(1)
    return (0 until count step step).map { start -> start until minOf(start + step, count) }
}
