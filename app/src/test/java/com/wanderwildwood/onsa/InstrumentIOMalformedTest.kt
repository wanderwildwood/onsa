package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.instruments.InstrumentIO
import org.junit.Assert.assertTrue
import org.junit.Test

/** A damaged instruments file is refused, not a crash. */
class InstrumentIOMalformedTest {
    @Test
    fun `a strings list cut off at the end of the file`() {
        val result = InstrumentIO.stringToInstruments("Version=1\n\nName=Lute\nStrings=[")
        assertTrue(result.instruments.isEmpty())
    }

    @Test
    fun `a strings list with something that is not a note`() {
        val result = InstrumentIO.stringToInstruments("Version=1\n\nName=Lute\nStrings=[nonsense]\nInstrument\n")
        assertTrue(result.instruments.isEmpty())
    }
}
