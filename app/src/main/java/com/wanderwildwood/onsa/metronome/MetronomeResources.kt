package com.wanderwildwood.onsa.metronome

import android.content.Context
import com.wanderwildwood.onsa.hilt.ApplicationScope
import com.wanderwildwood.onsa.resources.ResourcesBase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/** The metronome's own settings and saved tempos, in a file of their own beside the tuner's. */
@Singleton
class MetronomeResources @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope applicationScope: CoroutineScope
) : ResourcesBase("metronome", context, applicationScope) {

    private val settingsPref = createSerializablePreference("settings", MetronomeSettings())
    val settings = settingsPref.asStateFlow()
    fun writeSettings(settings: MetronomeSettings) {
        settingsPref.set(settings.valid())
    }

    private val presetsPref = createSerializablePreference("presets", listOf<MetronomePreset>())
    val presets = presetsPref.asStateFlow()

    fun addPreset(name: String) {
        val list = presets.value
        val id = (list.maxOfOrNull { it.id } ?: 0L) + 1L
        presetsPref.set(list + MetronomePreset(id, name.trim(), settings.value))
    }

    fun deletePreset(id: Long) {
        presetsPref.set(presets.value.filterNot { it.id == id })
    }
}
