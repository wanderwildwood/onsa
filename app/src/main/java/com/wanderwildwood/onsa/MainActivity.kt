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
package com.wanderwildwood.onsa

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import com.wanderwildwood.onsa.ui.common.Notice
import com.wanderwildwood.onsa.ui.common.NoticeStrip
import com.wanderwildwood.onsa.ui.common.onEachPress
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.wanderwildwood.onsa.instruments.Instrument
import com.wanderwildwood.onsa.instruments.InstrumentResources
import com.wanderwildwood.onsa.metronome.MetronomeEngine
import com.wanderwildwood.onsa.navigation.TunerRoute
import com.wanderwildwood.onsa.navigation.instrumentEditorGraph
import com.wanderwildwood.onsa.navigation.preferenceGraph
import com.wanderwildwood.onsa.navigation.mainGraph
import com.wanderwildwood.onsa.navigation.temperamentEditorGraph
import com.wanderwildwood.onsa.navigation.TemperamentEditorGraphRoute
import com.wanderwildwood.onsa.navigation.stretchTuningEditorGraph
import com.wanderwildwood.onsa.preferences.PreferenceResources
import com.wanderwildwood.onsa.preferences.migrateFromV6
import com.wanderwildwood.onsa.stretchtuning.StretchTuning
import com.wanderwildwood.onsa.temperaments.EditableTemperament
import com.wanderwildwood.onsa.temperaments.TemperamentResources
import com.wanderwildwood.onsa.temperaments.hasErrors
import com.wanderwildwood.onsa.temperaments.toTemperament3Custom
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {// ComponentActivity() {

    @Inject
    lateinit var pref: PreferenceResources

    @Inject
    lateinit var instruments: InstrumentResources

    @Inject
    lateinit var temperaments: TemperamentResources

    @Inject
    lateinit var metronome: MetronomeEngine

    // Sideways when the phone is. The manifest keeps this activity through a turn
    // (configChanges), so the microphone runs on and only the layout is drawn again.
    private val turner = TurnWithPhone(this)


    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark icons whatever the phone's dark-theme setting: the tuner is always white, and
        // the default would turn them white on a phone set to dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
//        Log.v("Tuner", "MainActivity2.onCreate: savedInstanceState = $savedInstanceState")

        if (isSecondCopyFromLauncher(isTaskRoot, intent?.action, intent?.categories)) {
            finish()
            return
        }

        runBlocking {
            migrateFromV6(this@MainActivity, pref, temperaments, instruments)
        }

        turner.set(pref.turnWithPhone.value)
        lifecycleScope.launch {
            // Read again whenever the setting changes, so the switch takes effect at once.
            pref.turnWithPhone.collect { turner.set(it) }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pref.screenAlwaysOn.collect {
                    if (it)
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        setContent {
            // No appearance to resolve. The panel has one, and TunerTheme is it.
            TunerTheme {
                val controller = rememberNavController()


                DisposableEffect(controller) {
                    val listener = NavController.OnDestinationChangedListener { _, _, _ ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            if (controller.previousBackStackEntry == null) {
                                setShowWhenLocked(pref.displayOnLockScreen.value)
                            } else {
                                setShowWhenLocked(false)
                            }
                        }
                    }
                    controller.addOnDestinationChangedListener(listener)
                    onDispose {
                        controller.removeOnDestinationChangedListener(listener)
                    }
                }

                // A notice along the foot stays until the next press anywhere, which takes it
                // down before that press does its own thing.
                Column(Modifier.fillMaxSize().onEachPress { Notice.text = null }) {
                NavHost(
                    modifier = Modifier.weight(1f),
                    navController = controller,
                    startDestination = TunerRoute
                ) {
                    mainGraph(
                        controller = controller,
                        onNavigateUpClicked = { controller.navigateUp() },
                        preferences = pref,
                        temperamentResources = temperaments,
                        metronomePlaying = metronome.playing,
                        onLoadInstruments = {
                            loadInstruments(it, instruments, this@MainActivity)
                        },
                        onLoadTemperaments = { temperamentList ->
                            loadTemperaments(
                                controller, temperamentList, temperaments, this@MainActivity
                            )
                        },
                        onLoadStretchTunings = { stretchTuningsList ->
                            loadStretchTunings(
                                stretchTuningsList, temperaments, this@MainActivity
                            )
                        }
                    )
                    preferenceGraph(
                        controller = controller,
                        onNavigateUpClicked = { controller.navigateUp() },
                        preferences = pref,
                        temperaments = temperaments
                    )

                    instrumentEditorGraph(
                        controller = controller,
                        preferences = pref,
                        instruments = instruments,
                        temperaments = temperaments
                    )

                    temperamentEditorGraph(
                        controller = controller,
                        preferences = pref
                    )

                    stretchTuningEditorGraph(
                        controller = controller,
                        preferences = pref
                    )
                }
                NoticeStrip()
                }
            }
        }
//        Log.v("Tuner", "Migrating instruments? ${pref.isMigratingFromV6}")
//        if (pref.isMigratingFromV6)
//            instruments.migratingFromV6(this)
    }

    override fun onResume() {
        super.onResume()
        turner.resume()
    }

    override fun onPause() {
        turner.pause()
        super.onPause()
    }

}

/**
 * True for a second copy of the tuner that a launcher has put on top of the one already open.
 * It happens when the app was first opened some other way than by the launcher's own icon (from
 * the installer's Open, or from another launcher): the icon then starts a new tuner over the
 * screen the reader left, which is how a playing metronome came to be hidden under a tuner that
 * listened to its clicks. The copy closes at once and the screen underneath is what comes back.
 */
internal fun isSecondCopyFromLauncher(isTaskRoot: Boolean, action: String?, categories: Set<String>?): Boolean =
    !isTaskRoot && action == Intent.ACTION_MAIN && categories?.contains(Intent.CATEGORY_LAUNCHER) == true

private fun loadTemperaments(
    controller: NavController,
    temperamentList: List<EditableTemperament>,
    temperamentResources: TemperamentResources,
    context: Context
) {
    if (temperamentList.firstOrNull { it.hasErrors() } == null) {
        Notice.say(context, context.resources.getQuantityString(R.plurals.load_temperaments, temperamentList.size, temperamentList.size))

        temperamentResources.appendTemperaments(
            temperamentList.mapNotNull { it.toTemperament3Custom() }
        )
    } else if (temperamentList.size == 1) { // one temperament with errors
        controller.navigate(
            TemperamentEditorGraphRoute(temperamentList[0])
        )
    } else { // several temperaments, some with errors: nothing is loaded, and the notice says why
        val numWithErrors = temperamentList.count { it.hasErrors() }
        Notice.say(context, context.resources.getQuantityString(R.plurals.load_temperaments_with_errors, numWithErrors, numWithErrors, temperamentList.size))
    }
}

private fun loadStretchTunings(
    stretchTuningsList: List<StretchTuning>,
    temperamentResources: TemperamentResources,
    context: Context
) {
    Notice.say(context, context.resources.getQuantityString(R.plurals.load_stretch_tunings, stretchTuningsList.size, stretchTuningsList.size))

    temperamentResources.appendStretchTunings(stretchTuningsList)
}

private fun loadInstruments(
    instrumentList: List<Instrument>,
    instrumentResources: InstrumentResources,
    context: Context
) {
    Notice.say(context, context.resources.getQuantityString(R.plurals.load_instruments, instrumentList.size, instrumentList.size))
    instrumentResources.appendInstruments(instrumentList)
}

