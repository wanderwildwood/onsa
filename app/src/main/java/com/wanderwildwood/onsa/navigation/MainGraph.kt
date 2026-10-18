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
package com.wanderwildwood.onsa.navigation

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import android.content.Context
import com.mudita.mmd.components.snackbar.SnackbarDurationMMD
import com.mudita.mmd.components.snackbar.SnackbarHostStateMMD
import com.mudita.mmd.components.snackbar.SnackbarResultMMD
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.instruments.InstrumentGroup
import com.wanderwildwood.onsa.instruments.toInstrumentGroup
import com.wanderwildwood.onsa.ui.instruments.InstrumentGroupPage
import com.wanderwildwood.onsa.instruments.Instrument
import com.wanderwildwood.onsa.instruments.InstrumentIcon
import com.wanderwildwood.onsa.musicalscale.MusicalScale2
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.preferences.PreferenceResources
import com.wanderwildwood.onsa.temperaments.EditableTemperament
import com.wanderwildwood.onsa.temperaments.Temperament3
import com.wanderwildwood.onsa.temperaments.TemperamentResources
import com.wanderwildwood.onsa.temperaments.toEditableTemperament
import com.wanderwildwood.onsa.ui.instruments.Instruments
import com.wanderwildwood.onsa.ui.preferences.ReferenceNoteDialog
import com.wanderwildwood.onsa.ui.preferences.RootNoteDialog
import com.wanderwildwood.onsa.ui.screens.InstrumentTuner
import com.wanderwildwood.onsa.ui.screens.ScientificTuner
import com.wanderwildwood.onsa.ui.temperaments.TemperamentDetailsDialog
import com.wanderwildwood.onsa.ui.temperaments.TemperamentsDialog2
import com.wanderwildwood.onsa.viewmodels.InstrumentTunerViewModel
import com.wanderwildwood.onsa.viewmodels.InstrumentViewModel
import com.wanderwildwood.onsa.viewmodels.ReferenceNoteDialogViewModel
import com.wanderwildwood.onsa.viewmodels.ScientificTunerViewModel
import com.wanderwildwood.onsa.viewmodels.TemperamentDialog2ViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.ui.platform.LocalResources
import com.wanderwildwood.onsa.misc.GetTextFromString
import com.wanderwildwood.onsa.stretchtuning.StretchTuning
import com.wanderwildwood.onsa.ui.screens.StretchTuningOverview
import com.wanderwildwood.onsa.viewmodels.StretchTuningOverviewViewModel

@Composable
private fun createTemperamentDialogViewModel(
    controller: NavController, backStackEntry: NavBackStackEntry
): TemperamentDialog2ViewModel? {
    val parentEntry = remember(backStackEntry) {
        try {
            controller.getBackStackEntry<TemperamentDialogRoute>()
        } catch (_: IllegalArgumentException) {
            null
        }
    }
    return if (parentEntry == null)
        null
    else
        hiltViewModel<TemperamentDialog2ViewModel>(parentEntry)
}


fun NavGraphBuilder.mainGraph(
    controller: NavController,
    onNavigateUpClicked: () -> Unit,
    preferences: PreferenceResources,
    temperamentResources: TemperamentResources,
    onLoadInstruments: (List<Instrument>) -> Unit,
    onLoadTemperaments: (List<EditableTemperament>) -> Unit,
    onLoadStretchTunings: (List<StretchTuning>) -> Unit
) {
    composable<TunerRoute> {
        val isScientificTuner by preferences.scientificMode.collectAsStateWithLifecycle()
//        Log.v("Tuner", "MainGraph : isScientificTuner = $isScientificTuner")
        if (isScientificTuner) {
            val viewModel: ScientificTunerViewModel = hiltViewModel()
            ScientificTuner(
                data = viewModel,
                canNavigateUp = false,
                onNavigateUpClicked = onNavigateUpClicked,
                onPreferenceButtonClicked = { controller.navigate(PreferencesGraphRoute) },
                onSharpFlatClicked = { preferences.switchEnharmonicPreference() },
                onReferenceNoteClicked = {
                    controller.navigate(
                        // provided by musicalScalePropertiesGraph
                        ReferenceFrequencyDialogRoute(
                            temperamentResources.musicalScale.value, null
                        )
                    )
                },
                onTemperamentClicked = { controller.navigate(TemperamentDialogRoute) },
                onStretchTuningClicked = { controller.navigate(StretchTuningOverviewRoute)}
            )
        } else {
            val viewModel: InstrumentTunerViewModel = hiltViewModel()
            InstrumentTuner(
                canNavigateUp = false,
                onNavigateUpClicked = onNavigateUpClicked,
                onPreferenceButtonClicked = { controller.navigate(PreferencesGraphRoute) },
                onSharpFlatClicked = { preferences.switchEnharmonicPreference() },
                onReferenceNoteClicked = {
                    controller.navigate(
                        // provided by musicalScalePropertiesGraph
                        ReferenceFrequencyDialogRoute(
                            temperamentResources.musicalScale.value, null
                        )
                    )
                },
                onTemperamentClicked = { controller.navigate(TemperamentDialogRoute) },
                onInstrumentButtonClicked = { controller.navigate(InstrumentsRoute) },
                onStretchTuningClicked = {
                    controller.navigate(StretchTuningOverviewRoute)
                },
                data = viewModel
            )
        }
    }

    composable<InstrumentsRoute> {
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        val musicalScale by temperamentResources.musicalScale.collectAsStateWithLifecycle()
        val viewModel: InstrumentViewModel = hiltViewModel()
        val context = LocalContext.current
        Instruments(
            state = viewModel,
            modifier = Modifier.fillMaxSize(),
            notePrintOptions = notePrintOptions,
//            musicalScale = musicalScale,
            onNavigateUpClicked = { controller.navigateUp() },
            onInstrumentClicked = {
                viewModel.setCurrentInstrument(it)
                controller.navigateUp()
            },
            onInstrumentGroupClicked = { controller.navigate(InstrumentGroupRoute(it.name)) },
            onEditInstrumentClicked = { instrument, copy ->
                controller.navigate(editorRouteFor(instrument, copy, context))
            },
            onCreateNewInstrumentClicked = {
                val newInstrument = Instrument(
                    nameResource = null,
                    name = "",
                    strings = arrayOf(musicalScale.referenceNote),
                    icon = InstrumentIcon.entries[0],
                    stableId = Instrument.NO_STABLE_ID
                )
                controller.navigate(InstrumentEditorGraphRoute.create(newInstrument))
            },
//            onSharpFlatClicked = { preferences.switchEnharmonicPreference() },
//            onReferenceNoteClicked = { // provided by musicalScalePropertiesGraph
//                controller.navigate(
//                    ReferenceFrequencyDialogRoute(
//                        temperamentResources.musicalScale.value, null
//                    )
//                )
//            },
//            onTemperamentClicked = { controller.navigate(TemperamentDialogRoute) },
            onPreferenceButtonClicked = { controller.navigate(PreferencesGraphRoute) },
            onLoadInstruments = onLoadInstruments
        )
    }

    composable<InstrumentGroupRoute> { entry ->
        val group = entry.toRoute<InstrumentGroupRoute>().groupName.toInstrumentGroup()
            ?: InstrumentGroup.guitar
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        val musicalScale by temperamentResources.musicalScale.collectAsStateWithLifecycle()
        // The list's own view model, which is still on the back stack beneath this page.
        val parentEntry = remember(entry) { controller.getBackStackEntry<InstrumentsRoute>() }
        val viewModel: InstrumentViewModel = hiltViewModel(parentEntry)
        val custom by viewModel.instruments.customInstruments.collectAsStateWithLifecycle()
        val active by viewModel.instruments.currentInstrument.collectAsStateWithLifecycle()
        val tunings = remember(group, custom) { viewModel.tuningsOf(group, custom) }
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostStateMMD() }
        val scope = rememberCoroutineScope()
        InstrumentGroupPage(
            group = group,
            tunings = tunings,
            activeInstrument = active,
            modifier = Modifier.fillMaxSize(),
            notePrintOptions = notePrintOptions,
            onTuningClicked = {
                viewModel.setCurrentInstrument(it)
                // Straight back to the tuner, past the instrument list.
                controller.popBackStack(InstrumentsRoute, inclusive = true)
            },
            onEditTuningClicked = { tuning, copy ->
                controller.navigate(editorRouteFor(tuning, copy, context))
            },
            // Undone the way the instrument list undoes a delete: the list as it was is put back.
            onDeleteTuningClicked = { tuning ->
                val before = viewModel.instruments.customInstruments.value
                viewModel.deleteCustomInstrument(tuning)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        context.resources.getQuantityString(R.plurals.items_deleted, 1, 1),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDurationMMD.Long
                    )
                    if (result == SnackbarResultMMD.ActionPerformed)
                        viewModel.instruments.writeCustomInstruments(before)
                }
            },
            snackbarHostState = snackbarHostState,
            onCreateNewTuningClicked = {
                val newInstrument = Instrument(
                    nameResource = null,
                    name = "",
                    strings = arrayOf(musicalScale.referenceNote),
                    icon = group.icon,
                    stableId = Instrument.NO_STABLE_ID,
                    group = group
                )
                controller.navigate(InstrumentEditorGraphRoute.create(newInstrument))
            },
            onNavigateUpClicked = { controller.navigateUp() }
        )
    }

    plainDialog<ReferenceFrequencyDialogRoute> {
        val state = it.toRoute<ReferenceFrequencyDialogRoute>()
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        val viewModel: ReferenceNoteDialogViewModel = hiltViewModel()

        ReferenceNoteDialog(
            initialState = state.musicalScale,
            onReferenceNoteChange = { newState ->
                temperamentResources.writeMusicalScale(newState)
                controller.navigateUp()
            },
            notePrintOptions = notePrintOptions,
            frequencyDetector = viewModel,
            warning = state.warning,
            onDismiss = { controller.navigateUp() }
        )
    }

    composable<TemperamentDialogRoute> { route ->
        val resources = LocalResources.current
        val viewModel: TemperamentDialog2ViewModel = createTemperamentDialogViewModel(
            controller = controller,
            backStackEntry = route
        )!!
        val context = LocalContext.current

        TemperamentsDialog2(
            state = viewModel,
            modifier = Modifier.fillMaxSize(),
            onNavigateUp = { controller.navigateUp() },
            onTemperamentClicked = { temperament, rootNote ->
                controller.navigate(
                    RootNoteDialogRoute(temperament, rootNote)
                ) {
                    popUpTo(TemperamentDialogRoute) { inclusive = true }
                }
            },
            onEditTemperamentClicked = { temperament, copy ->
                val name = temperament.name.value(context)
                controller.navigate(
                    TemperamentEditorGraphRoute(
                        temperament.toEditableTemperament(
                            context = context,
                            name = when {
                                copy && name == "" -> ""
                                copy -> "$name (${resources.getString(R.string.copy_)})"
                                else -> null // i.e. use name from temperament
                            },
                            stableId = if (copy) Temperament3.NO_STABLE_ID else null // null means use stable from temperament
                        )
                    )
                )
            },
            onLoadTemperaments = onLoadTemperaments,
            onTemperamentInfoClicked = { temperament ->
                controller.navigate(TemperamentInfoDialogRoute(temperament))
            }
        )
    }

    plainDialog<TemperamentInfoDialogRoute> {
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        val temperament = it.toRoute<TemperamentInfoDialogRoute>().obtainTemperament()
        TemperamentDetailsDialog(
            temperament = temperament,
            notePrintOptions = notePrintOptions,
            onDismiss = { controller.navigateUp() }
        )
    }

    plainDialog<RootNoteDialogRoute> {
        val data = it.toRoute<RootNoteDialogRoute>()
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        val resources = LocalResources.current

        RootNoteDialog(
            data.rootNote,
            data.temperament,
            onDone = { rootNote ->
                val currentReferenceNote = temperamentResources.musicalScale.value.referenceNote
                val noteNames = data.temperament.noteNames(rootNote)

                if (noteNames.hasNote(currentReferenceNote)) {
                    temperamentResources.writeMusicalScale(
                        temperament = data.temperament,
                        rootNote = rootNote
                    )
                    controller.navigateUp()
                } else {
                    val oldScale = temperamentResources.musicalScale.value
                    val proposedScale = MusicalScale2(
                        temperament = data.temperament,
                        _rootNote = rootNote,
                        _referenceNote = null,
                        referenceFrequency = oldScale.referenceFrequency,
                        frequencyMin = oldScale.frequencyMin,
                        frequencyMax = oldScale.frequencyMax,
                        _stretchTuning = oldScale.stretchTuning
                    )
                    controller.navigate(
                        ReferenceFrequencyDialogRoute(
                            proposedScale,
                            resources.getString(R.string.new_temperament_requires_adapting_reference_note)
                        )
                    ) {
                        popUpTo(data) { inclusive = true }
                    }
                }
            },
            onDismiss = {
                controller.navigateUp()
            },
            notePrintOptions = notePrintOptions
        )
    }

    composable<StretchTuningOverviewRoute> {
        val viewModel: StretchTuningOverviewViewModel = hiltViewModel()
        val context = LocalContext.current
        val resources = LocalResources.current

        StretchTuningOverview(
            viewModel,
            onStretchTuningClicked = {
                temperamentResources.writeMusicalScale(stretchTuning = it)
                controller.navigateUp()
            },
            onEditStretchTuningClicked = { stretchTuning, copy ->
                val name = stretchTuning.name.value(context)
                controller.navigate(
                    StretchTuningEditorGraphRoute(
                        stretchTuning.copy(
                            GetTextFromString(when {
                                copy && name == "" -> ""
                                copy -> "$name (${resources.getString(R.string.copy_)})"
                                else -> name
                            }),
                            stableId = if (copy) StretchTuning.NO_STABLE_ID else stretchTuning.stableId
                        )
                    )
                )
            },
            onStretchTuningInfoClicked = {
                controller.navigate(
                    StretchTuningInfoGraphRoute(stretchTuning = it)
                )
            },
            onNavigateUp = {
                controller.navigateUp()
            },
            onLoadStretchTunings = onLoadStretchTunings
        )
    }
//    dialog<TestRoute>(
//        deepLinks = listOf(
//            navDeepLink<TestRoute>(
//                basePath = "tuner://instruments",
//                deepLinkBuilder = {
//                    uriPattern = "*"
//                    action = Intent.ACTION_VIEW
//                }
//            )
//        )
//    ) {
//        AboutDialog(
//            onDismiss = {controller.navigateUp()}
//        )
//    }
}

@Serializable
data object TunerRoute

@Serializable
data object InstrumentsRoute

/** One instrument's page of tunings. The group travels by its name, as it is saved. */
@Serializable
data class InstrumentGroupRoute(val groupName: String)

/**
 * The editor, for a tuning as it is or for a copy of it. Either way the tuning becomes the
 * reader's own - named in full, keeping its instrument - since a built-in one cannot be
 * changed in place.
 */
private fun editorRouteFor(instrument: Instrument, copy: Boolean, context: Context) =
    InstrumentEditorGraphRoute.create(
        if (copy) {
            instrument.copy(
                nameResource = null,
                tuningNameResource = null,
                name = context.getString(R.string.copy_extension, instrument.getNameString(context)),
                stableId = Instrument.NO_STABLE_ID
            )
        } else {
            instrument.copy(
                nameResource = null,
                tuningNameResource = null,
                name = instrument.getNameString(context)
            )
        }
    )

// it seems that we cannot use complex arguments, so we must serialize ...
@Serializable
data class ReferenceFrequencyDialogRoute(
    val serializedString: String,
    val warning: String?
) {
    constructor(musicalScale: MusicalScale2, warning: String?) : this(
        Json.encodeToString(musicalScale),
        warning
    )
    val musicalScale get() = Json.decodeFromString<MusicalScale2>(serializedString)
}

@Serializable
data object TemperamentDialogRoute

@Serializable
data class TemperamentInfoDialogRoute(
    val serializedTemperament: String
) {
    constructor(temperament: Temperament3) : this(
        Json.encodeToString(temperament)
    )
    fun obtainTemperament(): Temperament3 {
        return Json.decodeFromString<Temperament3>(serializedTemperament)
    }
}

@Serializable
data class RootNoteDialogRoute(
    val serializedTemperament: String, val serializedRootNote: String
) {
    constructor(temperament: Temperament3, rootNote: MusicalNote) : this(
        Json.encodeToString(temperament),
        Json.encodeToString(rootNote)
    )
    val temperament get() = Json.decodeFromString<Temperament3>(serializedTemperament)
    val rootNote get() = Json.decodeFromString<MusicalNote>(serializedRootNote)
}

@Serializable
data object StretchTuningOverviewRoute
//@Serializable
//data object TestRoute