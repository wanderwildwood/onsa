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
package com.wanderwildwood.onsa.ui.preferences

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.wanderwildwood.onsa.ui.FitPagedColumn
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.preferences.PreferenceResources
import com.wanderwildwood.onsa.musicalscale.MusicalScale2
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.temperaments.Temperament3
import com.wanderwildwood.onsa.ui.misc.rememberNumberFormatter
import com.wanderwildwood.onsa.ui.notes.centAndRatioRows
import com.wanderwildwood.onsa.ui.notes.chainOfFifths
import com.wanderwildwood.onsa.ui.notes.circleOfFifthRows
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions2
import com.wanderwildwood.onsa.ui.notes.NoteSelector
import com.wanderwildwood.onsa.ui.theme.EInkAlertDialog
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition

@Composable
fun RootNoteDialog(
    initialRootNote: MusicalNote,
    temperament: Temperament3,
    onDone: (rootNote: MusicalNote) -> Unit,
    notePrintOptions: NotePrintOptions2,
    modifier: Modifier = Modifier,
    warning: String? = null,
    onDismiss: () -> Unit = {}
) {
    val rootNotes = remember(temperament) {
         temperament.possibleRootNotes()
    }

    var selectedRootNoteIndex by rememberSaveable { mutableIntStateOf(
        rootNotes
            .indexOfFirst { it.equalsIgnoreOctave(initialRootNote) }
            .coerceAtLeast(0)
    ) }

    val fifths = remember(temperament, selectedRootNoteIndex) {
        if (temperament.chainOfFifths() == null) emptyArray()
        else chainOfFifths(temperament, rootNotes[selectedRootNoteIndex])
    }

    val context = LocalContext.current

    EInkAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            OutlinedButtonMMD(
                onClick = {
                    onDone(
                        rootNotes[selectedRootNoteIndex]
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { TextMMD(stringResource(id = R.string.done)) }
        },
        dismissButton = {
            OutlinedButtonMMD(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { TextMMD(stringResource(id = R.string.abort)) }
        },
        title = {
            // TextMMD(stringResource(id = R.string.root_note))
            TextMMD(temperament.name.value(context))
        },
        text = {
            // Paged, not scrolled: a page turns on whole rows, with no scrollbar. At most 360 dp
            // leaves room for the title and both buttons on a 600 dp screen (at 420 the lower
            // button was cut away); with less in it the list is only as tall as its rows, so no
            // blank band is left above the buttons. The tables come as rows that wrap to the
            // width, so every note is seen and a page fills to its foot.
            FitPagedColumn(maxHeight = 360.dp) { width ->
                val rows = mutableListOf<@Composable () -> Unit>()
                // No row at all without a warning: an empty first row kept the list from knowing
                // it was on its first page, so the up arrow was drawn as if there were one before.
                if (warning != null) rows += {
                    TextMMD(
                        warning,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
                rows += {
                    TextMMD(
                        stringResource(id = R.string.root_note),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                rows += {
                    NoteSelector(
                        selectedIndex = selectedRootNoteIndex,
                        notes = rootNotes,
                        notePrintOptions = notePrintOptions,
                        onIndexChanged = { selectedRootNoteIndex = it }
                    )
                }
                rows += {
                    OutlinedButtonMMD(
                        onClick = {
                            selectedRootNoteIndex = 0
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp)
                    ) {
                        TextMMD(stringResource(id = R.string.use_default))
                    }
                }
                rows += {
                    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        HorizontalDividerMMD(modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp))
                        TextMMD(
                            stringResource(id = R.string.details),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                rows += centAndRatioRows(
                    temperament,
                    rootNotes[selectedRootNoteIndex],
                    notePrintOptions,
                    width
                )
                if (fifths.isNotEmpty()) {
                    rows += {
                        TextMMD(
                            stringResource(id = R.string.circle_of_fifths),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 4.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    rows += circleOfFifthRows(fifths, notePrintOptions, width)
                    rows += {
                        TextMMD(
                            stringResource(id = R.string.pythagorean_comma_desc),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp)
                        )
                    }
                }
                rows
            }
        },
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun RootNoteDialogTest() {
    TunerTheme {
        val state = remember { MusicalScale2.createTestEdo12() }
        val notePrintOptions = remember { NotePrintOptions2() }
        RootNoteDialog(
            state.rootNote,
            state.temperament,
            notePrintOptions = notePrintOptions,
            onDone = { }
        )
    }
}
