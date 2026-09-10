package com.wanderwildwood.onsa.instruments

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.wanderwildwood.onsa.hilt.ApplicationScope
import com.wanderwildwood.onsa.resources.ResourcesBase
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
class InstrumentResources @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope val applicationScope: CoroutineScope
) : ResourcesBase("instruments", context, applicationScope) {

    val predefinedInstruments = instrumentDatabase.toImmutableList()

    private val currentInstrumentPref = createSerializablePreference(
        "current instrument", predefinedInstruments[0],
        verifyAfterReading = { instrument ->
            reloadPredefinedInstrumentIfNeeded(instrument, predefinedInstruments)
        },
        onDeserializationFailure = {
           try {
                Json.decodeFromString<InstrumentOld>(it).toNew()
            } catch (_: Exception ){
                predefinedInstruments[0]
            }
        }
    )
    val currentInstrument = currentInstrumentPref.asStateFlow()

    fun writeCurrentInstrument(instrument: Instrument) {
        currentInstrumentPref.set(instrument)
    }

    private val customInstrumentsPref = createTransformablePreference(
        "custom instruments", persistentListOf<Instrument>(),
        { it.toTypedArray() }, { it.toPersistentList() }
    )

    val customInstruments = customInstrumentsPref.asStateFlow()

    fun writeCustomInstruments(instruments: PersistentList<Instrument>) {
        // if current instrument did change, update this also
        val currentInstrumentId = currentInstrument.value.stableId
        val modifiedCurrentInstrument = instruments.firstOrNull {
            it.stableId == currentInstrumentId
        }
        if (modifiedCurrentInstrument != null)
            writeCurrentInstrument(modifiedCurrentInstrument)
        customInstrumentsPref.set(instruments)
    }

    private val customInstrumentsExpandedPref =
        createPreference("custom instruments expanded", true)
    val customInstrumentsExpanded = customInstrumentsExpandedPref.asStateFlow()

    fun writeCustomInstrumentsExpanded(expanded: Boolean) {
        customInstrumentsExpandedPref.set(expanded)
    }

    private val predefinedInstrumentsExpandedPref =
        createPreference("predefined instruments expanded", true)
    val predefinedInstrumentsExpanded = predefinedInstrumentsExpandedPref.asStateFlow()

    fun writePredefinedInstrumentsExpanded(expanded: Boolean) {
        predefinedInstrumentsExpandedPref.set(expanded)
    }

    /** Add instrument if stable id does not exist, else replace it.*/
    fun addNewOrReplaceInstrument(instrument: Instrument) {
        val newInstrument = if (instrument.stableId == Instrument.NO_STABLE_ID)
            instrument.copy(stableId = getNewStableId())
        else
            instrument

        val oldInstruments = customInstruments.value
        val newInstruments = oldInstruments.mutate { mutated ->
            val index = oldInstruments.indexOfFirst { it.stableId == instrument.stableId }
            if (index >= 0)
                mutated[index] = newInstrument
            else
                mutated.add(newInstrument)
        }
        writeCustomInstruments(newInstruments)
    }

    fun appendInstruments(instruments: List<Instrument>) {
        val current = this.customInstruments.value
        val newInstrumentList = current.mutate { modified ->
            instruments.forEach {
                val newKey = getNewStableId(modified)
                modified.add(it.copy(stableId = newKey))
            }
        }
        writeCustomInstruments(newInstrumentList)
    }

    private fun getNewStableId(existingInstruments: List<Instrument> = customInstruments.value): Long {
        val currentKey = currentInstrument.value.stableId
        while (true) {
            val stableId = Random.nextLong(0, Long.MAX_VALUE - 1)
            if ((currentKey != stableId) && (existingInstruments.firstOrNull {it.stableId == stableId} == null))
                return stableId
        }
    }


}

private fun reloadPredefinedInstrumentIfNeeded(
    instrument: Instrument?,
    predefinedInstruments: List<Instrument>
): Instrument? {
    // if null, we get the string, not the id-based string, for predefined instruments, this
    // string is a unique identifier.
    val name = instrument?.getNameString(null)
    return if (name == null || name == "") {
        instrument
    } else {
        predefinedInstruments.firstOrNull {
            it.getNameString(null) == name
        } ?: instrument
    }
}
