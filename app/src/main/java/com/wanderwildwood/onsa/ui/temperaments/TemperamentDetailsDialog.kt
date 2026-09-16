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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.temperaments.Temperament3
import com.wanderwildwood.onsa.temperaments.predefinedTemperamentWerckmeisterVI
import com.wanderwildwood.onsa.ui.notes.CentAndRatioTable
import com.wanderwildwood.onsa.ui.notes.CircleOfFifthTable
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
    val hasChainOfFifths = remember(temperament) {
        temperament.chainOfFifths() != null
    }
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
            // Paged, not scrolled: MMD's list steps and stops, and brings its own rail.
            LazyColumnMMD(modifier = Modifier.heightIn(max = 420.dp)) {
                item {
                    CentAndRatioTable(
                        temperament,
                        rootNote = null,
                        notePrintOptions = notePrintOptions,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalContentPadding = 16.dp
                    )
                }
                item {
                    if (hasChainOfFifths) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextMMD(
                            stringResource(id = R.string.circle_of_fifths),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall
                        )
                        CircleOfFifthTable(
                            temperament = temperament,
                            rootNote = null,
                            notePrintOptions = notePrintOptions,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalContentPadding = 16.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextMMD(
                            stringResource(id = R.string.pythagorean_comma_desc),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
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