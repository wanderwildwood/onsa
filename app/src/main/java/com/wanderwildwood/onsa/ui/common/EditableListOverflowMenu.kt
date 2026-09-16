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
package com.wanderwildwood.onsa.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R

interface OverflowMenuCallbacks{
    fun onDeleteClicked()
    fun onShareClicked()
    fun onExportClicked()
    fun onImportClicked()
    fun onSettingsClicked()
}

@Composable
fun OverflowMenu(
    callbacks: OverflowMenuCallbacks,
    showSettings: Boolean = true
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = {
            expanded = !expanded
        }) {
            Icon(
                painter = painterResource(R.drawable.more_vert_24px),
                contentDescription = "menu"
            )
        }
        DropdownMenuMMD(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItemMMD(
                text = { TextMMD(stringResource(id = R.string.delete_items)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.delete_24px),
                        contentDescription = "delete"
                    )
                },
                onClick = {
                    callbacks.onDeleteClicked()
                    expanded = false
                }
            )
            HorizontalDividerMMD()
            DropdownMenuItemMMD(
                text = { TextMMD(stringResource(id = R.string.share)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.share_24px),
                        contentDescription = "share"
                    )
                },
                onClick = {
                    callbacks.onShareClicked()
                    expanded = false
                }
            )
            DropdownMenuItemMMD(
                text = { TextMMD(stringResource(id = R.string.save_to_disk)) },
                leadingIcon = {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_archive),
                        contentDescription = "archive"
                    )
                },
                onClick = {
                    callbacks.onExportClicked()
                    expanded = false
                }
            )
            DropdownMenuItemMMD(
                text = { TextMMD(stringResource(id = R.string.load_from_disk)) },
                leadingIcon = {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_unarchive),
                        contentDescription = "unarchive"
                    )
                },
                onClick = {
                    callbacks.onImportClicked()
                    expanded = false
                }
            )
            if (showSettings) {
                HorizontalDividerMMD()
                DropdownMenuItemMMD(
                    text = { TextMMD(stringResource(id = R.string.settings)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.settings_24px),
                            contentDescription = "settings"
                        )
                    },
                    onClick = {
                        callbacks.onSettingsClicked()
                        expanded = false
                    }
                )
            }
        }
    }
}
