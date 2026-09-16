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
 * The whole app, in black and white, on MMD.
 *
 * This one file is the lever: every screen in the app reads its colours and type through
 * here, so replacing the body with ThemeMMD re-clothes all of them at once — MMD's E Ink
 * type scale, [monochrome], and no ripple anywhere.
 *
 * `TunerTheme` and `tunerColors` keep their names and their types, so the eighty-odd screens
 * that read them compile exactly as they did. What changed is what comes out of them.
 *
 * `tunerTypography` is gone rather than kept. Its three styles carried a size and **no
 * typeface**, and an explicit `style` replaces `LocalTextStyle` rather than merging with it —
 * so every tick label, tolerance label, string label and note selector on the plots was
 * drawn in the platform's default face while the rest of the app was in Lato. Their sizes
 * were 15, 18 and 24, which are exactly MMD's `bodySmall`, `bodyMedium` and `titleLarge`, so
 * the plots now read those and the app has one typeface again.
 *
 * Light and dark are gone, and so is dynamic colour. The panel has one appearance — dark
 * marks on a light ground, in daylight, all the time — and offering a night mode that the
 * hardware cannot honour is a setting that lies. This takes no arguments about which
 * appearance to use, because there is one: the ignored darkTheme/dynamicColor/blackNightMode
 * parameters went when the last caller stopped passing them.
 */
@Composable
fun TunerTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTunerColors provides OnLightTunerColors) {
        ThemeMMD(colorScheme = monochrome, content = content)
    }
}
