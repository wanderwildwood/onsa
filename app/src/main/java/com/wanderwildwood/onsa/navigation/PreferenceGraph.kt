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

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import com.wanderwildwood.onsa.preferences.PreferenceResources
import com.wanderwildwood.onsa.temperaments.TemperamentResources
import com.wanderwildwood.onsa.ui.preferences.AboutDialog
import com.wanderwildwood.onsa.ui.preferences.LanguageDialog
import com.wanderwildwood.onsa.ui.preferences.NotationDialog
import com.wanderwildwood.onsa.ui.preferences.ResetDialog
import com.wanderwildwood.onsa.ui.preferences.WindowingFunctionDialog
import com.wanderwildwood.onsa.ui.screens.Preferences
import com.wanderwildwood.onsa.viewmodels.PreferencesViewModel
import kotlinx.serialization.Serializable

fun NavGraphBuilder.preferenceGraph(
    controller: NavController,
    onNavigateUpClicked: () -> Unit,
    preferences: PreferenceResources,
    temperaments: TemperamentResources
) {
    navigation<PreferencesGraphRoute>(
        startDestination = PreferencesRoute,
    ) {
        composable<PreferencesRoute> {
            val viewModel: PreferencesViewModel = hiltViewModel()
            Preferences(
                viewModel = viewModel,
                onNavigateUpClicked = onNavigateUpClicked,
                onLanguageClicked = { controller.navigate(LanguageDialogRoute) },
                onReferenceFrequencyClicked = {
                    controller.navigate(
                        ReferenceFrequencyDialogRoute(
                            temperaments.musicalScale.value,
                            null
                        )
                    )
                },
                onNotationClicked = { controller.navigate(NotationDialogRoute) },
                onTemperamentClicked = { controller.navigate(TemperamentDialogRoute) },
                onWindowingFunctionClicked = { controller.navigate(WindowingFunctionDialogRoute) },
                onStretchTuningClicked = {controller.navigate(StretchTuningOverviewRoute) },
                onResetClicked = { controller.navigate(ResetDialogRoute) },
                onAboutClicked = { controller.navigate(AboutDialogRoute) }
            )
        }

        dialog<LanguageDialogRoute> {
            LanguageDialog(
                onDismiss = { controller.navigateUp()}
            )
        }
        dialog<NotationDialogRoute> {
            val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
            NotationDialog(
                notePrintOptions = notePrintOptions,
                onNotationChange = { notation, octaveNotation ->
                    val newNotePrintOptions = preferences.notePrintOptions.value.copy(
                        notationType = notation, octaveNotation = octaveNotation,
                    )
                    preferences.writeNotePrintOptions(newNotePrintOptions)
                    controller.navigateUp()
                },
                onDismiss = { controller.navigateUp() }
            )
        }
        dialog<WindowingFunctionDialogRoute> {
            val windowing by preferences.windowing.collectAsStateWithLifecycle()
            WindowingFunctionDialog(
                initialWindowingFunction = windowing,
                onWindowingFunctionChanged = {
                    preferences.writeWindowing(it)
                    controller.navigateUp()
                } ,
                onDismiss = { controller.navigateUp() }
            )
        }
        dialog<ResetDialogRoute> {
            ResetDialog(
                onReset = {
                    preferences.resetAllSettings()
                    temperaments.resetAllSettings()
                    controller.navigateUp()
                },
                onDismiss = { controller.navigateUp() }
            )
        }
        dialog<AboutDialogRoute> {
            AboutDialog(
                onDismiss = { controller.navigateUp() }
            )
        }
    }
}

@Serializable
data object PreferencesGraphRoute
@Serializable
data object PreferencesRoute
@Serializable
data object LanguageDialogRoute
@Serializable
data object NotationDialogRoute
@Serializable
data object WindowingFunctionDialogRoute
@Serializable
data object ResetDialogRoute
@Serializable
data object AboutDialogRoute
