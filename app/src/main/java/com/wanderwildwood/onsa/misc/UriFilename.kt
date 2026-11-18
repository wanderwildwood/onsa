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
package com.wanderwildwood.onsa.misc

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.database.getStringOrNull

/** Display name of a picked document, or null when its provider gives none or refuses. */
fun getFilenameFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver?.query(
            uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst())
                cursor.getStringOrNull(nameIndex)
            else
                null
        }
    } catch (e: Exception) {
        null
    }
}
