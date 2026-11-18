package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.misc.readCappedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class ReadPickedTextTest {
    @Test
    fun smallFileIsReadWhole() {
        val text = "Version=0.1\n\nName=Guitar äö\n"
        val read = readCappedText(ByteArrayInputStream(text.toByteArray()), 1000)
        assertEquals(text, read)
    }

    @Test
    fun fileAtTheCapIsRead() {
        val bytes = ByteArray(20000) { 'a'.code.toByte() }
        assertEquals(20000, readCappedText(ByteArrayInputStream(bytes), 20000)?.length)
    }

    @Test
    fun fileOverTheCapIsRefused() {
        val bytes = ByteArray(20001) { 'a'.code.toByte() }
        assertNull(readCappedText(ByteArrayInputStream(bytes), 20000))
    }

    @Test
    fun emptyFileGivesEmptyText() {
        assertEquals("", readCappedText(ByteArrayInputStream(ByteArray(0)), 1000))
    }
}
