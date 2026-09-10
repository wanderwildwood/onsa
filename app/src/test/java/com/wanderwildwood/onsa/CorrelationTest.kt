package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.notedetection.Correlation
import org.junit.Test
import org.junit.Assert.assertEquals
import kotlin.random.Random

class CorrelationTest {

    private fun classicCorrelation(values: FloatArray): FloatArray {
        val result = FloatArray(values.size)

        // 0 1 2 3 4 5 6 7 8 9 ...
        //     0 1 2 3 4 5 6 7 8

        // 0 1 2
        // . 0 1
        // . . 0

        for (i in result.indices) {
            var v = 0f
            for (j in 0 until values.size - i) {
                v += values[j + i] * values[j]
            }
            result[i] = v
        }
        return result
    }

    @Test
    fun Random() {
        val values = floatArrayOf(
            0.63696f, 0.26979f, 0.04097f, 0.01653f, 0.81327f, 0.91276f, 0.60664f, 0.72950f, 0.54362f,
            0.93507f, 0.81585f, 0.00274f, 0.85740f, 0.03359f, 0.72966f, 0.17566f
        )
        val resultCheck = floatArrayOf(
            6.01020f, 3.78942f, 4.02729f, 3.51248f, 3.68700f, 3.76005f, 2.39249f, 2.09610f, 1.93074f,
            1.62987f, 1.30983f, 0.38935f, 0.58799f, 0.22544f, 0.51215f, 0.11189f
        )
        //val values = FloatArray(64) { Random.nextFloat() }

        val correlationCheck = classicCorrelation(values)

        resultCheck.zip(correlationCheck) {a, b ->
            assertEquals(a, b, 1e-4f)
        }

        val correlationObject = Correlation(values.size)
        val result = FloatArray(values.size)
        correlationObject.correlate(values, result)

        for (i in values.indices) {
            println("$i: check=${correlationCheck[i]}, corr=${result[i] / values.size} ")
            assertEquals(correlationCheck[i], result[i] / values.size, 1e-4f)
        }
    }
}