package com.wanderwildwood.onsa.stretchtuning

import android.util.Log
import androidx.compose.runtime.Immutable
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.misc.GetText
import com.wanderwildwood.onsa.misc.GetTextFromResId
import com.wanderwildwood.onsa.temperaments.centsToFrequency
import com.wanderwildwood.onsa.temperaments.ratioToCents
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.log10
import kotlin.random.Random

private fun DoubleArray.removeAt(position: Int): DoubleArray {
    return DoubleArray(size - 1) { this[if (it < position) it else it + 1] }
}
private fun DoubleArray.replace(i: Int, value: Double): DoubleArray {
    return DoubleArray(size) { if (it == i) value else this[it]}
}
private fun DoubleArray.add(position: Int, value: Double): DoubleArray {
    return DoubleArray(size + 1) {
        if (it < position) this[it]
        else if (it == position) value
        else this[it - 1]
    }
}
private fun IntArray.removeAt(position: Int): IntArray {
    return IntArray(size - 1) { this[if (it < position) it else it + 1] }
}
private fun IntArray.add(position: Int, value: Int): IntArray {
    return IntArray(size + 1) {
        if (it < position) this[it]
        else if (it == position) value
        else this[it - 1]
    }
}

private fun checkIsMonotonic(values: DoubleArray): Boolean {
    if (values.size <= 1)
        return true

    for (i in 1 until values.size) {
        if (values[i] <= values[i - 1])
            return false
    }
    return true
}

/** Stretch tuning.
 * @param name Name of stretch tuning.
 * @param description Description of stretch tuning.
 * @param unstretchedFrequencies List of unstretched frequencies. This must be strictly increasing.
 * @param stretchInCents  The cent values of the ratio of stretched / unstretched frequencies.
 * @param keys Unique key for each unstretchedFrequency
 *   (so unstretchedFrequencies.size == key.size).
 * @param stableId Unique id for the stretch tuning. Negative values are reserved for predefined
 *   stretch tunings. Positive values are used for user defined stretch tunings.
 */
@Immutable
@Serializable
data class StretchTuning(
    val name: GetText = GetTextFromResId(R.string.no_stretch_tuning),
    val description: GetText = GetTextFromResId(R.string.stretch_tuning_off),
    val unstretchedFrequencies: DoubleArray = doubleArrayOf(),
    val stretchInCents: DoubleArray = doubleArrayOf(),
    val keys: IntArray = IntArray(unstretchedFrequencies.size) { it },
    val stableId: Long = NO_STABLE_ID
) {
    /** Number of values, which define the stretch tuning. */
    val size get() = unstretchedFrequencies.size

    /** List of stretched frequencies.
     * One value for each unstretched frequency, so
     * unstretchedFrequencies.size == stretchedFrequencies). These values must be strictly
     * increasing.
     */
    @Transient
    val stretchedFrequencies = unstretchedFrequencies.zip(stretchInCents) { unstretched, cents ->
        centsToFrequency(cents, unstretched)
    }.toDoubleArray()

    /** Flag which tells whether stretched frequencies are strictly increasing.
     * @note For the stretch tuning to be valid, this must be true. However, we allow non-monotonic
     *   values while a user is editing the temperament.
     */
    @Transient
    val isMonotonic = checkIsMonotonic(stretchedFrequencies)

    /** Get a stretched frequency.
     * @param unstretchedFrequency Unstretched frequency.
     * @param referenceFrequency Reference frequency at which we do not stretch. Note that in case
     *   where the stretched frequencies of the input arrays of the class do not have zero stretch
     *   at this frequency, we will shift the stretch such that at the reference frequency the
     *   stretched value will also be the reference frequency.
     * @return Stretched frequency.
     */
    fun getStretchedFrequency(unstretchedFrequency: Double, referenceFrequency: Double): Double {
        val referenceStretch = getStretch(referenceFrequency)
        val stretch = getStretch(unstretchedFrequency) -referenceStretch

        val stretchedFrequency = centsToFrequency(stretch, unstretchedFrequency)
        //Log.v("Tuner", "StretchTuning.getStretchedFrequency: unstretched = $unstretchedFrequency, stretched = $stretchedFrequency, cents = ${stretch}")
        //if (unstretchedFrequency == Double.POSITIVE_INFINITY)
        //    throw RuntimeException("Frequency cannot be infinity")
        return stretchedFrequency
    }

    /** This basically interpolates the cent value at the given input frequency.
     * @param unstretchedFrequency Unstretched frequency.
     * @return Interpolated cent value.
     */
    private fun getStretch(unstretchedFrequency: Double): Double {
        if (unstretchedFrequencies.size <= 1)
            return 0.0

        val index = unstretchedFrequencies.binarySearch(unstretchedFrequency)
        if (index >= 0)
            return stretchInCents[index]

        val insertionIndex = -index - 1
        // the following line do linear interpolation on logscale, if outside the bounds,
        //   it does extrapolation
        val i0 = (insertionIndex - 1).coerceIn(0, unstretchedFrequencies.size - 2)
        val i1 = i0 + 1
        val lf = log10(unstretchedFrequency)
        val lf0 = log10(unstretchedFrequencies[i0])
        val lf1 = log10(unstretchedFrequencies[i1])
        val c0 = stretchInCents[i0]
        val c1 = stretchInCents[i1]
        return c0 + (c1 - c0) * (lf - lf0) / (lf1 - lf0)
    }

    /** Return a new object where the given value pair was added.
     * @note This object will not be changed!
     * @note In case the unstretchedFrequency already exists, the value pair will updated.
     * @param unstretchedFrequency Unstretched frequency.
     * @param stretchInCents Stretch in cents.
     * @return New StretchTuning object where the line was added if possible.
     */
    fun add(unstretchedFrequency: Double, stretchInCents: Double) : StretchTuning {
        return add(unstretchedFrequency, stretchInCents, generateKey())
    }

    /** Return a new object where the data pair of a given key is removed.
     * @param key Key of stretch tuning value pair.
     * @return New StretchTuning with the value pair removed.
     */
    fun remove(key: Int): StretchTuning {
        val position = keys.indexOf(key)
        return if (position >= 0) {
            StretchTuning(
                name = name,
                description = description,
                unstretchedFrequencies = unstretchedFrequencies.removeAt(position),
                stretchInCents = stretchInCents.removeAt(position),
                keys = keys.removeAt(position)
            )
        } else {
            this
        }
    }

    /** Return a new StretchTuning object, where a value pair of a given key is changed.
     * @param unstretchedFrequency Modified unstretched frequency.
     * @param stretchInCents Modified cent deviation.
     * @param key Key of value pair which should be changed. If the key does not exist, nothing
     *   will be changed.
     * @return In case that the class is changed a new StretchTuning object with the modified
     *   values.
     */
    fun modify(unstretchedFrequency: Double, stretchInCents: Double, key: Int) : StretchTuning {
        val position = keys.indexOfFirst { it == key }
        // Log.v("Tuner", "StretchTuning.modify: unstretchedFrequency=$unstretchedFrequency, stretchInCents=$stretchInCents, key=$key, position=$position")
        return if (position >= 0 && unstretchedFrequencies[position] == unstretchedFrequency) {
            StretchTuning(
                name = name,
                description = description,
                unstretchedFrequencies = unstretchedFrequencies,
                stretchInCents = this.stretchInCents.replace(position, stretchInCents),
                keys = keys
            )
        } else if (position >= 0) {
            this.remove(key).add(unstretchedFrequency, stretchInCents, key)
        } else {
            this
        }
    }

    private fun add(unstretchedFrequency: Double, stretchInCents: Double, key: Int) : StretchTuning {
        val position = unstretchedFrequencies.binarySearch(unstretchedFrequency)

        // in case the unstretched frequency already exists, overwrite it
        return if (position >= 0) {
            modify(unstretchedFrequency, stretchInCents, keys[position])
        } else {
            val positionResolved = -position - 1
            StretchTuning(
                name = name,
                description = description,
                unstretchedFrequencies = unstretchedFrequencies.add(
                    positionResolved,
                    unstretchedFrequency
                ),
                stretchInCents = this.stretchInCents.add(
                    positionResolved,
                    stretchInCents
                ),
                keys = keys.add(positionResolved, key)
            )
        }
    }

    private fun generateKey(): Int {
        var value = Random.nextInt(0, Int.MAX_VALUE - 1)
        while (keys.contains(value))
            value = Random.nextInt(0, Int.MAX_VALUE - 1)
        return value
    }
    companion object {

        /** When the value pair of the class has no defined key, use this value. */
        const val NO_KEY = Int.MAX_VALUE

        /** Notes that the stableId of the class is not (yet) defined. */
        const val NO_STABLE_ID = Long.MAX_VALUE
    }
}