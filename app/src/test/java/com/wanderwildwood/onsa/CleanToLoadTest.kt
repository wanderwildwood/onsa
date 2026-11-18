package com.wanderwildwood.onsa

import org.junit.Assert.assertEquals
import org.junit.Test

/** A file of several temperaments where some have errors: the clean ones load, the rest are skipped. */
class CleanToLoadTest {
    private data class Item(val name: String, val broken: Boolean)

    @Test
    fun keepsCleanItemsInFileOrder() {
        val items = listOf(Item("a", false), Item("b", true), Item("c", false), Item("d", true))
        assertEquals(listOf("a", "c"), cleanToLoad(items, { it.broken }, { it.name }))
    }

    @Test
    fun itemThatDoesNotConvertIsSkippedToo() {
        val items = listOf(Item("a", false), Item("", false), Item("c", true))
        assertEquals(listOf("a"), cleanToLoad(items, { it.broken }, { it.name.ifEmpty { null } }))
    }

    @Test
    fun allBrokenLoadsNothing() {
        val items = listOf(Item("a", true), Item("b", true))
        assertEquals(emptyList<String>(), cleanToLoad(items, { it.broken }, { it.name }))
    }
}
