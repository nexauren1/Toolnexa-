package com.toolnexa.app

import android.content.Context
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

class BasicPitchNativeEngine(
    context: Context,
    private val minMidi: Int,
    private val maxMidi: Int,
    private val onsetThreshold: Float,
    private val frameThreshold: Float,
    private val minNoteLengthFrames: Int,
    private val maxSeconds: Int,
    private val onProgress: (Int, String) -> Unit,
    private val onSuccess:
        (List<NeuralNote>, Double) -> Unit,
    private val onError: (String) -> Unit
) {

    data class NeuralNote(
        val startSeconds: Double,
        val durationSeconds: Double,
        val pitch: Int,
        val amplitude: Double,
        val pitchBends: List<Int>
    )

    private val appContext =
        context.applicationContext

    private val executor =
        Executors.newSingleThreadExecutor()

    @Volatile
    private var cancelled = false

    fun start(
        samples: FloatArray,
        sampleRate: Int,
        durationSeconds: Double
    ) {
        executor.execute {
            try {
                val clippedDuration =
                    durationSeconds
                        .coerceIn(
                            0.1,
                            maxSeconds.toDouble()
                        )

                val limitedSamples =
                    samples.copyOf(
                        min(
                            samples.size,
                            max(
                                1,
                                (
                                    sampleRate *
                                        clippedDuration
                                    ).roundToInt()
                                )
                            )
                        )

                onProgress(
                    2,
                    "A preparar o áudio para 22,05 kHz..."
                )

                val audio =
                    resample(
                        limitedSamples,
                        sampleRate,
                        TARGET_SAMPLE_RATE
                    )

                if (audio.isEmpty()) {
                    throw IllegalStateException(
                        "O áudio não contém dados suficientes."
                    )
                }

                val model =
                    loadModel()

                try {
                    transcribe(
                        model,
                        audio,
                        clippedDuration
                    )
                } finally {
                    model.close()
                }
            } catch (error: Exception) {
                if (!cancelled) {
                    onError(
                        error.message
                            ?: "O motor neural nativo falhou."
                    )
                }
            }
        }
    }

    fun close() {
        cancelled = true
        executor.shutdownNow()
    }

    private fun loadModel(): InterpreterApi {
        val modelFile =
            BasicPitchModelManager.modelFile(
                appContext
            )

        if (
            !BasicPitchModelManager.isInstalled(
                appContext
            ) ||
            !modelFile.exists()
        ) {
            throw IllegalStateException(
                "O modelo de IA ainda não foi instalado."
            )
        }

        ensureTfLiteRuntime()

        val options =
            InterpreterApi.Options()
                .setRuntime(
                    InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY
                )
                .apply {
                    setNumThreads(
                        max(
                            2,
                            min(
                                4,
                                Runtime.getRuntime()
                                    .availableProcessors()
                            )
                        )
                    )
                }

        return InterpreterApi.create(
            modelFile,
            options
        )
    }

    private fun ensureTfLiteRuntime() {
        val moduleInstallClient =
            ModuleInstall.getClient(
                appContext
            )

        val tfLiteApi =
            TfLite.getClient(
                appContext
            )

        val availability =
            Tasks.await(
                moduleInstallClient
                    .areModulesAvailable(
                        tfLiteApi
                    )
            )

        if (
            !availability
                .areModulesAvailable()
        ) {
            Tasks.await(
                moduleInstallClient
                    .installModules(
                        ModuleInstallRequest
                            .newBuilder()
                            .addApi(
                                tfLiteApi
                            )
                            .build()
                    )
            )
        }

        Tasks.await(
            TfLite.initialize(
                appContext
            )
        )
    }

    private fun transcribe(
        interpreter: InterpreterApi,
        audio: FloatArray,
        durationSeconds: Double
    ) {
        val inputTensor =
            interpreter.getInputTensor(0)

        if (
            inputTensor.dataType() !=
                DataType.FLOAT32
        ) {
            throw IllegalStateException(
                "O modelo Basic Pitch não usa entrada FLOAT32."
            )
        }

        val inputShape =
            inputTensor.shape()

        val expectedSamples =
            inputShape.lastIndex.let {
                if (
                    inputShape.size >= 2
                ) {
                    inputShape[
                        inputShape.size - 2
                    ]
                } else {
                    AUDIO_N_SAMPLES
                }
            }

        val windowSamples =
            if (
                expectedSamples > 0
            ) {
                expectedSamples
            } else {
                AUDIO_N_SAMPLES
            }

        val overlapSamples =
            OVERLAPPING_FRAMES *
                FFT_HOP

        val hopSamples =
            max(
                FFT_HOP,
                windowSamples -
                    overlapSamples
            )

        val padded =
            FloatArray(
                audio.size +
                    overlapSamples / 2
            )

        System.arraycopy(
            audio,
            0,
            padded,
            overlapSamples / 2,
            min(
                audio.size,
                padded.size -
                    overlapSamples / 2
            )
        )

        val windows =
            max(
                1,
                ceilDiv(
                    padded.size,
                    hopSamples
                )
            )

        val noteRows =
            FloatCollector()

        val onsetRows =
            FloatCollector()

        val contourRows =
            FloatCollector()

        var noteFreqs = 88
        var contourFreqs = 264
        var framesPerWindow = 172

        for (
            windowIndex
            in 0 until windows
        ) {
            if (cancelled) {
                return
            }

            val start =
                windowIndex *
                    hopSamples

            val input =
                ByteBuffer
                    .allocateDirect(
                        windowSamples * 4
                    )
                    .order(
                        ByteOrder.nativeOrder()
                    )

            val window =
                FloatArray(
                    windowSamples
                )

            val copyLength =
                min(
                    windowSamples,
                    max(
                        0,
                        padded.size -
                            start
                    )
                )

            if (copyLength > 0) {
                System.arraycopy(
                    padded,
                    start,
                    window,
                    0,
                    copyLength
                )
            }

            window.forEach {
                input.putFloat(it)
            }

            input.rewind()

            val outputs =
                LinkedHashMap<Int, ByteBuffer>()

            for (
                index
                in 0 until
                    interpreter.outputTensorCount
            ) {
                val tensor =
                    interpreter.getOutputTensor(
                        index
                    )

                if (
                    tensor.dataType() !=
                        DataType.FLOAT32
                ) {
                    throw IllegalStateException(
                        "O modelo devolveu um tensor que não é FLOAT32."
                    )
                }

                outputs[index] =
                    ByteBuffer
                        .allocateDirect(
                            tensor.numElements() * 4
                        )
                        .order(
                            ByteOrder.nativeOrder()
                        )

                val shape =
                    tensor.shape()

                if (
                    shape.size >= 2 &&
                    shape[
                        shape.size - 1
                    ] > 0
                ) {
                    val freq =
                        shape.last()

                    val frames =
                        shape[
                            shape.size - 2
                        ]

                    if (freq == 88) {
                        framesPerWindow =
                            frames
                    }

                    if (freq == 88) {
                        noteFreqs = 88
                    }

                    if (freq == 264) {
                        contourFreqs = 264
                    }
                }
            }

            interpreter.runForMultipleInputsOutputs(
                arrayOf(input),
                outputs.mapValues {
                    it.value
                }
            )

            for (
                index
                in 0 until
                    interpreter.outputTensorCount
            ) {
                val tensor =
                    interpreter.getOutputTensor(
                        index
                    )

                val shape =
                    tensor.shape()

                if (shape.size < 2) {
                    continue
                }

                val frames =
                    shape[
                        shape.size - 2
                    ]

                val freqs =
                    shape.last()

                val values =
                    readFloats(
                        outputs[index]!!
                    )

                when (
                    roleForTensor(
                        tensor.name(),
                        index,
                        freqs
                    )
                ) {
                    Role.CONTOUR -> {
                        contourRows.addMatrix(
                            values,
                            frames,
                            freqs,
                            trimOverlap = true
                        )
                    }

                    Role.NOTE -> {
                        noteRows.addMatrix(
                            values,
                            frames,
                            freqs,
                            trimOverlap = true
                        )
                    }

                    Role.ONSET -> {
                        onsetRows.addMatrix(
                            values,
                            frames,
                            freqs,
                            trimOverlap = true
                        )
                    }

                    null -> Unit
                }
            }

            val progress =
                (
                    8f +
                        (
                            (
                                windowIndex + 1
                            ).toFloat() /
                                windows.toFloat()
                            ) * 62f
                ).roundToInt()

            onProgress(
                progress.coerceIn(
                    8,
                    70
                ),
                "IA nativa a analisar janela " +
                    (windowIndex + 1) +
                    "/" +
                    windows +
                    "..."
            )
        }

        if (
            noteRows.size == 0 ||
            onsetRows.size == 0
        ) {
            throw IllegalStateException(
                "O modelo Basic Pitch não devolveu as matrizes de notas esperadas."
            )
        }

        onProgress(
            74,
            "A reconstruir a linha temporal..."
        )

        val frameCount =
            min(
                noteRows.rows,
                onsetRows.rows
            )

        val notes =
            decodeNotes(
                noteRows.toFloatArray(),
                onsetRows.toFloatArray(),
                frameCount,
                noteRows.columns,
                onsetThreshold,
                frameThreshold,
                minNoteLengthFrames
            )

        onProgress(
            88,
            "A calcular pitch bends..."
        )

        val contours =
            if (
                contourRows.rows > 0
            ) {
                contourRows.toFloatArray()
            } else {
                FloatArray(0)
            }

        val finalNotes =
            notes.map { note ->
                val bends =
                    if (
                        contours.isNotEmpty()
                    ) {
                        calculatePitchBends(
                            contours,
                            contourRows.rows,
                            contourRows.columns,
                            note.startFrame,
                            note.endFrame,
                            note.pitch
                        )
                    } else {
                        emptyList()
                    }

                NeuralNote(
                    startSeconds =
                        frameToTime(
                            note.startFrame
                        ).coerceIn(
                            0.0,
                            durationSeconds
                        ),
                    durationSeconds =
                        (
                            frameToTime(
                                note.endFrame
                            ) -
                                frameToTime(
                                    note.startFrame
                                )
                            ).coerceAtLeast(
                                0.03
                            ).coerceAtMost(
                                durationSeconds
                            ),
                    pitch =
                        note.pitch.coerceIn(
                            minMidi,
                            maxMidi
                        ),
                    amplitude =
                        note.amplitude.coerceIn(
                            0.0,
                            1.0
                        ),
                    pitchBends =
                        simplifyBends(
                            bends
                        )
                )
            }.filter {
                it.startSeconds <
                    durationSeconds &&
                    it.durationSeconds >
                        0.03 &&
                    it.pitch in
                        minMidi..maxMidi
            }

        if (finalNotes.isEmpty()) {
            throw IllegalStateException(
                "A IA não encontrou notas musicais claras."
            )
        }

        onProgress(
            100,
            "Transcrição neural nativa concluída."
        )

        onSuccess(
            finalNotes,
            durationSeconds
        )
    }

    private fun roleForTensor(
        name: String,
        index: Int,
        frequencyCount: Int
    ): Role? {
        val normalized =
            name.lowercase()

        if (
            frequencyCount == 264
        ) {
            return Role.CONTOUR
        }

        if (
            frequencyCount != 88
        ) {
            return null
        }

        return when {
            normalized.contains(
                "statefulpartitionedcall:2"
            ) ||
                normalized.contains(
                    "note"
                ) ||
                normalized.contains(
                    "identity_2"
                ) -> {
                Role.NOTE
            }

            normalized.contains(
                "statefulpartitionedcall:1"
            ) ||
                normalized.contains(
                    "onset"
                ) ||
                normalized.contains(
                    "identity_1"
                ) -> {
                Role.ONSET
            }

            index == 0 -> {
                Role.NOTE
            }

            index == 1 -> {
                Role.ONSET
            }

            else -> null
        }
    }

    private fun decodeNotes(
        frames: FloatArray,
        onsets: FloatArray,
        frameCount: Int,
        frequencyCount: Int,
        onsetThreshold: Float,
        frameThreshold: Float,
        minNoteLength: Int
    ): List<DecodedNote> {
        val safeFrames =
            frames.copyOf(
                frameCount *
                    frequencyCount
            )

        val safeOnsets =
            onsets.copyOf(
                frameCount *
                    frequencyCount
            )

        constrainFrequency(
            safeFrames,
            safeOnsets,
            frameCount,
            frequencyCount
        )

        inferOnsets(
            safeOnsets,
            safeFrames,
            frameCount,
            frequencyCount
        )

        val remaining =
            safeFrames.copyOf()

        val candidates =
            ArrayList<OnsetCandidate>()

        for (
            frequency
            in 0 until frequencyCount
        ) {
            for (
                frame
                in 1 until
                    frameCount - 1
            ) {
                val value =
                    safeOnsets[
                        frame *
                            frequencyCount +
                            frequency
                    ]

                if (
                    value >= onsetThreshold &&
                    value >
                        safeOnsets[
                            (frame - 1) *
                                frequencyCount +
                                frequency
                        ] &&
                    value >
                        safeOnsets[
                            (frame + 1) *
                                frequencyCount +
                                frequency
                        ]
                ) {
                    candidates.add(
                        OnsetCandidate(
                            frame,
                            frequency
                        )
                    )
                }
            }
        }

        candidates.sortWith(
            compareByDescending<
                OnsetCandidate
            > {
                safeOnsets[
                    it.frame *
                        frequencyCount +
                        it.frequency
                ]
            }
        )

        val result =
            ArrayList<DecodedNote>()

        for (
            candidate
            in candidates
        ) {
            val start =
                candidate.frame

            if (
                start >=
                    frameCount - 1
            ) {
                continue
            }

            val frequency =
                candidate.frequency

            var end =
                start + 1

            var below =
                0

            while (
                end <
                    frameCount - 1 &&
                below <
                    ENERGY_TOLERANCE
            ) {
                val value =
                    remaining[
                        end *
                            frequencyCount +
                            frequency
                    ]

                if (
                    value <
                        frameThreshold
                ) {
                    below++
                } else {
                    below = 0
                }

                end++
            }

            end -= below

            if (
                end - start <=
                    minNoteLength
            ) {
                continue
            }

            clearBand(
                remaining,
                frameCount,
                frequencyCount,
                start,
                end,
                frequency
            )

            val amplitude =
                mean(
                    safeFrames,
                    frameCount,
                    frequencyCount,
                    start,
                    end,
                    frequency
                )

            result.add(
                DecodedNote(
                    start,
                    end,
                    frequency +
                        MIDI_OFFSET,
                    amplitude
                )
            )
        }

        while (
            maxValue(remaining) >
                frameThreshold
        ) {
            if (result.size > 5000) {
                break
            }

            val peak =
                maxIndex(
                    remaining
                )

            val mid =
                peak / frequencyCount

            val frequency =
                peak %
                    frequencyCount

            remaining[peak] = 0f

            var start =
                mid

            var end =
                mid + 1

            var below =
                0

            while (
                end <
                    frameCount - 1 &&
                below <
                    ENERGY_TOLERANCE
            ) {
                val value =
                    remaining[
                        end *
                            frequencyCount +
                            frequency
                    ]

                if (
                    value <
                        frameThreshold
                ) {
                    below++
                } else {
                    below = 0
                }

                clearBand(
                    remaining,
                    frameCount,
                    frequencyCount,
                    end,
                    end + 1,
                    frequency
                )

                end++
            }

            end -= 1 +
                below

            below = 0

            start = mid - 1

            while (
                start > 0 &&
                below <
                    ENERGY_TOLERANCE
            ) {
                val value =
                    remaining[
                        start *
                            frequencyCount +
                            frequency
                    ]

                if (
                    value <
                        frameThreshold
                ) {
                    below++
                } else {
                    below = 0
                }

                clearBand(
                    remaining,
                    frameCount,
                    frequencyCount,
                    start,
                    start + 1,
                    frequency
                )

                start--
            }

            start += 1 +
                below

            if (
                end - start >
                    minNoteLength
            ) {
                result.add(
                    DecodedNote(
                        start,
                        end,
                        frequency +
                            MIDI_OFFSET,
                        mean(
                            safeFrames,
                            frameCount,
                            frequencyCount,
                            start,
                            end,
                            frequency
                        )
                    )
                )
            }
        }

        return result.sortedBy {
            it.startFrame
        }
    }

    private fun constrainFrequency(
        frames: FloatArray,
        onsets: FloatArray,
        frameCount: Int,
        frequencyCount: Int
    ) {
        val minIndex =
            (
                minMidi -
                    MIDI_OFFSET
                ).coerceIn(
                    0,
                    frequencyCount
                )

        val maxIndex =
            (
                maxMidi -
                    MIDI_OFFSET
                    + 1
                ).coerceIn(
                    0,
                    frequencyCount
                )

        for (
            frame
            in 0 until frameCount
        ) {
            val base =
                frame *
                    frequencyCount

            for (
                frequency
                in 0 until
                    frequencyCount
            ) {
                if (
                    frequency <
                        minIndex ||
                    frequency >=
                        maxIndex
                ) {
                    frames[
                        base +
                            frequency
                    ] = 0f

                    onsets[
                        base +
                            frequency
                    ] = 0f
                }
            }
        }
    }

    private fun inferOnsets(
        onsets: FloatArray,
        frames: FloatArray,
        frameCount: Int,
        frequencyCount: Int
    ) {
        var maxDiff =
            0f

        val diff =
            FloatArray(
                onsets.size
            )

        for (
            frame
            in 2 until frameCount
        ) {
            for (
                frequency
                in 0 until
                    frequencyCount
            ) {
                var best =
                    0f

                for (
                    delta
                    in 1..2
                ) {
                    val now =
                        frames[
                            frame *
                                frequencyCount +
                                frequency
                        ]

                    val previousFrame =
                        max(
                            0,
                            frame - delta
                        )

                    val before =
                        frames[
                            previousFrame *
                                frequencyCount +
                                frequency
                        ]

                    best =
                        max(
                            best,
                            now - before
                        )
                }

                diff[
                    frame *
                        frequencyCount +
                        frequency
                ] = max(
                    0f,
                    best
                )

                maxDiff =
                    max(
                        maxDiff,
                        diff[
                            frame *
                                frequencyCount +
                                frequency
                        ]
                    )
            }
        }

        if (
            maxDiff <= 0f
        ) {
            return
        }

        var maxOnset =
            0f

        for (value in onsets) {
            maxOnset =
                max(
                    maxOnset,
                    value
                )
        }

        if (
            maxOnset <= 0f
        ) {
            return
        }

        val scale =
            maxOnset /
                maxDiff

        for (
            index
            in onsets.indices
        ) {
            onsets[index] =
                max(
                    onsets[index],
                    diff[index] *
                        scale
                )
        }
    }

    private fun calculatePitchBends(
        contours: FloatArray,
        contourRows: Int,
        contourColumns: Int,
        startFrame: Int,
        endFrame: Int,
        midiPitch: Int
    ): List<Int> {
        if (
            contourRows <= 0 ||
            contourColumns <= 0 ||
            startFrame >=
                contourRows
        ) {
            return emptyList()
        }

        val safeEnd =
            min(
                endFrame,
                contourRows
            )

        val center =
            (
                12f *
                    3f *
                    kotlin.math.log2(
                        midiToHz(
                            midiPitch
                        ) /
                            27.5f
                    )
                ).roundToInt()

        val tolerance =
            25

        val result =
            ArrayList<Int>(
                max(
                    1,
                    safeEnd -
                        startFrame
                )
            )

        for (
            frame
            in startFrame until
                safeEnd
        ) {
            var bestScore =
                Float.NEGATIVE_INFINITY

            var bestOffset =
                0

            val from =
                max(
                    0,
                    center -
                        tolerance
                )

            val to =
                min(
                    contourColumns - 1,
                    center +
                        tolerance
                )

            for (
                bin
                in from..to
            ) {
                val distance =
                    bin -
                        center

                val gaussian =
                    exp(
                        -0.5 *
                            (
                                distance /
                                    5f
                            ).pow(2)
                    )

                val score =
                    contours[
                        frame *
                            contourColumns +
                            bin
                    ] *
                        gaussian.toFloat()

                if (
                    score >
                        bestScore
                ) {
                    bestScore = score
                    bestOffset = distance
                }
            }

            result.add(
                bestOffset
            )
        }

        return result
    }

    private fun simplifyBends(
        bends: List<Int>
    ): List<Int> {
        if (
            bends.size <=
                MAX_BEND_POINTS
        ) {
            return bends
        }

        val result =
            ArrayList<Int>(
                MAX_BEND_POINTS
            )

        for (
            i in 0 until
                MAX_BEND_POINTS
        ) {
            val sourceIndex =
                (
                    (
                        i *
                            (bends.size - 1)
                        ).toDouble() /
                            (MAX_BEND_POINTS - 1)
                    ).roundToInt()

            result.add(
                bends[
                    sourceIndex.coerceIn(
                        0,
                        bends.lastIndex
                    )
                ]
            )
        }

        return result
    }

    private fun frameToTime(
        frame: Int
    ): Double {
        val original =
            frame *
                FFT_HOP.toDouble() /
                TARGET_SAMPLE_RATE

        val windowNumber =
            floor(
                frame.toDouble() /
                    ANNOTATION_FRAMES
            )

        val windowOffset =
            (
                FFT_HOP.toDouble() /
                    TARGET_SAMPLE_RATE
                ) *
                (
                    ANNOTATION_FRAMES -
                        (
                            AUDIO_N_SAMPLES.toDouble() /
                                FFT_HOP.toDouble()
                            )
                    ) +
                0.0018

        return (
            original -
                windowOffset *
                windowNumber
            ).coerceAtLeast(
                0.0
            )
    }

    private fun resample(
        input: FloatArray,
        sourceRate: Int,
        targetRate: Int
    ): FloatArray {
        if (
            sourceRate <= 0 ||
            sourceRate ==
                targetRate
        ) {
            return input
        }

        val outputLength =
            max(
                1,
                (
                    input.size.toDouble() *
                        targetRate.toDouble() /
                        sourceRate.toDouble()
                ).roundToInt()
            )

        val output =
            FloatArray(
                outputLength
            )

        val ratio =
            sourceRate.toDouble() /
                targetRate.toDouble()

        for (
            index
            in output.indices
        ) {
            val sourcePosition =
                index *
                    ratio

            val left =
                floor(
                    sourcePosition
                ).toInt()
                    .coerceIn(
                        0,
                        input.lastIndex
                    )

            val right =
                min(
                    input.lastIndex,
                    left + 1
                )

            val fraction =
                sourcePosition -
                    floor(
                        sourcePosition
                    )

            output[index] =
                (
                    input[left] +
                        (
                            input[right] -
                                input[left]
                            ) *
                            fraction.toFloat()
                    )
        }

        return output
    }

    private fun readFloats(
        buffer: ByteBuffer
    ): FloatArray {
        buffer.rewind()

        val count =
            buffer.remaining() / 4

        val result =
            FloatArray(
                count
            )

        for (
            index
            in result.indices
        ) {
            result[index] =
                buffer.getFloat()
        }

        return result
    }

    private fun clearBand(
        values: FloatArray,
        frameCount: Int,
        frequencyCount: Int,
        start: Int,
        end: Int,
        frequency: Int
    ) {
        val fromFrame =
            start.coerceAtLeast(0)

        val toFrame =
            end.coerceAtMost(
                frameCount
            )

        for (
            frame
            in fromFrame until
                toFrame
        ) {
            val base =
                frame *
                    frequencyCount

            values[
                base +
                    frequency
            ] = 0f

            if (
                frequency >
                    0
            ) {
                values[
                    base +
                        frequency -
                        1
                ] = 0f
            }

            if (
                frequency <
                    frequencyCount -
                        1
            ) {
                values[
                    base +
                        frequency +
                        1
                ] = 0f
            }
        }
    }

    private fun mean(
        values: FloatArray,
        frameCount: Int,
        frequencyCount: Int,
        start: Int,
        end: Int,
        frequency: Int
    ): Double {
        val safeStart =
            start.coerceAtLeast(0)

        val safeEnd =
            end.coerceAtMost(
                frameCount
            )

        if (
            safeEnd <=
                safeStart
        ) {
            return 0.0
        }

        var sum =
            0.0

        var count =
            0

        for (
            frame
            in safeStart until
                safeEnd
        ) {
            sum +=
                values[
                    frame *
                        frequencyCount +
                        frequency
                ]

            count++
        }

        return if (
            count > 0
        ) {
            sum /
                count.toDouble()
        } else {
            0.0
        }
    }

    private fun maxValue(
        values: FloatArray
    ): Float {
        var result =
            0f

        for (value in values) {
            result =
                max(
                    result,
                    value
                )
        }

        return result
    }

    private fun maxIndex(
        values: FloatArray
    ): Int {
        var best =
            0

        var bestValue =
            Float.NEGATIVE_INFINITY

        for (
            index
            in values.indices
        ) {
            if (
                values[index] >
                    bestValue
            ) {
                bestValue =
                    values[index]
                best = index
            }
        }

        return best
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

    private fun ceilDiv(
        value: Int,
        divisor: Int
    ): Int {
        return (
            value +
                divisor -
                1
            ) /
            divisor
    }

    private enum class Role {
        NOTE,
        ONSET,
        CONTOUR
    }

    private data class OnsetCandidate(
        val frame: Int,
        val frequency: Int
    )

    private data class DecodedNote(
        val startFrame: Int,
        val endFrame: Int,
        val pitch: Int,
        val amplitude: Double
    )

    private class FloatCollector {
        private var data =
            FloatArray(1024)

        private var count =
            0

        var rows =
            0
            private set

        var columns =
            0
            private set

        val size: Int
            get() = count

        fun addMatrix(
            values: FloatArray,
            rowsPerMatrix: Int,
            columnsPerMatrix: Int,
            trimOverlap: Boolean
        ) {
            if (
                columns == 0
            ) {
                columns =
                    columnsPerMatrix
            }

            val trim =
                if (
                    trimOverlap &&
                    rowsPerMatrix >
                        OVERLAPPING_FRAMES
                ) {
                    OVERLAPPING_FRAMES / 2
                } else {
                    0
                }

            val start =
                trim *
                    columnsPerMatrix

            val end =
                (
                    rowsPerMatrix -
                        trim
                    ) *
                    columnsPerMatrix

            ensure(
                max(
                    0,
                    end -
                        start
                )
            )

            for (
                index
                in start until
                    end
            ) {
                data[count++] =
                    values[index]
            }

            rows +=
                max(
                    0,
                    rowsPerMatrix -
                        trim * 2
                )
        }

        fun toFloatArray():
            FloatArray {
            return data.copyOf(
                count
            )
        }

        private fun ensure(
            extra: Int
        ) {
            val required =
                count +
                    extra

            if (
                required <=
                    data.size
            ) {
                return
            }

            var size =
                data.size

            while (
                size < required
            ) {
                size *= 2
            }

            data =
                data.copyOf(
                    size
                )
        }
    }

    private companion object {
        const val TARGET_SAMPLE_RATE =
            22050

        const val FFT_HOP =
            256

        const val AUDIO_N_SAMPLES =
            TARGET_SAMPLE_RATE * 2 -
                FFT_HOP

        const val OVERLAPPING_FRAMES =
            30

        const val ANNOTATION_FRAMES =
            TARGET_SAMPLE_RATE / FFT_HOP * 2

        const val MIDI_OFFSET =
            21

        const val ENERGY_TOLERANCE =
            11

        const val MAX_BEND_POINTS =
            32
    }
}
