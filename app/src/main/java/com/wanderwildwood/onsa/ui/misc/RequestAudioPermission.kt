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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R

/** State of the microphone permission as seen by a screen that needs it.
 * @param granted True if the microphone may be used.
 * @param showBanner True if the screen should show the "grant microphone access" banner.
 * @param blocked True if the system will not ask again, so the button must open the settings.
 * @param onButtonClicked Ask again or, if the system will not ask any more, open the app's
 *   settings page.
 */
class TunerAudioPermission(
    val granted: Boolean,
    val showBanner: Boolean,
    val blocked: Boolean,
    val onButtonClicked: () -> Unit
)

/** Open the settings page of this app, where the microphone permission can be switched on. */
fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberTunerAudioPermission(): TunerAudioPermission {
    val context = LocalContext.current

    // True once the system has answered a request with "no". Before that the banner stays away,
    // so it does not flash up over the system dialog.
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val permission = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO) {
        if (!it)
            askedOnce = true
    }
    val granted by remember { derivedStateOf { permission.status.isGranted }}

    LaunchedEffect(permission.status) {
        if (!permission.status.isGranted && !askedOnce)
            permission.launchPermissionRequest()
    }
    val blocked = !granted && askedOnce && !permission.status.shouldShowRationale
    return TunerAudioPermission(
        granted = granted,
        showBanner = !granted && askedOnce,
        blocked = blocked,
        onButtonClicked = {
            if (blocked)
                openAppSettings(context)
            else
                permission.launchPermissionRequest()
        }
    )
}

/** Banner which sits in the layout (above the content, never over it) while the microphone is
 * not allowed. Shows nothing if the permission is granted or not yet answered. */
@Composable
fun AudioPermissionBanner(permission: TunerAudioPermission, modifier: Modifier = Modifier) {
    if (!permission.showBanner)
        return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface)
            .padding(8.dp)
    ) {
        TextMMD(
            stringResource(
                if (permission.blocked)
                    R.string.audio_record_permission_blocked
                else
                    R.string.audio_record_permission_rationale
            ),
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        OutlinedButtonMMD(
            onClick = permission.onButtonClicked,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            TextMMD(
                stringResource(
                    if (permission.blocked) R.string.settings else R.string.grant_permission
                )
            )
        }
    }
}
