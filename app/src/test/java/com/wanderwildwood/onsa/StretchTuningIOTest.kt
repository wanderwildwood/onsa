package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.misc.GetTextFromString
import com.wanderwildwood.onsa.stretchtuning.StretchTuningIO
import org.junit.Test

class StretchTuningIOTest {
    @Test
    fun simple() {
        val string = """Studio ABC (1,4-2,0 m): C4 -16,1 -13,9 -11,8 -9,9 -6,9 -5,1 -8,4
                
Concert DEF (>4,7 m): A0 -6,5 -6 -5,5 -5 -4,6 -4,2 -3,8 -3,4 -3,1 -2,8 -2,5 -2,2 -1,9 -1,6


Stretch 5: F#4 -12,6 -12,25 -11,9 -11,55 -11,2 -10,85 -10,5 -10,15 -9,8 -9,45 -9,1 -8,75

Stretch M: Gb4 -2 -1,5 -1 -0,5 0 0 0 0 0 0 0 0,35 0,7 1,05 1,4 1,75 2,1

Stretch L: A0  -8,67 -8 -7,33 -6,67 -4 -3,33 0 0 0 0 0 0,5 1  8 8,5 9 9,5 13,5  17 17,5 18
"""
//        val tunings = StretchTuningIO.parseStretchTunings(string)
//        println("Number of tunings = ${tunings.size}")
//        for (t in tunings) {
//            println((t.name as GetTextFromString).string)
//            for (i in 0 until t.size)
//                println("${t.unstretchedFrequencies[i]} ${t.stretchInCents[i]}")
//        }
    }
}