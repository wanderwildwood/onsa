package com.wanderwildwood.onsa.metronome

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the metronome is meant to be playing, and which run of the audio thread is the live
 * one.
 *
 * [playing] is what the reader asked for, and it changes inside the press itself: [begin] and
 * [end] never wait for the audio thread or for the service. Each [begin] hands out a new [Run],
 * and only the live run may show a beat or say that playing has ended, so an audio thread that
 * is still winding down from an earlier Stop cannot end the run a later Start began.
 */
class MetronomeRuns {
    /** One start of the audio thread. Only its identity matters. */
    class Run internal constructor()

    private val _playing = MutableStateFlow(false)
    val playing = _playing.asStateFlow()

    private val _beat = MutableStateFlow(NO_BEAT)
    val beat = _beat.asStateFlow()

    /** The last run stopped because its sound could not be played; said on screen until cleared. */
    private val _failed = MutableStateFlow(false)
    val failed = _failed.asStateFlow()

    @Volatile private var live: Run? = null

    /** Starts a run and returns it, or null when one is live already (nothing to start). */
    @Synchronized
    fun begin(): Run? {
        if (live != null) return null
        val run = Run()
        live = run
        _failed.value = false
        _playing.value = true
        return run
    }

    /** Ends whatever run is live. Its audio thread sees this at its next look and winds down. */
    @Synchronized
    fun end() {
        live = null
        _playing.value = false
        _beat.value = NO_BEAT
    }

    /** True while [run] is the one meant to be heard. The audio thread asks this every chunk. */
    fun isLive(run: Run): Boolean = live === run

    /** Shows [beat], unless [run] has been ended since. */
    @Synchronized
    fun showBeat(run: Run, beat: Int) {
        if (live === run) _beat.value = beat
    }

    /**
     * The audio thread of [run] is over, of its own accord or because it was ended. Playing
     * stops only if [run] is still the live one: a run that was ended already changes nothing.
     */
    @Synchronized
    fun finished(run: Run) {
        if (live === run) end()
    }

    /**
     * The sound of [run] could not be played (no audio track, or it broke off). If [run] is still
     * the live one, playing stops and [failed] says so; a run already ended changes nothing.
     */
    @Synchronized
    fun failed(run: Run) {
        if (live !== run) return
        end()
        _failed.value = true
    }

    /** The failure has been read: the next press or leaving the screen takes it away. */
    fun clearFailed() {
        _failed.value = false
    }

    companion object {
        const val NO_BEAT = -1
    }
}

/**
 * Reads what each blocking AudioTrack.write said. A lost audio route or a dead track makes write
 * return an error code rather than throw, and does not block: the clicks would go silent while
 * the loop spun and the notification stayed. A negative count, or [MAX_EMPTY] writes in a row
 * that took nothing, means the sound has broken off.
 */
class TrackWrites {
    private var empty = 0

    /** Whether the sound is still going after a write that returned [result]. */
    fun accept(result: Int): Boolean {
        if (result < 0) return false
        if (result > 0) {
            empty = 0
            return true
        }
        empty++
        return empty < MAX_EMPTY
    }

    companion object {
        const val MAX_EMPTY = 5
    }
}

/** What the service does with a command, given whether the metronome is meant to be playing. */
enum class ServiceStep {
    /** Go to the foreground, show the notification, hold the wake lock. */
    Hold,

    /** Go to the foreground as the system requires of a service started that way, then let go. */
    HoldThenLetGo,

    /** Drop the notification and the wake lock and stop. */
    LetGo,

    /** Leave everything as it is: a later command is on its way. */
    Nothing,
}

/**
 * The view model sets [MetronomeRuns.playing] first and then sends the service a command, PLAY
 * after a start and QUIET after a stop. Commands arrive later and in order, and by then the
 * reader may have pressed again, so the service follows what is wanted now and not what the
 * command said: a QUIET that arrives while playing is wanted is an old one and is left alone.
 * [stopAsked] is the notification's own Stop, which the service carries out itself.
 */
fun serviceStep(play: Boolean, stopAsked: Boolean, playing: Boolean): ServiceStep = when {
    stopAsked -> ServiceStep.LetGo
    play -> if (playing) ServiceStep.Hold else ServiceStep.HoldThenLetGo
    else -> if (playing) ServiceStep.Nothing else ServiceStep.LetGo
}
