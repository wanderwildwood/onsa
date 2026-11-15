package com.wanderwildwood.onsa.metronome

import com.wanderwildwood.onsa.isSecondCopyFromLauncher
import com.wanderwildwood.onsa.ui.screens.presetName
import com.wanderwildwood.onsa.viewmodels.isNameTaken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MetronomeRunsTest {
    @Test
    fun aRunWhoseSoundFailsStopsAndSaysSo() {
        val runs = MetronomeRuns()
        val run = runs.begin()!!
        runs.failed(run)
        assertFalse(runs.playing.value)
        assertTrue(runs.failed.value)
        runs.clearFailed()
        assertFalse(runs.failed.value)
    }

    @Test
    fun theNextStartTakesTheFailureAway() {
        val runs = MetronomeRuns()
        runs.failed(runs.begin()!!)
        runs.begin()
        assertFalse(runs.failed.value)
        assertTrue(runs.playing.value)
    }

    @Test
    fun aRunAlreadyEndedFailingChangesNothing() {
        val runs = MetronomeRuns()
        val old = runs.begin()!!
        runs.end()
        val now = runs.begin()!!
        runs.failed(old)
        assertFalse(runs.failed.value)
        assertTrue(runs.isLive(now))
        assertTrue(runs.playing.value)
    }

    @Test
    fun playingChangesInsideThePress() {
        val runs = MetronomeRuns()
        assertFalse(runs.playing.value)
        val run = runs.begin()
        assertNotNull(run)
        assertTrue(runs.playing.value)
        assertTrue(runs.isLive(run!!))
        runs.end()
        assertFalse(runs.playing.value)
        assertFalse(runs.isLive(run))
    }

    @Test
    fun everyPressToggles() {
        // What Start/Stop does: read playing, then begin or end. Thirty presses with nothing
        // in between (no audio thread, no service) must alternate without one being lost.
        val runs = MetronomeRuns()
        for (press in 1..30) {
            if (runs.playing.value) runs.end() else assertNotNull(runs.begin())
            assertEquals(press % 2 == 1, runs.playing.value)
        }
    }

    @Test
    fun aSecondStartWhilePlayingStartsNothing() {
        val runs = MetronomeRuns()
        val first = runs.begin()!!
        assertNull(runs.begin())
        assertTrue(runs.isLive(first))
    }

    @Test
    fun anOldThreadCannotEndANewerRun() {
        val runs = MetronomeRuns()
        val old = runs.begin()!!
        runs.end()
        val new = runs.begin()!!
        // The old audio thread only now gets to its finally block.
        runs.finished(old)
        assertTrue(runs.playing.value)
        assertTrue(runs.isLive(new))
        assertFalse(runs.isLive(old))
    }

    @Test
    fun anOldThreadCannotShowABeatInANewerRun() {
        val runs = MetronomeRuns()
        val old = runs.begin()!!
        runs.showBeat(old, 2)
        assertEquals(2, runs.beat.value)
        runs.end()
        assertEquals(MetronomeRuns.NO_BEAT, runs.beat.value)
        val new = runs.begin()!!
        runs.showBeat(old, 3)
        assertEquals(MetronomeRuns.NO_BEAT, runs.beat.value)
        runs.showBeat(new, 0)
        assertEquals(0, runs.beat.value)
    }

    @Test
    fun aThreadThatDiesOnItsOwnEndsPlaying() {
        val runs = MetronomeRuns()
        val run = runs.begin()!!
        runs.showBeat(run, 1)
        runs.finished(run)
        assertFalse(runs.playing.value)
        assertEquals(MetronomeRuns.NO_BEAT, runs.beat.value)
        assertNotNull(runs.begin())
    }

    @Test
    fun aSlowThreadFromEveryEarlierRunLeavesTheLastOneAlone() {
        // Real threads standing in for audio threads that outlive their Stop: each waits until
        // every press has been made, then reports that it finished.
        val runs = MetronomeRuns()
        val allPressed = CountDownLatch(1)
        val threads = ArrayList<Thread>()
        var last: MetronomeRuns.Run? = null
        for (press in 1..41) {
            if (runs.playing.value) {
                runs.end()
            } else {
                val run = runs.begin()!!
                last = run
                threads += Thread {
                    allPressed.await(5, TimeUnit.SECONDS)
                    if (!runs.isLive(run)) runs.finished(run)
                }.also { it.start() }
            }
        }
        allPressed.countDown()
        threads.forEach { it.join(5000) }
        assertTrue(runs.playing.value)
        assertTrue(runs.isLive(last!!))
    }

    @Test
    fun theServiceFollowsWhatIsWantedNow() {
        // Start arrives while playing is wanted: hold the foreground.
        assertEquals(ServiceStep.Hold, serviceStep(play = true, stopAsked = false, playing = true))
        // Start arrives after the reader already pressed Stop: foreground as required, then go.
        assertEquals(ServiceStep.HoldThenLetGo, serviceStep(play = true, stopAsked = false, playing = false))
        // Stop arrives and nothing is wanted: go.
        assertEquals(ServiceStep.LetGo, serviceStep(play = false, stopAsked = false, playing = false))
        // Stop arrives after the reader already pressed Start again: an old command, left alone.
        assertEquals(ServiceStep.Nothing, serviceStep(play = false, stopAsked = false, playing = true))
        // The notification's Stop always stops.
        assertEquals(ServiceStep.LetGo, serviceStep(play = false, stopAsked = true, playing = true))
        assertEquals(ServiceStep.LetGo, serviceStep(play = false, stopAsked = true, playing = false))
    }

    @Test
    fun onlyALauncherCopyOnTopOfTheAppCloses() {
        val launcher = setOf("android.intent.category.LAUNCHER")
        assertTrue(isSecondCopyFromLauncher(false, "android.intent.action.MAIN", launcher))
        assertFalse(isSecondCopyFromLauncher(true, "android.intent.action.MAIN", launcher))
        assertFalse(isSecondCopyFromLauncher(false, "android.intent.action.VIEW", null))
        assertFalse(isSecondCopyFromLauncher(false, "android.intent.action.MAIN", null))
        assertFalse(isSecondCopyFromLauncher(false, null, null))
    }

    @Test
    fun aTempoKeepsItsDefaultNameWhenNothingIsTyped() {
        assertEquals("100 bpm, 5 beats", presetName("", "100 bpm, 5 beats"))
        assertEquals("100 bpm, 5 beats", presetName("   ", "100 bpm, 5 beats"))
        assertEquals("Waltz", presetName(" Waltz ", "100 bpm, 5 beats"))
    }

    @Test
    fun aTemperamentNameAlreadyInUseIsNoticed() {
        val others = listOf("Meantone", " Well tempered ")
        assertTrue(isNameTaken("Meantone", others))
        assertTrue(isNameTaken(" meantone ", others))
        assertTrue(isNameTaken("Well tempered", others))
        assertFalse(isNameTaken("Meantone 2", others))
        assertFalse(isNameTaken("", others))
        assertFalse(isNameTaken("   ", listOf("")))
    }
}
