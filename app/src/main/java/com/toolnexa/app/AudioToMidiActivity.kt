package com.toolnexa.app

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class AudioToMidiActivity : Activity() {

    private val analytics by lazy { AnalyticsTracker(this) }

    private val executor =
        Executors.newSingleThreadExecutor()

    private var selectedUri: Uri? = null
    private var selectedName = "audio"
    private var resultFile: File? = null

    private var sensitivity = 50
    private var progressText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var convertButton: Button? = null

    private val blue by lazy {
        getColor(R.color.toolnexa_blue)
    }

    private val bg by lazy {
        getColor(R.color.toolnexa_bg)
    }

    private val surface by lazy {
        getColor(R.color.toolnexa_surface)
    }

    private val textColor by lazy {
        getColor(R.color.toolnexa_text)
    }

    private val muted by lazy {
        getColor(R.color.toolnexa_muted)
    }

    private val border by lazy {
        getColor(R.color.toolnexa_border)
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        LanguageManager.apply(this)
        analytics.screen("audio_to_midi")
        showStage1()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun showStage1() {
        buildBase(
            "Audio → MIDI"
        )

        addTitle("Audio → MIDI")
        addText(
            "Converta uma melodia gravada ou um instrumento " +
                "em notas MIDI editáveis."
        )

        add(
            infoCard(
                "PROCESSAMENTO LOCAL",
                "O áudio é descodificado no próprio aparelho. " +
                    "Não enviamos o ficheiro para um servidor."
            )
        )

        val input = card()
        input.addView(
            title(
                "Começar com um áudio",
                18f
            )
        )

        input.addView(
            bodyText(
                "MP3, WAV, M4A, OGG e outros formatos que " +
                    "o Android consiga descodificar."
            )
        )

        val choose =
            button(
                "Escolher áudio",
                true
            )

        choose.setOnClickListener {
            analytics.event(
                "audio_to_midi_picker"
            )

            startActivityForResult(
                Intent(
                    Intent.ACTION_OPEN_DOCUMENT
                ).apply {
                    type = "audio/*"
                    addCategory(
                        Intent.CATEGORY_OPENABLE
                    )
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    )
                },
                REQUEST_PICK
            )
        }

        input.addView(choose)
        add(input)

        add(
            infoCard(
                "NOTAS IMPORTANTES",
                "A primeira versão é otimizada para uma voz ou " +
                    "um instrumento por vez. Áudio com vários " +
                    "instrumentos pode precisar de edição no MIDI."
            )
        )

        root()?.post {
            if (!isFinishing) {
                I18n.localizeWindow(this)
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode != REQUEST_PICK ||
            resultCode != RESULT_OK ||
            data?.data == null
        ) {
            return
        }

        selectedUri = data.data

        try {
            contentResolver.takePersistableUriPermission(
                selectedUri!!,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        selectedName =
            queryDisplayName(
                contentResolver,
                selectedUri!!
            ) ?: "audio"

        analytics.event(
            "audio_to_midi_selected",
            "mime" to
                (
                    contentResolver.getType(
                        selectedUri!!
                    ) ?: ""
                )
        )

        showStage2()
    }

    private fun showStage2() {
        buildBase(
            "Configuração"
        )

        addTitle(
            "Preparar conversão"
        )

        addText(
            "Ajuste a sensibilidade. Valores mais altos " +
                "ignoram trechos muito baixos."
        )

        val fileCard = card()

        fileCard.addView(
            title(
                selectedName,
                17f
            )
        )

        fileCard.addView(
            bodyText(
                "Modo: Melody MIDI • deteção de uma linha musical"
            )
        )

        add(fileCard)

        val sensitivityCard = card()

        sensitivityCard.addView(
            title(
                "Sensibilidade: $sensitivity%",
                17f
            )
        )

        val seek =
            SeekBar(this).apply {
                max = 100
                progress = sensitivity
                setOnSeekBarChangeListener(
                    object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            sensitivity =
                                progress.coerceIn(
                                    20,
                                    80
                                )
                            (
                                sensitivityCard
                                    .getChildAt(0)
                                    as? TextView
                                )?.text =
                                "Sensibilidade: " +
                                    sensitivity +
                                    "%"
                        }

                        override fun onStartTrackingTouch(
                            seekBar: SeekBar?
                        ) = Unit

                        override fun onStopTrackingTouch(
                            seekBar: SeekBar?
                        ) = Unit
                    }
                )
            }

        sensitivityCard.addView(
            seek,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        sensitivityCard.addView(
            bodyText(
                "Recomendado: 40–60%. Use valores maiores " +
                    "em gravações com ruído baixo."
            )
        )

        add(sensitivityCard)

        val convert =
            button(
                "Converter para MIDI",
                true
            )

        convertButton = convert

        convert.setOnClickListener {
            startConversion()
        }

        add(convert)

        add(
            infoCard(
                "RESULTADO",
                "O ToolNexa vai criar um ficheiro .mid padrão, " +
                    "compatível com DAWs e editores MIDI."
            )
        )

        root()?.post {
            if (!isFinishing) {
                I18n.localizeWindow(this)
            }
        }
    }

    private fun startConversion() {
        val uri = selectedUri ?: return

        convertButton?.isEnabled = false

        buildBase(
            "A converter"
        )

        addTitle(
            "A converter áudio"
        )

        addText(
            "A analisar o áudio e a criar as notas MIDI. " +
                "Não feche esta tela durante o processo."
        )

        progressBar =
            ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                max = 100
                progress = 0
            }

        add(
            progressBar!!,
            dp(16)
        )

        progressText =
            bodyText(
                "A preparar o áudio..."
            )

        add(
            progressText!!
        )

        add(
            infoCard(
                "PROCESSAMENTO",
                "Tudo está a ser executado em segundo plano " +
                    "para manter a interface responsiva."
            )
        )

        root()?.post {
            if (!isFinishing) {
                I18n.localizeWindow(this)
            }
        }

        val selectedSensitivity =
            sensitivity

        executor.execute {
            try {
                val decoded =
                    AudioDecoder.decode(
                        this,
                        uri,
                        MAX_SECONDS
                    ) { progress ->
                        runOnUiThread {
                            if (!isFinishing) {
                                progressBar?.progress =
                                    progress

                                progressText?.text =
                                    "A analisar áudio... " +
                                        progress +
                                        "%"
                            }
                        }
                    }

                val threshold =
                    PitchTranscriber
                        .thresholdForSensitivity(
                            selectedSensitivity
                        )

                val result =
                    PitchTranscriber.transcribe(
                        decoded.samples,
                        decoded.sampleRate,
                        threshold
                    ) { progress ->
                        runOnUiThread {
                            if (!isFinishing) {
                                progressBar?.progress =
                                    progress

                                progressText?.text =
                                    "A detetar notas... " +
                                        progress +
                                        "%"
                            }
                        }
                    }

                if (result.notes.isEmpty()) {
                    throw IllegalStateException(
                        "Não foram encontradas notas musicais claras. " +
                            "Tente uma gravação mais limpa ou aumente a sensibilidade."
                    )
                }

                val file =
                    File(
                        cacheDir,
                        "toolnexa-audio-midi-" +
                            System.currentTimeMillis() +
                            ".mid"
                    )

                MidiFileWriter.write(
                    file,
                    result.notes
                )

                resultFile = file

                runOnUiThread {
                    if (!isFinishing) {
                        showResult(
                            file,
                            result,
                            decoded.durationSeconds
                        )
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (!isFinishing) {
                        Toast.makeText(
                            this,
                            error.message
                                ?: "Não foi possível converter o áudio.",
                            Toast.LENGTH_LONG
                        ).show()

                        showStage2()
                    }
                }
            }
        }
    }

    private fun showResult(
        file: File,
        result: TranscriptionResult,
        durationSeconds: Double
    ) {
        buildBase(
            "Resultado"
        )

        addTitle(
            "MIDI pronto"
        )

        addText(
            "A conversão terminou. O ficheiro está pronto para " +
                "abrir, partilhar ou guardar."
        )

        val resultCard = card()

        resultCard.addView(
            title(
                file.name,
                17f
            )
        )

        resultCard.addView(
            bodyText(
                "Notas detetadas: " +
                    result.notes.size +
                    " • Duração: " +
                    formatDuration(
                        durationSeconds
                    )
            )
        )

        resultCard.addView(
            bodyText(
                "Tempo MIDI: 120 BPM • Canal 1"
            )
        )

        add(resultCard)

        val open =
            button(
                "Abrir / partilhar MIDI",
                true
            )

        open.setOnClickListener {
            shareMidi(file)
        }

        add(open)

        val save =
            button(
                "Guardar no armazenamento",
                false
            )

        save.setOnClickListener {
            saveMidi(file)
        }

        add(save)

        val again =
            button(
                "Converter outro áudio",
                false
            )

        again.setOnClickListener {
            selectedUri = null
            resultFile = null
            showStage1()
        }

        add(again)

        add(
            infoCard(
                "SOBRE O MOTOR",
                "A primeira versão usa análise de espectro e " +
                    "deteção de frequência para criar notas MIDI. " +
                    "Para maior precisão polifónica, uma futura versão " +
                    "poderá usar um modelo dedicado de transcrição musical."
            )
        )

        root()?.post {
            if (!isFinishing) {
                I18n.localizeWindow(this)
            }
        }
    }

    private fun shareMidi(
        file: File
    ) {
        try {
            val uri =
                FileProvider.getUriForFile(
                    this,
                    "com.toolnexa.app.fileprovider",
                    file
                )

            startActivity(
                Intent.createChooser(
                    Intent(
                        Intent.ACTION_SEND
                    ).apply {
                        type = "audio/midi"
                        putExtra(
                            Intent.EXTRA_STREAM,
                            uri
                        )
                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    },
                    "Partilhar MIDI"
                )
            )

            analytics.event(
                "audio_to_midi_share"
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Não foi possível abrir o partilhador.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveMidi(
        file: File
    ) {
        try {
            val directory =
                File(
                    filesDir,
                    "midi-results"
                )

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val target =
                File(
                    directory,
                    safeFileName(
                        selectedName
                            .substringBeforeLast(
                                "."
                            )
                    ) +
                        ".mid"
                )

            file.copyTo(
                target,
                overwrite = true
            )

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "com.toolnexa.app.fileprovider",
                    target
                )

            NexaurenHistory.add(
                this,
                "Audio to MIDI",
                uri,
                target.name,
                target.length()
            )

            Toast.makeText(
                this,
                "MIDI guardado com sucesso.",
                Toast.LENGTH_SHORT
            ).show()

            analytics.event(
                "audio_to_midi_saved",
                "notes" to
                    "saved"
            )
        } catch (error: Exception) {
            Toast.makeText(
                this,
                error.message
                    ?: "Não foi possível guardar o MIDI.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun buildBase(
        subtitle: String
    ) {
        val scroll =
            ScrollView(this).apply {
                setBackgroundColor(bg)
            }

        val body =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(16),
                    dp(18),
                    dp(16),
                    dp(30)
                )
            }

        val toolbar =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val back =
            Button(this).apply {
                text = "‹"
                textSize = 28f
                setTextColor(blue)
                backgroundColorTransparent()
                setOnClickListener {
                    finish()
                }
            }

        toolbar.addView(
            back,
            LinearLayout.LayoutParams(
                dp(52),
                dp(52)
            )
        )

        toolbar.addView(
            TextView(this).apply {
                text = subtitle
                textSize = 18f
                setTextColor(textColor)
                typeface =
                    android.graphics.Typeface
                        .DEFAULT_BOLD
                gravity =
                    Gravity.CENTER_VERTICAL
            },
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            )
        )

        body.addView(
            toolbar
        )

        scroll.addView(
            body
        )

        setContentView(scroll)

        this.body = body
    }

    private var body: LinearLayout? = null

    private fun root(): ViewGroup? {
        return body
    }

    private fun addTitle(
        text: String
    ) {
        body?.addView(
            title(
                text,
                28f
            )
        )
    }

    private fun addText(
        text: String
    ) {
        body?.addView(
            bodyText(text).apply {
                setPadding(
                    0,
                    dp(4),
                    0,
                    dp(14)
                )
            }
        )
    }

    private fun add(
        view: View
    ) {
        body?.addView(
            view,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin =
                    dp(12)
            }
        )
    }

    private fun add(
        view: View,
        heightDp: Int
    ) {
        body?.addView(
            view,
            LinearLayout.LayoutParams(
                -1,
                dp(heightDp)
            ).apply {
                bottomMargin =
                    dp(14)
            }
        )
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
            )
            background =
                android.graphics.drawable.GradientDrawable()
                    .apply {
                        setColor(surface)
                        setStroke(
                            dp(1),
                            border
                        )
                        cornerRadius =
                            dp(18).toFloat()
                    }
        }
    }

    private fun infoCard(
        heading: String,
        text: String
    ): LinearLayout {
        val box = card()

        box.addView(
            TextView(this).apply {
                this.text = heading
                textSize = 12.5f
                setTextColor(blue)
                typeface =
                    android.graphics.Typeface
                        .DEFAULT_BOLD
            }
        )

        box.addView(
            bodyText(text).apply {
                setPadding(
                    0,
                    dp(6),
                    0,
                    0
                )
            }
        )

        return box
    }

    private fun title(
        text: String,
        size: Float
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(textColor)
            typeface =
                android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    private fun bodyText(
        text: String
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(muted)
            setLineSpacing(
                0f,
                1.15f
            )
        }
    }

    private fun button(
        text: String,
        primary: Boolean
    ): Button {
        return Button(this).apply {
            this.text = text
            textSize = 14.5f
            minHeight = dp(50)
            isAllCaps = false
            stateListAnimator = null

            if (primary) {
                setTextColor(
                    android.graphics.Color.WHITE
                )
                background =
                    android.graphics.drawable.GradientDrawable()
                        .apply {
                            setColor(blue)
                            cornerRadius =
                                dp(14).toFloat()
                        }
            } else {
                setTextColor(blue)
                background =
                    android.graphics.drawable.GradientDrawable()
                        .apply {
                            setColor(
                                android.graphics.Color.TRANSPARENT
                            )
                            setStroke(
                                dp(1),
                                blue
                            )
                            cornerRadius =
                                dp(14).toFloat()
                        }
            }
        }
    }

    private fun dp(
        value: Int
    ): Int {
        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }

    private fun formatDuration(
        seconds: Double
    ): String {
        val total =
            seconds
                .roundToInt()
                .coerceAtLeast(0)

        val minutes =
            total / 60

        val remainder =
            total % 60

        return String.format(
            Locale.US,
            "%d:%02d",
            minutes,
            remainder
        )
    }

    private fun queryDisplayName(
        resolver: ContentResolver,
        uri: Uri
    ): String? {
        return try {
            resolver.query(
                uri,
                arrayOf(
                    OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun safeFileName(
        value: String
    ): String {
        return value
            .replace(
                Regex("[^a-zA-Z0-9._-]"),
                "_"
            )
            .trim('_')
            .ifBlank {
                "audio_to_midi"
            }
            .take(80)
    }

    private fun Button.backgroundColorTransparent() {
        background =
            android.graphics.drawable.ColorDrawable(
                android.graphics.Color.TRANSPARENT
            )
    }

    private data class DecodedAudio(
        val samples: FloatArray,
        val sampleRate: Int,
        val durationSeconds: Double
    )

    private data class NoteEvent(
        val startSeconds: Double,
        val durationSeconds: Double,
        val pitch: Int,
        val velocity: Int
    )

    private data class TranscriptionResult(
        val notes: List<NoteEvent>
    )

    private object AudioDecoder {

        fun decode(
            context: Context,
            uri: Uri,
            maxSeconds: Int,
            onProgress: (Int) -> Unit
        ): DecodedAudio {
            val extractor =
                MediaExtractor()

            var codec: MediaCodec? = null

            try {
                extractor.setDataSource(
                    context,
                    uri,
                    null
                )

                var audioTrack = -1

                for (
                    index in
                    0 until extractor.trackCount
                ) {
                    val format =
                        extractor.getTrackFormat(
                            index
                        )

                    val mime =
                        format.getString(
                            MediaFormat.KEY_MIME
                        ) ?: continue

                    if (mime.startsWith("audio/")) {
                        audioTrack = index
                        break
                    }
                }

                if (audioTrack < 0) {
                    throw IllegalArgumentException(
                        "O ficheiro não contém uma faixa de áudio compatível."
                    )
                }

                extractor.selectTrack(
                    audioTrack
                )

                val format =
                    extractor.getTrackFormat(
                        audioTrack
                    )

                val mime =
                    format.getString(
                        MediaFormat.KEY_MIME
                    )
                        ?: throw IllegalArgumentException(
                            "Formato de áudio inválido."
                        )

                codec =
                    MediaCodec.createDecoderByType(
                        mime
                    )

                codec.configure(
                    format,
                    null,
                    null,
                    0
                )

                codec.start()

                var sampleRate =
                    format.getInteger(
                        MediaFormat.KEY_SAMPLE_RATE,
                        44100
                    )

                var channels =
                    format.getInteger(
                        MediaFormat.KEY_CHANNEL_COUNT,
                        1
                    )

                var pcmEncoding =
                    format.getInteger(
                        MediaFormat.KEY_PCM_ENCODING,
                        android.media.AudioFormat.ENCODING_PCM_16BIT
                    )

                val maxSamples =
                    max(
                        1,
                        sampleRate *
                            maxSeconds
                    )

                val collector =
                    FloatCollector(
                        min(
                            maxSamples,
                            44100 *
                                maxSeconds
                        )
                    )

                val bufferInfo =
                    MediaCodec.BufferInfo()

                var inputEos = false
                var outputEos = false
                var lastProgress = -1

                while (!outputEos) {
                    if (!inputEos) {
                        val inputIndex =
                            codec.dequeueInputBuffer(
                                TIMEOUT_US
                            )

                        if (inputIndex >= 0) {
                            val input =
                                codec.getInputBuffer(
                                    inputIndex
                                )

                            val size =
                                if (input != null) {
                                    input.clear()

                                    val read =
                                        extractor.readSampleData(
                                            input,
                                            0
                                        )

                                    if (
                                        read >= 0
                                    ) {
                                        codec.queueInputBuffer(
                                            inputIndex,
                                            0,
                                            read,
                                            max(
                                                0L,
                                                extractor.sampleTime
                                            ),
                                            0
                                        )

                                        extractor.advance()
                                    }

                                    read
                                } else {
                                    -1
                                }

                            if (size < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )

                                inputEos = true
                            }
                        }
                    }

                    val outputIndex =
                        codec.dequeueOutputBuffer(
                            bufferInfo,
                            TIMEOUT_US
                        )

                    when {
                        outputIndex ==
                            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outputFormat =
                                codec.outputFormat

                            sampleRate =
                                outputFormat.getInteger(
                                    MediaFormat.KEY_SAMPLE_RATE,
                                    sampleRate
                                )

                            channels =
                                outputFormat.getInteger(
                                    MediaFormat.KEY_CHANNEL_COUNT,
                                    channels
                                )

                            pcmEncoding =
                                outputFormat.getInteger(
                                    MediaFormat.KEY_PCM_ENCODING,
                                    pcmEncoding
                                )
                        }

                        outputIndex >= 0 -> {
                            val output =
                                codec.getOutputBuffer(
                                    outputIndex
                                )

                            if (
                                output != null &&
                                bufferInfo.size > 0
                            ) {
                                val duplicate =
                                    output.duplicate()
                                        .order(
                                            ByteOrder.nativeOrder()
                                        )

                                duplicate.position(
                                    bufferInfo.offset
                                )

                                duplicate.limit(
                                    bufferInfo.offset +
                                        bufferInfo.size
                                )

                                appendPcm(
                                    duplicate,
                                    pcmEncoding,
                                    channels,
                                    collector,
                                    maxSamples
                                )
                            }

                            codec.releaseOutputBuffer(
                                outputIndex,
                                false
                            )

                            val duration =
                                max(
                                    1L,
                                    format.getLong(
                                        MediaFormat.KEY_DURATION,
                                        1L
                                    )
                                )

                            val currentPts =
                                max(
                                    0L,
                                    bufferInfo.presentationTimeUs
                                )

                            val progress =
                                (
                                    (
                                        currentPts
                                            .toDouble() /
                                            duration
                                    ) *
                                        100.0
                                    )
                                    .roundToInt()
                                    .coerceIn(
                                        0,
                                        100
                                    )

                            if (
                                progress !=
                                    lastProgress
                            ) {
                                lastProgress =
                                    progress
                                onProgress(
                                    progress
                                )
                            }

                            if (
                                bufferInfo.flags and
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM !=
                                    0
                            ) {
                                outputEos = true
                            }
                        }
                    }

                    if (
                        collector.size >=
                            maxSamples
                    ) {
                        break
                    }
                }

                codec.stop()

                if (collector.size < 1024) {
                    throw IllegalArgumentException(
                        "O áudio é demasiado curto ou não pôde ser descodificado."
                    )
                }

                val original =
                    collector.toArray()

                val targetRate =
                    22050

                val samples =
                    if (
                        sampleRate ==
                            targetRate
                    ) {
                        original
                    } else {
                        resample(
                            original,
                            sampleRate,
                            targetRate
                        )
                    }

                val durationSeconds =
                    samples.size.toDouble() /
                        targetRate.toDouble()

                if (
                    durationSeconds <
                        MIN_SECONDS
                ) {
                    throw IllegalArgumentException(
                        "Use um áudio com pelo menos " +
                            MIN_SECONDS +
                            " segundos."
                    )
                }

                onProgress(100)

                return DecodedAudio(
                    samples,
                    targetRate,
                    durationSeconds
                )
            } catch (error: Exception) {
                throw error
            } finally {
                try {
                    codec?.release()
                } catch (_: Exception) {
                }

                try {
                    extractor.release()
                } catch (_: Exception) {
                }
            }
        }

        private fun appendPcm(
            buffer: ByteBuffer,
            encoding: Int,
            channels: Int,
            collector: FloatCollector,
            maxSamples: Int
        ) {
            val channelCount =
                channels.coerceAtLeast(1)

            when (encoding) {
                android.media.AudioFormat.ENCODING_PCM_FLOAT -> {
                    val floats =
                        buffer
                            .order(
                                ByteOrder.nativeOrder()
                            )
                            .asFloatBuffer()

                    val temp =
                        FloatArray(
                            min(
                                floats.remaining(),
                                channelCount *
                                    4096
                            )
                        )

                    while (
                        floats.hasRemaining() &&
                        collector.size <
                            maxSamples
                    ) {
                        val count =
                            min(
                                floats.remaining(),
                                temp.size
                            )

                        floats.get(
                            temp,
                            0,
                            count
                        )

                        var i = 0

                        while (
                            i + channelCount <=
                                count
                        ) {
                            var sum = 0f

                            for (
                                ch in
                                0 until
                                channelCount
                            ) {
                                sum +=
                                    temp[
                                        i + ch
                                    ]
                            }

                            collector.add(
                                (
                                    sum /
                                        channelCount
                                )
                                    .coerceIn(
                                        -1f,
                                        1f
                                    )
                            )

                            i +=
                                channelCount
                        }
                    }
                }

                else -> {
                    val shorts =
                        buffer
                            .order(
                                ByteOrder.LITTLE_ENDIAN
                            )
                            .asShortBuffer()

                    val temp =
                        ShortArray(
                            min(
                                shorts.remaining(),
                                channelCount *
                                    4096
                            )
                        )

                    while (
                        shorts.hasRemaining() &&
                        collector.size <
                            maxSamples
                    ) {
                        val count =
                            min(
                                shorts.remaining(),
                                temp.size
                            )

                        shorts.get(
                            temp,
                            0,
                            count
                        )

                        var i = 0

                        while (
                            i + channelCount <=
                                count
                        ) {
                            var sum = 0

                            for (
                                ch in
                                0 until
                                channelCount
                            ) {
                                sum +=
                                    temp[
                                        i + ch
                                    ].toInt()
                            }

                            collector.add(
                                (
                                    sum.toFloat() /
                                        channelCount.toFloat()
                                ) /
                                    32768f
                            )

                            i +=
                                channelCount
                        }
                    }
                }
            }
        }

        private fun resample(
            source: FloatArray,
            sourceRate: Int,
            targetRate: Int
        ): FloatArray {
            if (
                sourceRate <= 0 ||
                sourceRate == targetRate
            ) {
                return source
            }

            val outputSize =
                (
                    source.size.toDouble() *
                        targetRate.toDouble() /
                        sourceRate.toDouble()
                    )
                    .roundToInt()
                    .coerceAtLeast(1)

            val result =
                FloatArray(
                    outputSize
                )

            val ratio =
                sourceRate.toDouble() /
                    targetRate.toDouble()

            for (i in result.indices) {
                val position =
                    i *
                        ratio

                val left =
                    position
                        .toInt()
                        .coerceIn(
                            0,
                            source.lastIndex
                        )

                val right =
                    (
                        left + 1
                    ).coerceAtMost(
                        source.lastIndex
                    )

                val fraction =
                    position -
                        left

                result[i] =
                    (
                        source[left] *
                            (1.0 - fraction) +
                            source[right] *
                                fraction
                        )
                        .toFloat()
            }

            return result
        }
    }

    private object PitchTranscriber {

        private const val FFT_SIZE = 2048
        private const val HOP = 1024
        private const val MIN_MIDI = 36
        private const val MAX_MIDI = 96

        fun thresholdForSensitivity(
            sensitivity: Int
        ): Float {
            val normalized =
                sensitivity
                    .coerceIn(20, 80)
                    .toFloat()

            return (
                0.018f -
                    (
                        normalized - 20f
                    ) *
                    0.00020f
                )
                    .coerceIn(
                        0.006f,
                        0.018f
                    )
        }

        fun transcribe(
            samples: FloatArray,
            sampleRate: Int,
            rmsThreshold: Float,
            onProgress: (Int) -> Unit
        ): TranscriptionResult {
            val analyzer =
                SpectrumAnalyzer(
                    FFT_SIZE
                )

            val raw =
                mutableListOf<Int>()

            val rmsValues =
                mutableListOf<Float>()

            val totalFrames =
                if (
                    samples.size <=
                        FFT_SIZE
                ) {
                    1
                } else {
                    1 +
                        (
                            samples.size -
                                FFT_SIZE
                        ) /
                            HOP
                }

            var lastProgress = -1

            var frameIndex = 0

            while (
                frameIndex <
                    totalFrames
            ) {
                val offset =
                    frameIndex *
                        HOP

                val rms =
                    calculateRms(
                        samples,
                        offset,
                        FFT_SIZE
                    )

                rmsValues.add(
                    rms
                )

                val pitch =
                    if (
                        rms < rmsThreshold
                    ) {
                        -1
                    } else {
                        analyzer.detectMidiPitch(
                            samples,
                            offset,
                            sampleRate
                        )
                    }

                raw.add(pitch)

                val progress =
                    (
                        frameIndex.toDouble() /
                            max(
                                1,
                                totalFrames - 1
                            )
                        * 100.0
                    )
                    .roundToInt()
                    .coerceIn(0, 100)

                if (
                    progress !=
                        lastProgress
                ) {
                    lastProgress =
                        progress
                    onProgress(
                        progress
                    )
                }

                frameIndex++
            }

            val smooth =
                smoothPitches(
                    raw
                )

            val notes =
                buildNotes(
                    smooth,
                    rmsValues,
                    HOP,
                    sampleRate
                )

            return TranscriptionResult(
                notes
            )
        }

        private fun calculateRms(
            samples: FloatArray,
            offset: Int,
            size: Int
        ): Float {
            var sum = 0.0

            val end =
                min(
                    samples.size,
                    offset + size
                )

            if (
                offset >= end
            ) {
                return 0f
            }

            for (
                i in
                offset until end
            ) {
                val sample =
                    samples[i]
                        .toDouble()

                sum +=
                    sample *
                        sample
            }

            return sqrt(
                sum /
                    (end - offset).toDouble()
            )
                .toFloat()
        }

        private fun smoothPitches(
            input: List<Int>
        ): List<Int> {
            if (input.size < 3) {
                return input
            }

            val result =
                MutableList(
                    input.size
                ) {
                    input[it]
                }

            for (
                i in
                1 until
                input.lastIndex
            ) {
                val current =
                    input[i]

                val left =
                    input[i - 1]

                val right =
                    input[i + 1]

                if (
                    current < 0 &&
                    left >= 0 &&
                    right == left
                ) {
                    result[i] =
                        left
                } else if (
                    current >= 0 &&
                    left == right &&
                    abs(
                        current - left
                    ) > 1
                ) {
                    result[i] =
                        left
                }
            }

            return result
        }

        private fun buildNotes(
            pitches: List<Int>,
            rms: List<Float>,
            hop: Int,
            sampleRate: Int
        ): List<NoteEvent> {
            val notes =
                mutableListOf<NoteEvent>()

            var currentPitch = -1
            var startFrame = -1
            var lastActiveFrame = -1

            fun closeNote(
                endFrame: Int
            ) {
                if (
                    startFrame < 0 ||
                    currentPitch < 0
                ) {
                    return
                }

                val startSeconds =
                    (
                        startFrame *
                            hop
                    ).toDouble() /
                        sampleRate

                val endSeconds =
                    (
                        (
                            endFrame + 1
                        ) *
                            hop
                    ).toDouble() /
                        sampleRate

                val duration =
                    max(
                        0.0,
                        endSeconds -
                            startSeconds
                    )

                if (
                    duration < 0.055
                ) {
                    return
                }

                var energySum =
                    0.0

                var energyCount = 0

                val endIndex =
                    min(
                        endFrame,
                        rms.lastIndex
                    )

                for (
                    index in
                    startFrame..endIndex
                ) {
                    energySum +=
                        rms[index]
                            .toDouble()

                    energyCount++
                }

                val averageRms =
                    if (
                        energyCount > 0
                    ) {
                        energySum /
                            energyCount
                    } else {
                        0.05
                    }

                val velocity =
                    (
                        38 +
                            (
                                (
                                    averageRms
                                        .coerceAtMost(
                                            0.45
                                        ) /
                                        0.45
                                ) *
                                    88
                                )
                    )
                        .roundToInt()
                        .coerceIn(
                            35,
                            118
                        )

                notes.add(
                    NoteEvent(
                        startSeconds,
                        duration,
                        currentPitch
                            .coerceIn(
                                0,
                                127
                            ),
                        velocity
                    )
                )
            }

            for (
                i in
                pitches.indices
            ) {
                val pitch =
                    pitches[i]

                if (
                    pitch < 0
                ) {
                    if (
                        currentPitch >= 0 &&
                        lastActiveFrame >=
                            0 &&
                        i - lastActiveFrame >
                            2
                    ) {
                        closeNote(
                            lastActiveFrame
                        )

                        currentPitch =
                            -1

                        startFrame =
                            -1
                    }

                    continue
                }

                val stablePitch =
                    pitch
                        .coerceIn(
                            MIN_MIDI,
                            MAX_MIDI
                        )

                if (
                    currentPitch < 0
                ) {
                    currentPitch =
                        stablePitch

                    startFrame =
                        i

                    lastActiveFrame =
                        i

                    continue
                }

                val difference =
                    abs(
                        stablePitch -
                            currentPitch
                    )

                if (
                    difference <= 1
                ) {
                    lastActiveFrame =
                        i
                } else {
                    closeNote(
                        lastActiveFrame
                    )

                    currentPitch =
                        stablePitch

                    startFrame =
                        i

                    lastActiveFrame =
                        i
                }
            }

            if (
                currentPitch >= 0 &&
                lastActiveFrame >= 0
            ) {
                closeNote(
                    lastActiveFrame
                )
            }

            return mergeAdjacentNotes(
                notes
            )
        }

        private fun mergeAdjacentNotes(
            input: List<NoteEvent>
        ): List<NoteEvent> {
            if (input.isEmpty()) {
                return emptyList()
            }

            val result =
                mutableListOf<NoteEvent>()

            for (note in input) {
                val previous =
                    result.lastOrNull()

                if (
                    previous != null &&
                    previous.pitch ==
                        note.pitch &&
                    note.startSeconds -
                        (
                            previous.startSeconds +
                                previous.durationSeconds
                            ) < 0.08
                ) {
                    result[result.lastIndex] =
                        previous.copy(
                            durationSeconds =
                                max(
                                    previous.durationSeconds,
                                    (
                                        note.startSeconds +
                                            note.durationSeconds
                                    ) -
                                        previous.startSeconds
                                ),
                            velocity =
                                max(
                                    previous.velocity,
                                    note.velocity
                                )
                        )
                } else {
                    result.add(note)
                }
            }

            return result
        }

        private class SpectrumAnalyzer(
            private val size: Int
        ) {
            private val real =
                DoubleArray(size)

            private val imag =
                DoubleArray(size)

            private val magnitude =
                DoubleArray(size / 2)

            private val window =
                DoubleArray(size) {
                    0.5 -
                        0.5 *
                        cos(
                            (
                                2.0 *
                                    PI *
                                    it
                            ) /
                                (size - 1)
                        )
                }

            private val cosTable =
                DoubleArray(size / 2)

            private val sinTable =
                DoubleArray(size / 2)

            init {
                for (
                    i in
                    cosTable.indices
                ) {
                    val angle =
                        -2.0 *
                            PI *
                            i.toDouble() /
                            size.toDouble()

                    cosTable[i] =
                        cos(angle)

                    sinTable[i] =
                        sin(angle)
                }
            }

            fun detectMidiPitch(
                samples: FloatArray,
                offset: Int,
                sampleRate: Int
            ): Int {
                for (
                    i in
                    0 until size
                ) {
                    val index =
                        offset + i

                    real[i] =
                        if (
                            index <
                                samples.size
                        ) {
                            samples[index]
                                .toDouble() *
                                window[i]
                        } else {
                            0.0
                        }

                    imag[i] =
                        0.0
                }

                fft()

                for (
                    i in
                    magnitude.indices
                ) {
                    magnitude[i] =
                        sqrt(
                            real[i] *
                                real[i] +
                                imag[i] *
                                    imag[i]
                        )
                }

                var bestMidi =
                    -1

                var bestScore =
                    0.0

                var bestEnergy =
                    0.0

                for (
                    midi in
                    MIN_MIDI..MAX_MIDI
                ) {
                    val frequency =
                        440.0 *
                            2.0.pow(
                                (
                                    midi - 69
                                ) /
                                    12.0
                            )

                    var score =
                        0.0

                    var harmonicWeight =
                        1.0

                    for (
                        harmonic in
                        1..5
                    ) {
                        val harmonicFrequency =
                            frequency *
                                harmonic

                        if (
                            harmonicFrequency >=
                                sampleRate /
                                    2.0
                        ) {
                            break
                        }

                        val bin =
                            harmonicFrequency *
                                size.toDouble() /
                                sampleRate.toDouble()

                        score +=
                            sampleMagnitude(
                                bin
                            ) /
                                harmonicWeight

                        harmonicWeight +=
                            0.55
                    }

                    if (
                        score >
                            bestScore
                    ) {
                        bestScore =
                            score
                        bestMidi =
                            midi
                    }
                }

                for (
                    i in
                    1 until
                    magnitude.lastIndex
                ) {
                    bestEnergy +=
                        magnitude[i]
                }

                if (
                    bestMidi < 0 ||
                    bestEnergy <=
                        0.0 ||
                    bestScore <
                        (
                            bestEnergy *
                                0.012
                            )
                ) {
                    return -1
                }

                return bestMidi
            }

            private fun sampleMagnitude(
                position: Double
            ): Double {
                val left =
                    position
                        .floor()
                        .toInt()
                        .coerceIn(
                            1,
                            magnitude.lastIndex
                        )

                val right =
                    (
                        left + 1
                    ).coerceAtMost(
                        magnitude.lastIndex
                    )

                val fraction =
                    position -
                        left.toDouble()

                return (
                    magnitude[left] *
                        (1.0 - fraction) +
                        magnitude[right] *
                            fraction
                    )
            }

            private fun fft() {
                var j = 0

                for (
                    i in
                    1 until size
                ) {
                    var bit =
                        size shr 1

                    while (
                        j and bit !=
                            0
                    ) {
                        j =
                            j xor bit
                        bit =
                            bit shr 1
                    }

                    j =
                        j xor bit

                    if (
                        i < j
                    ) {
                        val tempReal =
                            real[i]

                        real[i] =
                            real[j]

                        real[j] =
                            tempReal

                        val tempImag =
                            imag[i]

                        imag[i] =
                            imag[j]

                        imag[j] =
                            tempImag
                    }
                }

                var length = 2

                while (
                    length <= size
                ) {
                    val half =
                        length / 2

                    val step =
                        size /
                            length

                    var i = 0

                    while (
                        i < size
                    ) {
                        var k = 0

                        for (
                            j2 in
                            0 until half
                        ) {
                            val even =
                                i + j2

                            val odd =
                                even +
                                    half

                            val cosValue =
                                cosTable[k]

                            val sinValue =
                                sinTable[k]

                            val oddReal =
                                real[odd]

                            val oddImag =
                                imag[odd]

                            val transformedReal =
                                oddReal *
                                    cosValue -
                                    oddImag *
                                        sinValue

                            val transformedImag =
                                oddReal *
                                    sinValue +
                                    oddImag *
                                        cosValue

                            real[odd] =
                                real[even] -
                                    transformedReal

                            imag[odd] =
                                imag[even] -
                                    transformedImag

                            real[even] +=
                                transformedReal

                            imag[even] +=
                                transformedImag

                            k += step
                        }

                        i += length
                    }

                    length =
                        length shl 1
                }
            }
        }
    }

    private object MidiFileWriter {

        private const val PPQ = 480
        private const val TEMPO_US = 500_000

        fun write(
            file: File,
            notes: List<NoteEvent>
        ) {
            val events =
                mutableListOf<MidiEvent>()

            events.add(
                MidiEvent(
                    0,
                    byteArrayOf(
                        0xFF.toByte(),
                        0x51.toByte(),
                        0x03,
                        (
                            TEMPO_US shr 16
                        ).toByte(),
                        (
                            TEMPO_US shr 8
                        ).toByte(),
                        TEMPO_US.toByte()
                    )
                )
            )

            events.add(
                MidiEvent(
                    0,
                    byteArrayOf(
                        0xC0.toByte(),
                        0
                    )
                )
            )

            for (note in notes) {
                val startTick =
                    max(
                        0L,
                        (
                            note.startSeconds *
                                2.0 *
                                PPQ
                            ).roundToInt()
                                .toLong()
                    )

                val endTick =
                    max(
                        startTick + 1L,
                        (
                            (
                                note.startSeconds +
                                    note.durationSeconds
                                ) *
                                2.0 *
                                PPQ
                            )
                                .roundToInt()
                                .toLong()
                    )

                events.add(
                    MidiEvent(
                        startTick,
                        byteArrayOf(
                            0x90.toByte(),
                            note.pitch.toByte(),
                            note.velocity.toByte()
                        )
                    )
                )

                events.add(
                    MidiEvent(
                        endTick,
                        byteArrayOf(
                            0x80.toByte(),
                            note.pitch.toByte(),
                            0
                        )
                    )
                )
            }

            val sorted =
                events.sortedWith(
                    compareBy<MidiEvent> {
                        it.tick
                    }.thenBy {
                        if (
                            it.data.firstOrNull()
                                ?.and(
                                    0xF0
                                ) == 0x80
                        ) {
                            0
                        } else {
                            1
                        }
                    }
                )

            val track =
                ByteArrayOutputStream()

            var previousTick =
                0L

            for (event in sorted) {
                val delta =
                    max(
                        0L,
                        event.tick -
                            previousTick
                    )

                track.write(
                    variableLength(
                        delta
                    )
                )

                track.write(
                    event.data
                )

                previousTick =
                    event.tick
            }

            track.write(
                0
            )

            track.write(
                0xFF
            )

            track.write(
                0x2F
            )

            track.write(
                0
            )

            val trackBytes =
                track.toByteArray()

            FileOutputStream(
                file
            ).use { output ->
                output.write(
                    byteArrayOf(
                        'M'.code.toByte(),
                        'T'.code.toByte(),
                        'h'.code.toByte(),
                        'd'.code.toByte()
                    )
                )

                output.write(
                    intToBytes(
                        6
                    )
                )

                output.write(
                    shortToBytes(
                        0
                    )
                )

                output.write(
                    shortToBytes(
                        1
                    )
                )

                output.write(
                    shortToBytes(
                        PPQ
                    )
                )

                output.write(
                    byteArrayOf(
                        'M'.code.toByte(),
                        'T'.code.toByte(),
                        'r'.code.toByte(),
                        'k'.code.toByte()
                    )
                )

                output.write(
                    intToBytes(
                        trackBytes.size
                    )
                )

                output.write(
                    trackBytes
                )
            }
        }

        private data class MidiEvent(
            val tick: Long,
            val data: ByteArray
        )

        private fun variableLength(
            value: Long
        ): ByteArray {
            var current =
                value.coerceAtLeast(0)

            val buffer =
                ByteArray(5)

            var index =
                4

            buffer[index] =
                (
                    current and
                        0x7F
                    ).toByte()

            while (
                {
                    current =
                        current shr 7
                    current != 0
                }()
            ) {
                index--

                buffer[index] =
                    (
                        (current and
                            0x7F) or
                            0x80
                        ).toByte()
            }

            return buffer.copyOfRange(
                index,
                5
            )
        }

        private fun shortToBytes(
            value: Int
        ): ByteArray {
            return byteArrayOf(
                (
                    value shr 8
                ).toByte(),
                value.toByte()
            )
        }

        private fun intToBytes(
            value: Int
        ): ByteArray {
            return byteArrayOf(
                (
                    value shr 24
                ).toByte(),
                (
                    value shr 16
                ).toByte(),
                (
                    value shr 8
                ).toByte(),
                value.toByte()
            )
        }
    }

    private class FloatCollector(
        initialCapacity: Int
    ) {
        private var values =
            FloatArray(
                initialCapacity.coerceAtLeast(
                    1024
                )
            )

        var size = 0
            private set

        fun add(
            value: Float
        ) {
            if (
                size >=
                    values.size
            ) {
                values =
                    values.copyOf(
                        values.size *
                            2
                    )
            }

            values[size++] =
                value
        }

        fun toArray(): FloatArray {
            return values.copyOf(
                size
            )
        }
    }

    private fun Int.floor(): Int {
        return this
    }

    companion object {
        private const val REQUEST_PICK = 1201
        private const val TIMEOUT_US = 10_000L
        private const val MIN_SECONDS = 0.8
        private const val MAX_SECONDS = 120
    }
}
