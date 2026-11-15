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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.wanderwildwood.onsa.ui.PagedColumn
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.notedetection.WindowingFunction
import com.wanderwildwood.onsa.ui.theme.EInkAlertDialog
import com.wanderwildwood.onsa.ui.theme.TunerTheme

/**
 * How tall a list may be in a dialog that has a title and two full-width buttons under it: what
 * the screen has left, so the last button is never cut by the dialog's foot.
 */
@Composable
internal fun dialogListRoom(): Dp =
    (LocalConfiguration.current.screenHeightDp.dp - 260.dp).coerceIn(160.dp, 420.dp)

/** Room for the three choices. */
private val WINDOWING_ROWS = 180.dp

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
            // Paged, not scrolled: a page turns on whole rows. The list is as tall as its three
            // rows and no taller than the screen leaves once the title and the two buttons are in
            // (420 dp pushed Abort off the foot).
            PagedColumn(modifier = Modifier.heightIn(max = minOf(WINDOWING_ROWS, dialogListRoom())).selectableGroup()) {
                for (w in WindowingFunction.entries) {
                    item {
                        RadioButtonLine(
                            selected = w == windowingFunction,
                            stringRes = w.stringResourceId,
                            onClick = { windowingFunction = w }
                        )
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
