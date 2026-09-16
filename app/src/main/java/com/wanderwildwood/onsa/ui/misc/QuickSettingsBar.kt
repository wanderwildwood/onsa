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
package com.wanderwildwood.onsa.ui.misc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.musicalscale.MusicalScale2
import com.wanderwildwood.onsa.stretchtuning.predefinedStretchTunings
import com.wanderwildwood.onsa.ui.notes.Note
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions2
import com.wanderwildwood.onsa.ui.theme.TunerTheme

@Composable
fun QuickSettingsBar(
    musicalScale: MusicalScale2,
    notePrintOptions: NotePrintOptions2,
    modifier: Modifier = Modifier,
    onSharpFlatClicked: () -> Unit = {},
    onTemperamentClicked: () -> Unit = {},
    onReferenceNoteClicked: () -> Unit = {},
    onStretchTuningClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    val decimalFormat = rememberNumberFormatter()

    BottomAppBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .clickable { onReferenceNoteClicked() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Note(
                musicalNote = musicalScale.referenceNote,
                notePrintOptions = notePrintOptions,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextMMD(
                stringResource(
                    id = R.string.hertz_str,
                    decimalFormat.format(musicalScale.referenceFrequency)
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .clickable { onTemperamentClicked() },
            contentAlignment = Alignment.Center
        ) {
            val abbreviation =
                musicalScale.temperament.abbreviation.value(context).let { abbr ->
                    if (abbr == "")
                        musicalScale.temperament.name.value(context).let { nme ->
                            if (nme == "") "-" else nme
                        }
                    else
                        abbr
                }

            TextMMD(
                abbreviation,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .clickable { onSharpFlatClicked() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                ImageVector.vectorResource(
                    if (notePrintOptions.enharmonicVariant == 1)
                        R.drawable.ic_prefer_flat_isflat
                    else
                        R.drawable.ic_prefer_flat_issharp
                ),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (musicalScale.stretchTuning.size > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable { onStretchTuningClicked() },
                contentAlignment = Alignment.Center
            ) {
                TextMMD(
                    musicalScale.stretchTuning.name.value(context),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(widthDp = 300, heightDp = 48)
@Composable
private fun QuickSettingsBarPreview() {
    TunerTheme {
        var notePrintOptions by remember { mutableStateOf(NotePrintOptions2()) }
        val musicalScale = remember { MusicalScale2.createTestEdo12() }

        QuickSettingsBar(
            musicalScale = musicalScale,
            notePrintOptions = notePrintOptions,
            onSharpFlatClicked = {
                notePrintOptions = notePrintOptions.copy(
                    enharmonicVariant = if (notePrintOptions.enharmonicVariant == 0) 1 else 0,
                )
            }
        )
    }
}

@Preview(widthDp = 300, heightDp = 48)
@Composable
private fun QuickSettingsBar2Preview() {
    TunerTheme {
        var notePrintOptions by remember { mutableStateOf(NotePrintOptions2()) }
        val musicalScale = remember {
            MusicalScale2.createTestEdo12()
                .copy(_stretchTuning = predefinedStretchTunings()[1])
        }

        QuickSettingsBar(
            musicalScale = musicalScale,
            notePrintOptions = notePrintOptions,
            onSharpFlatClicked = {
                notePrintOptions = notePrintOptions.copy(
                    enharmonicVariant = if (notePrintOptions.enharmonicVariant == 0) 1 else 0,
                )
            }
        )
    }
}