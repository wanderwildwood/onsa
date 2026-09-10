package com.wanderwildwood.onsa

import com.wanderwildwood.onsa.temperaments.FifthModification
import com.wanderwildwood.onsa.temperaments.RationalNumber
import com.wanderwildwood.onsa.temperaments.circleOfFifthsPythagorean
import com.wanderwildwood.onsa.temperaments.circleOfFifthsQuarterCommaMeanTone
import com.wanderwildwood.onsa.temperaments.circleOfFifthsYoung2
import com.wanderwildwood.onsa.temperaments.extendedQuarterCommaMeantone
import com.wanderwildwood.onsa.temperaments.predefinedTemperamentEDO
import com.wanderwildwood.onsa.temperaments.predefinedTemperamentExtendedQuarterCommaMeanTone
import com.wanderwildwood.onsa.temperaments.predefinedTemperamentFifthCommaMeanTone
import com.wanderwildwood.onsa.temperaments.predefinedTemperamentPythagorean
import com.wanderwildwood.onsa.temperaments.predefinedTemperamentQuarterCommaMeanTone
import com.wanderwildwood.onsa.temperaments.predefinedTemperamentYoung2
import com.wanderwildwood.onsa.temperaments.ratioToCents
import com.wanderwildwood.onsa.ui.notes.Fifth
import org.junit.Assert.assertEquals
import org.junit.Test

class ChainOfFifthsTest {

    @Test
    fun pythagorean() {
        val chain = predefinedTemperamentPythagorean(0L).chainOfFifths()
        val circle = circleOfFifthsPythagorean

        circle.getRatios().zip(chain.getSortedRatios()).forEach {
            println("${it.first}, ${it.second}")
            assertEquals(it.first, it.second, 1e-12)
        }
        val lastFifth = chain.getClosingCircleCorrection()
        println(lastFifth)
    }

    @Test
    fun quarterCommaMeanTone() {
        val chain = predefinedTemperamentQuarterCommaMeanTone(0L).chainOfFifths()
        val circle = circleOfFifthsQuarterCommaMeanTone

        circle.getRatios().zip(chain.getSortedRatios()).forEach {
            println("${it.first}, ${it.second}")
            assertEquals(it.first, it.second, 1e-12)
        }
        val lastFifth = chain.getClosingCircleCorrection()
        println(lastFifth)
    }

    @Test
    fun extendedQuarterCommaMeanToneTest() {
        val chain = predefinedTemperamentExtendedQuarterCommaMeanTone(0L).chainOfFifths()
        val cents = extendedQuarterCommaMeantone

        chain.getSortedRatios().zip(cents).forEach {
            val centChain = ratioToCents(it.first)
            println("$centChain, ${it.second}")
            //assertEquals(centChain, it.second, 1e-12)
        }
    }

    @Test
    fun testOther() {
        val chain = predefinedTemperamentYoung2(0L).chainOfFifths()
        val circle = circleOfFifthsYoung2
        
        circle.getRatios().zip(chain.getSortedRatios()).forEach {
            println("${it.first}, ${it.second}")
            assertEquals(it.first, it.second, 1e-12)
        }
        val lastFifth = chain.getClosingCircleCorrection()
        println(lastFifth)
    }
    @Test
    fun other() {
        val chain = predefinedTemperamentFifthCommaMeanTone(0L).chainOfFifths()

        chain.getSortedRatios().forEach {
            println("$it")
        }
        val lastFifth = chain.getClosingCircleCorrection()
        println(lastFifth)
        println(lastFifth.toDouble())
        val ref = FifthModification(RationalNumber(-1, 1), syntonicComma = RationalNumber(11, 5))
        println(ref.toDouble())
    }

    @Test
    fun chainNames() {
        val temperament = predefinedTemperamentEDO(12, 0L)
        val chain = temperament.chainOfFifths()
        if (chain == null) {
            arrayOf()
        } else {
            val unsorted = chain.getRatiosAlongFifths()
            val sorted = chain.getSortedRatios()
            val notes = temperament.noteNames(null)
            unsorted.mapIndexed { index, us ->
                val i = sorted.indexOfFirst { us == it }
                println ("$i: $us =? ${sorted[i]}, c=${ratioToCents(us)}, m?=${chain.fifths.getOrNull(index)==null}, name=${notes[i].base} ${notes[i].modifier}")
                Fifth(notes[i], chain.fifths.getOrNull(index),
                    isRoot = false, drawNoteLight = false, drawModificationLight = true)
            }.toTypedArray()
        }
    }
}