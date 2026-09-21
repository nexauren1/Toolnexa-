package com.toolnexa.app

import android.media.MediaPlayer
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MidiPreviewPlayer(
    private val cacheDir: File
) {

    private var player:
        MediaPlayer? = null

    private var previewFile:
        File? = null

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

        val lastNoteEnd =
            notes.maxOfOrNull {
                it.startSeconds +
                    it.durationSeconds
            } ?: 0.0

        val duration =
            minOf(
                durationSeconds.coerceIn(
                    0.1,
                    300.0
                ),
                (lastNoteEnd + 0.35)
                    .coerceAtLeast(
                        0.5
                    )
            )

        synchronized(lock) {
            running =
                true
        }

        worker =
            Thread {
                var file:
                    File? = null

                try {
                    file =
                        createPreviewWav(
                            notes,
                            duration
                        )

                    synchronized(lock) {
                        if (!running) {
                            file.delete()
                            return@Thread
                        }

                        previewFile =
                            file
                    }

                    val localPlayer =
                        MediaPlayer().apply {
                            setDataSource(
                                file.absolutePath
                            )

                            setOnCompletionListener {
                                finishPlayback(
                                    this,
                                    file,
                                    onStopped
                                )
                            }

                            setOnErrorListener { mp, _, _ ->
                                finishPlayback(
                                    mp,
                                    file,
                                    onStopped
                                )
                                true
                            }

                            prepare()
                        }

                    synchronized(lock) {
                        if (!running) {
                            try {
                                localPlayer.release()
                            } catch (_: Exception) {
                            }
                            return@Thread
                        }

                        player =
                            localPlayer
                    }

                    localPlayer.start()
                } catch (
                    error: Exception
                ) {
                    synchronized(lock) {
                        running =
                            false
                    }

                    file?.delete()

                    onStopped?.invoke()
                }
            }

        worker?.start()
    }

    fun stop() {
        val localPlayer:
            MediaPlayer?
        val localFile:
            File?

        synchronized(lock) {
            running =
                false

            localPlayer =
                player

            localFile =
                previewFile

            player =
                null

            previewFile =
                null

            worker?.interrupt()
            worker =
                null
        }

        try {
            localPlayer?.stop()
        } catch (_: Exception) {
        }

        try {
            localPlayer?.release()
        } catch (_: Exception) {
        }

        localFile?.delete()
    }

    private fun finishPlayback(
        mediaPlayer:
            MediaPlayer,
        file: File?,
        onStopped:
            (() -> Unit)?
    ) {
        synchronized(lock) {
            if (
                player ===
                    mediaPlayer
            ) {
                player =
                    null

                running =
                    false

                previewFile =
                    null
            }
        }

        try {
            mediaPlayer.release()
        } catch (_: Exception) {
        }

        file?.delete()

        onStopped?.invoke()
    }

    private fun createPreviewWav(
        notes:
            List<AudioMidiPianoRollView.Note>,
        durationSeconds: Double
    ): File {
        val sampleRate =
            44100

        val totalFrames =
            (
                durationSeconds *
                    sampleRate
                ).toLong()
                .coerceAtLeast(
                    1L
                )

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val file =
            File.createTempFile(
                "toolnexa-midi-preview-",
                ".wav",
                cacheDir
            )

        BufferedOutputStream(
            FileOutputStream(
                file
            ),
            64 * 1024
        ).use { output ->
            writeWavHeader(
                output,
                sampleRate,
                totalFrames
            )

            val chunkFrames =
                4096

            val pcm =
                ByteBuffer
                    .allocate(
                        chunkFrames * 2
                    )
                    .order(
                        ByteOrder.LITTLE_ENDIAN
                    )

            var frame =
                0L

            while (
                frame <
                    totalFrames &&
                isRunning()
            ) {
                pcm.clear()

                val count =
                    minOf(
                        chunkFrames,
                        (
                            totalFrames -
                                frame
                            ).toInt()
                    )

                val chunkStart =
                    frame.toDouble() /
                        sampleRate

                val chunkEnd =
                    (
                        frame +
                            count
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
                    0.22 /
                        max(
                            1.0,
                            sqrt(
                                activeNotes.size
                                    .toDouble()
                            )
                        )

                for (
                    index
                    in 0 until count
                ) {
                    val time =
                        (
                            frame +
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

                        val frequency =
                            midiToHz(
                                note.pitch
                            ) *
                                2.0.pow(
                                    bendAt(
                                        note,
                                        relative
                                    ) /
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

                        sample +=
                            (
                                sin(
                                    phase
                                ) +
                                    sin(
                                        phase * 2.0
                                    ) * 0.12 +
                                    sin(
                                        phase * 3.0
                                    ) * 0.05
                                ) *
                                velocity *
                                envelope *
                                activeGain
                    }

                    val pcmSample =
                        (
                            sample.coerceIn(
                                -0.85,
                                0.85
                            ) *
                                Short.MAX_VALUE
                            ).toInt()
                                .toShort()

                    pcm.putShort(
                        pcmSample
                    )
                }

                output.write(
                    pcm.array(),
                    0,
                    count * 2
                )

                frame +=
                    count
            }
        }

        return file
    }

    private fun writeWavHeader(
        output:
            BufferedOutputStream,
        sampleRate: Int,
        frames: Long
    ) {
        val dataSize =
            frames * 2L

        val riffSize =
            36L + dataSize

        fun writeAscii(
            value: String
        ) {
            output.write(
                value.toByteArray(
                    Charsets.US_ASCII
                )
            )
        }

        fun writeInt32(
            value: Long
        ) {
            output.write(
                byteArrayOf(
                    (value and 0xFF).toByte(),
                    ((value shr 8) and 0xFF).toByte(),
                    ((value shr 16) and 0xFF).toByte(),
                    ((value shr 24) and 0xFF).toByte()
                )
            )
        }

        fun writeInt16(
            value: Int
        ) {
            output.write(
                byteArrayOf(
                    (value and 0xFF).toByte(),
                    ((value shr 8) and 0xFF).toByte()
                )
            )
        }

        writeAscii("RIFF")
        writeInt32(riffSize)
        writeAscii("WAVE")
        writeAscii("fmt ")
        writeInt32(16)
        writeInt16(1)
        writeInt16(1)
        writeInt32(sampleRate.toLong())
        writeInt32(
            sampleRate.toLong() * 2L
        )
        writeInt16(2)
        writeInt16(16)
        writeAscii("data")
        writeInt32(dataSize)
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
                ).toInt()
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

    private fun isRunning(): Boolean {
        synchronized(lock) {
            return running
        }
    }
}
