package com.wanderwildwood.onsa.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.wanderwildwood.onsa.preferences.PreferenceResources
import com.wanderwildwood.onsa.stretchtuning.StretchTuning
import com.wanderwildwood.onsa.ui.screens.StretchTuningEditor
import com.wanderwildwood.onsa.ui.stretchtuning.ModifyStretchTuningLineDialog
import com.wanderwildwood.onsa.ui.stretchtuning.StretchTuningInfo
import com.wanderwildwood.onsa.viewmodels.StretchTuningEditorViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Composable
private fun createStretchTuningEditorViewModel(
    controller: NavController, backStackEntry: NavBackStackEntry
)
        : StretchTuningEditorViewModel {
    val parentEntry = remember(backStackEntry) {
        controller.getBackStackEntry<StretchTuningEditorGraphRoute>()
    }
    val context = LocalContext.current
    val stretchTuning = parentEntry.toRoute<StretchTuningEditorGraphRoute>().stretchTuning
    return hiltViewModel<StretchTuningEditorViewModel, StretchTuningEditorViewModel.Factory>(
        parentEntry
    ) { factory ->
        factory.create(
            initialStretchTuning = stretchTuning,
            initialName = stretchTuning.name.value(context),
            initialDescription = stretchTuning.description.value(context)
        )
    }
}


fun NavGraphBuilder.stretchTuningEditorGraph(
    controller: NavController,
    preferences: PreferenceResources
) {
    navigation<StretchTuningEditorGraphRoute>(
        startDestination = StretchTuningEditorRoute
    ) {
        composable<StretchTuningEditorRoute> { backStackEntry ->
            val viewModel = createStretchTuningEditorViewModel(
                controller = controller, backStackEntry = backStackEntry
            )
            StretchTuningEditor(
                viewModel,
                onAbortClicked = {
                    controller.navigateUp()
                },
                onSaveClicked = {
                    viewModel.saveStretchTuning()
                    controller.navigateUp()
                },
                onEditLineClicked = { key ->
                    val stretchTuning = viewModel.stretchTuning.value
                    val index = stretchTuning.keys.indexOfFirst { it == key }
                    val unstretchedFrequency = stretchTuning.unstretchedFrequencies.getOrNull(index)
                    val stretchInCents = stretchTuning.stretchInCents.getOrNull(index)
                    if (unstretchedFrequency != null && stretchInCents != null) {
                        controller.navigate(ModifyStretchTuningLineDialogRoute(
                            unstretchedFrequency, stretchInCents, key
                        ))
                    }
                }
            )
        }

        composable<StretchTuningInfoGraphRoute> {
            val stretchTuning = it.toRoute<StretchTuningInfoGraphRoute>().stretchTuning
            StretchTuningInfo(
                stretchTuning,
                onNavigateUpClicked = { controller.navigateUp()}
            )
        }

        dialog<ModifyStretchTuningLineDialogRoute> {
            val data = it.toRoute<ModifyStretchTuningLineDialogRoute>()
            val viewModel = createStretchTuningEditorViewModel(
                controller = controller, backStackEntry = it
            )
            ModifyStretchTuningLineDialog(
                data.unstretchedFrequency,
                data.stretchInCents,
                data.key,
                onAbortClicked = {
                    controller.navigateUp()
                },
                onConfirmedClicked = { unstretchedFrequency, stretchInCents, key ->
                   viewModel.modifyLine(unstretchedFrequency, stretchInCents, key)
                    controller.navigateUp()
                }
            )
        }
    }
}

@Serializable
data class StretchTuningEditorGraphRoute(
    val serializedStretchTuning: String,
) {
    constructor(stretchTuning: StretchTuning) : this(
        Json.encodeToString(stretchTuning)
    )

    val stretchTuning
        get() = Json.decodeFromString<StretchTuning>(serializedStretchTuning)
}
@Serializable
data class StretchTuningInfoGraphRoute(
    val serializedStretchTuning: String,
) {
    constructor(stretchTuning: StretchTuning) : this(
        Json.encodeToString(stretchTuning)
    )

    val stretchTuning
        get() = Json.decodeFromString<StretchTuning>(serializedStretchTuning)
}

@Serializable
data object StretchTuningEditorRoute

@Serializable
data class ModifyStretchTuningLineDialogRoute(
    val unstretchedFrequency: Double, val stretchInCents: Double, val key: Int
)