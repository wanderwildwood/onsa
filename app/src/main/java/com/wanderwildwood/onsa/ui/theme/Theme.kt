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
package com.wanderwildwood.onsa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.mudita.mmd.ThemeMMD

/**
 * The whole app, in black and white, on MMD.
 *
 * This one file is the lever: every screen in the app reads its colours and type through
 * here, so replacing the body with ThemeMMD re-clothes all of them at once — MMD's E Ink
 * type scale, a monochrome scheme, and no ripple anywhere.
 *
 * The shape of the file is deliberately unchanged. `TunerTheme`, `tunerColors` and
 * `tunerTypography` keep their names and their types, so the eighty-odd screens that read
 * them compile exactly as they did. What changed is what comes out of them.
 *
 * Light and dark are gone, and so is dynamic colour. The panel has one appearance — dark
 * marks on a light ground, in daylight, all the time — and offering a night mode that the
 * hardware cannot honour is a setting that lies.
 */

/**
 * In tune and out of tune, which upstream says in green and red.
 *
 * They are both black here, and that is not a compromise. This screen has sixteen greys
 * and no colour: a green and a red arrive as two near-identical mid-tones, so an app that
 * leans on them to say "sharp" or "in tune" says nothing at all, and says it confidently.
 *
 * The information has to be carried by position instead — where the needle sits against
 * the centre line — which is how a mechanical tuner did it before anyone had a colour
 * display, and which is legible at arm's length on a music stand.
 */
@Immutable
data class TunerColors(
    val positive: Color = Color.Unspecified,
    val onPositive: Color = Color.Unspecified,
    val negative: Color = Color.Unspecified,
    val onNegative: Color = Color.Unspecified
)

val LocalTunerColors = staticCompositionLocalOf { TunerColors() }

val MaterialTheme.tunerColors: TunerColors
    @Composable
    @ReadOnlyComposable
    get() = LocalTunerColors.current

/** One set now, because there is one appearance. Both names kept so callers still build. */
val OnLightTunerColors = TunerColors(
    positive = Color.Black,
    onPositive = Color.White,
    negative = Color.Black,
    onNegative = Color.White,
)
val OnDarkTunerColors = OnLightTunerColors

/**
 * Type for the plots.
 *
 * A shade larger than upstream's. These labels sit beside a moving line on a panel that is
 * read at arm's length on a music stand rather than held at reading distance, and 14sp
 * that works on an OLED in the hand does not survive that.
 */
@Immutable
data class TunerTypography(
    val plotSmall: TextStyle = TextStyle(fontSize = 15.sp),
    val plotMedium: TextStyle = TextStyle(fontSize = 18.sp),
    val plotLarge: TextStyle = TextStyle(fontSize = 24.sp)
)

val LocalTunerTypography = staticCompositionLocalOf { TunerTypography() }

val MaterialTheme.tunerTypography: TunerTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalTunerTypography.current

val tunerTypography = TunerTypography()

/**
 * [darkTheme], [dynamicColor] and [blackNightMode] are still accepted and are all ignored.
 *
 * They are kept so the settings screens and previews that pass them still compile, and
 * because removing a parameter that a fork's callers set is a change with no upside. There
 * is one appearance, and it is this one.
 */
@Composable
fun TunerTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    @Suppress("UNUSED_PARAMETER") blackNightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalTunerColors provides OnLightTunerColors,
        LocalTunerTypography provides tunerTypography
    ) {
        ThemeMMD(content = content)
    }
}
