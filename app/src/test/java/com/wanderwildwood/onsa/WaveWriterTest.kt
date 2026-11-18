package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.misc.WaveWriter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveWriterTest {
    @Test
    fun snapshotIsLetGoEvenWhenItCannotBeWritten() = runBlocking {
        val writer = WaveWriter()
        writer.setBufferSize(4)
        writer.appendData(floatArrayOf(0.1f, 0.2f, 0.3f))
        writer.storeSnapshot()
        assertNotNull(snapshotOf(writer))
        // no file to write to: the write fails and reports it
        assertFalse(writer.writeStoredSnapshot(null, null, 44100))
        // and the snapshot is gone all the same
        assertNull(snapshotOf(writer))
    }

    @Test
    fun droppedSnapshotIsNotWritten() = runBlocking {
        val writer = WaveWriter()
        writer.setBufferSize(4)
        writer.appendData(floatArrayOf(0.1f, 0.2f))
        writer.storeSnapshot()
        assertNotNull(snapshotOf(writer))
        writer.dropSnapshot()
        assertNull(snapshotOf(writer))
    }

    @Test
    fun clearedRecordingLeavesAnEmptySnapshot() = runBlocking {
        val writer = WaveWriter()
        writer.setBufferSize(4)
        writer.appendData(floatArrayOf(0.1f, 0.2f, 0.3f))
        writer.clear()
        // appending after a clear keeps nothing until the next setBufferSize
        writer.appendData(floatArrayOf(0.4f))
        writer.storeSnapshot()
        val snapshot = snapshotOf(writer)
        assertTrue(snapshot != null && snapshot.isEmpty())
    }

    private fun snapshotOf(writer: WaveWriter): FloatArray? {
        val field = WaveWriter::class.java.getDeclaredField("snapShot")
        field.isAccessible = true
        return field.get(writer) as FloatArray?
    }
}
