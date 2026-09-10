package com.wanderwildwood.onsa.stretchtuning

import android.content.Context
import android.net.Uri
import android.util.Log
import com.wanderwildwood.onsa.BuildConfig
import com.wanderwildwood.onsa.misc.DefaultValues
import com.wanderwildwood.onsa.misc.FileCheck
import com.wanderwildwood.onsa.misc.GetTextFromString
import com.wanderwildwood.onsa.musicalscale.MusicalScale2
import com.wanderwildwood.onsa.notenames.BaseNote
import com.wanderwildwood.onsa.notenames.MusicalNote
import com.wanderwildwood.onsa.notenames.NoteModifier
import com.wanderwildwood.onsa.temperaments.predefinedTemperamentEDO
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.StringWriter
import java.util.Locale

private const val VERSION_KEY = "version"

object StretchTuningIO {
    data class StretchTuningsAndFileCheckResult(
        val fileCheck: FileCheck,
        val stretchTunings: List<StretchTuning>
    )

    fun stretchTuningsToString(context: Context, stretchTunings: List<StretchTuning>)
            : String {
        val writer = StringWriter()
        writeStretchTunings(stretchTunings, writer.buffered(), context)
        return writer.toString()
    }

    fun writeStretchTunings(stretchTunings: List<StretchTuning>, writer: BufferedWriter, context: Context) {
        writeVersion(writer)
        stretchTunings.forEach { stretchTuning ->
            writer.writeLine("")
            writeStretchTuning(stretchTuning, writer, context)
        }
    }

    fun writeStretchTuning(
        stretchTuning: StretchTuning, writer: BufferedWriter, context: Context
    ) {
        writer.writeLine("# ${stretchTuning.name.value(context)}")
        val descriptionString = stretchTuning.description.value(context)
            .replace("\n", " ")
        writer.writeLine("# $descriptionString")
        stretchTuning.unstretchedFrequencies
            .zip(stretchTuning.stretchInCents) { f, c ->
                writer.writeLine("%.4f %.4f".format(Locale.ENGLISH, f, c))
            }
    }

    fun readStretchTuningsFromFile(context: Context, uri: Uri): StretchTuningsAndFileCheckResult {
        return context.contentResolver?.openInputStream(uri)?.use { reader ->
            parseStretchTunings(reader.bufferedReader())
        } ?: StretchTuningsAndFileCheckResult(FileCheck.Invalid, listOf())
    }

    fun parseStretchTunings(reader: BufferedReader): StretchTuningsAndFileCheckResult {
        return parseStretchTuningsImpl(reader)
    }
}

private fun BufferedWriter.writeLine(line: String) {
    write(line)
    newLine()
}

private fun writeVersion(writer: BufferedWriter) {
    writer.writeLine("# $VERSION_KEY=${BuildConfig.VERSION_NAME}")
}

private fun isCommentLine(string: String): Boolean {
    return string.trimStart().getOrNull(0) == '#'
}
private fun parseVersionLine(string: String): String? {
    val trimmed = string.trim()
    if (trimmed.getOrNull(0) != '#')
        return null

    val keyAndValue = trimmed.drop(1).split('=', limit = 2)
    if (keyAndValue.size < 2)
        return null
    val key = keyAndValue[0].trim()
    val value = keyAndValue[1].trim()
    if (key == VERSION_KEY)
        return value
    return null
}

private fun parseCommentLineString(string: String): String? {
    val trimmed = string.trim()
    if (trimmed.getOrNull(0) != '#')
        return null
    val value = trimmed.drop(1).trim()
    return value.ifEmpty { null }
}

private fun parseDataLine(string: String): Pair<Double, Double>? {
    if (string.isBlank() || isCommentLine(string))
        return null

    val values = string.trim().split("\\s+".toRegex())
    if (values.size < 2)
        return null
    val unstretchedFrequency = values[0]
        .trim()
        .replace(",", ".")
        .toDoubleOrNull() ?: return null

    if (unstretchedFrequency <= 0.0) {
        return null
    }
    val stretchInCents = values[1]
        .trim()
        .replace(",", ".")
        .toDoubleOrNull() ?: return null
    return Pair(unstretchedFrequency, stretchInCents)
}

private enum class ReaderState {
    SearchVersion,
    SearchName,
    SearchDescription,
    SearchData
}

fun parseStretchTuningsImpl(reader: BufferedReader): StretchTuningIO.StretchTuningsAndFileCheckResult {
    val collectedStretchTunings = ArrayList<StretchTuning>()

    var version: String? = null
    var name: String? = null
    var description: String? = null

    var fileCheckResult = FileCheck.Ok
    var readerState = ReaderState.SearchVersion

    val unstretchedFrequencies = ArrayList<Double>()
    val stretchInCents = ArrayList<Double>()

    var lineCount = 0
    reader.forEachLine { lineRaw ->
        // skip byte order mark at beginning of file if necessary
        val line = if (lineCount == 0 && lineRaw.getOrNull(0) == '\ufeff') {
            lineRaw.substring(1)
        } else {
            lineRaw
        }
        lineCount += 1

        var lineConsumed = false
        val data = parseDataLine(line)

        if (data != null) {
            unstretchedFrequencies.add(data.first)
            stretchInCents.add(data.second)
            readerState = ReaderState.SearchData
            lineConsumed = true
            // Log.v("Tuner", "StretchTuningIO.parseStretchTuningImpl: data = ${data.first}, ${data.second}")
        }

        // one section ends with a black line (or with the file end)
        if (!lineConsumed && unstretchedFrequencies.isNotEmpty() && line.isBlank()) {
            collectedStretchTunings.add(StretchTuning(
                name = GetTextFromString(name ?: ""),
                description = GetTextFromString(description ?: ""),
                unstretchedFrequencies = unstretchedFrequencies.toDoubleArray(),
                stretchInCents = stretchInCents.toDoubleArray()
            ))
            name = null
            description = null
            unstretchedFrequencies.clear()
            stretchInCents.clear()
            readerState = ReaderState.SearchName
            lineConsumed = true
            // Log.v("Tuner", "StretchTuningIO.parseStretchTuningImpl: stretch tuning saved")
        }

        if (!lineConsumed && readerState == ReaderState.SearchVersion) {
            version = parseVersionLine(line)
            if (version != null) {
                readerState = ReaderState.SearchName
                lineConsumed = true
                // Log.v("Tuner", "StretchTuningIO.parseStretchTuningImpl: version found = $version")
            }
        }

        // we don't know if we really find a version string, so allow to also directly read a name even
        // if we didn't find a version yet
        if (!lineConsumed && (readerState == ReaderState.SearchVersion || readerState == ReaderState.SearchName)) {
            name = parseCommentLineString(line)
            if (name != null) {
                readerState = ReaderState.SearchDescription
                lineConsumed = true
                // Log.v("Tuner", "StretchTuningIO.parseStretchTuningImpl: name found = $name")
            }
        }

        if (!lineConsumed && readerState == ReaderState.SearchDescription) {
            description = parseCommentLineString(line)
            if (description != null) {
                readerState = ReaderState.SearchData
                lineConsumed = true
                // Log.v("Tuner", "StretchTuningIO.parseStretchTuningImpl: description found = $description")
            }
        }

        // skip any numbers of blank lines inbetween sections
        if (!lineConsumed && line.isBlank() && (readerState == ReaderState.SearchVersion || readerState == ReaderState.SearchName)) {
            lineConsumed = true
            // Log.v("Tuner", "StretchTuningIO.parseStretchTuningImpl: blank line skipped")
        }

        // try simple single line format (Title: note0 space-separated-cent-list,
        //    e.g.  Grand piano: A0 -3.4 -2.1 0.0 4.4  5)
        if (!lineConsumed && !isCommentLine(line) && unstretchedFrequencies.isEmpty()) {
            val stretchTuning = parseSingleLineSimpleFormat(line)
            if (stretchTuning != null) {
                readerState = ReaderState.SearchName
                name = null
                description = null
                lineConsumed = true
                collectedStretchTunings.add(stretchTuning)
                // Log.v("Tuner", "StretchTuningIO.parseStretchTuningImpl: simple format line found")
            }
        }

        if (!lineConsumed && !isCommentLine(line)) {
            fileCheckResult = FileCheck.Invalid
            // Log.v("Tuner", "StretchTuningIO.parseStretchTuningImpl: invalid line found $line")
            return@forEachLine
        }
    }

    if (fileCheckResult == FileCheck.Ok && unstretchedFrequencies.isNotEmpty()) {
        collectedStretchTunings.add(
            StretchTuning(
                name = GetTextFromString(name ?: ""),
                description = GetTextFromString(description ?: ""),
                unstretchedFrequencies = unstretchedFrequencies.toDoubleArray(),
                stretchInCents = stretchInCents.toDoubleArray()
            )
        )
    }

    if (fileCheckResult != FileCheck.Ok)
        collectedStretchTunings.clear()
    // Log.v("Tuner", "StretchTuningIO.parseStretchTuningImpl. fileCheckResult=$fileCheckResult")
    return StretchTuningIO.StretchTuningsAndFileCheckResult(
        fileCheckResult, collectedStretchTunings
    )
}
private fun parseSimpleNote(string: String): MusicalNote? {
    var noteString = string

    // first find base note
    val baseNote = BaseNote.entries.firstOrNull { noteString.startsWith(it.name) }
        ?: return null
    if (baseNote == BaseNote.None)
        return null

    // cut away string responsible for base note
    noteString = noteString.substring(baseNote.name.length)
//            Log.v("Tuner", "TemperamentIO.parseNote, step 1: $noteString, baseNote=$baseNote")
    // then find note modifier
    val noteModifier = when (noteString.firstOrNull()) {
        '#' -> {
            noteString = noteString.substring(1)
            NoteModifier.Sharp
        }
        'b' -> {
            noteString = noteString.substring(1)
            NoteModifier.Flat
        }
        else -> {
            NoteModifier.None
        }
    }
//            Log.v("Tuner", "TemperamentIO.parseNote, step 2: $noteString, noteModifier=$noteModifier")
    // find octave offset
    val octaveString = "^\\d+".toRegex().find(noteString)?.value
    val octave = octaveString?.toIntOrNull() ?: 0
    return MusicalNote(baseNote, noteModifier, octave)
}

private fun parseSingleLineSimpleFormat(line: String): StretchTuning? {
    val parts = line.split(":")
    if (parts.size != 2)
        return null
    val name = parts[0].trim()
    // Log.v("Tuner", "StretchTuningIO.parseSingleLineSimpleFormat: name = $name")
    val values = parts[1].trim().split("\\s+".toRegex())
    // Log.v("Tuner", "StretchTuningIO.parseSingleLineSimpleFormat: values.size = ${values.size}")
    if (values.size < 2)
        return null
    val startNote = parseSimpleNote(values[0])
    // Log.v("Tuner", "StretchTuningIO.parseSingleLineSimpleFormat: startNote = $startNote")

    if (startNote == null)
        return null

    val edo12 = predefinedTemperamentEDO(12, 0)
    val notes = edo12.noteNames(null)
    val startNoteResolved = notes.notes
        .firstOrNull { it.match(startNote, true) }
        ?.copy(octave = startNote.octave)
    if (startNoteResolved == null)
        return null

    val edo12Scale = MusicalScale2(
        edo12,
        _rootNote = null, _referenceNote = null,
        referenceFrequency = DefaultValues.REFERENCE_FREQUENCY,
        frequencyMin = DefaultValues.FREQUENCY_MIN,
        frequencyMax = DefaultValues.FREQUENCY_MAX,
        _stretchTuning = null
    )
    val index0 = edo12Scale.getNoteIndex2(startNoteResolved)
    // Log.v("Tuner", "StretchTuningIO.parseSingleLineSimpleFormat: index0 = $index0")
    if (index0 == Int.MAX_VALUE)
        return null

    val unstretchedFrequencies = DoubleArray(values.size - 1) // first value is base note
    val cents = DoubleArray(values.size - 1)

    values.asSequence().drop(1).forEachIndexed { index, string ->
        unstretchedFrequencies[index] =
            edo12Scale.getNoteFrequency(index0 + index).toDouble()
        val cent = string.replace(",", ".").toDoubleOrNull()
        // Log.v("Tuner", "StretchTuningIO.parseSingleLineSimpleFormat: cent=$cent")
        if (cent == null)
            return null
        else
            cents[index] = cent
    }
    val result = StretchTuning(
        name = GetTextFromString(name),
        description = GetTextFromString(""),
        unstretchedFrequencies,
        cents
    )
    // Log.v("Tuner", "StretchTuningIO.parseSingleLineSimpleFormat: isMonotonic = ${result.isMonotonic}")
    return if (result.isMonotonic)
        result
    else
        null
}
