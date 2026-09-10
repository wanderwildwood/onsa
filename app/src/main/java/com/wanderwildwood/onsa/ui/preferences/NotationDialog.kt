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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.ui.notes.NotationType
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions2
import com.wanderwildwood.onsa.ui.notes.OctaveNotation
import com.wanderwildwood.onsa.ui.theme.TunerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotationDialog(
    notePrintOptions: NotePrintOptions2,
    onNotationChange: (notation: NotationType, octaveNotation: OctaveNotation) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var notationType by remember { mutableStateOf(notePrintOptions.notationType) }
    var octaveNotation by remember { mutableStateOf(notePrintOptions.octaveNotation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onNotationChange(notationType, octaveNotation)
                }
            ) {
                Text(stringResource(id = R.string.done))
            }
        },
        modifier = modifier,
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(id = R.string.abort))
            }
        },
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_solfege),
                contentDescription = null
            )
        },
        title = {
            Text(stringResource(id = R.string.notation))
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                var isNotationExpanded by remember { mutableStateOf(false) }
                var isOctaveNotationExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = isNotationExpanded,
                    onExpandedChange = { isNotationExpanded = it }
                ) {
                    TextField(
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        value = stringResource(notationType.stringResourceId),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.notation)) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isNotationExpanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = isNotationExpanded,
                        onDismissRequest = { isNotationExpanded = false },
                    ) {
                        for (n in NotationType.entries) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            stringResource(n.stringResourceId),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                        Text(
                                            stringResource(n.descriptionResId),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    notationType = n
                                    isNotationExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                ExposedDropdownMenuBox(
                    expanded = isOctaveNotationExpanded,
                    onExpandedChange = { isOctaveNotationExpanded = it }
                ) {
                    // Log.v("Tuner", "NotationDialog: octave expanded: $isOctaveNotationExpanded")
                    TextField(
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        value = stringResource(octaveNotation.stringResourceId),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.octave_representation)) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isOctaveNotationExpanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = isOctaveNotationExpanded,
                        onDismissRequest = { isOctaveNotationExpanded = false },
                    ) {
                        for (n in OctaveNotation.entries) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            stringResource(n.stringResourceId),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                        Text(
                                            stringResource(n.descriptionResId),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    octaveNotation = n
                                    isOctaveNotationExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Preview(widthDp = 300, heightDp = 500, showBackground = true)
@Composable
private fun NotationDialogTest() {
    TunerTheme {
        var notePrintOptions by remember { mutableStateOf(NotePrintOptions2()) }
        NotationDialog(
            notePrintOptions = notePrintOptions,
            onNotationChange = { n, h ->
                notePrintOptions = notePrintOptions.copy(
                    notationType = n, octaveNotation = h
                )
            }
        )
    }
}
