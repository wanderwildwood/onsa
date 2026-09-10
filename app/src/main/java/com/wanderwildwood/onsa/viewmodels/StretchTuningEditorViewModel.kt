package com.wanderwildwood.onsa.viewmodels

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.wanderwildwood.onsa.misc.GetTextFromString
import com.wanderwildwood.onsa.stretchtuning.StretchTuning
import com.wanderwildwood.onsa.temperaments.TemperamentResources
import com.wanderwildwood.onsa.temperaments.centsToFrequency
import com.wanderwildwood.onsa.temperaments.centsToRatio
import com.wanderwildwood.onsa.ui.plot.GestureBasedViewPort
import com.wanderwildwood.onsa.ui.screens.StretchTuningEditorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log10
import kotlin.math.pow

@HiltViewModel (assistedFactory = StretchTuningEditorViewModel.Factory::class)
class StretchTuningEditorViewModel @AssistedInject constructor(
    @Assisted initialStretchTuning: StretchTuning,
    @Assisted("initialName") initialName: String, // we cannot read this directly from stretchTuning, since we need a context to resolve the string
    @Assisted("initialDescription") initialDescription: String, // we cannot read this directly from stretchTuning, since we need a context to resolve the string
    val pref: TemperamentResources
    ) : ViewModel(), StretchTuningEditorState {

    @AssistedFactory
    interface Factory {
        fun create(
            initialStretchTuning: StretchTuning,
            @Assisted("initialName") initialName: String,
            @Assisted("initialDescription") initialDescription: String
        ): StretchTuningEditorViewModel
    }

    private val _name = mutableStateOf(initialName)
    override val name: State<String> get() = _name

    private val _description = mutableStateOf(initialDescription)
    override val description: State<String> get() = _description

    private val stableId = initialStretchTuning.stableId
    private val _stretchTuning = MutableStateFlow(initialStretchTuning)
    override val stretchTuning = _stretchTuning.asStateFlow()

    private val _selectedKey = MutableStateFlow(StretchTuning.NO_KEY)
    override val selectedKey = _selectedKey.asStateFlow()
    override val gestureBasedViewPort = GestureBasedViewPort()
    override val lazyListState = LazyListState()

    override fun modifyName(name: String) {
        _name.value = name
    }

    override fun modifyDescription(description: String) {
        _description.value = description
    }

    override fun select(key: Int) {
        _selectedKey.value = key
    }

    override fun addLine() {
        val currentStretchTuning = stretchTuning.value
        when (currentStretchTuning.size) {
            0 -> {
                _stretchTuning.value = currentStretchTuning.add(
                    pref.musicalScale.value.referenceFrequency.toDouble(),
                    0.0,
                )
                _selectedKey.value = _stretchTuning.value.keys.lastOrNull() ?: StretchTuning.NO_KEY
            }
            1 -> {
                val newFrequency = centsToFrequency(
                    100.0,
                    currentStretchTuning.unstretchedFrequencies[0]
                )
                _stretchTuning.value = currentStretchTuning.add(
                    newFrequency,
                    currentStretchTuning.stretchInCents[0]
                )
                _selectedKey.value = _stretchTuning.value.keys.lastOrNull() ?: StretchTuning.NO_KEY
            }
            else -> {
                val keyIndex = currentStretchTuning.keys.indexOfFirst { it == selectedKey.value }
                val i0 = if (keyIndex == -1)
                    currentStretchTuning.size - 2
                else
                    keyIndex.coerceIn(0, currentStretchTuning.size - 2)
                val lf0 = log10(currentStretchTuning.unstretchedFrequencies[i0])
                val lf1 = log10(currentStretchTuning.unstretchedFrequencies[i0 + 1])
                val c0 = currentStretchTuning.stretchInCents[i0]
                val c1 = currentStretchTuning.stretchInCents[i0 + 1]
                val lfnew = if (keyIndex == -1 || keyIndex == currentStretchTuning.size-1) {
                    2 * lf1 - lf0
                } else {
                    0.5 * (lf1 + lf0)
                }
                val cnew = c0 + (c1 - c0) * (lfnew - lf0) / (lf1 - lf0)
                val fnew = 10.0.pow(lfnew)
                _stretchTuning.value = currentStretchTuning.add(
                    fnew, cnew
                )
                val index = _stretchTuning.value.unstretchedFrequencies.binarySearch(fnew)
                if (index >= 0)
                    _selectedKey.value = _stretchTuning.value.keys[index]
            }
        }
    }

    override fun removeLine(key: Int) {
        // store index of line to be deleted if it is selected
        val lineIndex = if (selectedKey.value == key) {
            _stretchTuning.value.keys.indexOfFirst { it == key }
        } else {
            -1
        }
        _stretchTuning.value = stretchTuning.value.remove(key)

        // update selected line if necessary
        if (lineIndex >= 0) {
            if (stretchTuning.value.size > 0) {
                _selectedKey.value = stretchTuning.value.keys[
                    lineIndex.coerceIn(0, stretchTuning.value.size - 1)
                ]
            } else {
                _selectedKey.value = StretchTuning.NO_KEY
            }
        }
    }

    override fun modifyLine(
        unstretchedFrequency: Double,
        stretchInCents: Double,
        key: Int
    ) {
        // Log.v("Tuner", "StretchTuningEditorViewModel: modifyLine: unstretched frequency = $unstretchedFrequency, stretchInCents = $stretchInCents, key=$key")
        _stretchTuning.value = stretchTuning.value.modify(unstretchedFrequency, stretchInCents, key)
    }

    fun saveStretchTuning() {
        val newStretchTuning = StretchTuning(
            name = GetTextFromString(name.value),
            description = GetTextFromString(description.value),
            unstretchedFrequencies = stretchTuning.value.unstretchedFrequencies,
            stretchInCents = stretchTuning.value.stretchInCents,
            stableId = stableId
        )
        pref.addNewOrReplaceStretchTuning(newStretchTuning)
    }
}