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
package com.wanderwildwood.onsa.ui.temperaments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.wanderwildwood.onsa.ui.FitPagedColumn
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.temperaments.Temperament3
import com.wanderwildwood.onsa.temperaments.predefinedTemperamentWerckmeisterVI
import com.wanderwildwood.onsa.ui.notes.centAndRatioRows
import com.wanderwildwood.onsa.ui.notes.chainOfFifths
import com.wanderwildwood.onsa.ui.notes.circleOfFifthRows
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions2
import com.wanderwildwood.onsa.ui.theme.EInkAlertDialog
import com.wanderwildwood.onsa.ui.theme.TunerTheme

@Composable
fun TemperamentDetailsDialog(
    temperament: Temperament3,
    notePrintOptions: NotePrintOptions2,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    val fifths = remember(temperament) { chainOfFifths(temperament, null) }
    EInkAlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            OutlinedButtonMMD(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { TextMMD(stringResource(id = R.string.acknowledged)) }
        },
        title = { TextMMD(stringResource(id = R.string.details)) },
        text = {
            // Paged, not scrolled: a page turns on whole rows, with no scrollbar. Only as tall as
            // its rows when they fit, and the tables wrap to the width (see the key note dialog).
            FitPagedColumn(maxHeight = 420.dp) { width ->
                val rows = mutableListOf<@Composable () -> Unit>()
                rows += centAndRatioRows(temperament, null, notePrintOptions, width)
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

@Preview(widthDp = 400, heightDp = 500)
@Composable
private fun TemperamentDetailsDialogPreview() {
    TunerTheme {
        val temperament = remember { predefinedTemperamentWerckmeisterVI(0L) }

        TemperamentDetailsDialog(
            temperament,
            NotePrintOptions2()
        )
    }
}