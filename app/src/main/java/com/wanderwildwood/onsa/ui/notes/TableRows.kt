package com.wanderwildwood.onsa.ui.notes

import com.wanderwildwood.onsa.notenames.BaseNote
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.notenames.NoteModifier
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

/** How many signs [modifier] draws beside its letter (♯, ♭, and the up and down arrows). */
internal fun modifierSigns(modifier: NoteModifier): Int {
    val name = modifier.name
    return listOf("Flat", "Sharp", "Up", "Down").sumOf { sign -> name.windowed(sign.length).count { it == sign } }
}

/** The signs a note's heading draws: letter and signs, and its enharmonic's after a slash. */
internal fun headingSigns(note: MusicalNote): Int {
    var signs = if (note.base == BaseNote.None) 0 else 1 + modifierSigns(note.modifier)
    if (note.enharmonicBase != BaseNote.None) signs += 1 + modifierSigns(note.enharmonicModifier)
    return signs
}

/**
 * Width one note of the cents table needs, in dp: 48 holds "D♯ / E♭" (four signs); each sign past
 * that takes about 10 more, so the headings of a larger temperament ("^C / D♭") do not run into
 * their neighbours.
 */
internal fun centCellWidth(notes: List<MusicalNote>): Float {
    val widest = notes.maxOfOrNull { headingSigns(it) } ?: 0
    return 48f + 10f * (widest - 4).coerceAtLeast(0)
}
