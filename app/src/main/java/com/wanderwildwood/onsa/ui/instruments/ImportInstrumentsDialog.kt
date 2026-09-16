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
package com.wanderwildwood.onsa.ui.instruments

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.instruments.Instrument
import com.wanderwildwood.onsa.instruments.InstrumentIO
import com.wanderwildwood.onsa.ui.preferences.RadioButtonLine
import com.wanderwildwood.onsa.ui.theme.EInkAlertDialog
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private data class ImportOption(
    val insertMode: InstrumentIO.InsertMode,
    @StringRes val stringResource: Int
)

private val importOptions = persistentListOf(
    ImportOption(InstrumentIO.InsertMode.Replace, R.string.replace_current_list),
    ImportOption(InstrumentIO.InsertMode.Prepend, R.string.prepend_current_list),
    ImportOption(InstrumentIO.InsertMode.Append, R.string.append_current_list),
)

@Composable
fun ImportInstrumentsDialog(
    instruments: ImmutableList<Instrument>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = { },
    onImport: (insertMode: InstrumentIO.InsertMode, instruments: ImmutableList<Instrument>) -> Unit = { _,_ -> }
) {
    var importChoice by rememberSaveable { mutableStateOf(InstrumentIO.InsertMode.Append) }
    val context = LocalContext.current
    EInkAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            OutlinedButtonMMD(
                onClick = {
                onImport(importChoice, instruments)
            },
                modifier = Modifier.fillMaxWidth(),
            ) { TextMMD(stringResource(id = R.string.ok)) }
        },
        dismissButton = {
            OutlinedButtonMMD(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { TextMMD(stringResource(id = R.string.abort)) }
        },
        title = {
            TextMMD(
                context.resources.getQuantityString(
                    R.plurals.load_instruments, instruments.size, instruments.size
                )
            )
        },
        text = {
            Column(Modifier.selectableGroup()) {
                importOptions.forEach {
                    RadioButtonLine(
                        selected = (it.insertMode == importChoice),
                        stringRes = it.stringResource,
                        onClick = { importChoice = it.insertMode }
                    )
                }
            }
        },
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun ImportInstrumentsDialogTest() {
    TunerTheme {
        ImportInstrumentsDialog(
            instruments = persistentListOf()
        )
    }
}
