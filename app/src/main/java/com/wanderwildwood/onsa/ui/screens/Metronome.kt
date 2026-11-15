package com.wanderwildwood.onsa.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.metronome.ClickSound
import com.wanderwildwood.onsa.metronome.MetronomeEngine
import com.wanderwildwood.onsa.metronome.MetronomePreset
import com.wanderwildwood.onsa.metronome.MetronomeSettings
import com.wanderwildwood.onsa.metronome.Subdivision
import com.wanderwildwood.onsa.ui.PagedColumn
import com.wanderwildwood.onsa.ui.common.DottedRule
import com.wanderwildwood.onsa.ui.common.onEachPress
import com.wanderwildwood.onsa.ui.misc.TunerScaffoldWithoutBottomBar
import com.wanderwildwood.onsa.ui.preferences.SimplePreference
import com.wanderwildwood.onsa.ui.preferences.SwitchPreference
import com.wanderwildwood.onsa.ui.theme.DialogButtons
import com.wanderwildwood.onsa.ui.theme.EInkDialog
import com.wanderwildwood.onsa.ui.theme.HouseButton
import com.wanderwildwood.onsa.ui.theme.rememberArmedKey
import com.wanderwildwood.onsa.viewmodels.MetronomeViewModel
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * The metronome. Back to the tuner stops it, since the tuner would only hear the clicks; the
 * screen going off or another app coming up does not.
 */
@Composable
fun Metronome(
    data: MetronomeViewModel,
    onNavigateUp: () -> Unit,
    onSavedTemposClicked: () -> Unit,
) {
    val settings by data.settings.collectAsStateWithLifecycle()
    val playing by data.playing.collectAsStateWithLifecycle()
    val failed by data.failed.collectAsStateWithLifecycle()
    // A sound that would not play is said until the next press, or until the screen is left.
    DisposableEffect(Unit) { onDispose { data.clearFailed() } }
    var dialog by rememberSaveable { mutableStateOf(MetronomeDialog.None) }

    val leave = {
        data.stop()
        onNavigateUp()
    }
    BackHandler(onBack = leave)

    TunerScaffoldWithoutBottomBar(
        canNavigateUp = true,
        onNavigateUpClicked = leave,
        showPreferenceButton = false,
        title = stringResource(R.string.metronome),
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .onEachPress { data.clearFailed() }
        ) {
            TempoFigure(settings.bpm, onClick = { dialog = MetronomeDialog.Bpm })
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (step in listOf(-5, -1, 1, 5)) {
                    HouseButton(
                        label = if (step < 0) "−${-step}" else "+$step",
                        onClick = { data.stepBpm(step) },
                        enabled = settings.bpm + step in MetronomeSettings.MIN_BPM..MetronomeSettings.MAX_BPM,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            BeatDots(data, settings)
            PagedColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(end = 4.dp),
                showCutRow = true,
            ) {
                item {
                    BeatsRow(settings.beatsPerBar, onStep = data::stepBeats)
                }
                item {
                    SwitchPreference(
                        name = stringResource(R.string.metronome_accent),
                        checked = settings.accentFirstBeat,
                        onCheckChange = data::setAccent,
                        modifier = Modifier.clickable { data.setAccent(!settings.accentFirstBeat) }
                    )
                }
                item {
                    SimplePreference(
                        name = stringResource(R.string.metronome_subdivision),
                        supporting = stringResource(settings.subdivision.label),
                        modifier = Modifier.clickable { dialog = MetronomeDialog.Subdivision }
                    )
                }
                item {
                    SimplePreference(
                        name = stringResource(R.string.metronome_sound),
                        supporting = stringResource(settings.sound.label),
                        modifier = Modifier.clickable { dialog = MetronomeDialog.Sound }
                    )
                }
                item {
                    SimplePreference(
                        name = stringResource(R.string.metronome_saved_tempos),
                        modifier = Modifier.clickable(onClick = onSavedTemposClicked)
                    )
                }
            }
            if (failed) {
                TextMMD(
                    text = stringResource(R.string.metronome_could_not_play),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HouseButton(
                    label = stringResource(R.string.metronome_tap),
                    onClick = data::tap,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                )
                HouseButton(
                    label = stringResource(if (playing) R.string.metronome_stop else R.string.metronome_start),
                    onClick = data::startStop,
                    on = playing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                )
            }
        }
    }

    when (dialog) {
        MetronomeDialog.None -> {}
        MetronomeDialog.Bpm -> BpmDialog(
            initial = settings.bpm,
            onDismiss = { dialog = MetronomeDialog.None },
            onDone = {
                data.setBpm(it)
                dialog = MetronomeDialog.None
            }
        )
        MetronomeDialog.Subdivision -> ChoiceDialog(
            title = stringResource(R.string.metronome_subdivision),
            options = Subdivision.entries.map { stringResource(it.label) },
            selected = settings.subdivision.ordinal,
            onDismiss = { dialog = MetronomeDialog.None },
            onPick = {
                data.setSubdivision(Subdivision.entries[it])
                dialog = MetronomeDialog.None
            }
        )
        MetronomeDialog.Sound -> ChoiceDialog(
            title = stringResource(R.string.metronome_sound),
            options = ClickSound.entries.map { stringResource(it.label) },
            selected = settings.sound.ordinal,
            onDismiss = { dialog = MetronomeDialog.None },
            onPick = {
                data.setSound(ClickSound.entries[it])
                dialog = MetronomeDialog.None
            }
        )
    }
}

private enum class MetronomeDialog { None, Bpm, Subdivision, Sound }

@Composable
private fun TempoFigure(bpm: Int, onClick: () -> Unit) {
    val description = stringResource(R.string.metronome_bpm_description, bpm)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // A figure the instrument shows, like the tuner's note, not running type.
        TextMMD(
            text = bpm.toString(),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 72.sp, lineHeight = 80.sp),
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        TextMMD(
            text = stringResource(R.string.metronome_bpm),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 14.dp)
        )
    }
}

/**
 * One ring per beat, the first drawn heavier when it is accented; the beat being heard is
 * filled. Read here and nowhere else, so a beat redraws these rings and nothing around them.
 */
@Composable
private fun BeatDots(data: MetronomeViewModel, settings: MetronomeSettings) {
    val beat by data.beat.collectAsStateWithLifecycle()
    val n = settings.beatsPerBar
    val accent = settings.accentFirstBeat && n > 1
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        val slot = size.width / n
        val r = minOf(size.height / 2f, slot * 0.36f)
        for (i in 0 until n) {
            val c = Offset(slot * (i + 0.5f), size.height / 2f)
            if (i == beat) {
                drawCircle(ink, r, c)
            } else {
                val w = if (i == 0 && accent) 4.dp.toPx() else 2.dp.toPx()
                drawCircle(ink, r - w / 2f, c, style = Stroke(w))
            }
        }
    }
    if (beat == MetronomeEngine.TOO_QUICK) {
        TextMMD(
            text = stringResource(R.string.metronome_too_quick_to_show),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BeatsRow(beats: Int, onStep: (Int) -> Unit) {
    val fewer = stringResource(R.string.cd_fewer_beats)
    val more = stringResource(R.string.cd_more_beats)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextMMD(
            text = stringResource(R.string.metronome_beats_in_a_bar),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        HouseButton(
            label = "−",
            onClick = { onStep(-1) },
            modifier = Modifier.semantics { contentDescription = fewer },
            enabled = beats > 1,
            compact = true
        )
        TextMMD(
            text = beats.toString(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp)
        )
        HouseButton(
            label = "+",
            onClick = { onStep(1) },
            modifier = Modifier.semantics { contentDescription = more },
            enabled = beats < MetronomeSettings.MAX_BEATS_PER_BAR,
            compact = true
        )
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    EInkDialog(onDismiss = onDismiss) {
        DialogTitle(title)
        options.forEachIndexed { i, label ->
            HouseButton(
                label = label,
                onClick = { onPick(i) },
                on = i == selected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ColumnScope.DialogTitle(title: String) {
    TextMMD(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun BpmDialog(initial: Int, onDismiss: () -> Unit, onDone: (Int) -> Unit) {
    var text by remember {
        mutableStateOf(TextFieldValue(initial.toString(), TextRange(0, initial.toString().length)))
    }
    val value = text.text.toIntOrNull()
    val valid = value != null && value in MetronomeSettings.MIN_BPM..MetronomeSettings.MAX_BPM
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    EInkDialog(onDismiss = onDismiss) {
        // Asked from inside the dialog, a frame after it is drawn: the field lives in the
        // dialog's own window, which a request from the screen beneath does not reach.
        LaunchedEffect(Unit) {
            withFrameNanos { }
            runCatching { focus.requestFocus() }
            keyboard?.show()
        }
        DialogTitle(stringResource(R.string.metronome_tempo))
        TextFieldMMD(
            value = text,
            onValueChange = { text = it },
            isError = !valid,
            singleLine = true,
            supportingText = {
                TextMMD(
                    stringResource(
                        R.string.metronome_bpm_range,
                        MetronomeSettings.MIN_BPM, MetronomeSettings.MAX_BPM
                    )
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = { if (valid) onDone(value) }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
        )
        Spacer(Modifier.height(16.dp))
        DialogButtons(
            cancel = stringResource(R.string.metronome_cancel),
            confirm = stringResource(R.string.done),
            onCancel = onDismiss,
            onConfirm = { if (valid) onDone(value) },
            confirmEnabled = valid
        )
    }
}

/** Saved tempos: tap one to bring it back; Delete asks in its own face, "Delete — tap again". */
@Composable
fun MetronomeSavedTempos(
    data: MetronomeViewModel,
    onNavigateUp: () -> Unit,
) {
    val presets by data.presets.collectAsStateWithLifecycle()
    val settings by data.settings.collectAsStateWithLifecycle()
    var naming by rememberSaveable { mutableStateOf(false) }
    var armedId by rememberArmedKey<Long>()

    TunerScaffoldWithoutBottomBar(
        canNavigateUp = true,
        onNavigateUpClicked = onNavigateUp,
        showPreferenceButton = false,
        title = stringResource(R.string.metronome_saved_tempos),
    ) { padding ->
        PagedColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(end = 4.dp),
            showCutRow = true,
        ) {
            item {
                SimplePreference(
                    name = stringResource(R.string.metronome_save_current),
                    supporting = summary(settings),
                    modifier = Modifier.clickable { naming = true }
                )
            }
            if (presets.isEmpty()) {
                item {
                    TextMMD(
                        text = stringResource(R.string.metronome_nothing_saved),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(presets.size, key = { presets[it].id }) { i ->
                val preset = presets[i]
                DottedRule()
                PresetRow(
                    preset = preset,
                    armed = armedId == preset.id,
                    onOpen = {
                        data.loadPreset(preset)
                        onNavigateUp()
                    },
                    onDelete = {
                        if (armedId == preset.id) {
                            data.deletePreset(preset.id)
                            armedId = null
                        } else {
                            armedId = preset.id
                        }
                    }
                )
            }
        }
    }

    if (naming) {
        NameDialog(
            initial = summary(settings),
            onDismiss = { naming = false },
            onSave = {
                data.savePreset(it)
                naming = false
            }
        )
    }
}

@Composable
private fun summary(s: MetronomeSettings): String {
    val base = stringResource(R.string.metronome_preset_summary, s.bpm, s.beatsPerBar)
    return if (s.subdivision == Subdivision.None) base
    else base + ", " + stringResource(s.subdivision.label).lowercase()
}

@Composable
private fun PresetRow(
    preset: MetronomePreset,
    armed: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            TextMMD(preset.name, style = MaterialTheme.typography.bodyLarge)
            TextMMD(summary(preset.settings), style = MaterialTheme.typography.labelSmall)
        }
        TextMMD(
            text = stringResource(if (armed) R.string.metronome_delete_armed else R.string.delete),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (armed) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .clickable(onClick = onDelete)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        )
    }
}

/** The name a tempo is saved under: what was typed, or [default] when nothing was. */
internal fun presetName(typed: String, default: String): String = typed.trim().ifEmpty { default }

/**
 * The field opens empty, with the name the tempo gets by default shown in it as the placeholder
 * (it goes at the first letter typed): typing gives it a name of your own and Save with nothing typed keeps the default.
 * The default used to be put in the field as selected text, and a tap in the field dropped the
 * selection and left the cursor in the middle of it.
 */
@Composable
private fun NameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    EInkDialog(onDismiss = onDismiss) {
        // Asked from inside the dialog, a frame after it is drawn: the field lives in the
        // dialog's own window, which a request from the screen beneath does not reach.
        LaunchedEffect(Unit) {
            withFrameNanos { }
            runCatching { focus.requestFocus() }
            keyboard?.show()
        }
        DialogTitle(stringResource(R.string.metronome_name_tempo))
        TextFieldMMD(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            placeholder = { TextMMD(initial) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave(presetName(text, initial)) }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
        )
        Spacer(Modifier.height(16.dp))
        DialogButtons(
            cancel = stringResource(R.string.metronome_cancel),
            confirm = stringResource(R.string.save),
            onCancel = onDismiss,
            onConfirm = { onSave(presetName(text, initial)) },
        )
    }
}
