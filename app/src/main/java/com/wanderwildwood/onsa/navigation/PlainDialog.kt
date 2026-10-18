package com.wanderwildwood.onsa.navigation

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog

/**
 * A dialog destination with nothing dimmed behind it.
 *
 * Navigation puts each dialog destination in a window of its own, and [EInkDialog] then opens
 * its panel in a second one inside it. The outer window dims by default: a screenful of grey
 * on E Ink, and while a dimming window is up Android turns the status-bar icons white, so on
 * the white bar they vanish. This clears the dim on that outer window, as EInkDialog does on
 * its own.
 */
inline fun <reified T : Any> NavGraphBuilder.plainDialog(
    noinline content: @Composable (NavBackStackEntry) -> Unit,
) {
    dialog<T> { entry ->
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window
                ?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        content(entry)
    }
}
