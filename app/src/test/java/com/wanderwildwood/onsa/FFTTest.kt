package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.notedetection.FFT
import com.wanderwildwood.onsa.notedetection.getFrequencyComplex
import com.wanderwildwood.onsa.notedetection.getFrequencyReal
import com.wanderwildwood.onsa.notedetection.numFrequenciesComplex
import com.wanderwildwood.onsa.notedetection.numFrequenciesReal
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class FFTTest {
    @Test
    fun sinTestComplex() {
        val numSamples = 16
        val frequency = 1f
        val amp = 2f
        val offset = 2.3f
        val samples = FloatArray(2 * numSamples) {0f}
        for (i in 0 until numSamples) {
            samples[2*i] = offset + amp * sin(2 * PI.toFloat() * frequency * i / numSamples.toFloat())
        }

        val fft = FFT(2 * numSamples)
        val result = FloatArray(2 * numSamples)
        fft.fft(samples, result)
        assertEquals(numSamples, FFT.numFrequenciesComplex(2 * numSamples))
        for (i in 0 until numSamples) {
            val f = FFT.getFrequencyComplex(i,  2* numSamples, 1f / numSamples)
            println("f = ${f}: Re = ${result[2*i]}, Im = ${result[2*i+1]}")
        }
        val offsetFFT = result[0] / numSamples
        //assertEquals(offset, offsetFFT, 1e-5f)
        val ampFFT = -result[3] / numSamples * 2
        //assertEquals(amp, ampFFT, 1e-5f)
        val ampFFT2 = -result[2 * (numSamples + 1) - 3] / numSamples * 2
        assertEquals(ampFFT2, -ampFFT, 1e-5f)

        val ifftResult = FloatArray(2 * numSamples)
        fft.ifft(result, ifftResult)

        for (i in 0 until numSamples) {
            println("Re = ${ifftResult[2*i] / numSamples} / ${samples[2*i]}, Im = ${ifftResult[2*i+1] / numSamples} / ${samples[2*i+1]}")
        }
        ifftResult.zip(samples) { a, b ->
            assertEquals(b, a / numSamples, 1e-6f)
        }
    }

    @Test
    fun randomTestComplex() {
        val numSamples = 16
        val samples = floatArrayOf(
            0.636962f, 0.863179f,0.269787f, 0.541461f,0.040974f, 0.299712f,0.016528f, 0.422687f,
            0.813270f, 0.028320f,0.912756f, 0.124283f,0.606636f, 0.670624f,0.729497f, 0.647190f,
            0.543625f, 0.615385f,0.935072f, 0.383678f,0.815854f, 0.997210f,0.002739f, 0.980835f,
            0.857404f, 0.685542f,0.033586f, 0.650459f,0.729655f, 0.688447f,0.175656f, 0.388921f
        )
        // expected  result computed with numpy fft
        val resultOther = floatArrayOf(
            8.119998f, 8.987933f,-3.830690f, -0.441174f,0.623124f, 1.534330f,1.837989f, 1.754941f,
            -0.081609f, -1.690349f,0.869224f, 0.591218f,-0.871009f, 0.882308f,1.410356f, -1.589122f,
            1.968761f, 0.708904f,0.769433f, 1.333645f,-1.727598f, 0.954003f,-0.426617f, 0.883407f,
            1.397895f, 0.763215f,-0.063509f, -0.315977f,0.015132f, -0.311831f,0.180508f, -0.234587f
        )

        val fft = FFT(2 * numSamples)
        val result = FloatArray(2 * numSamples)
        fft.fft(samples, result)
        result.zip(resultOther) { a, b ->
            assertEquals(b, a, 1e-5f)
        }
        for (i in 0 until numSamples) {
            val f = FFT.getFrequencyComplex(i,  2* numSamples, 1f / numSamples)
            println("f = $f: Re = ${result[2*i]}, Im = ${result[2*i+1]}")
        }

        val ifftResult = FloatArray(2 * numSamples)
        fft.ifft(result, ifftResult)

        ifftResult.zip(samples) { a, b ->
            assertEquals(b, a / numSamples, 1e-6f)
        }

        println()
        for (i in 0 until numSamples) {
            println("Re = ${ifftResult[2*i] / numSamples} / ${samples[2*i]}, Im = ${ifftResult[2*i+1] / numSamples} / ${samples[2*i+1]}")
        }
    }

    @Test
    fun randomTestReal() {
        val numSamples = 16
        val samples = floatArrayOf(
            0.636962f,0.269787f,0.040974f,0.016528f,0.813270f,0.912756f,0.606636f,0.729497f,
            0.543625f,0.935072f,0.815854f,0.002739f,0.857404f,0.033586f,0.729655f,0.175656f,
        )
        // expected  result computed with numpy fft
        val resultOther = floatArrayOf(
            8.119998f, 0.000000f,-1.825091f, -0.103294f,0.319128f, 0.923080f,0.887240f, 1.035459f,
            0.658143f, -1.226782f,0.221303f, -0.146095f,-1.299304f, -0.035848f,1.089895f, -1.461384f,
            1.968761f, 0.000000f

        )

        val fft = FFT(numSamples)
        val result = FloatArray(numSamples + 2)
        fft.fftReal(samples, result)
        result.zip(resultOther) { a, b ->
            assertEquals(b, a, 1e-5f)
        }
        assertEquals(numSamples / 2 + 1, FFT.numFrequenciesReal(numSamples))
        for (i in 0 .. numSamples/2) {
            val f = FFT.getFrequencyReal(i,  numSamples, 1f / numSamples)
            println("f = $f: Re = ${result[2*i]}, Im = ${result[2*i+1]}")
        }

        val ifftResult = FloatArray(numSamples)
        fft.ifftReal(result, ifftResult)

        println()
        for (i in 0 until numSamples) {
            println("${2 * ifftResult[i] / numSamples} / ${samples[i]}")
        }
        ifftResult.zip(samples) { a, b ->
            assertEquals(b, 2 * a / numSamples, 1e-6f)
        }
    }

    @Test
    fun sinTest() {
        val numSamples = 32
        val frequency = 1f
        val amp = 2f
        val offset = 2.3f
        val samples =
            FloatArray(numSamples) { i -> offset + amp * sin(2 * PI.toFloat() * frequency * i / numSamples.toFloat()) }
        val fft = FFT(numSamples)
        val result = FloatArray(numSamples + 2)
        fft.fftReal(samples, result)
        val offsetFFT = result[0] / numSamples
        assertEquals(offset, offsetFFT, 1e-5f)
        val ampFFT = -result[3] / numSamples * 2
        assertEquals(amp, ampFFT, 1e-5f)

        val ifftResult = FloatArray(numSamples)
        fft.ifftReal(result, ifftResult)

        samples.zip(ifftResult) { a, b ->
            println("$a, ${2 * b / samples.size}")
            assertEquals(a, 2 * b / samples.size, 1e-5f)
        }
    }

    @Test
    fun cosTest() {
        val numSamples = 32
        val frequency = 2f
        val amp = 2f
        val offset = 2.3f
        val samples =
            FloatArray(numSamples) { i -> offset + amp * cos(2 * PI.toFloat() * frequency * i / numSamples.toFloat()) }
        val fft = FFT(numSamples)
        val result = FloatArray(numSamples + 2)
        fft.fftReal(samples, result)
        val offsetFFT = result[0] / numSamples
        assertEquals(offset, offsetFFT, 1e-5f)
        val ampFFT = result[4] / numSamples * 2
        assertEquals(amp, ampFFT, 1e-5f)
    }

//    @Test
//    fun freqTest() {
//        val numSamples = 32
//        val frequency = 1f
//        val dt = 1f / numSamples
//        val freqFFT = RealFFT.getFrequency(1, numSamples, dt)
//        assertEquals(frequency, freqFFT, 1e-6f)
//    }
}