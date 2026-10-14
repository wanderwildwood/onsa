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
package com.wanderwildwood.onsa.ui.notes

import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.lazy.LazyRowMMD
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.musicalscale.MusicalScale2
import com.wanderwildwood.onsa.notenames.NoteNamesEDOGenerator
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import kotlinx.coroutines.flow.drop

/** Select between available notes.
 * @param selectedIndex List index of selected note in noteNameScale.notes
 * @param modifier Modifier.
 * @param singleNoteSize Maximum size of a single note.
 * @param onIndexChanged Callback when another note index was selected. This refers to the
 *   note of noteNameScale.notes
 * @param content Notes in lazy list.
 */
@Composable
private fun NoteSelectorBase(
    selectedIndex: Int,
    modifier: Modifier,
    singleNoteSize: DpSize,
    onIndexChanged: (index: Int) -> Unit,
    content: LazyListScope.() -> Unit
) {
    val state = rememberLazyListState(selectedIndex)
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnIndexChanged by rememberUpdatedState(onIndexChanged)

    // MMD's LazyRow does its own dragging: a swipe jumps the row a few notes with scrollToItem,
    // and the row's own scrolling is switched off. So no scroll ever reached a nested-scroll
    // listener, and a swipe moved the notes without choosing one. The note in the middle is
    // the first visible one, the content padding being half a row wide, so whichever note a
    // swipe stops on is taken as chosen.
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }
            .drop(1)
            .collect { index -> if (index != currentSelected) currentOnIndexChanged(index) }
    }

    // A jump rather than a glide: an e-ink screen shows a glide as a smear of half-drawn rows.
    LaunchedEffect(key1 = selectedIndex, key2 = state) {
        if (selectedIndex >= 0 && state.firstVisibleItemIndex != selectedIndex)
            state.scrollToItem(selectedIndex)
    }

    // One note high. MMD's LazyRow fills all the height it is offered, so without this the
    // selector took the rest of the instrument editor, and the strings above it got none.
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(singleNoteSize.height),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(singleNoteSize),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) { }

        LazyRowMMD(
            modifier = Modifier.height(singleNoteSize.height),
            contentPadding = PaddingValues(
                horizontal = (maxWidth - singleNoteSize.width).coerceAtLeast(0.dp) / 2
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            state = state,
            isScrollbarVisible = false,
            content = content,
        )
    }
}

/** Select between available notes.
 * @param selectedIndex List index of selected note in noteNameScale.notes
 * @param notes Notes of the note chooser.
 * @param notePrintOptions How to print the notes.
 * @param modifier Modifier.
 * @param fontSize Font size of notes.
 * @param textStyle Text style of notes.
 * @param onIndexChanged Callback when another note index was selected. This refers to the
 *   note of noteNameScale.notes
 */
@Composable
fun NoteSelector(
    selectedIndex: Int,
    notes: Array<MusicalNote>,
    notePrintOptions: NotePrintOptions2,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    textStyle: TextStyle? = MaterialTheme.typography.labelLarge,
    onIndexChanged: (index: Int) -> Unit = {}
) {
    val fontSizeResolved = fontSize.takeOrElse {
        (textStyle?.fontSize ?: TextUnit.Unspecified).takeOrElse {
            LocalTextStyle.current.fontSize.takeOrElse { 12.sp }
        }
    }
    val fontWeightResolved = textStyle?.fontWeight

    val minSingleNoteSize = rememberMaxNoteSize(
        notes = notes,
        notePrintOptions = notePrintOptions,
        fontSize = fontSize,
        fontWeight = fontWeightResolved,
        octaveRange = null
    ) + DpSize(16.dp, 4.dp)

    val singleNoteSize = DpSize(
        if (minSingleNoteSize.width >= 48.dp) minSingleNoteSize.width else 48.dp,
        if (minSingleNoteSize.height >= 40.dp) minSingleNoteSize.height else 40.dp
    )

    NoteSelectorBase(
        selectedIndex = selectedIndex,
        modifier = modifier,
        singleNoteSize = singleNoteSize,
        onIndexChanged = onIndexChanged
    ) {
        itemsIndexed(notes) { index, note ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(singleNoteSize)
                    .clickable { onIndexChanged(index) }
            ) {
                Note(
                    note,
                    notePrintOptions = notePrintOptions,
                    withOctave = false,
                    fontSize = fontSizeResolved,
                    style = textStyle,
                    color = if (selectedIndex == index)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Select between available notes.
 * @param selectedIndex List index of selected note in noteNameScale.notes
 * @param musicalScale Scale with the available notes.
 * @param notePrintOptions How to print the notes.
 * @param modifier Modifier.
 * @param fontSize Font size of notes.
 * @param textStyle Text style of notes.
 * @param onIndexChanged Callback when another note index was selected. This refers to the
 *   note of noteNameScale.notes
 */
@Composable
fun NoteSelector(
    selectedIndex: Int,
    musicalScale: MusicalScale2,
    notePrintOptions: NotePrintOptions2,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    textStyle: TextStyle? = MaterialTheme.typography.labelLarge,
    onIndexChanged: (index: Int) -> Unit = {}
) {
    val fontSizeResolved = fontSize.takeOrElse {
        (textStyle?.fontSize ?: TextUnit.Unspecified).takeOrElse {
            LocalTextStyle.current.fontSize.takeOrElse { 12.sp }
        }
    }
    val fontWeightResolved = textStyle?.fontWeight

    val octaveRange = remember(musicalScale) {
        musicalScale.getNote(musicalScale.noteIndexBegin).octave .. musicalScale.getNote(musicalScale.noteIndexEnd-1).octave
    }

    val noteNames = remember(musicalScale.temperament, musicalScale.rootNote) {
        musicalScale.temperament.noteNames(musicalScale.rootNote)
    }

    val minSingleNoteSize = rememberMaxNoteSize(
        notes = noteNames.notes,
        notePrintOptions = notePrintOptions,
        fontSize = fontSizeResolved,
        fontWeight = fontWeightResolved,
        octaveRange = octaveRange
    ) + DpSize(16.dp, 4.dp)

    val singleNoteSize = DpSize(
        if (minSingleNoteSize.width >= 48.dp) minSingleNoteSize.width else 48.dp,
        if (minSingleNoteSize.height >= 40.dp) minSingleNoteSize.height else 40.dp
    )

    val numNotes = musicalScale.noteIndexEnd - musicalScale.noteIndexBegin
    NoteSelectorBase(
        selectedIndex = selectedIndex,
        modifier = modifier,
        singleNoteSize = singleNoteSize,
        onIndexChanged = onIndexChanged
    ) {
        items(numNotes) { index ->
            val note = musicalScale.getNote(index + musicalScale.noteIndexBegin)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(singleNoteSize)
                    .clickable { onIndexChanged(index) }
            ) {
                Note(
                    note,
                    notePrintOptions = notePrintOptions,
                    withOctave = true,
                    fontSize = fontSizeResolved,
                    style = textStyle,
                    color = if (selectedIndex == index)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 200)
@Composable
private fun NoteSelectorPreview() {
    TunerTheme {
        val noteNameScale = remember { NoteNamesEDOGenerator.getNoteNames(53, null) }

        val notePrintOptions = remember {
            NotePrintOptions2(
                octaveNotation = OctaveNotation.Scientific,
                notationType = NotationType.Standard
            )
        }

        var selectedIndex by remember { mutableIntStateOf(7) }

        Column {
            NoteSelector(
                selectedIndex = selectedIndex,
                notes = noteNameScale!!.notes,
                notePrintOptions = notePrintOptions
            ) { selectedIndex = it }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButtonMMD(onClick = {  }) {
                TextMMD("Text button")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 200)
@Composable
private fun NoteSelector2Preview() {
    TunerTheme {
        val musicalScale = remember { MusicalScale2.createTestEdo12() }

        val notePrintOptions = remember {
            NotePrintOptions2(
                octaveNotation = OctaveNotation.Scientific,
                notationType = NotationType.Standard
            )
        }

        var selectedIndex by remember { mutableIntStateOf(3) }

        Column {
            NoteSelector(
                selectedIndex = selectedIndex,
                musicalScale = musicalScale,
                notePrintOptions = notePrintOptions
            ) { selectedIndex = it }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButtonMMD(onClick = { }) {
                TextMMD("Text button")
            }
        }
    }
}