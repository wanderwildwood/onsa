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
package com.wanderwildwood.onsa.ui.plot

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.toRect

/** Transformation class to transform coordinates between screen and raw coordinate systems.
 * @param viewPortScreen Box with screen coordinates (left, top, bottom right).
 * @param viewPortRaw Box with corresponding raw coordinates.
 * @param viewPortCornerRadius Corner radius of used view port.
 */
data class Transformation(
    val viewPortScreen: IntRect,
    val viewPortRaw: Rect,
    val viewPortCornerRadius: Float = 0f
) {
    /** Transformation matrix to transform a raw coordinate to a screen coordinate. */
    val matrixRawToScreen = Matrix().apply {
        translate(viewPortScreen.left.toFloat(), viewPortScreen.center.y.toFloat())
        scale(viewPortScreen.width / viewPortRaw.width, viewPortScreen.height / viewPortRaw.height)
        translate(-viewPortRaw.left, -viewPortRaw.center.y)
//        Log.v("Tuner", "Transformation: create, from ${viewPortRaw} to $viewPortScreen")
//        Log.v("Tuner", "Transformation: create, translate: ${-viewPortRaw.left}, ${-viewPortRaw.center.y}")
//
//        Log.v("Tuner", "Transformation: create, scale: ${viewPortScreen.width / viewPortRaw.width}, ${viewPortScreen.height / viewPortRaw.height}")
//
//        Log.v("Tuner", "Transformation: create, translate: ${viewPortScreen.left}, ${viewPortScreen.center.y}")
    }
//    private val matrixRawToScreen = Matrix().apply {
//        setRectToRect(viewPortRaw, viewPortScreenFloat, Matrix.ScaleToFit.FILL)
//        postScale(1f, -1f, 0f, viewPortScreenFloat.centerY())
//    }
    /** Transformation matrix to transform a screen coordinate to a raw coordinate. */
    val matrixScreenToRaw = Matrix().apply {
        setFrom(matrixRawToScreen)
        invert()
    }

    /** Transform a rectangle to screen coordinates.
     * @param rect Rectangle in raw coordinates.
     * @return Rectangle in screen coordinates.
     */
    fun toScreen(rect: Rect) = matrixRawToScreen.map(rect)

    /** Transform a rectangle to screen coordinates.
     * @param point Point in raw coordinates.
     * @return Point in screen coordinates.
     */
    fun toScreen(point: Offset) = matrixRawToScreen.map(point)

    /** Transform a rectangle to raw coordinates.
     * @param rect Rectangle in screen coordinates.
     * @return Rectangle in raw coordinates.
     */
    fun toRaw(rect: Rect) = matrixScreenToRaw.map(rect)

    /** Transform a point to raw coordinates.
     * @param point Point in screen coordinates.
     * @return Point in raw coordinates.
     */
    fun toRaw(point: Offset) = matrixScreenToRaw.map(point)

    /** Transform a velocity to raw coordinates.
     * @note This is similar to transforming a point, but it dies not apply the offset.
     * @param velocity Velocity in screen coordinates.
     * @return Velocity in raw coordinates.
     */
    fun toRaw(velocity: Velocity): Velocity {
        val result = (toRaw(Offset.Zero) - toRaw(Offset(velocity.x, velocity.y)))
        return Velocity(result.x, result.y)
    }

    /** Return the clip shape of the view port.
     * @note This essentially returns that shape with the given rounded corners.
     */
    @Composable
    fun rememberClipShape() = remember(viewPortScreen, viewPortCornerRadius) {
        GenericShape { _, _ ->
            val r = CornerRadius(viewPortCornerRadius)
            addRoundRect(RoundRect(viewPortScreen.toRect(), r, r, r, r))
        }
    }
}