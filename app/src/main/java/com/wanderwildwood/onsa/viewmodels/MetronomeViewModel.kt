package com.wanderwildwood.onsa.viewmodels

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import com.wanderwildwood.onsa.metronome.ClickSound
import com.wanderwildwood.onsa.metronome.MetronomeEngine
import com.wanderwildwood.onsa.metronome.MetronomePreset
import com.wanderwildwood.onsa.metronome.MetronomeResources
import com.wanderwildwood.onsa.metronome.MetronomeService
import com.wanderwildwood.onsa.metronome.MetronomeSettings
import com.wanderwildwood.onsa.metronome.Subdivision
import com.wanderwildwood.onsa.metronome.TapTempo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class MetronomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resources: MetronomeResources,
    private val engine: MetronomeEngine,
) : ViewModel() {
    val settings = resources.settings
    val presets = resources.presets
    val playing = engine.playing
    val beat = engine.beat

    private val tapTempo = TapTempo()

    private fun change(transform: (MetronomeSettings) -> MetronomeSettings) {
        resources.writeSettings(transform(settings.value))
        if (playing.value) {
            engine.update(settings.value)
            MetronomeService.play(context) // refreshes the notification's tempo
        }
    }

    fun stepBpm(by: Int) {
        tapTempo.reset()
        change { it.copy(bpm = it.bpm + by) }
    }

    fun setBpm(bpm: Int) {
        tapTempo.reset()
        change { it.copy(bpm = bpm) }
    }
    fun stepBeats(by: Int) = change { it.copy(beatsPerBar = it.beatsPerBar + by) }
    fun setAccent(on: Boolean) = change { it.copy(accentFirstBeat = on) }
    fun setSubdivision(s: Subdivision) = change { it.copy(subdivision = s) }
    fun setSound(s: ClickSound) = change { it.copy(sound = s) }

    fun tap() {
        tapTempo.tap(SystemClock.elapsedRealtime())?.let { bpm -> change { it.copy(bpm = bpm) } }
    }

    /**
     * Start or Stop. What the button says changes inside the press: the engine is told here,
     * on the spot, and the service (notification, wake lock) follows afterwards. Waiting for the
     * service to do it left a moment in which a second press read the old state.
     */
    fun startStop() {
        tapTempo.reset()
        if (playing.value) stop() else start()
    }

    private fun start() {
        engine.start(settings.value)
        MetronomeService.play(context)
    }

    fun stop() {
        if (!playing.value) return
        engine.stop()
        MetronomeService.quiet(context)
    }

    fun savePreset(name: String) = resources.addPreset(name)
    fun deletePreset(id: Long) = resources.deletePreset(id)
    fun loadPreset(preset: MetronomePreset) {
        tapTempo.reset()
        change { preset.settings }
    }
}
