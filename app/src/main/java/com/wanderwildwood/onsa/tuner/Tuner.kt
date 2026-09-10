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
package com.wanderwildwood.onsa.tuner

import android.content.Context
import android.net.Uri
import com.wanderwildwood.onsa.instruments.Instrument
import com.wanderwildwood.onsa.misc.DefaultValues
import com.wanderwildwood.onsa.misc.MemoryPool
import com.wanderwildwood.onsa.misc.WaveWriter
import com.wanderwildwood.onsa.notedetection.AcousticZeroWeighting
import com.wanderwildwood.onsa.notedetection.FrequencyDetectionCollectedResults
import com.wanderwildwood.onsa.notedetection.FrequencyDetectionResultCollector
import com.wanderwildwood.onsa.notedetection.FrequencyEvaluationResult
import com.wanderwildwood.onsa.notedetection.FrequencyEvaluator
import com.wanderwildwood.onsa.notedetection.launchSoundSourceJob
import com.wanderwildwood.onsa.notedetection.testFunction
import com.wanderwildwood.onsa.preferences.PreferenceResources
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.musicalscale.MusicalScale2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Tuner.
 * @note Tuner starts after calling `connect()` and pauses when calling `disconnect()`.
 * @param pref Preferences tuner parameters.
 * @param instrument Currently used instrument (which notes are possible target notes).
 * @param musicalScale Musical scale which maps frequencies to notes.
 * @param scope Coroutine scope where the tuner runs.
 * @param onResultAvailableListener Listener to which we send results (on Dispatchers.Main)
 */
class Tuner(
    private val pref: PreferenceResources,
    private var instrument: StateFlow<Instrument>,
    private var musicalScale: StateFlow<MusicalScale2>,
    private val scope: CoroutineScope,
    private val onResultAvailableListener: OnResultAvailableListener
) {
    /** Callback which is used to send results. */
    interface OnResultAvailableListener {
        /** Method called when a new frequency is detected.
         * @param result The frequency detection result.
         */
        fun onFrequencyDetected(result: FrequencyDetectionCollectedResults)

        /** Method called when a detected frequency is postprocessed.
         * This means the resulting frequency is filtered, smoothed and a target note is set.
         * @param result The postprocessing result.
         */
        fun onFrequencyEvaluated(result: FrequencyEvaluationResult)
    }

    private val _userDefinedTargetNote = MutableStateFlow<MusicalNote?>(null)
    private val userDefinedTargetNote = _userDefinedTargetNote.asStateFlow()

    // private sealed class Command {
    //     data class Reconnect(
    //         val scope: CoroutineScope, val listener: OnResultAvailableListener
    //     ) : Command()
    //     data object Disconnect : Command()
    //     data object ChangePreferences : Command()
    // }
    private enum class Command { Reconnect, Disconnect, ChangePreferences }
    private val channel = Channel<Command>(Channel.CONFLATED)
    private val restartChannel = Channel<Command>(Channel.CONFLATED)

    private val waveWriter = WaveWriter()

    init {
        scope.launch {
            pref.overlap.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.windowSize.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.windowing.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.numMovingAverage.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.toleranceInCents.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.pitchHistoryNumFaultyValues.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.maxNoise.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.minHarmonicEnergyContent.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.sensitivity.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        // TODO: make sure to call: changeMusicalScale on changes!
//        scope.launch {
//            pref.musicalScale.collect { restartChannel.trySend(Command.ChangePreferences) }
//        }
        scope.launch {
            pref.waveWriterDurationInSeconds.collect { restartChannel.trySend(Command.ChangePreferences) }
        }

        scope.launch {
            instrument.collect { restartChannel.trySend(Command.ChangePreferences) }
        }

        scope.launch {
            musicalScale.collect { restartChannel.trySend(Command.ChangePreferences) }
        }

        scope.launch {
//            var job: Job? = null
            // reconnect is outside the following merge, so it will keep track if there is
            // a disconnect ore a connect
            var reconnect = false
            merge(channel.consumeAsFlow(), restartChannel.consumeAsFlow())
                .map { v ->
                    when (v) {
                        Command.Reconnect -> {
                            reconnect = true
                        }
                        Command.Disconnect -> {
                            reconnect = false
                        }
                        // in case of a setting-change, we do emit the current reconnect state
                        // ... so in the end it will recreate the task in the collectLatest block
                        // if reconnect is currently on, else it wont.
                        else -> {}
                    }
                    reconnect
                }
                .buffer(1, BufferOverflow.DROP_OLDEST)
                // collectLatest will CANCEL running jobs (run) when a new value is emitted
                // so each time a value arrives, we cancel the run job, and only restart it
                // if reconnect is not null
                .collectLatest { r ->
                    // Log.v("Tuner", "Tuner: buffer before delay")
                    // sometimes, several settings are changed at the same time,
                    // which would lead to many tuner restarts in a very short time.
                    // the small delay avoid these restarts.
                    delay(50)
                    // Log.v("Tuner", "Tuner: buffer after delay $v, $reconnect, $isActive")
                    if (isActive && r)
                        run()
                    // Log.v("Tuner", "Tuner: after 'if run'")
                }
        }
    }

    /** Start the tuner job. */
    fun connect() {
        channel.trySend(Command.Reconnect)
    }

    /** Stop the tuner job. */
    fun disconnect() {
        channel.trySend(Command.Disconnect)
    }

    private suspend fun run() = coroutineScope {
//        Log.v("Tuner", "Tuner: start running again ...")
        waveWriter.setBufferSize(pref.sampleRate * pref.waveWriterDurationInSeconds.value)

        val frequencyDetectionResultsChannel =
            Channel<MemoryPool<FrequencyDetectionCollectedResults>.RefCountedMemory>(
                capacity = 2,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
                onUndeliveredElement = { launch { it.decRef() } }
        )

        // Log.v("Tuner", "Tuner: overlap=${pref.overlap.value}, windowSize=${pref.windowSize.value}")
        // Log.v("Tuner", "Tuner: samplerate=${pref.sampleRate}, windowing=${pref.windowing.value}")
        val soundSourceJobAndChannel = launchSoundSourceJob(
            overlap = pref.overlap.value,
            windowSize = pref.windowSize.value,
            sampleRate = pref.sampleRate,
            testFunction = testFunction,
            waveWriter = if (pref.waveWriterDurationInSeconds.value > 0) waveWriter else null
        )

        launch(Dispatchers.Default) {
            val frequencyDetectionResultCollector = FrequencyDetectionResultCollector(
                frequencyMin = DefaultValues.FREQUENCY_MIN,
                frequencyMax = DefaultValues.FREQUENCY_MAX,
                subharmonicsTolerance = 0.1f,
                subharmonicsPeakRatio = 0.75f,
                harmonicTolerance = 0.11f,
                minimumFactorOverLocalMean = 3f,
                maxGapBetweenHarmonics = 5,
                maxNumHarmonicsForInharmonicity = 8,
                windowType = pref.windowing.value,
                acousticWeighting = AcousticZeroWeighting()
            )

            for (sampleData in soundSourceJobAndChannel.channel) {
                val result = frequencyDetectionResultCollector.collectResults(sampleData)
                result.incRef()
                frequencyDetectionResultsChannel.trySend(result)
                sampleData.decRef() // sampleData is not needed anymore, so we can decrement ref to allow recycling
                withContext(Dispatchers.Main) {
                    onResultAvailableListener.onFrequencyDetected(result.memory) // better send this to freqEval flow and directly afterwards run the listener
                }
                result.decRef()
            }
        }

        // Log.v("Tuner", "Tuner: movingAvg=${pref.numMovingAverage.value}, tolerance=${pref.toleranceInCents.value}")
        // Log.v("Tuner", "Tuner: numFaulty=${pref.pitchHistoryNumFaultyValues.value}, maxNoise=${pref.maxNoise.value}")
        // Log.v("Tuner", "Tuner: minHarmonicEnergy=${pref.minHarmonicEnergyContent.value}, sensitivity=${pref.sensitivity.value}")
        launch(Dispatchers.Main) {
            val freqEvaluator = FrequencyEvaluator(
                pref.numMovingAverage.value,
                pref.toleranceInCents.value.toFloat(),
                pref.pitchHistoryNumFaultyValues.value,
                pref.maxNoise.value,
                pref.minHarmonicEnergyContent.value,
                pref.sensitivity.value.toFloat(),
                musicalScale.value,
                instrument.value
            )

            frequencyDetectionResultsChannel
                .consumeAsFlow()
                .combine(userDefinedTargetNote) {  noteDetectionResult, targetNote ->
                    val result = freqEvaluator.evaluate(noteDetectionResult.memory, targetNote)
                    noteDetectionResult.decRef()
                    result
                }
                .flowOn(Dispatchers.Default)
                .collect {
                    onResultAvailableListener.onFrequencyEvaluated(it)
                }
        }
    }

    suspend fun storeCurrentWaveWriterSnapshot() {
        waveWriter.storeSnapshot()
    }

    suspend fun writeStoredWaveWriterSnapshot(context: Context, uri: Uri, sampleRate: Int) {
        waveWriter.writeStoredSnapshot(context, uri, sampleRate)
    }
}