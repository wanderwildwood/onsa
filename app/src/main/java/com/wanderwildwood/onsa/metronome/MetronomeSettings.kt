package com.wanderwildwood.onsa.metronome

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.wanderwildwood.onsa.R
import kotlinx.serialization.Serializable

/** Ticks between two beats: none, or the beat split into two, three or four. */
@Serializable
enum class Subdivision(val perBeat: Int, @StringRes val label: Int) {
    None(1, R.string.metronome_subdivision_none),
    Eighths(2, R.string.metronome_subdivision_eighths),
    Triplets(3, R.string.metronome_subdivision_triplets),
    Sixteenths(4, R.string.metronome_subdivision_sixteenths),
}

/** The click. The sounds come from Metronome by Michael Moessner (GPL-3.0), 16-bit mono at 48 kHz. */
@Serializable
enum class ClickSound(@RawRes val raw: Int, @StringRes val label: Int) {
    Woodblock(R.raw.metronome_woodblock, R.string.metronome_sound_woodblock),
    Claves(R.raw.metronome_claves, R.string.metronome_sound_claves),
    Sticks(R.raw.metronome_sticks, R.string.metronome_sound_sticks),
    HiHat(R.raw.metronome_hihat, R.string.metronome_sound_hihat),
    Drum(R.raw.metronome_drum, R.string.metronome_sound_drum),
}

@Serializable
data class MetronomeSettings(
    val bpm: Int = 96,
    val beatsPerBar: Int = 4,
    val accentFirstBeat: Boolean = true,
    val subdivision: Subdivision = Subdivision.None,
    val sound: ClickSound = ClickSound.Woodblock,
) {
    /** The same settings, held inside the ranges the metronome plays. */
    fun valid() = copy(
        bpm = bpm.coerceIn(MIN_BPM, MAX_BPM),
        beatsPerBar = beatsPerBar.coerceIn(1, MAX_BEATS_PER_BAR),
    )

    companion object {
        const val MIN_BPM = 30
        const val MAX_BPM = 250
        const val MAX_BEATS_PER_BAR = 12
    }
}

/** A saved tempo: a name the reader gave it and the settings it brings back. */
@Serializable
data class MetronomePreset(
    val id: Long,
    val name: String,
    val settings: MetronomeSettings,
)
