/*
 * Copyright 2020 Michael Moessner
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

package com.wanderwildwood.onsa.notedetection

import kotlin.math.*
import kotlin.math.roundToInt
import kotlin.ranges.until

private fun bitReverse(value : Int, numBits : Int) : Int {
    var myValue = value
    var rev = 0

    repeat(numBits) {
        rev = rev shl (1)
        rev = rev or (myValue and 1)
        myValue = myValue shr (1)
    }

    return rev
}

/** Number of frequencies, for which we get a result from the a real fft.
 * @param size Size as passed to the constructor of the FFT.
 * @return Number of frequencies for which when using realFFT
 */
fun FFT.Companion.numFrequenciesReal(size: Int) : Int {
    return size / 2 + 1
}

/** Number of frequencies, for which we get a result from the fft.
 * @param size Size as passed to the constructor of the FFT. Note, that this is not the number of
 *   complex values passed to fft, but the corresponding number of complex numbers would be size/2
 * @return Number of frequencies as result for fft.
 */
fun FFT.Companion.numFrequenciesComplex(size: Int) : Int {
    return size / 2
}


/** Frequency for a specific index computed by the fftReal.
 * @param idx Index for which the frequency should be computed.
 * @param size Size as passed to the constructor of the FFT.
 * @param dt Time step width between two input samples.
 * @return Frequency value for the given value.
 */
fun FFT.Companion.getFrequencyReal(idx : Int, size: Int, dt : Float) : Float {
    return idx / (dt * size)
}

/** Frequency for a specific index computed by the fft.
 * @param idx Index of complex number for which the frequency should be computed.
 * @param size Size as passed to the constructor of the FFT.
 * @param dt Time step width between two input samples.
 * @return Frequency value for the given value.
 */
fun FFT.Companion.getFrequencyComplex(idx : Int, size: Int, dt : Float) : Float {
    val numValues = size / 2
    val half = numValues / 2

    return if (idx < half)
        idx / (dt * numValues)
    else
        (idx - numValues) / (dt * numValues)
}


/** Fast Fourier transform for pure real input data.
 * @param size Number of floating point values of input. This means:
 *   For real fft: size = number of values to be transformed.
 *   For complex fft: size = number of complex numbers / 2
 *   Must be a power of two!.
 */
class FFT(val size : Int) {
    companion object;

    private val cosTable = FloatArray(size / 2)
    private val sinTable = FloatArray(size / 2)
    private val cosTableH = FloatArray(size / 2)
    private val sinTableH = FloatArray(size / 2)
    val bitReverseTable = IntArray(size / 2)
    private val nBits = log2(size.toFloat()).roundToInt()

    init {
        val sizeCheck = 1 shl nBits
        if (size != sizeCheck) {
            throw kotlin.RuntimeException("RealFFT size must be a power of 2 but $size given.")
        }
        val halfSize = size / 2

        val fac: Float = -2.0f * PI.toFloat() / size
        for (i in 0 until halfSize) {
            sinTable[i] = sin(2 * i * fac)
            cosTable[i] = cos(2 * i * fac)

            sinTableH[i] = sin(i * fac)
            cosTableH[i] = cos(i * fac)

            bitReverseTable[i] = bitReverse(i, nBits - 1)
        }
    }

    /** Perform fast Fourier transform for complex numbers.
     * @param input Input data to be transformed. Size must be the size given in the constructor.
     *   So size/2 complex numbers, where real and imaginary part are in alternating order, e.g.
     *      v0_re, v0_im, v1_re, v1_im, ...
     * @param output Array where we will store the output. Size must be the same as the input. .
     *   The results will be stored as pairs of real and imaginary part:
     *    -> real part at given index: output[2 * index]
     *    -> imaginary part at given index: output[2 * index + 1]
     *   Order of corresponding frequencies is:
     *      0, 1/T, 2/T, ..., (N/2-1)/T, -N/T, -(N-1)/T, -(N-2)/T, ..., -1/T
     *   Where T is dt*N, and N is the number of complex numbers (i.e. N = size/2)
     */
    fun fft(input: FloatArray, output: FloatArray) {
        require(input.size == size) { "FFT input is of invalid size" }
        require(output.size == size) { "FFT output is of invalid size" }

        bitReverse(input, output)
        fftBitReversed(output)
    }

    /** Perform inverse fast Fourier transform for complex numbers.
     * @param input Input data to be transformed. Size must be the size given in the constructor.
     *   So size/2 complex numbers, where real and imaginary part are in alternating order, e.g.
     *      v0_re, v0_im, v1_re, v1_im, ...
     *   Expected order of corresponding frequencies is:
     *      0, 1/T, 2/T, ..., (N/2-1)/T, -N/T, -(N-1)/T, -(N-2)/T, ..., -1/T
     *   Where T is dt*N, and N is the number of complex numbers (i.e. N = size/2)
     * @param output Array where we will store the output. Size must be the same as the input.
     *   The results will be stored as pairs of real and imaginary part:
     *    -> real part at given index: output[2 * index]
     *    -> imaginary part at given index: output[2 * index + 1]
     */
    fun ifft(input: FloatArray, output: FloatArray) {
        require(input.size == size) { "FFT input is of invalid size" }
        require(output.size == size) { "FFT output is of invalid size" }
        bitReverse(input, output)
        ifftBitReversed(output)
    }

    /** Perform fast Fourier transform for real numbers.
     * @param input Input data to be transformed (size must be the size given in the constructor)
     * @param output Array where we will store the output. Size must be "size of input + 2".
     *   The results will be stored as pairs of real and imaginary part:
     *    -> real part at given index: output[2 * index]
     *    -> imaginary part at given index: output[2 * index + 1]
     *   The corresponding frequencies will be:
     *   0, 1/T, 2/T, ..., (size/2) / T
     * Note: The imaginary parts of the first and the last numbers will always be zero (i.e.
     *   output[1] == 0, output[size+1] == 0)
     */
    fun fftReal(input: FloatArray, output: FloatArray) {
        require(input.size >= size) { "FFT input is of invalid size" }
        require(output.size == size + 2) { "FFT output is of invalid size" }

        bitReverse(input, output)
        fftBitReversed(output)
        combineFFTResultForRealFFT(output)
    }

    /** Perform inverse fast Fourier transform where expected output are real numbers.
     * From theoretical point of view, this means, that the value of negative frequencies are
     * the conjugate complex of the positive ones. So the negative values are skipped and not
     * even provided.
     * @param input Input data to be transformed (size must be the size given in the constructor + 2)
     *   The ordering of data must be the same as the result of the fftReal! The data must be
     *   stored in pairs of real in imaginary parts, i.e. [v0_re, v0_im, v1_re, v1_im, ...]
     * @param output Array where we will store the output. Size must be the size given in the
     *   constructor.".
     */
    fun ifftReal(input: FloatArray, output: FloatArray) {
        require(input.size == size + 2) { "FFT input is of invalid size" }
        require(output.size >= size) { "FFT output is of invalid size" }
        prepareDataForInverseRealFFT(input, output)

        bitReverseInPlace(output)
        ifftBitReversed(output)
    }

    /** Transform already bit-reversed data in-place
     * @param output Data array to be transformed.
     */
    fun fftBitReversed(output: FloatArray) {
        require(output.size >= size) { "size of output must equal or larger than fftSize" }
        val halfSize = size / 2
        var numInner = 1
        var wStep = halfSize / 2

        repeat(nBits - 1) {
            var idx1 = 0
            var idx2 = halfSize / 2

            for (i in 0 until numInner) {
                val cos1 = cosTable[idx1]
                val sin1 = sinTable[idx1]
                val cos2 = cosTable[idx2]
                val sin2 = sinTable[idx2]

                var k1re = 2 * i

                repeat(wStep){
                    val k1im = k1re + 1
                    val k2re = k1re + 2 * numInner
                    val k2im = k2re + 1

                    val tmp2Re = output[k2re]
                    val tmp2Im = output[k2im]

                    output[k2re] = output[k1re] + cos2 * tmp2Re - sin2 * tmp2Im
                    output[k2im] = output[k1im] + cos2 * tmp2Im + sin2 * tmp2Re
                    output[k1re] += cos1 * tmp2Re - sin1 * tmp2Im
                    output[k1im] += cos1 * tmp2Im + sin1 * tmp2Re

                    k1re += 4 * numInner
                }
                idx1 += wStep
                idx2 += wStep
            }
            numInner *= 2
            wStep /= 2
        }
    }

    /** Transform already bit-reversed data in-place
     * @param output Data array to be transformed.
     */
    fun ifftBitReversed(output: FloatArray) {
        require(output.size >= size) { "size of output must equal or larger than fftSize" }
        val halfSize = size / 2
        var numInner = 1
        var wStep = halfSize / 2

        repeat(nBits - 1) {
            var idx1 = 0
            var idx2 = halfSize / 2

            for (i in 0 until numInner) {
                val cos1 = cosTable[idx1]
                val sin1 = sinTable[idx1]
                val cos2 = cosTable[idx2]
                val sin2 = sinTable[idx2]

                var k1re = 2 * i

                repeat(wStep) {
                    val k1im = k1re + 1
                    val k2re = k1re + 2 * numInner
                    val k2im = k2re + 1

                    val tmp2Re = output[k2re]
                    val tmp2Im = output[k2im]

                    output[k2re] = output[k1re] + cos2 * tmp2Re + sin2 * tmp2Im
                    output[k2im] = output[k1im] + cos2 * tmp2Im - sin2 * tmp2Re
                    output[k1re] += cos1 * tmp2Re + sin1 * tmp2Im
                    output[k1im] += cos1 * tmp2Im - sin1 * tmp2Re

                    k1re += 4 * numInner
                }
                idx1 += wStep
                idx2 += wStep
            }
            numInner *= 2
            wStep /= 2
        }
    }

    /** Store bit-reversed input in output.
     * Values are handled in pairs, emulating inline complex numbers.
     * @param input Input array (size must be >= size), but we will only use the first size numbers.
     * @param output Output array (size must be >= size), and we will only fill the first size
     *   numbers.
     */
    private fun bitReverse(input: FloatArray, output: FloatArray) {
        val halfSize = size / 2

        for (i in 0 until halfSize) {
            val ir2 = 2 * bitReverseTable[i]
            val i2 = 2 * i
            if (i2 >= ir2) {
                val tmp1 = input[i2]
                output[i2] = input[ir2]
                output[ir2] = tmp1
                val tmp2 = input[i2 + 1]
                output[i2 + 1] = input[ir2 + 1]
                output[ir2 + 1] = tmp2
            }
        }
    }

    private fun bitReverseInPlace(data: FloatArray) {
        require(data.size >= size) { "size of data must be fft size" }
        bitReverse(data, data)
    }

    /** This will combine a FFT result needed for real-numbered FFT
     * @param output Result of complex fft, which got only real valued input.
     *   After the function call the output will contain the real-fft result.
     */
    fun combineFFTResultForRealFFT(output: FloatArray) {
        require(output.size == size + 2) { "FFT output is of invalid size" }

        val halfSize = size / 2

        var k1re = 0
        var k1im = 1
        var k2re = 2 * halfSize
        var k2im = k2re + 1

        for (i in 1 .. halfSize / 2) {
            k1re += 2
            k1im += 2
            k2re -= 2
            k2im -= 2

            val cos1 = cosTableH[i]
            val sin1 = sinTableH[i]
            val cos2 = cosTableH[halfSize - i]
            val sin2 = sinTableH[halfSize - i]

            val frRe = 0.5f * (output[k1re] + output[k2re])
            val frIm = 0.5f * (output[k1im] - output[k2im])
            val grRe = 0.5f * (output[k1im] + output[k2im])
            val grIm = 0.5f * (output[k2re] - output[k1re])

            output[k1re] = frRe + cos1 * grRe - sin1 * grIm
            output[k1im] = frIm + sin1 * grRe + cos1 * grIm
            output[k2re] = frRe + cos2 * grRe + sin2 * grIm
            output[k2im] = -frIm + sin2 * grRe - cos2 * grIm
        }

        output[size] = output[0] - output[1]
        output[0] = output[0] + output[1]
        output[1] = 0.0f
        output[size+1] = 0.0f
    }

    /** Prepare frequency domain data, which only contains the positive frequencies
     *   to obtain a real ifft result.
     *  @note input and output are allowed to be the same variable. However, output only
     *     the first size values will be filled.
     *  @param input Frequency domain data as e.g. obtain by a real-fft.
     *  @param output Prepared data which can go into a inverse complex fft, and
     *    such that one gets real-valued time domain data.
     */
    private fun prepareDataForInverseRealFFT(input: FloatArray, output: FloatArray) {
        require(input.size == size + 2) { "FFT input is of invalid size" }
        require(output.size >= size) { "FFT output is of invalid size" }

        val halfSize = size / 2

        var k1re = 0
        var k1im = 1
        var k2re = 2 * halfSize
        var k2im = k2re + 1

        for (i in 1 .. halfSize / 2) {
            k1re += 2 // range of k1re during loop: 2 .. size/2
            k1im += 2 // range of k1re during loop: 3 .. size/2 + 1
            k2re -= 2 // range of k2re during loop: size-2 .. size/2
            k2im -= 2 // range of k2im during loop: size-1 .. size/2 + 1

            val cos1 = cosTableH[i]
            val sin1 = sinTableH[i]
            val cos2 = cosTableH[halfSize - i]
            val sin2 = sinTableH[halfSize - i]

            val frRe = 0.5f * (input[k1re] + input[k2re])
            val frIm = 0.5f * (input[k1im] - input[k2im])
            val grRe = 0.5f * (input[k1im] + input[k2im])
            val grIm = 0.5f * (input[k2re] - input[k1re])

            output[k1re] = frRe - cos1 * grRe - sin1 * grIm
            output[k1im] = frIm + sin1 * grRe - cos1 * grIm
            output[k2re] = frRe - cos2 * grRe + sin2 * grIm
            output[k2im] = -frIm + sin2 * grRe + cos2 * grIm
        }

        output[1] = 0.5f * (input[0] - input[size])
        output[0] = 0.5f * (input[0] + input[size])
    }
//    fun getFreq(idx : Int, dt : Float) : Float {
//        return idx / (dt * size)
//    }
}
