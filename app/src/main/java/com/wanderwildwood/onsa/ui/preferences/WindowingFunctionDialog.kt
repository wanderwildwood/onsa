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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
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
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.notedetection.WindowingFunction
import com.wanderwildwood.onsa.ui.theme.EInkAlertDialog
import com.wanderwildwood.onsa.ui.theme.TunerTheme

@Composable
fun WindowingFunctionDialog(
    initialWindowingFunction: WindowingFunction,
    onWindowingFunctionChanged: (windowingFunction: WindowingFunction) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var windowingFunction by remember { mutableStateOf(initialWindowingFunction) }

    EInkAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            OutlinedButtonMMD(
                onClick = {
                    onWindowingFunctionChanged(windowingFunction)
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
            TextMMD(stringResource(id = R.string.windowing_function))
        },
        text = {
            // Paged, not scrolled: MMD's list steps and stops, and brings its own rail.
            LazyColumnMMD(modifier = Modifier.heightIn(max = 420.dp)) {
                item {
                    Column(Modifier.selectableGroup()) {
                        for (w in WindowingFunction.entries) {
                            RadioButtonLine(
                                selected = w == windowingFunction,
                                stringRes = w.stringResourceId,
                                onClick = { windowingFunction = w }
                            )
                        }
                    }
                }
            }
        },
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun WindowingFunctionDialogTest() {
    TunerTheme {
        WindowingFunctionDialog(
            WindowingFunction.Hann,
            onWindowingFunctionChanged = {}
        )
    }
}
