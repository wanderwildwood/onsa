package com.wanderwildwood.onsa.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.hilt.ApplicationScope
import com.wanderwildwood.onsa.stretchtuning.StretchTuning
import com.wanderwildwood.onsa.stretchtuning.StretchTuningIO
import com.wanderwildwood.onsa.temperaments.TemperamentResources
import com.wanderwildwood.onsa.ui.common.EditableListData
import com.wanderwildwood.onsa.ui.common.EditableListPredefinedSectionImmutable
import com.wanderwildwood.onsa.ui.screens.StretchTuningOverviewState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StretchTuningOverviewViewModel @Inject constructor(
    val pref: TemperamentResources,
    @ApplicationScope val applicationScope: CoroutineScope
) : StretchTuningOverviewState, ViewModel() {

    private val musicalScale = pref.musicalScale.value

    override val defaultStretchTuning = pref.defaultStretchTuning

    private val activeStretchTuning = MutableStateFlow(musicalScale.stretchTuning)

    override val listData = EditableListData(
        predefinedItemSections = persistentListOf(
            EditableListPredefinedSectionImmutable(
                sectionStringResourceId = R.string.predefined_items,
                items = pref.predefinedStretchTunings,
                isExpanded = pref.predefinedStretchTuningsExpanded,
                toggleExpanded = { pref.writePredefinedStretchTuningsExpanded(it) }
            )
        ),
        editableItemsSectionResId = R.string.custom_item,
        editableItems = pref.customStretchTunings,
        editableItemsExpanded = pref.customStretchTuningsExpanded,
        toggleEditableItemsExpanded = { pref.writeCustomStretchTuningsExpanded(it) },
        getStableId = { it.stableId },
        activeItem = activeStretchTuning,
        setNewItems = { pref.writeCustomStretchTunings(it) }
        )

    override fun saveStretchTunings(
        context: Context,
        uri: Uri,
        stretchTunings: List<StretchTuning>
    ) {
       applicationScope.launch(Dispatchers.IO) {
          context.contentResolver?.openOutputStream(uri, "wt")?.use { stream ->
             stream.bufferedWriter().use { writer ->
                 StretchTuningIO.writeStretchTunings(stretchTunings, writer, context)
             }
          }
       }
    }
}