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
package com.wanderwildwood.onsa.temperaments

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.wanderwildwood.onsa.hilt.ApplicationScope
import com.wanderwildwood.onsa.misc.DefaultValues
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.musicalscale.MusicalScale
import com.wanderwildwood.onsa.musicalscale.MusicalScale2
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.resources.ResourcesBase
import com.wanderwildwood.onsa.stretchtuning.StretchTuning
import com.wanderwildwood.onsa.stretchtuning.predefinedStretchTunings
import com.wanderwildwood.onsa.ui.common.EditableListPredefinedSection
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class TemperamentResources @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope val applicationScope: CoroutineScope
) : ResourcesBase("temperaments", context, applicationScope) {
    // -----------------------------------------------------------------------------------
    // temperament resources
    // -----------------------------------------------------------------------------------

    /** Predefined temperaments. */
    val predefinedTemperaments = predefinedTemperaments().toImmutableList()

    // preference: predefined temperaments expanded:
    private val predefinedTemperamentsExpandedPref = createPreference(
        "predefined temperaments expanded", true
    )
    val predefinedTemperamentsExpanded  = predefinedTemperamentsExpandedPref.asStateFlow()
    fun writePredefinedTemperamentsExpanded(expanded: Boolean) {
        predefinedTemperamentsExpandedPref.set(expanded)
    }

    // preference: edo temperaments expanded (must come before edo temperament itself since
    //   we need this for the definition)

    private val edoTemperamentsExpandedPref = createPreference(
        "edo temperaments expanded", true
    )
    val edoTemperamentsExpanded = edoTemperamentsExpandedPref.asStateFlow()
    fun writeEdoTemperamentsExpanded(expanded: Boolean) {
        edoTemperamentsExpandedPref.set(expanded)
    }

    // preference: edo temperaments
    val edoTemperaments = object : EditableListPredefinedSection<Temperament3> {
        private val minEdo = 5
        private val maxEdo = 72
        private val minPredefinedKey = predefinedTemperaments.minOf { it.stableId }
        override val sectionStringResourceId = R.string.edo_temperaments
        override val size = maxEdo - minEdo + 1
        override fun get(index: Int): Temperament3EDO {
            val edo = minEdo + index
            return predefinedTemperamentEDO(edo, minPredefinedKey - 1 -index)
        }

        override val isExpanded get() = edoTemperamentsExpanded
        override val toggleExpanded: (isExpanded: Boolean) -> Unit = {
            writeEdoTemperamentsExpanded(it)
        }
    }

    // preference: custom temperaments

    private val customTemperamentsPref = createTransformablePreference(
        "custom temperaments", persistentListOf(),
        { it.toTypedArray() }, { it.toList().toPersistentList() },
        onDeserializationFailure = { serialized ->
            try {
                Json.decodeFromString<Array<TemperamentWithNoteNames>>(serialized)
                    .map { old -> old.toNew() }.toPersistentList()
            } catch (_: Exception) {
                persistentListOf()
            }
        }
    )
    val customTemperaments = customTemperamentsPref.asStateFlow()

    fun writeCustomTemperaments(temperaments: PersistentList<Temperament3Custom>) {
//        Log.v("Tuner", "TemperamentResources.writeCustomTemperaments: $temperaments")
        val currentMusicalScale = musicalScale.value
        // if currently active temperament did change, update this also
        val currentTemperamentId = currentMusicalScale.temperament.stableId
        val modifiedCurrentTemperament = temperaments.firstOrNull {
            it.stableId == currentTemperamentId
        }
        if (modifiedCurrentTemperament != null) {
            writeMusicalScale(modifiedCurrentTemperament)
        }

        customTemperamentsPref.set(temperaments)
    }

    // preference: custom temperaments expanded
    private val customTemperamentsExpandedPref = createPreference(
        "custom temperaments expanded", true
    )

    val customTemperamentsExpanded = customTemperamentsExpandedPref.asStateFlow()

    fun writeCustomTemperamentsExpanded(expanded: Boolean) {
        customTemperamentsExpandedPref.set(expanded)
    }

    // not really a reference, but this is the default temperament
    val defaultTemperament = predefinedTemperaments.first {
        it.equalOctaveDivision() == 12
    }

    // modification of custom temperaments list

   /** Add temperament if stable id does not exist, else replace it.*/
    fun addNewOrReplaceTemperament(temperament: Temperament3Custom) {
        val newTemperament = if (temperament.stableId == Temperament3.NO_STABLE_ID) {
            temperament.copy(stableId = getNewTemperamentStableId())
        } else {
            temperament
        }

        val oldTemperaments = customTemperaments.value
        val newTemperaments = oldTemperaments.mutate { mutated ->
            val index = oldTemperaments.indexOfFirst { it.stableId == temperament.stableId }
//            Log.v("Tuner", "TemperamentResource.addNewOrReplaceTemperament: Writing temperament to index $index")
            if (index >= 0)
                mutated[index] = newTemperament
            else
                mutated.add(newTemperament)
        }
        writeCustomTemperaments(newTemperaments)
    }

    fun appendTemperaments(temperaments: List<Temperament3Custom>) {
        val current = this.customTemperaments.value
        val newTemperamentsList = current.mutate { modified ->
            temperaments.forEach {
                val newKey = getNewTemperamentStableId(modified)
                modified.add(it.copy(stableId = newKey))
            }
        }
//        Log.v("Tuner", "TemperamentResources.appendTemperaments: size=${temperaments.size}")
        writeCustomTemperaments(newTemperamentsList)
    }

    fun prependTemperaments(temperaments: List<Temperament3Custom>) {
        val current = this.customTemperaments.value
        val newTemperamentsList = current.mutate { modified ->
            temperaments.forEachIndexed { index, temperament ->
                val newKey = getNewTemperamentStableId(modified)
                modified.add(index, temperament.copy(stableId = newKey))
            }
        }
        writeCustomTemperaments(newTemperamentsList)
    }

    fun replaceTemperaments(temperaments: List<Temperament3Custom>) {
        var key = 0L
        val currentKey = musicalScale.value.temperament.stableId
        val newTemperamentsList = temperaments.map {
            ++key
            if (key == currentKey)
                ++key
            it.copy(stableId = key)
        }.toPersistentList()
        writeCustomTemperaments(newTemperamentsList)
    }

    private fun getNewTemperamentStableId(
        existingTemperaments: List<Temperament3> = customTemperaments.value
    ): Long {
        val currentKey = musicalScale.value.temperament.stableId
        while (true) {
            val stableId = Random.nextLong(0, Long.MAX_VALUE - 1)
            if ((currentKey != stableId) && (existingTemperaments.firstOrNull {it.stableId == stableId} == null))
                return stableId
        }
    }

    // -----------------------------------------------------------------------------------
    // stretch tuning resources
    // -----------------------------------------------------------------------------------

    // preference: predefined stretch tunings
    val predefinedStretchTunings = predefinedStretchTunings().toImmutableList()

    val defaultStretchTuning = predefinedStretchTunings[0] // this is "no stretch tuning"

    // preference: predefined stretch tunings expanded

    private val predefinedStretchTuningsExpandedPref = createPreference(
        "predefined stretch tunings expanded", true
    )
    val predefinedStretchTuningsExpanded = predefinedStretchTuningsExpandedPref.asStateFlow()
    fun writePredefinedStretchTuningsExpanded(expanded: Boolean) {
        predefinedStretchTuningsExpandedPref.set(expanded)
    }

    //private val currentStretchTuningPref = createSerializablePreference(
    //    "current stretch tuning", predefinedStretchTunings[0]
    //)
    //val currentStretchTuning = currentStretchTuningPref.asStateFlow()

    // preference: custom stretch tunings
    private val customStretchTuningsPref = createTransformablePreference(
        "custom stretch tunings", persistentListOf<StretchTuning>(),
        makeSerializable = { it.toTypedArray() },
        fromSerializable = { it.toList().toPersistentList() }
    )
    val customStretchTunings = customStretchTuningsPref.asStateFlow()
    fun writeCustomStretchTunings(newStretchTunings: PersistentList<StretchTuning>) {
        customStretchTuningsPref.set(newStretchTunings)
    }

    // preference: custom stretch tunings expanded
    private val customStretchTuningsExpandedPref = createPreference(
        "custom stretch tunings expanded", true
    )
    val customStretchTuningsExpanded = customStretchTuningsExpandedPref.asStateFlow()
    fun writeCustomStretchTuningsExpanded(expanded: Boolean) {
        customStretchTuningsExpandedPref.set(expanded)
    }

    /** Add stretch tuning if stable id does not exist, else replace it.*/
    fun addNewOrReplaceStretchTuning(stretchTuning: StretchTuning) {
        val newStretchTuning = if (stretchTuning.stableId == StretchTuning.NO_STABLE_ID) {
            stretchTuning.copy(stableId = getNewStretchTuningStableId())
        } else {
            stretchTuning
        }

        val oldStretchTunings = customStretchTunings.value
        val newStretchTunings = oldStretchTunings.mutate { mutated ->
            val index = oldStretchTunings.indexOfFirst { it.stableId == stretchTuning.stableId }
            if (index >= 0)
                mutated[index] = newStretchTuning
            else
                mutated.add(newStretchTuning)
        }
        writeCustomStretchTunings(newStretchTunings)
    }

    private fun getNewStretchTuningStableId(
        existingStretchTunings: List<StretchTuning> = customStretchTunings.value
    ): Long {
        val currentKey = musicalScale.value.stretchTuning.stableId
        while (true) {
            // Only positive values allowed, since negative values are reserved for predefined
            // stretch tunings
            val stableId = Random.nextLong(0, Long.MAX_VALUE - 1)
            if ((currentKey != stableId) && (existingStretchTunings.firstOrNull {it.stableId == stableId} == null))
                return stableId
        }
    }

    fun appendStretchTunings(stretchTunings: List<StretchTuning>) {
        val current = this.customStretchTunings.value
        val newStretchTuningsList = current.mutate { modified ->
            stretchTunings.forEach {
                val newKey = getNewStretchTuningStableId(modified)
                modified.add(it.copy(stableId = newKey))
            }
        }
//        Log.v("Tuner", "TemperamentResources.appendTemperaments: size=${temperaments.size}")
        writeCustomStretchTunings(newStretchTuningsList)
    }



    // -----------------------------------------------------------------------------------
    // musical scale
    // -----------------------------------------------------------------------------------

    private val musicalScaleDefault = MusicalScale2(
        temperament = defaultTemperament,
        _rootNote = null,
        _referenceNote = null,
        referenceFrequency = DefaultValues.REFERENCE_FREQUENCY,
        frequencyMin = DefaultValues.FREQUENCY_MIN,
        frequencyMax = DefaultValues.FREQUENCY_MAX,
        _stretchTuning = defaultStretchTuning
    )

    private val musicalScalePref = createSerializablePreference(
        "musical scale", musicalScaleDefault,
        verifyAfterReading = { scale ->
            val scale2 = reloadPredefinedTemperamentIfNeeded(
                musicalScale = scale,
                predefinedTemperaments = predefinedTemperaments
            )
            reloadPredefinedStretchTuningIfNecessary(
                musicalScale = scale2,
                predefinedStretchTunings = predefinedStretchTunings
            )
        },
        onDeserializationFailure = { serialized ->
            try {
                val scale2 = reloadPredefinedTemperamentIfNeeded(
                    musicalScale = Json.decodeFromString<MusicalScale>(serialized).toNew(),
                    predefinedTemperaments = predefinedTemperaments
                )
                reloadPredefinedStretchTuningIfNecessary(
                    musicalScale = scale2,
                    predefinedStretchTunings = predefinedStretchTunings
                )
            } catch (_: Exception) {
                musicalScaleDefault
            }
        }
    )

    val musicalScale = musicalScalePref.asStateFlow()

    fun writeMusicalScale(musicalScale: MusicalScale2) {
        musicalScalePref.set(musicalScale)
    }

    fun writeMusicalScale(
        temperament: Temperament3? = null,
        referenceNote: MusicalNote? = null,
        rootNote: MusicalNote? = null,
        referenceFrequency: Float? = null,
        stretchTuning: StretchTuning? = null
    ) {
        val currentMusicalScale = musicalScale.value
        val temperamentResolved = temperament ?: currentMusicalScale.temperament

        val rootNoteResolved = if (rootNote != null) {
            rootNote
        } else {
            val possibleRootNotes = temperamentResolved.possibleRootNotes()
            if (possibleRootNotes.contains(currentMusicalScale.rootNote)) {
                currentMusicalScale.rootNote
            } else {
                temperamentResolved.noteNames(null)[0]
            }
        }

        val referenceNoteResolved = if (referenceNote != null) {
            referenceNote
        } else {
            val noteNames = temperamentResolved.noteNames(rootNoteResolved)
            if (noteNames.hasNote(currentMusicalScale.referenceNote))
                currentMusicalScale.referenceNote
            else
                noteNames.defaultReferenceNote
        }

//        Log.v("Tuner", "TemperamentResources:writeMusicalScale: ofmin=${currentMusicalScale.frequencyMin}, ofmax=${currentMusicalScale.frequencyMax}")
        val newMusicalScale = MusicalScale2(
            temperament = temperamentResolved,
            _rootNote = rootNoteResolved,
            _referenceNote = referenceNoteResolved,
            referenceFrequency = referenceFrequency ?: currentMusicalScale.referenceFrequency,
            frequencyMin = currentMusicalScale.frequencyMin,
            frequencyMax = currentMusicalScale.frequencyMax,
            _stretchTuning = stretchTuning ?: currentMusicalScale.stretchTuning
        )
        musicalScalePref.set(newMusicalScale)
    }


    fun resetAllSettings() {
        val noteNames = defaultTemperament.noteNames(null)
        writeMusicalScale(
            temperament = defaultTemperament,
            referenceNote = noteNames.defaultReferenceNote,
            rootNote = noteNames[0],
            referenceFrequency = DefaultValues.REFERENCE_FREQUENCY,
            stretchTuning = defaultStretchTuning
        )
    }
}

private fun reloadPredefinedTemperamentIfNeeded(
    musicalScale: MusicalScale2?,
    predefinedTemperaments: List<Temperament3>
): MusicalScale2? {
    if (musicalScale == null)
        return null
    val identifier = when(musicalScale.temperament) {
        is Temperament3ChainOfFifthsNoEnharmonics -> musicalScale.temperament.uniqueIdentifier
        is Temperament3ChainOfFifthsEDONames -> musicalScale.temperament.uniqueIdentifier
        is Temperament3Custom -> null
        is Temperament3EDO -> null
        is Temperament3RationalNumbersEDONames -> musicalScale.temperament.uniqueIdentifier
    }
    return if (identifier != null) {
        val reloadedTemperament = predefinedTemperaments.firstOrNull { temperament ->
            when (temperament) {
                is Temperament3ChainOfFifthsNoEnharmonics -> temperament.uniqueIdentifier == identifier
                is Temperament3ChainOfFifthsEDONames -> temperament.uniqueIdentifier == identifier
                is Temperament3Custom -> false
                is Temperament3EDO -> false
                is Temperament3RationalNumbersEDONames -> temperament.uniqueIdentifier == identifier
            }
        }
        if (reloadedTemperament != null)
            musicalScale.copy(temperament = reloadedTemperament)
        else
            musicalScale
    } else {
        musicalScale
    }
}

/** In case the the musical scale uses a predefined stretch tuning, load the tuning from scratch.
 * @note predefined stretch tunings are detected by negative stable ids.
 * This basically helps fixes in predefined stretch tunings are used without manual reloading the
 *   stretch tuning. Even more important, it updates string resources, which itself are not very
 *   stable.
 */
private fun reloadPredefinedStretchTuningIfNecessary(
    musicalScale: MusicalScale2?,
    predefinedStretchTunings: List<StretchTuning>
): MusicalScale2? {
    if (musicalScale == null)
        return null

    val stableId = musicalScale.stretchTuning.stableId
    if (stableId >= 0)
        return musicalScale

    val stretchTuning = predefinedStretchTunings.firstOrNull { it.stableId == stableId }

    if (stretchTuning == null)
        return musicalScale

    return musicalScale.copy(_stretchTuning = stretchTuning)
}
