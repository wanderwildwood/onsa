package com.wanderwildwood.onsa.ui

import com.wanderwildwood.onsa.ui.notes.cellsPerRow
import com.wanderwildwood.onsa.ui.notes.rowRanges
import org.junit.Assert.assertEquals
import org.junit.Test

class TableRowsTest {
    @Test
    fun cellsThatFitTheWidth() {
        assertEquals(4, cellsPerRow(230f, 52f))
        assertEquals(5, cellsPerRow(260f, 52f))
        // Never fewer than asked for, however narrow.
        assertEquals(2, cellsPerRow(40f, 52f))
        assertEquals(3, cellsPerRow(40f, 52f, atLeast = 3))
        assertEquals(2, cellsPerRow(200f, 0f))
    }

    @Test
    fun equalTemperamentFiveInRowsOfFour() {
        // Five notes and the octave: six cells.
        assertEquals(listOf(0..3, 4..5), rowRanges(6, 4))
    }

    @Test
    fun nineteenNotesAllShown() {
        val rows = rowRanges(20, 4)
        assertEquals(5, rows.size)
        assertEquals((0 until 20).toList(), rows.flatten())
    }

    @Test
    fun emptyAndOddSizes() {
        assertEquals(emptyList<IntRange>(), rowRanges(0, 4))
        assertEquals(listOf(0..2), rowRanges(3, 4))
        assertEquals(listOf(0..0, 1..1), rowRanges(2, 0))
    }
}
