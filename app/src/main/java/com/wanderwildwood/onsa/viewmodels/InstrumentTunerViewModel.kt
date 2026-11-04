/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.wanderwildwood.onsa.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.wanderwildwood.onsa.instruments.Instrument
import com.wanderwildwood.onsa.instruments.InstrumentResources
import com.wanderwildwood.onsa.notedetection.FrequencyDetectionCollectedResults
import com.wanderwildwood.onsa.notedetection.FrequencyEvaluationResult
import com.wanderwildwood.onsa.notedetection.TuningState
import com.wanderwildwood.onsa.notedetection.checkTuning
import com.wanderwildwood.onsa.preferences.PreferenceResources
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.musicalscale.MusicalScale2
import com.wanderwildwood.onsa.temperaments.TemperamentResources
import com.wanderwildwood.onsa.tuner.Tuner
import com.wanderwildwood.onsa.ui.instruments.StringWithInfo
import com.wanderwildwood.onsa.ui.instruments.StringsState
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions2
import com.wanderwildwood.onsa.ui.plot.GestureBasedViewPort
import com.wanderwildwood.onsa.ui.screens.InstrumentTunerData
import com.wanderwildwood.onsa.ui.tuning.PitchHistoryState
import com.wanderwildwood.onsa.ui.tuning.ShownReading
import com.wanderwildwood.onsa.ui.tuning.ShownReadingGate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InstrumentTunerViewModel @Inject constructor (
    val pref: PreferenceResources,
    val instruments: InstrumentResources,
    val temperaments: TemperamentResources
) : ViewModel(), InstrumentTunerData {
    override val musicalScale: StateFlow<MusicalScale2> get() = temperaments.musicalScale
    override val notePrintOptions: StateFlow<NotePrintOptions2> get() = pref.notePrintOptions
    override val toleranceInCents: StateFlow<Int> get() = pref.toleranceInCents

    private var autodetectedTargetNote = musicalScale.value.referenceNote
    private var currentSmoothedFrequency = musicalScale.value.referenceFrequency

    override var pitchHistoryState: PitchHistoryState = PitchHistoryState(
        computePitchHistorySize()
    )
        private set

    override val pitchHistoryGestureBasedViewPort: GestureBasedViewPort
            = GestureBasedViewPort()
    // The pitch detection updates these at its full rate; the screen sees them through
    // [shownGate], at most twice a second and only when what it draws would change.
    private var liveTuningState = TuningState.Unknown
    private var liveTargetNote = autodetectedTargetNote

    override var tuningState by mutableStateOf(TuningState.Unknown)
        private set

    override var targetNote by mutableStateOf(autodetectedTargetNote)
        private set

    private val shownGate = ShownReadingGate(liveReading())
    private var shownClock: Job? = null
    override var targetNoteForLockButton: MusicalNote by mutableStateOf(autodetectedTargetNote)
        private set

    override var selectedNoteKey by mutableStateOf<Int?>(null)
        private set

    override val instrument: StateFlow<Instrument> get() = instruments.currentInstrument

    override var strings by mutableStateOf<ImmutableList<StringWithInfo>?>(
        instrument.value.strings.mapIndexed { index, note ->
            StringWithInfo(note, index) //, musicalScale.value.getNoteIndex(note))
        }.toImmutableList()
    )
        private set

    override val stringsState = StringsState(-musicalScale.value.noteIndexBegin)

    override fun onStringClicked(key: Int, note: MusicalNote) {
        selectedNoteKey = if (selectedNoteKey == key) null else key
        handleTargetNoteOnSelectionChange(selectedNoteKey)
        // leave tuningState unknown in case, this should only be changed by the
        // frequencyEvaluator callback
        resetTuningState(null)
        showNow()
    }

    override fun onClearFixedTargetClicked() {
        selectedNoteKey = null
        handleTargetNoteOnSelectionChange(selectedNoteKey)
        resetTuningState(null)
        showNow()
    }

    private val tuner = Tuner(
        pref,
        instruments.currentInstrument,
        musicalScale,
        viewModelScope,
        onResultAvailableListener = object : Tuner.OnResultAvailableListener {
            override fun onFrequencyDetected(result: FrequencyDetectionCollectedResults) { }

            override fun onFrequencyEvaluated(result: FrequencyEvaluationResult) {
                if (result.smoothedFrequency > 0f) {
                    pitchHistoryState.record(result.smoothedFrequency)
                    currentSmoothedFrequency = result.smoothedFrequency

                    result.target?.let { tuningTarget ->
                        handleTargetNoteOnAutodetectChange(tuningTarget.note)
                    }
                }
                //Log.v("Tuner", "ScientificTunerViewModel: dt = ${result.timeSinceThereIsNoFrequencyDetectionResult}")
                resetTuningState(result.timeSinceThereIsNoFrequencyDetectionResult)
                shownGate.offer(liveReading())
            }
        }
    )


    init {
        viewModelScope.launch {
            pref.pitchHistoryDuration.collect {
                pitchHistoryState.resize(computePitchHistorySize())
            }
        }
        viewModelScope.launch {
            pref.windowSize.collect {
                pitchHistoryState.resize(computePitchHistorySize())
            }
        }
        viewModelScope.launch {
            pref.overlap.collect {
                pitchHistoryState.resize(computePitchHistorySize())
            }
        }

        viewModelScope.launch {
            instruments.currentInstrument.collect {
                strings = it.strings.mapIndexed { index, note ->
                    StringWithInfo(note, index) //, musicalScale.value.getNoteIndex(note))
                }.toImmutableList()
            }
        }
    }
    override fun startTuner() {
        tuner.connect()
        if (shownClock?.isActive != true) {
            shownClock = viewModelScope.launch {
                while (isActive) {
                    delay(ShownReadingGate.INTERVAL_MS)
                    shownGate.take()?.let { show(it) }
                }
            }
        }
    }
    override fun stopTuner() {
        tuner.disconnect()
        shownClock?.cancel()
        shownClock = null
    }

    private fun liveReading(): ShownReading {
        val scale = musicalScale.value
        return ShownReading.of(
            liveTargetNote,
            liveTuningState,
            currentSmoothedFrequency,
            scale.getNoteFrequency(scale.getNoteIndex2(liveTargetNote))
        )
    }

    private fun show(reading: ShownReading) {
        targetNote = reading.targetNote
        tuningState = reading.tuningState
        pitchHistoryState.publish()
    }

    /** A tap on a string or the lock button: shown at once, not on the next tick. */
    private fun showNow() {
        val reading = liveReading()
        shownGate.showNow(reading)
        show(reading)
    }

    private fun handleTargetNoteOnSelectionChange(
        selectedNoteKey: Int? = null,
    ) {
        this.selectedNoteKey = selectedNoteKey
        liveTargetNote = if (selectedNoteKey == null) {
            autodetectedTargetNote
        } else if (instrument.value.isChromatic) {
            musicalScale.value.getNote(
                musicalScale.value.noteIndexBegin + selectedNoteKey
            )
        } else {
            strings?.find { it.key == selectedNoteKey }?.note ?: autodetectedTargetNote
        }

        if (selectedNoteKey != null) {
            targetNoteForLockButton = liveTargetNote
        }
    }

    private fun handleTargetNoteOnAutodetectChange(
        autodetectedTargetNote: MusicalNote
    ) {
        this.autodetectedTargetNote = autodetectedTargetNote
        if (selectedNoteKey == null)
            liveTargetNote = autodetectedTargetNote
    }

    /** Only provide the timeWithoutFreqDetectionResult if available */
    private fun resetTuningState(timeSinceThereIsNoFrequencyDetectionResult: Float? = null) {
        liveTuningState = if (timeSinceThereIsNoFrequencyDetectionResult == null
            && liveTuningState == TuningState.Unknown) {
            TuningState.Unknown
        } else if (timeSinceThereIsNoFrequencyDetectionResult != null
            && timeSinceThereIsNoFrequencyDetectionResult > DURATION_FOR_MARKING_NOTEDETECTION_AS_INACTIVE) {
            TuningState.Unknown
        } else {
            val noteIndex = musicalScale.value.getNoteIndex2(liveTargetNote)
            checkTuning(
                currentSmoothedFrequency,
                musicalScale.value.getNoteFrequency(noteIndex),
                toleranceInCents.value.toFloat()
            )
        }
    }

    /** Compute number of samples to be stored in pitch history. */
    private fun computePitchHistorySize() = PitchHistoryState.computePitchHistorySize(
        pref.pitchHistoryDuration.value,
        pref.sampleRate, pref.windowSize.value,
        pref.overlap.value
    )

    companion object {
        const val DURATION_FOR_MARKING_NOTEDETECTION_AS_INACTIVE = 0.5f // in seconds
    }

}