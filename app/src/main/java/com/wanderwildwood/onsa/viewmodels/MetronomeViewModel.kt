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
            MetronomeService.start(context) // refreshes the notification's tempo
        }
    }

    fun stepBpm(by: Int) = change { it.copy(bpm = it.bpm + by) }
    fun setBpm(bpm: Int) = change { it.copy(bpm = bpm) }
    fun stepBeats(by: Int) = change { it.copy(beatsPerBar = it.beatsPerBar + by) }
    fun setAccent(on: Boolean) = change { it.copy(accentFirstBeat = on) }
    fun setSubdivision(s: Subdivision) = change { it.copy(subdivision = s) }
    fun setSound(s: ClickSound) = change { it.copy(sound = s) }

    fun tap() {
        tapTempo.tap(SystemClock.elapsedRealtime())?.let { setBpm(it) }
    }

    fun startStop() {
        if (playing.value) MetronomeService.stop(context) else MetronomeService.start(context)
    }

    fun stop() {
        if (playing.value) MetronomeService.stop(context)
    }

    fun savePreset(name: String) = resources.addPreset(name)
    fun deletePreset(id: Long) = resources.deletePreset(id)
    fun loadPreset(preset: MetronomePreset) = change { preset.settings }
}
