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

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.hilt.ApplicationScope
import com.wanderwildwood.onsa.instruments.Instrument
import com.wanderwildwood.onsa.instruments.InstrumentGroup
import com.wanderwildwood.onsa.instruments.InstrumentIO
import com.wanderwildwood.onsa.instruments.InstrumentResources
import com.wanderwildwood.onsa.ui.common.EditableListPredefinedSectionState
import com.wanderwildwood.onsa.ui.common.EditableListData
import com.wanderwildwood.onsa.ui.instruments.InstrumentsData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InstrumentViewModel @Inject constructor(
    val instruments: InstrumentResources,
    @ApplicationScope val applicationScope: CoroutineScope
): ViewModel(), InstrumentsData {

    /**
     * The rows of the list's first section: each instrument once. An instrument with several
     * tunings - its own and any the reader has added to it - is one row standing for all of
     * them, which opens its page; one with a single tuning is that tuning, and a tap selects it.
     */
    private val mainRows = mutableStateOf(buildMainRows(instruments.customInstruments.value))

    private val tuningCountsFlow = instruments.customInstruments
        .map { countTunings(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, countTunings(instruments.customInstruments.value))
    override val tuningCounts: StateFlow<Map<InstrumentGroup, Int>> = tuningCountsFlow

    /** The reader's own instruments that stand alone; those in a group are on its page. */
    private val ungroupedCustom = instruments.customInstruments
        .map { list -> list.filter { it.group == null }.toPersistentList() }
        .stateIn(
            viewModelScope, SharingStarted.Eagerly,
            instruments.customInstruments.value.filter { it.group == null }.toPersistentList()
        )

    init {
        viewModelScope.launch {
            instruments.customInstruments.collect { mainRows.value = buildMainRows(it) }
        }
    }

    override val listData = EditableListData(
        predefinedItemSections = persistentListOf(
            EditableListPredefinedSectionState(
                R.string.predefined_items,
                mainRows,
                instruments.predefinedInstrumentsExpanded,
                { instruments.writePredefinedInstrumentsExpanded(it) }
            )
        ),
        editableItemsSectionResId = R.string.custom_item,
        editableItems = ungroupedCustom,
        editableItemsExpanded = instruments.customInstrumentsExpanded,
        toggleEditableItemsExpanded = { instruments.writeCustomInstrumentsExpanded(it) },
        getStableId = { it.stableId },
        activeItem = instruments.currentInstrument,
        // The section shows only the standalone ones, so a reorder or a delete there must not
        // drop the grouped ones it never showed: they are written back after it, unchanged.
        setNewItems = { shown ->
            val grouped = instruments.customInstruments.value.filter { it.group != null }
            instruments.writeCustomInstruments((shown + grouped).toPersistentList())
        },
    )

    override fun allCustomInstruments(): List<Instrument> = instruments.customInstruments.value

    /** Every tuning of [group]: the built-in ones first, in their order, then the reader's. */
    fun tuningsOf(group: InstrumentGroup, custom: List<Instrument>): ImmutableList<Instrument> =
        (instruments.predefinedInstruments.filter { it.group == group } +
            custom.filter { it.group == group }).toImmutableList()

    fun deleteCustomInstrument(instrument: Instrument) {
        instruments.writeCustomInstruments(
            instruments.customInstruments.value
                .filterNot { it.stableId == instrument.stableId }
                .toPersistentList()
        )
    }

    private fun countTunings(custom: List<Instrument>): Map<InstrumentGroup, Int> =
        (instruments.predefinedInstruments + custom)
            .mapNotNull { it.group }
            .groupingBy { it }
            .eachCount()

    private fun buildMainRows(custom: List<Instrument>): ImmutableList<Instrument> {
        val counts = countTunings(custom)
        val shown = mutableSetOf<InstrumentGroup>()
        val rows = mutableListOf<Instrument>()
        for (instrument in instruments.predefinedInstruments) {
            val group = instrument.group
            when {
                group == null -> rows += instrument
                group in shown -> Unit
                else -> {
                    shown += group
                    rows += if ((counts[group] ?: 0) > 1) Instrument.groupRow(group) else instrument
                }
            }
        }
        return rows.toImmutableList()
    }

    fun setCurrentInstrument(instrument: Instrument) {
        instruments.writeCurrentInstrument(instrument)
    }

    override fun saveInstruments(
        context: Context,
        uri: Uri,
        instruments: List<Instrument>,
        onDone: (Boolean) -> Unit
    ) {
        applicationScope.launch(Dispatchers.IO) {
            val written = runCatching {
                val stream = context.contentResolver?.openOutputStream(uri, "wt")
                    ?: throw java.io.IOException("no stream")
                stream.use {
                    it.write(InstrumentIO.instrumentsListToString(context, instruments).toByteArray())
                }
            }.isSuccess
            kotlinx.coroutines.withContext(Dispatchers.Main) { onDone(written) }
        }
    }
}
