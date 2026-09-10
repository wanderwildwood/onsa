package com.wanderwildwood.onsa.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.wanderwildwood.onsa.hilt.ApplicationScope
import com.wanderwildwood.onsa.notedetection.WindowingFunction
import com.wanderwildwood.onsa.resources.ResourcesBase
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions2
import com.wanderwildwood.onsa.ui.notes.OctaveNotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.roundToInt

@Singleton
class PreferenceResources @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope val applicationScope: CoroutineScope
): ResourcesBase("settings", context, applicationScope) {
    @Serializable
    data class Appearance(
        val mode: NightMode = NightMode.Auto,
        val blackNightEnabled: Boolean = false,
        val useSystemColorAccents: Boolean = true
    )

    val sampleRate = 44100

    // migrations
    private val migrationsFromV6CompletePref = createPreference(
        "migrations_complete", false
    )
    val migrationsFromV6Complete = migrationsFromV6CompletePref.asStateFlow()
    fun writeMigrationsFromV6Complete() {
        migrationsFromV6CompletePref.set(true)
    }

    // appearance
    private val appearancePref = createSerializablePreference(
        "appearance", Appearance()
    )
    val appearance = appearancePref.asStateFlow()
    fun writeAppearance(appearance: Appearance) {
        appearancePref.set(appearance)
    }

    // keep screen on
    // Defaulted on, where upstream defaults it off.
    //
    // A tuner sits on a music stand and is glanced at between phrases, with both hands on
    // the instrument: you cannot put down a bow to wake a phone. Upstream is right to make
    // it a setting and it stays one — this only changes which way it starts.
    private val screenAlwaysOnPref = createPreference("screenon", true)
    val screenAlwaysOn = screenAlwaysOnPref.asStateFlow()
    fun writeScreenAlwaysOn(screenAlwaysOn: Boolean) {
        screenAlwaysOnPref.set(screenAlwaysOn)
    }

    // display on lock screen
    private val displayOnLockScreenPref = createPreference("onlockscreen", false)
    val displayOnLockScreen = displayOnLockScreenPref.asStateFlow()
    fun writeDisplayOnLockScreen(displayOnLockScreen: Boolean) {
        displayOnLockScreenPref.set(displayOnLockScreen)
    }

    // note print options
//    private val notePrintOptionsPref = createSerializablePreference(
//        "note_print_options", NotePrintOptions()
//    )
    private val notePrintOptionsPref = createSerializablePreference(
        "note_print_options",
        default = NotePrintOptions2(),
        onDeserializationFailure = {
            try {
                val old = Json.decodeFromString<NotePrintOptions>(it)
                NotePrintOptions2(
                    enharmonicVariant = if (old.useEnharmonic) 1 else 0,
                    octaveNotation = if (old.helmholtzNotation) OctaveNotation.Helmholtz else OctaveNotation.Scientific,
                    notationType = old.notationType
                )
            } catch (_: Exception) {
                NotePrintOptions2()
            }
        }
    )

    val notePrintOptions = notePrintOptionsPref.asStateFlow()
    fun writeNotePrintOptions(notePrintOptions: NotePrintOptions2) {
        notePrintOptionsPref.set(notePrintOptions)
    }

    fun switchEnharmonicPreference() {
        val currentEnharmonicVariant = notePrintOptions.value.enharmonicVariant
        writeNotePrintOptions(
            notePrintOptions.value.copy(
                enharmonicVariant = if (currentEnharmonicVariant == 0) 1 else 0,
            )
        )
    }

    // scientific mode
    private val scientificModePref = createPreference("scientific_mode", false)
    val scientificMode = scientificModePref.asStateFlow()
    fun writeScientificMode(scientificMode: Boolean) {
        scientificModePref.set(scientificMode)
    }

    // windowing
    private val windowingPref = createTransformablePreference(
        "windowing",
        default = WindowingFunction.Tophat,
        makeSerializable = { it.name },
        fromSerializable = { WindowingFunction.valueOf(it) },
        onDeserializationFailure = {
            // old style of storing did not use serialization, but directly store the string. we catch this
            try {
                WindowingFunction.valueOf(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    )
    val windowing = windowingPref.asStateFlow()
    fun writeWindowing(windowing: WindowingFunction) {
        windowingPref.set(windowing)
    }

    // overlap
    private val overlapPref = createPreference("overlap", default = 75f / 100f)
    val overlap = overlapPref.asStateFlow()
    fun writeOverlap(overlapPercent: Int) {
        overlapPref.set(overlapPercent.toFloat() / 100f)
    }

    // window size
    private val windowSizeExponentPref = createPreference("window_size_exponent", 12)
    val windowSizeExponent = windowSizeExponentPref.asStateFlow()
    val windowSize = windowSizeExponent
        .map { 2f.pow(it).roundToInt() }
        .stateIn(applicationScope, SharingStarted.Eagerly, 2f.pow(12).roundToInt())

    fun writeWindowSize(windowSizeExponent: Int) {
        windowSizeExponentPref.set(windowSizeExponent)
    }

    // pitch history duration
    private val pitchHistoryDurationPref = createPreference(
        "pitch_history_duration", 1.5f
    )
    val pitchHistoryDuration = pitchHistoryDurationPref.asStateFlow()
    fun writePitchHistoryDuration(pitchHistoryDuration: Float) {
        pitchHistoryDurationPref.set(pitchHistoryDuration)
    }

    private val pitchHistoryNumFaultyValuesPref = createPreference(
        "pitch_history_num_faulty_values", 3
    )
    val pitchHistoryNumFaultyValues = pitchHistoryNumFaultyValuesPref.asStateFlow()
    fun writePitchHistoryNumFaultyValues(pitchHistoryNumFaultyValues: Int) {
        pitchHistoryNumFaultyValuesPref.set(pitchHistoryNumFaultyValues)
    }

    private val numMovingAveragePref = createPreference("num_moving_average", 5)
    val numMovingAverage = numMovingAveragePref.asStateFlow()
    fun writeNumMovingAverage(numMovingAverage: Int) {
        numMovingAveragePref.set(numMovingAverage)
    }

    private val maxNoisePref = createPreference("max_noise", 0.1f)
    val maxNoise = maxNoisePref.asStateFlow()
    fun writeMaxNoise(maxNoisePercent: Int) {
        maxNoisePref.set(maxNoisePercent.toFloat() / 100f)
    }

    private val minHarmonicEnergyContentPref = createPreference(
        "min_harmonic_energy_content", 0.1f
    )
    val minHarmonicEnergyContent = minHarmonicEnergyContentPref.asStateFlow()
    fun writeMinHarmonicEnergyContent(minHarmonicEnergyContentPercent: Int) {
        minHarmonicEnergyContentPref.set(minHarmonicEnergyContentPercent.toFloat() / 100f)
    }

    private val sensitivityPref = createPreference("sensitivity", 90)
    val sensitivity = sensitivityPref.asStateFlow()
    fun writeSensitivity(sensitivity: Int) {
        sensitivityPref.set(sensitivity)
    }

    private val toleranceInCentsPref = createPreference("tolerance_in_cents", 5)
    val toleranceInCents = toleranceInCentsPref.asStateFlow()
    fun writeToleranceInCents(toleranceInCents: Int) {
        toleranceInCentsPref.set(toleranceInCents)
    }

    private val waveWriterDurationInSecondsPref = createPreference(
        "wave_writer_duration_in_seconds", 0
    )
    val waveWriterDurationInSeconds = waveWriterDurationInSecondsPref.asStateFlow()
    fun writeWaveWriterDurationInSeconds(waveWriterDurationInSeconds: Int) {
        waveWriterDurationInSecondsPref.set(waveWriterDurationInSeconds)
    }

    fun resetAllSettings() {
        resetAllPreferences()
    }

    companion object {
        const val ReferenceFrequencyDefault = 440f
    }
}