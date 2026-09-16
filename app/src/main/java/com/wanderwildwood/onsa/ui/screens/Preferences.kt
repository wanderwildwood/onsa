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
package com.wanderwildwood.onsa.ui.screens

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.lazy.LazyRowMMD
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.ui.misc.TunerScaffoldWithoutBottomBar
import com.wanderwildwood.onsa.ui.misc.rememberNumberFormatter
import com.wanderwildwood.onsa.ui.notes.asAnnotatedString
import com.wanderwildwood.onsa.ui.preferences.Section
import com.wanderwildwood.onsa.ui.preferences.SimplePreference
import com.wanderwildwood.onsa.ui.preferences.SliderPreference
import com.wanderwildwood.onsa.ui.preferences.SwitchPreference
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import com.wanderwildwood.onsa.viewmodels.PreferencesViewModel
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalResources
import com.wanderwildwood.onsa.ui.preferences.LanguageSelections
import kotlinx.coroutines.delay

@Composable
fun Preferences(
    viewModel: PreferencesViewModel,
    modifier: Modifier = Modifier,
    onNavigateUpClicked: () -> Unit = {},
    onAppearanceClicked: () -> Unit = {},
    onReferenceFrequencyClicked: () -> Unit = {},
    onLanguageClicked: () -> Unit = {},
    onNotationClicked: () -> Unit = {},
    onTemperamentClicked: () -> Unit = {},
    onWindowingFunctionClicked: () -> Unit = {},
    onStretchTuningClicked: () -> Unit = {},
    onResetClicked: () -> Unit = {},
    onAboutClicked: () -> Unit = {}
) {
    val pref = viewModel.pref
    val context = LocalContext.current

    val notePrintOptions by pref.notePrintOptions.collectAsStateWithLifecycle()
    val musicalScale by viewModel.musicalScale.collectAsStateWithLifecycle()
    val decimalFormat = rememberNumberFormatter()

    TunerScaffoldWithoutBottomBar(
        modifier = modifier,
        canNavigateUp = true,
        onNavigateUpClicked = onNavigateUpClicked,
        title = stringResource(id = R.string.settings),
        showPreferenceButton = false,
    ) { paddingValues ->
        LazyColumnMMD(
            modifier = Modifier.consumeWindowInsets(paddingValues),
            contentPadding = paddingValues
        ) {
            item {
                Section(
                    title = stringResource(id = R.string.basic)
                )
            }

            item {
                val selectedLocale = remember {
                    LanguageSelections.entries.firstOrNull {
                        it.tag == AppCompatDelegate.getApplicationLocales()[0]?.language
                    } ?: LanguageSelections.SystemDefault
                }
                SimplePreference(
                    name = stringResource(id = R.string.language),
                    supporting = { TextMMD(stringResource(selectedLocale.resId))},
                    modifier = Modifier.clickable { onLanguageClicked() }
                )
            }

            // No appearance row. This fork has one appearance - dark marks on a light
            // ground, which is what the panel does - so a light/dark/system choice would
            // be a setting that cannot be honoured. The preference itself is left in
            // place, unread, rather than ripped out of the migration code.
            item {
                val screenAlwaysOn by pref.screenAlwaysOn.collectAsStateWithLifecycle()
                SwitchPreference(
                    name = stringResource(id = R.string.keep_screen_on),
                    checked = screenAlwaysOn,
                    onCheckChange = { pref.writeScreenAlwaysOn(it) }
                )
            }

            item {
//            val referenceNote by pref.referenceFrequencyAsString.collectAsStateWithLifecycle()
                SimplePreference(
                    name = stringResource(id = R.string.reference_frequency),
                    supporting = {
                        val textStyle = LocalTextStyle.current
                        val resources = LocalResources.current
                        val frequencyAsString =
                            decimalFormat.format(musicalScale.referenceFrequency)
                        val summary =
                            remember(musicalScale, notePrintOptions, textStyle, resources) {
                                buildAnnotatedString {
                                    append(
                                        musicalScale.referenceNote.asAnnotatedString(
                                            notePrintOptions,
                                            textStyle.fontSize,
                                            textStyle.fontWeight,
                                            withOctave = true,
                                            resources = resources
                                        )
                                    )
                                    append(" = ")
                                    append(
                                        resources.getString(
                                            R.string.hertz_str,
                                            frequencyAsString
                                        )
                                    )
                                }
                            }
                        TextMMD(summary)
                    },
                    modifier = Modifier.clickable { onReferenceFrequencyClicked() }
                )
            }
            item {
                SimplePreference(
                    name = stringResource(id = R.string.temperament),
                    supporting = {
                        val resources = LocalResources.current
                        val textStyle = LocalTextStyle.current
                        val summary = remember(musicalScale, resources, textStyle, notePrintOptions) {
                            buildAnnotatedString {
                                //append(resources.getString(getTuningNameResourceId(musicalScale.temperamentType)))
                                append(musicalScale.temperament.name.value(context))
                                append(resources.getString(R.string.comma_separator))
                                append(
                                    musicalScale.rootNote.asAnnotatedString(
                                        notePrintOptions,
                                        textStyle.fontSize,
                                        textStyle.fontWeight,
                                        withOctave = false,
                                        resources = resources
                                    )
                                )
                            }
                        }
                        TextMMD(summary)
                    },
                    modifier = Modifier.clickable { onTemperamentClicked() }
                )
            }
            item {
                val toleranceInCents by pref.toleranceInCents.collectAsStateWithLifecycle()

                SliderPreference(
                    name = stringResource(id = R.string.tolerance_in_cents),
                    supporting = stringResource(R.string.tolerance_summary, toleranceInCents),
                    value = toleranceInCents.toFloat(),
                    valueRange = 1f..20f,
                    steps = 18,
                    onValueChange = { pref.writeToleranceInCents(it.roundToInt()) }
                )
            }
            item {
                SwitchPreference(
                    name = stringResource(id = R.string.prefer_flat),
                    checked = notePrintOptions.enharmonicVariant == 1,
                    onCheckChange = {
                        val newNotePrintOptions = notePrintOptions.copy(
                            enharmonicVariant = if (it) 1 else 0
                        )
                        pref.writeNotePrintOptions(newNotePrintOptions)
                    }
                )
            }
            item {
                SimplePreference(
                    name = stringResource(id = R.string.notation),
                    supporting = {
                        val summary = remember(notePrintOptions, context) {
                            "${context.getString(notePrintOptions.notationType.stringResourceId)}, ${context.getString(notePrintOptions.octaveNotation.stringResourceId)}"
                        }
                        TextMMD(summary)
                    },
                    modifier = Modifier.clickable { onNotationClicked() }
                )
            }
            item {
                val sensitivity by pref.sensitivity.collectAsStateWithLifecycle()
                SliderPreference(
                    name = stringResource(id = R.string.sensitivity),
                    supporting = "$sensitivity",
                    value = sensitivity.toFloat(),
                    valueRange = 0f..100f,
                    steps = 99,
                    onValueChange = { pref.writeSensitivity(it.roundToInt()) }
                )
            }
            item {
                HorizontalDividerMMD()
            }
            item {
                Section(title = stringResource(id = R.string.expert))
            }
            item {
                val scientificMode by pref.scientificMode.collectAsStateWithLifecycle()
                SwitchPreference(
                    name = stringResource(id = R.string.scientific_mode),
                    checked = scientificMode,
                    onCheckChange = { pref.writeScientificMode(it) }
                )
            }
            item {
                val numMovingAverage by pref.numMovingAverage.collectAsStateWithLifecycle()
                SliderPreference(
                    name = stringResource(id = R.string.num_moving_average),
                    supporting = LocalContext.current.resources.getQuantityString(
                        R.plurals.num_moving_average_summary,
                        numMovingAverage,
                        numMovingAverage
                    ),
                    value = numMovingAverage.toFloat(),
                    valueRange = 1f..15f,
                    steps = 13,
                    onValueChange = { pref.writeNumMovingAverage(it.roundToInt()) }
                )
            }
            item {
                SimplePreference(
                    name = stringResource(id = R.string.stretch_tuning),
                    supporting = {
                        TextMMD(musicalScale.stretchTuning.name.value(context))
                    },
                    modifier = Modifier.clickable { onStretchTuningClicked() }
                )
            }
            item {
                val windowSizeExponent by pref.windowSizeExponent.collectAsStateWithLifecycle()
                val windowSize = 2f.pow(windowSizeExponent).roundToInt()
                val resources = LocalResources.current
                val summary = remember(windowSize, resources) {
                    "$windowSize " + resources.getString(R.string.samples) +
                            " (" + resources.getString(R.string.minimum_frequency) +
                            resources.getString(
                                R.string.hertz,
                                2 * pref.sampleRate / windowSize.toFloat()
                            ) + ")"
                }
                SliderPreference(
                    name = stringResource(id = R.string.window_size),
                    supporting = summary,
                    value = windowSizeExponent.toFloat(),
                    valueRange = 7f..15f,
                    steps = 7,
                    onValueChange = { pref.writeWindowSize(it.roundToInt()) }
                )
            }
            item {
                val windowingFunction by pref.windowing.collectAsStateWithLifecycle()
                SimplePreference(
                    name = stringResource(id = R.string.windowing_function),
                    supporting = stringResource(id = windowingFunction.stringResourceId),
                    modifier = Modifier.clickable { onWindowingFunctionClicked() }
                )
            }
            item {
                val overlap by pref.overlap.collectAsStateWithLifecycle()
                SliderPreference(
                    name = stringResource(id = R.string.overlap),
                    value = 100 * overlap,
                    supporting = stringResource(
                        id = R.string.percent,
                        (100 * overlap).roundToInt()
                    ),
                    valueRange = 0f..80f,
                    steps = 15,
                    onValueChange = { pref.writeOverlap(it.roundToInt()) }
                )
            }
            item {
                val pitchHistoryDuration by pref.pitchHistoryDuration.collectAsStateWithLifecycle()
                SliderPreference(
                    name = stringResource(id = R.string.pitch_history_duration),
                    value = pitchHistoryDuration,
                    supporting = stringResource(id = R.string.seconds, pitchHistoryDuration),
                    valueRange = 0.25f..10f,
                    steps = 38, // maybe better have progressive stps?
                    onValueChange = { pref.writePitchHistoryDuration(it) }
                )
            }
            item {
                val pitchHistoryNumFaultyValues by pref.pitchHistoryNumFaultyValues.collectAsStateWithLifecycle()
                val resources = LocalContext.current.resources
                val summary = remember(resources, pitchHistoryNumFaultyValues) {
                    resources.getQuantityString(
                        R.plurals.pitch_history_num_faulty_values_summary,
                        pitchHistoryNumFaultyValues,
                        pitchHistoryNumFaultyValues
                    )
                }
                SliderPreference(
                    name = stringResource(id = R.string.pitch_history_num_faulty_values),
                    value = pitchHistoryNumFaultyValues.toFloat(),
                    supporting = summary,
                    valueRange = 1f..12f,
                    steps = 10,
                    onValueChange = { pref.writePitchHistoryNumFaultyValues(it.roundToInt()) }
                )
            }
            item {
                val duration by pref.waveWriterDurationInSeconds.collectAsStateWithLifecycle()
                SliderPreference(
                    name = stringResource(id = R.string.capture),
                    value = duration.toFloat(),
                    supporting = if (duration == 0)
                        stringResource(R.string.no_capture_duration)
                    else
                        stringResource(R.string.capture_duration, duration),
                    valueRange = 0f..5f,
                    steps = 4,
                    onValueChange = { pref.writeWaveWriterDurationInSeconds(it.roundToInt()) }
                )
            }
            item {
                HorizontalDividerMMD()
            }
            item {
                Section(title = stringResource(id = R.string.others))
            }
            item {
                val displayOnLockScreen by pref.displayOnLockScreen.collectAsStateWithLifecycle()
                SwitchPreference(
                    name = stringResource(id = R.string.display_on_lock_screen),
                    checked = displayOnLockScreen,
                    onCheckChange = { pref.writeDisplayOnLockScreen(it) }
                )
            }
            item {
                // The row asks, not a dialog. A dialog is two full-panel repaints to ask one
                // question; the row is one, and it asks in the place the answer belongs. It
                // disarms itself after four seconds, so a stray tap leaves nothing live.
                var armed by remember { mutableStateOf(false) }
                LaunchedEffect(armed) {
                    if (armed) {
                        delay(4000)
                        armed = false
                    }
                }
                SimplePreference(
                    name = stringResource(
                        id = if (armed) R.string.reset_all_settings_armed
                        else R.string.reset_all_settings
                    ),
                    modifier = Modifier.clickable {
                        if (armed) {
                            onResetClicked()
                            armed = false
                        } else {
                            armed = true
                        }
                    }
                )
            }
            item {
                SimplePreference(
                    name = stringResource(id = R.string.about),
                    modifier = Modifier.clickable { onAboutClicked() }
                )
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 800, showBackground = true)
@Composable
private fun PreferencesPreview() {
    TunerTheme {
        Preferences(
            viewModel = hiltViewModel(),
            modifier = Modifier.fillMaxSize()
        )
    }
}