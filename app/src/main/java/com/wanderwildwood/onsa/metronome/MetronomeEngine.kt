package com.wanderwildwood.onsa.metronome

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The clicks. One thread writes the sound into an [AudioTrack] ten milliseconds at a time, and
 * each click is placed on its exact sample, so the beat keeps time however busy the phone is;
 * a late thread only delays the whole stream, never one click against the next.
 *
 * A change of tempo or of beats takes effect from the next click. The first beat of a bar is
 * the same sound pitched up and louder; the ticks between beats are quieter.
 */
@Singleton
class MetronomeEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _playing = MutableStateFlow(false)
    val playing = _playing.asStateFlow()

    /**
     * The beat being heard, from 0, or [NO_BEAT]. It moves only when the beat itself is heard,
     * and only when beats are at least half a second apart (120 bpm or slower): the e-ink
     * panel is asked to redraw at most about twice a second. Faster than that it stays at
     * [TOO_QUICK] and the screen keeps still.
     */
    private val _beat = MutableStateFlow(NO_BEAT)
    val beat = _beat.asStateFlow()

    @Volatile private var settings = MetronomeSettings()
    @Volatile private var running = false
    private var thread: Thread? = null

    fun update(settings: MetronomeSettings) {
        this.settings = settings.valid()
    }

    @Synchronized
    fun start(settings: MetronomeSettings) {
        update(settings)
        if (thread?.isAlive == true) return
        running = true
        _playing.value = true
        thread = Thread(::play, "metronome").also { it.start() }
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.join(500)
        thread = null
        _playing.value = false
        _beat.value = NO_BEAT
    }

    private class Voice(val data: FloatArray, val step: Float, val gain: Float, var delay: Int) {
        var pos = 0.0
        val done get() = pos.toInt() + 1 >= data.size
    }

    private fun play() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val sounds = HashMap<ClickSound, FloatArray>()
        fun samples(sound: ClickSound) = sounds.getOrPut(sound) { readClick(context, sound.raw) }
        var track: AudioTrack? = null
        try {
            val minBytes = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBytes, RATE / 10 * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            samples(settings.sound)
            track.play()

            val out = FloatArray(CHUNK)
            val voices = ArrayList<Voice>()
            val heard = ArrayDeque<Pair<Long, Int>>() // (frame, beat) still to be heard
            var written = 0L
            var nextClick = 0.0
            var tick = 0
            var beatInBar = 0

            while (running) {
                val s = settings
                val chunkEnd = written + CHUNK
                while (nextClick < chunkEnd) {
                    beatInBar %= s.beatsPerBar
                    val onBeat = tick == 0
                    val accent = onBeat && beatInBar == 0 && s.accentFirstBeat && s.beatsPerBar > 1
                    val offset = (nextClick - written).toInt().coerceIn(0, CHUNK - 1)
                    voices += Voice(
                        data = samples(s.sound),
                        step = if (accent) ACCENT_PITCH else 1f,
                        gain = when {
                            accent -> 1f
                            onBeat -> BEAT_GAIN
                            else -> TICK_GAIN
                        },
                        delay = offset
                    )
                    if (onBeat) heard.addLast(nextClick.toLong() to beatInBar)
                    tick++
                    if (tick >= s.subdivision.perBeat) {
                        tick = 0
                        beatInBar = (beatInBar + 1) % s.beatsPerBar
                    }
                    nextClick += RATE * 60.0 / s.bpm / s.subdivision.perBeat
                }

                out.fill(0f)
                val iter = voices.iterator()
                while (iter.hasNext()) {
                    val v = iter.next()
                    var i = v.delay
                    while (i < CHUNK && !v.done) {
                        val k = v.pos.toInt()
                        val f = (v.pos - k).toFloat()
                        out[i] += v.gain * (v.data[k] * (1f - f) + v.data[k + 1] * f)
                        v.pos += v.step
                        i++
                    }
                    v.delay = 0
                    if (v.done) iter.remove()
                }
                for (i in 0 until CHUNK) out[i] = out[i].coerceIn(-1f, 1f)

                track.write(out, 0, CHUNK, AudioTrack.WRITE_BLOCKING)
                written = chunkEnd

                val head = track.playbackHeadPosition.toLong() and 0xffffffffL
                var latest = -1
                while (heard.isNotEmpty() && heard.first().first <= head) latest = heard.removeFirst().second
                if (latest >= 0) {
                    _beat.value = if (60.0 / s.bpm >= MIN_SECONDS_PER_REDRAW) latest else TOO_QUICK
                }
            }
        } catch (e: Exception) {
            Log.w("Metronome", "playback stopped", e)
        } finally {
            try {
                track?.stop()
            } catch (_: IllegalStateException) {
            }
            track?.release()
            running = false
            _playing.value = false
            _beat.value = NO_BEAT
        }
    }

    companion object {
        const val NO_BEAT = -1
        const val TOO_QUICK = -2
        private const val RATE = 48000
        private const val CHUNK = RATE / 100
        private const val ACCENT_PITCH = 1.5f
        private const val BEAT_GAIN = 0.7f
        private const val TICK_GAIN = 0.35f
        private const val MIN_SECONDS_PER_REDRAW = 0.5
    }
}

/** Tempo from taps: the average of the last few gaps, starting over after a two-second pause. */
class TapTempo {
    private val taps = ArrayDeque<Long>()

    /** Records a tap at [nowMillis]; the tempo once there are two taps, else null. */
    fun tap(nowMillis: Long): Int? {
        if (taps.isNotEmpty() && nowMillis - taps.last() > 2000) taps.clear()
        taps.addLast(nowMillis)
        while (taps.size > 5) taps.removeFirst()
        if (taps.size < 2) return null
        val gap = (taps.last() - taps.first()).toDouble() / (taps.size - 1)
        if (gap <= 0) return null
        return (60000.0 / gap).toInt()
            .coerceIn(MetronomeSettings.MIN_BPM, MetronomeSettings.MAX_BPM)
    }
}
