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

    @Volatile private var live: Run? = null

    /** Starts a run and returns it, or null when one is live already (nothing to start). */
    @Synchronized
    fun begin(): Run? {
        if (live != null) return null
        val run = Run()
        live = run
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

    companion object {
        const val NO_BEAT = -1
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
