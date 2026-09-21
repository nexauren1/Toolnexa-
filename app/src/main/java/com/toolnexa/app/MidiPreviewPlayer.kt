package com.toolnexa.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MidiPreviewPlayer {

    private var track:
        AudioTrack? = null

    private var worker:
        Thread? = null

    @Volatile
    private var running =
        false

    private val lock =
        Any()

    fun play(
        sourceNotes:
            List<AudioMidiPianoRollView.Note>,
        durationSeconds: Double,
        onStopped:
            (() -> Unit)? = null
    ) {
        stop()

        val notes =
            sourceNotes
                .map {
                    it.copy()
                }
                .filter {
                    it.durationSeconds >
                        0.03
                }
                .sortedBy {
                    it.startSeconds
                }

        if (
            notes.isEmpty() ||
            durationSeconds <= 0.0
        ) {
            onStopped?.invoke()
            return
        }

        val sampleRate =
            22050

        val minBuffer =
            AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

        if (
            minBuffer <= 0
        ) {
            onStopped?.invoke()
            return
        }

        val localTrack =
            try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(
                                AudioAttributes.USAGE_MEDIA
                            )
                            .setContentType(
                                AudioAttributes.CONTENT_TYPE_MUSIC
                            )
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(
                                sampleRate
                            )
                            .setEncoding(
                                AudioFormat.ENCODING_PCM_16BIT
                            )
                            .setChannelMask(
                                AudioFormat.CHANNEL_OUT_MONO
                            )
                            .build()
                    )
                    .setBufferSizeInBytes(
                        max(
                            minBuffer,
                            sampleRate / 2
                        )
                    )
                    .setTransferMode(
                        AudioTrack.MODE_STREAM
                    )
                    .build()
            } catch (_: Exception) {
                onStopped?.invoke()
                return
            }

        synchronized(lock) {
            track =
                localTrack
            running =
                true
        }

        worker =
            Thread {
                try {
                    localTrack.play()

                    val chunkFrames =
                        1024

                    val buffer =
                        ShortArray(
                            chunkFrames
                        )

                    val totalFrames =
                        (
                            durationSeconds *
                                sampleRate
                            ).toLong()

                    var framePosition =
                        0L

                    while (
                        isRunning(
                            localTrack
                        ) &&
                        framePosition <
                            totalFrames
                    ) {
                        val chunkStart =
                            framePosition.toDouble() /
                                sampleRate

                        val framesToWrite =
                            minOf(
                                chunkFrames,
                                (
                                    totalFrames -
                                        framePosition
                                    ).toInt()
                            )

                        val chunkEnd =
                            (
                                framePosition +
                                    framesToWrite
                                ).toDouble() /
                                sampleRate

                        val activeNotes =
                            notes.filter {
                                it.startSeconds <
                                    chunkEnd &&
                                    (
                                        it.startSeconds +
                                            it.durationSeconds
                                        ) >
                                        chunkStart
                            }

                        val activeGain =
                            0.19 /
                                max(
                                    1.0,
                                    sqrt(
                                        activeNotes.size
                                            .toDouble()
                                    )
                                )

                        for (
                            index
                            in 0 until
                                framesToWrite
                        ) {
                            val time =
                                (
                                    framePosition +
                                        index
                                    ).toDouble() /
                                    sampleRate

                            var sample =
                                0.0

                            for (
                                note
                                in activeNotes
                            ) {
                                val noteEnd =
                                    note.startSeconds +
                                        note.durationSeconds

                                if (
                                    time <
                                        note.startSeconds ||
                                    time >=
                                        noteEnd
                                ) {
                                    continue
                                }

                                val relative =
                                    time -
                                        note.startSeconds

                                val bend =
                                    bendAt(
                                        note,
                                        relative
                                    )

                                val frequency =
                                    midiToHz(
                                        note.pitch
                                    ) *
                                        2.0.pow(
                                            bend /
                                                12.0
                                        )

                                val phase =
                                    2.0 *
                                        PI *
                                        frequency *
                                        relative

                                val envelope =
                                    envelope(
                                        relative,
                                        note.durationSeconds
                                    )

                                val velocity =
                                    (
                                        note.velocity /
                                            127.0
                                        ).coerceIn(
                                            0.0,
                                            1.0
                                        )

                                val fundamental =
                                    sin(
                                        phase
                                    )

                                val harmonic2 =
                                    sin(
                                        phase * 2.0
                                    ) * 0.12

                                val harmonic3 =
                                    sin(
                                        phase * 3.0
                                    ) * 0.05

                                sample +=
                                    (
                                        fundamental +
                                            harmonic2 +
                                            harmonic3
                                        ) *
                                        velocity *
                                        envelope *
                                        activeGain
                            }

                            buffer[index] =
                                (
                                    sample.coerceIn(
                                        -0.8,
                                        0.8
                                    ) *
                                        Short.MAX_VALUE
                                    ).toInt()
                                        .toShort()
                        }

                        val written =
                            localTrack.write(
                                buffer,
                                0,
                                framesToWrite,
                                AudioTrack.WRITE_BLOCKING
                            )

                        if (
                            written <= 0
                        ) {
                            break
                        }

                        framePosition +=
                            written
                    }

                    try {
                        localTrack.stop()
                    } catch (_: Exception) {
                    }
                } catch (_: InterruptedException) {
                } catch (_: Exception) {
                } finally {
                    synchronized(lock) {
                        if (
                            track ===
                                localTrack
                        ) {
                            track =
                                null
                            running =
                                false
                        }
                    }

                    try {
                        localTrack.release()
                    } catch (_: Exception) {
                    }

                    synchronized(lock) {
                        if (
                            worker ===
                                Thread.currentThread()
                        ) {
                            worker =
                                null
                        }
                    }

                    onStopped?.invoke()
                }
            }

        worker?.start()
    }

    fun stop() {
        val localTrack:
            AudioTrack?

        synchronized(lock) {
            running =
                false
            localTrack =
                track
        }

        try {
            localTrack?.pause()
            localTrack?.flush()
        } catch (_: Exception) {
        }

        synchronized(lock) {
            worker?.interrupt()
        }
    }

    private fun isRunning(
        localTrack: AudioTrack
    ): Boolean {
        synchronized(lock) {
            return running &&
                track ===
                    localTrack
        }
    }

    private fun bendAt(
        note:
            AudioMidiPianoRollView.Note,
        relativeSeconds: Double
    ): Double {
        val bends =
            note.pitchBends

        if (
            bends.isEmpty()
        ) {
            return 0.0
        }

        val ratio =
            (
                relativeSeconds /
                    note.durationSeconds.coerceAtLeast(
                        0.001
                    )
                ).coerceIn(
                    0.0,
                    1.0
                )

        val index =
            (
                ratio *
                    (
                        bends.size -
                            1
                        )
                ).roundToInt()
                    .coerceIn(
                        0,
                        bends.lastIndex
                    )

        return bends[
            index
        ] /
            3.0
    }

    private fun envelope(
        time: Double,
        duration: Double
    ): Double {
        val attack =
            (
                time /
                    0.018
                ).coerceIn(
                    0.0,
                    1.0
                )

        val release =
            (
                (
                    duration -
                        time
                    ) /
                    0.06
                ).coerceIn(
                    0.0,
                    1.0
                )

        return minOf(
            attack,
            release,
            1.0
        ).coerceAtLeast(
            0.0
        )
    }

    private fun midiToHz(
        midi: Int
    ): Double {
        return 440.0 *
            2.0.pow(
                (
                    midi -
                        69
                    ) /
                    12.0
            )
    }
}
