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

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

interface TickLevel {
    fun getTicksRange(
        startValue: Float,
        endValue: Float,
        maxNumTicks: Int,
        labelSizeRaw: Float
    ): TicksRange

    fun getTickValue(level: Int, index: Int): Float
}

data class TicksRange(
    val level: Int,
    val indexBegin: Int,
    val indexEnd: Int
)

/// Coarse ticks come first, then get finer
class TickLevelExplicitRanges(
    private val ticks: ImmutableList<FloatArray>
) : TickLevel {
    private val minimumDistance = ticks.map {
        var dMin = Float.MAX_VALUE
        for (i in 1 until it.size)
            dMin = min(dMin, (it[i] - it[i-1]).absoluteValue)
//        Log.v("Tuner", "dmin=$dMin, size=${it.size}")
        dMin
    }
    override fun getTicksRange(
        startValue: Float,
        endValue: Float,
        maxNumTicks: Int,
        labelSizeRaw: Float
    ): TicksRange {
        var level = 0

        val minimumAllowedDistance = (endValue - startValue).absoluteValue / (maxNumTicks + 1)
        for (l in ticks.size-1 downTo 0) {
            level = l
            if (minimumDistance[l] > minimumAllowedDistance)
                break
        }

        val i0 = ticks[level].binarySearch(startValue - labelSizeRaw)
        val iBegin = if (i0 >= 0) i0 else -i0 - 1
        val i1 = ticks[level].binarySearch(endValue + labelSizeRaw)
        val iEnd = if (i1 >= 0) i1 else -i1 - 1

        return TicksRange(level, iBegin, iEnd)
    }

    override fun getTickValue(level: Int, index: Int) = ticks[level][index]
}

/**
 * Like [TickLevelExplicitRanges], coarse ticks first, but a level is judged by the gaps
 * between its ticks where the plot is looking, not anywhere along it: notes are far closer
 * together in the bass than in the treble, so judged along the whole scale, no level but the
 * coarsest would ever fit. The finest level whose labels have room not to overlap is used.
 */
class TickLevelVisibleRanges(
    private val ticks: ImmutableList<FloatArray>
) : TickLevel {
    override fun getTicksRange(
        startValue: Float,
        endValue: Float,
        maxNumTicks: Int,
        labelSizeRaw: Float
    ): TicksRange {
        var range = TicksRange(0, 0, 0)
        for (l in ticks.size - 1 downTo 0) {
            val level = ticks[l]
            val i0 = level.binarySearch(startValue - labelSizeRaw)
            val iBegin = if (i0 >= 0) i0 else -i0 - 1
            val i1 = level.binarySearch(endValue + labelSizeRaw)
            val iEnd = if (i1 >= 0) i1 else -i1 - 1
            range = TicksRange(l, iBegin, iEnd)
            var dMin = Float.MAX_VALUE
            for (i in max(iBegin, 1) until min(iEnd + 1, level.size))
                dMin = min(dMin, (level[i] - level[i - 1]).absoluteValue)
            if (dMin >= labelSizeRaw.absoluteValue)
                break
        }
        return range
    }

    override fun getTickValue(level: Int, index: Int) = ticks[level][index]
}

/// smallest deltas come first
class TickLevelDeltaBased(
    private val deltas: ImmutableList<Float>
) : TickLevel {
    constructor(vararg deltas: Float) : this(deltas.toList().toImmutableList())

    override fun getTicksRange(
        startValue: Float,
        endValue: Float,
        maxNumTicks: Int,
        labelSizeRaw: Float
    ): TicksRange {
        val minimumAllowedDistance = (endValue - startValue).absoluteValue / (maxNumTicks + 1)
        val dI = deltas.binarySearch(minimumAllowedDistance)
        val deltaIndex = min(if (dI >= 0) dI else -dI - 1, deltas.size - 1)
        val delta = deltas[deltaIndex]

        val iBegin = floor((startValue - labelSizeRaw) / delta).toInt()
        val iEnd = ceil((endValue + labelSizeRaw) / delta).toInt() + 1

        return TicksRange(deltaIndex, iBegin, iEnd)
    }

    override fun getTickValue(level: Int, index: Int) = index * deltas[level]
}