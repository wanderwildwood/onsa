package com.wanderwildwood.onsa.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Calls [onPress] as each press anywhere on this layout ends (a tap, a scroll, a hold), before
 * that press does anything itself. A message that stays "until the next press" is taken down
 * here; a press that says something new says it afterwards, so the new message stays. Taking it
 * down as the finger lifts, not as it lands, keeps the page still under the finger. Nothing is
 * consumed. [onPress] is read once: it should only write state.
 */
fun Modifier.onEachPress(onPress: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
        } while (event.changes.any { it.pressed })
        onPress()
    }
}
