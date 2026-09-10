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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import com.mudita.mmd.components.slider.SliderMMD
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.ui.theme.TunerTheme

@Composable
fun SliderPreference(
    name: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (newValue: Float) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null
) {
    ListItem(
        headlineContent = {
            Text(name)
        },
        supportingContent = supporting?.let {{
            Column {
                Text(it)
                SliderMMD(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps
                )

//                Slider(
//                    value = 0.5f,
//                    onValueChange = {}
//                )
            }
        }},
        // No icon. The house style keeps a settings row to its label and its value: on a
        // 4.3" panel an icon beside every row costs a column of width and renders a small
        // glyph as a smudge.
        modifier = modifier
    )
}

@Preview(widthDp = 400, heightDp = 200, showBackground = true)
@Composable
private fun SliderPreferencePreview() {
    TunerTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            var value by remember { mutableFloatStateOf(50f)}
            SliderPreference(
                value = value,
                valueRange = 0f..100f,
                steps = 9,
                name = "My preference",
                onValueChange = { value = it },
                supporting = "Extra text $value"
            )
            HorizontalDivider()
        }
    }
}