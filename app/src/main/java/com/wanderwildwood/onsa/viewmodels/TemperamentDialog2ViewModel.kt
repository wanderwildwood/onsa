package com.wanderwildwood.onsa.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.hilt.ApplicationScope
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.temperaments.Temperament3
import com.wanderwildwood.onsa.temperaments.Temperament3Custom
import com.wanderwildwood.onsa.temperaments.TemperamentIO
import com.wanderwildwood.onsa.temperaments.TemperamentResources
import com.wanderwildwood.onsa.ui.common.EditableListData
import com.wanderwildwood.onsa.ui.common.EditableListPredefinedSectionImmutable
import com.wanderwildwood.onsa.ui.temperaments.TemperamentsDialog2Data
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemperamentDialog2ViewModel @Inject constructor(
    val pref: TemperamentResources,
    @ApplicationScope val applicationScope: CoroutineScope
) : TemperamentsDialog2Data, ViewModel() {
    private val musicalScale = pref.musicalScale.value

    private val activeTemperament = MutableStateFlow(musicalScale.temperament)

    override val listData = EditableListData(
        predefinedItemSections = persistentListOf(
            pref.edoTemperaments,
            EditableListPredefinedSectionImmutable(
                sectionStringResourceId = R.string.other_temperaments,
                items = pref.predefinedTemperaments,
                isExpanded = pref.predefinedTemperamentsExpanded,
                toggleExpanded = { pref.writePredefinedTemperamentsExpanded(it) }
            )
        ),
        editableItemsSectionResId = R.string.custom_temperaments,
        editableItems = pref.customTemperaments,
        getStableId = { it.stableId },
        editableItemsExpanded = pref.customTemperamentsExpanded,
        activeItem = activeTemperament,
        setNewItems = {
            val newTemperaments = it.filterIsInstance<Temperament3Custom>()
            pref.writeCustomTemperaments(newTemperaments.toPersistentList())
        },
        toggleEditableItemsExpanded = { pref.writeCustomTemperamentsExpanded(it) }
    )

    override val defaultTemperament: Temperament3 get() = pref.defaultTemperament

    override fun saveTemperaments(
        context: Context,
        uri: Uri,
        temperaments: List<Temperament3Custom>
    ) {
        applicationScope.launch(Dispatchers.IO) {
            context.contentResolver?.openOutputStream(uri, "wt")?.use { stream ->
                stream.bufferedWriter().use { writer ->
                    TemperamentIO.writeTemperaments(temperaments, writer, context)
                }
            }
        }
    }

    override fun proposeRootNote(temperament: Temperament3): MusicalNote {
        val rootNote = pref.musicalScale.value.rootNote
        val rootNoteInTemperament =
            temperament.possibleRootNotes().firstOrNull { it.equalsIgnoreOctave(rootNote) } == null
        return if (rootNoteInTemperament)
            rootNote
        else
            temperament.possibleRootNotes()[0]
    }
}