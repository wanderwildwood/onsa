package com.wanderwildwood.onsa.stretchtuning

import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.misc.GetTextFromResId

const val STRETCH_TUNING_ID_NO_STRETCH = -1L
const val STRETCH_TUNING_ID_RAILSBACK = -2L

fun predefinedStretchTunings(): ArrayList<StretchTuning> {
    val result = ArrayList<StretchTuning>()

    result.add(StretchTuning(
        name = GetTextFromResId(R.string.no_stretch_tuning),
        description = GetTextFromResId(R.string.stretch_tuning_off),
        unstretchedFrequencies = doubleArrayOf(), stretchInCents = doubleArrayOf(),
        stableId = STRETCH_TUNING_ID_NO_STRETCH
    ))
    result.add(StretchTuning(
        name = GetTextFromResId(R.string.railsback),
        description = GetTextFromResId(R.string.railsback_description),
        unstretchedFrequencies = doubleArrayOf(
            28.409, 33.377, 39.215, 46.074, 54.132, 63.599, 74.723, 87.792, 103.146, 121.186,
            142.381, 167.283, 196.541, 230.915, 271.302, 318.752, 374.501, 440.000, 516.955,
            607.369, 713.596, 838.403, 985.037, 1157.318, 1359.730, 1597.543, 1876.950, 2205.224,
            2590.912, 3044.056, 3576.454, 4201.967
        ),
        stretchInCents = doubleArrayOf(
            -35.949, -27.623, -21.288, -16.107, -12.202, -8.756, -6.136, -4.509, -3.601, -2.863,
            -2.295, -1.767, -1.245, -0.904, -0.643, -0.389, -0.196, 0.000, 0.254, 0.508, 0.763,
            1.443, 2.156, 3.549, 5.130, 6.889, 9.121, 11.786, 15.102, 19.362, 24.684, 32.065
        ),

        stableId = STRETCH_TUNING_ID_RAILSBACK
    ))

    return result
}