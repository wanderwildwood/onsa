package com.wanderwildwood.onsa.ui.theme

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A titled dialog with a press at the bottom, in [EInkDialog]'s panel.
 *
 * This exists because upstream asked thirteen questions through Material's `AlertDialog`,
 * which is wrong here three times over: it animates in, it sizes itself to its buttons, and
 * its container is drawn from `surfaceContainerHigh` — a role MMD does not set, so every one
 * of those thirteen dialogs was painting **nothing at all** behind its text. See
 * [monochrome] for why an unset role does not fail loudly.
 *
 * Material's centred icon above the title is gone with it. A dialog that has just been
 * opened deliberately does not need a picture of the thing it was opened from.
 *
 * The slots are the same as `AlertDialog`'s so the thirteen call sites read the same, but a
 * button in them is a full-width [OutlinedButtonMMD], as it is in every other app here —
 * a dialog asks in its own face rather than in a strip of small text at the bottom right.
 */
@Composable
fun EInkAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable ColumnScope.() -> Unit,
    dismissButton: @Composable (ColumnScope.() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    EInkDialog(onDismiss = onDismissRequest) {
        if (title != null) {
            CompositionLocalProvider(
                LocalTextStyle provides
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            ) { title() }
            Spacer(Modifier.height(12.dp))
        }
        if (text != null) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodySmall,
            ) { text() }
            Spacer(Modifier.height(16.dp))
        }
        confirmButton()
        if (dismissButton != null) {
            Spacer(Modifier.height(8.dp))
            dismissButton()
        }
    }
}
