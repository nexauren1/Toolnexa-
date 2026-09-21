package com.toolnexa.app

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
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

    data class PreviewNote(
        val startSeconds: Double,
        val durationSeconds: Double,
        val pitch: Int,
        val velocity: Int,
        val pitchBends: List<Int> = emptyList()
    )

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    private var player:
        MediaPlayer? = null

    private var previewFile:
        File? = null

    private var worker:
        Thread? = null

    private var requestId =
        0L

    private val progressTick =
        object : Runnable {
            override fun run() {
                val current =
                    player

                if (
                    current != null &&
                    current.isPlaying
                ) {
                    onProgress?.invoke(
                        current.currentPosition,
                        current.duration
                    )

                    mainHandler.postDelayed(
                        this,
                        100L
                    )
                }
            }
        }

    private var onProgress:
        ((currentMs: Int, totalMs: Int) -> Unit)? =
        null

    private var onPrepared:
        ((totalMs: Int) -> Unit)? =
        null

    private var onStopped:
        (() -> Unit)? =
        null

    fun play(
        sourceNotes:
            List<PreviewNote>,
        durationSeconds: Double,
        onPrepared:
            ((totalMs: Int) -> Unit)? = null,
        onProgress:
            ((currentMs: Int, totalMs: Int) -> Unit)? = null,
        onStopped:
            (() -> Unit)? = null
    ) {
        stop(
            notify = false
        )

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

        val lastEnd =
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
                max(
                    0.5,
                    lastEnd + 0.35
                )
            )

        this.onPrepared =
            onPrepared
        this.onProgress =
            onProgress
        this.onStopped =
            onStopped

        val currentRequest =
            synchronized(this) {
                requestId += 1L
                requestId
            }

        worker =
            Thread {
                var file:
                    File? = null

                try {
                    file =
                        createPreviewWav(
                            notes,
                            duration,
                            currentRequest
                        )

                    if (
                        file == null ||
                        !isCurrent(
                            currentRequest
                        )
                    ) {
                        file?.delete()
                        return@Thread
                    }

                    mainHandler.post {
                        if (
                            !isCurrent(
                                currentRequest
                            )
                        ) {
                            file?.delete()
                            return@post
                        }

                        preparePlayerOnMain(
                            file,
                            currentRequest
                        )
                    }
                } catch (_: Exception) {
                    file?.delete()

                    mainHandler.post {
                        if (
                            isCurrent(
                                currentRequest
                            )
                        ) {
                            clearCallbacks()
                            onStopped?.invoke()
                        }
                    }
                }
            }

        worker?.start()
    }

    fun pause() {
        mainHandler.post {
            try {
                player?.pause()
                emitProgress()
            } catch (_: Exception) {
            }
        }
    }

    fun resume() {
        mainHandler.post {
            try {
                val current =
                    player

                if (
                    current != null
                ) {
                    current.start()
                    scheduleProgress()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun seekTo(
        positionMs: Int
    ) {
        mainHandler.post {
            try {
                val current =
                    player

                if (
                    current != null
                ) {
                    current.seekTo(
                        positionMs.coerceIn(
                            0,
                            current.duration
                        )
                    )
                    emitProgress()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun isPlaying(): Boolean {
        return try {
            player?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    fun stop(
        notify: Boolean = true
    ) {
        synchronized(this) {
            requestId += 1L
        }

        worker?.interrupt()
        worker =
            null

        mainHandler.removeCallbacks(
            progressTick
        )

        val current =
            player

        player =
            null

        val file =
            previewFile

        previewFile =
            null

        try {
            current?.stop()
        } catch (_: Exception) {
        }

        try {
            current?.release()
        } catch (_: Exception) {
        }

        file?.delete()

        if (notify) {
            clearCallbacks(
                notifyStopped = true
            )
        } else {
            clearCallbacks()
        }
    }

    fun release() {
        stop()
    }

    private fun preparePlayerOnMain(
        file: File,
        currentRequest: Long
    ) {
        try {
            val mediaPlayer =
                MediaPlayer()

            mediaPlayer.setDataSource(
                file.absolutePath
            )

            mediaPlayer.setOnPreparedListener {
                if (
                    !isCurrent(
                        currentRequest
                    )
                ) {
                    try {
                        it.release()
                    } catch (_: Exception) {
                    }
                    file.delete()
                    return@setOnPreparedListener
                }

                previewFile =
                    file

                player =
                    it

                onPrepared?.invoke(
                    it.duration
                )

                emitProgress()

                it.start()

                scheduleProgress()
            }

            mediaPlayer.setOnCompletionListener {
                if (
                    isCurrent(
                        currentRequest
                    )
                ) {
                    previewFile?.delete()
                    previewFile =
                        null

                    player =
                        null

                    try {
                        it.release()
                    } catch (_: Exception) {
                    }

                    clearCallbacks(
                        notifyStopped = true
                    )
                } else {
                    try {
                        it.release()
                    } catch (_: Exception) {
                    }
                }
            }

            mediaPlayer.setOnErrorListener { mp, _, _ ->
                if (
                    isCurrent(
                        currentRequest
                    )
                ) {
                    try {
                        mp.release()
                    } catch (_: Exception) {
                    }

                    file.delete()

                    player =
                        null

                    previewFile =
                        null

                    clearCallbacks(
                        notifyStopped = true
                    )
                } else {
                    try {
                        mp.release()
                    } catch (_: Exception) {
                    }
                }

                true
            }

            mediaPlayer.prepareAsync()
        } catch (_: Exception) {
            file.delete()
            clearCallbacks(
                notifyStopped = true
            )
        }
    }

    private fun scheduleProgress() {
        mainHandler.removeCallbacks(
            progressTick
        )
        mainHandler.post(
            progressTick
        )
    }

    private fun emitProgress() {
        val current =
            player ?: return

        try {
            onProgress?.invoke(
                current.currentPosition,
                current.duration
            )
        } catch (_: Exception) {
        }
    }

    private fun clearCallbacks(
        notifyStopped: Boolean = false
    ) {
        mainHandler.removeCallbacks(
            progressTick
        )

        val callback =
            onStopped

        onProgress =
            null
        onPrepared =
            null
        onStopped =
            null

        if (
            notifyStopped
        ) {
            callback?.invoke()
        }
    }

    private fun isCurrent(
        currentRequest: Long
    ): Boolean {
        return synchronized(this) {
            requestId ==
                currentRequest
        }
    }

    private fun createPreviewWav(
        notes:
            List<PreviewNote>,
        durationSeconds:
            Double,
        currentRequest:
            Long
    ): File? {
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

        if (
            !isCurrent(
                currentRequest
            )
        ) {
            return null
        }

        if (
            !cacheDir.exists()
        ) {
            cacheDir.mkdirs()
        }

        val file =
            File.createTempFile(
                "toolnexa-midi-preview-",
                ".wav",
                cacheDir
            )

        try {
            BufferedOutputStream(
                FileOutputStream(file),
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
                        totalFrames
                ) {
                    if (
                        !isCurrent(
                            currentRequest
                        )
                    ) {
                        return null
                    }

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
                            frame + count
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
                        0.28 /
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
                                        ) * 0.10 +
                                        sin(
                                            phase * 3.0
                                        ) * 0.035
                                    ) *
                                    velocity *
                                    envelope *
                                    activeGain
                        }

                        val pcmSample =
                            (
                                sample.coerceIn(
                                    -0.90,
                                    0.90
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
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    private fun bendAt(
        note:
            PreviewNote,
        relativeSeconds:
            Double
    ): Double {
        if (
            note.pitchBends.isEmpty()
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
                        note.pitchBends.size -
                            1
                        )
                ).toInt()
                .coerceIn(
                    0,
                    note.pitchBends.lastIndex
                )

        return note.pitchBends[index] /
            3.0
    }

    private fun envelope(
        time: Double,
        duration: Double
    ): Double {
        val attack =
            (
                time / 0.012
                ).coerceIn(
                    0.0,
                    1.0
                )

        val release =
            (
                (
                    duration -
                        time
                    ) / 0.08
                ).coerceIn(
                    0.0,
                    1.0
                )

        return minOf(
            attack,
            release
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

    private fun writeWavHeader(
        output:
            BufferedOutputStream,
        sampleRate:
            Int,
        frames:
            Long
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
                    (
                        value and
                            0xFF
                        ).toByte(),
                    (
                        value shr 8 and
                            0xFF
                        ).toByte(),
                    (
                        value shr 16 and
                            0xFF
                        ).toByte(),
                    (
                        value shr 24 and
                            0xFF
                        ).toByte()
                )
            )
        }

        fun writeInt16(
            value: Int
        ) {
            output.write(
                byteArrayOf(
                    (
                        value and
                            0xFF
                        ).toByte(),
                    (
                        value shr 8 and
                            0xFF
                        ).toByte()
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
        writeInt32(
            sampleRate.toLong()
        )
        writeInt32(
            sampleRate.toLong() *
                2L
        )
        writeInt16(2)
        writeInt16(16)
        writeAscii("data")
        writeInt32(dataSize)
    }
}
