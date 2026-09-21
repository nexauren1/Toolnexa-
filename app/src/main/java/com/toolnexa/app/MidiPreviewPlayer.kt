package com.toolnexa.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.pow

class MidiPreviewPlayer {

    private var track: AudioTrack? = null
    private var worker: Thread? = null

    @Volatile
    private var running = false

    fun play(
        notes: List<AudioMidiPianoRollView.Note>,
        durationSeconds: Double,
        onStopped: (() -> Unit)? = null
    ) {
        stop()

        if (
            notes.isEmpty() ||
            durationSeconds <= 0.0
        ) {
            return
        }

        val sampleRate = 44100
        val minBuffer =
            AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

        if (minBuffer <= 0) {
            onStopped?.invoke()
            return
        }

        val localTrack =
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

        track =
            localTrack

        running = true

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

                    var framePosition =
                        0L

                    val totalFrames =
                        (
                            durationSeconds *
                                sampleRate
                            ).toLong()

                    while (
                        running &&
                        framePosition <
                            totalFrames
                    ) {
                        val framesToWrite =
                            minOf(
                                chunkFrames,
                                (
                                    totalFrames -
                                        framePosition
                                    ).toInt()
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
                                in notes
                            ) {
                                if (
                                    time <
                                        note.startSeconds ||
                                    time >=
                                        note.startSeconds +
                                            note.durationSeconds
                                ) {
                                    continue
                                }

                                val relative =
                                    (
                                        time -
                                            note.startSeconds
                                        )

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
                                            (
                                                bend /
                                                    12.0
                                            )
                                        )

                                val phase =
                                    (
                                        2.0 *
                                            PI *
                                            frequency *
                                            relative
                                        )

                                val amplitude =
                                    (
                                        note.velocity /
                                            127.0
                                        ) *
                                        envelope(
                                            relative,
                                            note.durationSeconds
                                        ) *
                                        0.18

                                sample +=
                                    sin(
                                        phase
                                    ) *
                                        amplitude
                            }

                            sample =
                                sample.coerceIn(
                                    -0.95,
                                    0.95
                                )

                            buffer[index] =
                                (
                                    sample *
                                        Short.MAX_VALUE
                                    ).toInt()
                                        .toShort()
                        }

                        localTrack.write(
                            buffer,
                            0,
                            framesToWrite
                        )

                        framePosition +=
                            framesToWrite
                    }

                    localTrack.stop()
                } catch (_: Exception) {
                } finally {
                    localTrack.release()

                    if (
                        track ===
                            localTrack
                    ) {
                        track = null
                    }

                    running = false

                    onStopped?.invoke()
                }
            }.also {
                it.start()
            }
    }

    fun stop() {
        running = false

        try {
            track?.pause()
            track?.flush()
            track?.stop()
        } catch (_: Exception) {
        }

        track?.release()
        track = null

        worker?.interrupt()
        worker = null
    }

    private fun bendAt(
        note: AudioMidiPianoRollView.Note,
        relativeSeconds: Double
    ): Double {
        val bends =
            note.pitchBends

        if (bends.isEmpty()) {
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
                ).toInt()
                    .coerceIn(
                        0,
                        bends.lastIndex
                    )

        return bends[index] /
            3.0
    }

    private fun envelope(
        time: Double,
        duration: Double
    ): Double {
        val attack =
            (
                time /
                    0.015
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
                    0.04
                ).coerceIn(
                    0.0,
                    1.0
                )

        return max(
            0.0,
            minOf(
                1.0,
                attack,
                release
            )
        )
    }

    private fun midiToHz(
        midi: Int
    ): Double {
        return 440.0 *
            2.0.pow(
                (
                    midi - 69
                ) /
                    12.0
            )
    }
}
