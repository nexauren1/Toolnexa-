package com.toolnexa.audio

import com.toolnexa.app.*

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
import android.widget.HorizontalScrollView
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
    private var bpm = 120
    private var transposeSemitones = 0
    private var quantizeGrid = 0
    private var detectionProfile = "melody"
    private var cleanupEnabled = true
    private var conversionEngine = "neural"
    private var progressText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var convertButton: Button? = null
    private var sensitivityLabel: TextView? = null

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

    private val audioAccent by lazy {
        getColor(R.color.toolnexa_purple)
    }

    private val audioAccent2 by lazy {
        getColor(R.color.toolnexa_cyan)
    }

    private val success by lazy {
        getColor(R.color.toolnexa_green)
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        previewPlayer =
            MidiPreviewPlayer(
                cacheDir
            )

        LanguageManager.apply(this)
        analytics.screen("audio_to_midi")

        if (
            BasicPitchModelManager.isInstalled(
                this
            )
        ) {
            showStage1()
        } else {
            showModelSetup()
        }
    }

    private fun showModelSetup() {
        buildBase(
            "Audio → MIDI"
        )

        val hero =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(22),
                    dp(20),
                    dp(22)
                )

                background =
                    android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                        intArrayOf(
                            audioAccent,
                            audioAccent2
                        )
                    ).apply {
                        cornerRadius =
                            dp(24).toFloat()
                    }
            }

        hero.addView(
            TextView(this).apply {
                text =
                    "TOOLNEXA AI MODEL"

                textSize =
                    11.5f

                setTextColor(
                    android.graphics.Color.WHITE
                )

                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD

                letterSpacing =
                    0.12f
            }
        )

        hero.addView(
            title(
                I18n.t(
                    this,
                    "Preparar IA Neural"
                ),
                28f
            ).apply {
                setTextColor(
                    android.graphics.Color.WHITE
                )

                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(5)
                )
            }
        )

        hero.addView(
            TextView(this).apply {
                text =
                    I18n.t(
                        this@AudioToMidiActivity,
                        "Baixe o modelo uma vez para ativar a transcrição polifónica. Depois ele fica guardado no aparelho."
                    )

                textSize =
                    14.5f

                setTextColor(
                    android.graphics.Color.WHITE
                )

                setLineSpacing(
                    0f,
                    1.15f
                )
            }
        )

        add(
            hero
        )

        val modelCard =
            card()

        modelCard.addView(
            title(
                "Basic Pitch",
                19f
            )
        )

        modelCard.addView(
            bodyText(
                I18n.t(
                    this,
                    "Modelo neural de transcrição musical"
                )
            ).apply {
                setPadding(
                    0,
                    dp(5),
                    0,
                    dp(12)
                )
            }
        )

        val sizeRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        sizeRow.addView(
            TextView(this).apply {
                text =
                    I18n.t(
                        this@AudioToMidiActivity,
                        "Tamanho do modelo"
                    )

                textSize =
                    14f

                setTextColor(
                    muted
                )
            },
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        sizeRow.addView(
            TextView(this).apply {
                text =
                    BasicPitchModelManager
                        .displaySizeMb()

                textSize =
                    16f

                setTextColor(
                    audioAccent
                )

                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
            }
        )

        modelCard.addView(
            sizeRow
        )

        modelCard.addView(
            bodyText(
                I18n.t(
                    this,
                    "O modelo é baixado uma vez e reutilizado nas próximas conversões."
                )
            ).apply {
                setPadding(
                    0,
                    dp(8),
                    0,
                    0
                )
            }
        )

        val downloadProgress =
            ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                max =
                    100
                progress =
                    0
                visibility =
                    View.GONE
            }

        modelCard.addView(
            downloadProgress,
            LinearLayout.LayoutParams(
                -1,
                dp(10)
            ).apply {
                topMargin =
                    dp(14)
            }
        )

        val statusText =
            bodyText(
                I18n.t(
                    this,
                    "Pronto para download"
                )
            ).apply {
                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(0)
                )
            }

        modelCard.addView(
            statusText
        )

        add(
            modelCard
        )

        val downloadButton =
            button(
                I18n.t(
                    this,
                    "Baixar modelo"
                ) +
                    " • " +
                    BasicPitchModelManager
                        .displaySizeMb(),
                true
            )

        downloadButton.setOnClickListener {
            downloadButton.isEnabled =
                false

            downloadProgress.visibility =
                View.VISIBLE

            statusText.text =
                I18n.t(
                    this@AudioToMidiActivity,
                    "A preparar o download..."
                )

            BasicPitchModelManager.download(
                context = this,
                onProgress = {
                    downloaded,
                    total,
                    percent ->
                    val downloadedMb =
                        downloaded /
                            1_000_000.0

                    val totalMb =
                        total /
                            1_000_000.0

                    downloadProgress.progress =
                        percent

                    statusText.text =
                        String.format(
                            Locale.US,
                            "%s %d%% • %.2f / %.2f MB",
                            I18n.t(
                                this@AudioToMidiActivity,
                                "A baixar modelo..."
                            ),
                            percent,
                            downloadedMb,
                            totalMb
                        )
                },
                onComplete = {
                    statusText.text =
                        I18n.t(
                            this@AudioToMidiActivity,
                            "Modelo instalado. A preparar a IA..."
                        )

                    downloadProgress.progress =
                        100

                    Toast.makeText(
                        this,
                        I18n.t(
                            this@AudioToMidiActivity,
                            "Modelo de IA instalado com sucesso."
                        ),
                        Toast.LENGTH_SHORT
                    ).show()

                    showStage1()
                },
                onError = { message ->
                    downloadButton.isEnabled =
                        true

                    statusText.text =
                        message

                    downloadProgress.visibility =
                        View.VISIBLE
                }
            )
        }

        add(
            downloadButton
        )

        add(
            button(
                I18n.t(
                    this,
                    "Usar modo local sem IA"
                ),
                false
            ).apply {
                setOnClickListener {
                    conversionEngine =
                        "local"

                    showStage1()
                }
            }
        )

        add(
            infoCard(
                I18n.t(
                    this,
                    "MODELO LOCAL"
                ),
                I18n.t(
                    this,
                    "O modelo fica guardado no armazenamento privado do aplicativo. O áudio continua a ser processado no próprio aparelho."
                )
            )
        )

        root()?.post {
            if (!isFinishing) {
                I18n.localizeWindow(
                    this
                )
            }
        }
    }

    override fun onDestroy() {
        neuralEngine?.close()
        neuralEngine = null
        previewPlayer?.release()
        executor.shutdownNow()
        super.onDestroy()
    }

    private var neuralEngine:
        BasicPitchNativeEngine? = null

    private var previewPlayer:
        MidiPreviewPlayer? = null

    private fun showStage1() {
        buildBase("Audio → MIDI")

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(audioAccent, audioAccent2)
            ).apply {
                cornerRadius = dp(24).toFloat()
            }
        }

        hero.addView(TextView(this).apply {
            text = "TOOLNEXA STUDIO"
            textSize = 11.5f
            setTextColor(android.graphics.Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.12f
        })

        hero.addView(TextView(this).apply {
            text = "Audio → MIDI"
            textSize = 30f
            setTextColor(android.graphics.Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(8), 0, dp(4))
        })

        hero.addView(TextView(this).apply {
            text = "Transforme melodias gravadas em MIDI editável, com controlo de BPM, transposição, quantização e limpeza."
            textSize = 14.5f
            setTextColor(android.graphics.Color.WHITE)
            setLineSpacing(0f, 1.15f)
        })

        val badges = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(14), 0, 0)
        }

        badges.addView(
            pill("AI NEURAL", android.graphics.Color.WHITE, audioAccent)
        )
        badges.addView(
            pill("LOCAL FALLBACK", android.graphics.Color.WHITE, audioAccent2).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(32)
                ).apply {
                    leftMargin = dp(8)
                }
            }
        )
        badges.addView(
            pill("MIDI", android.graphics.Color.WHITE, audioAccent2).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(32)
                ).apply {
                    leftMargin = dp(8)
                }
            }
        )

        hero.addView(badges)
        add(hero)

        addTitle("Comece com o seu áudio")
        addText(
            "Escolha uma gravação de voz, baixo, guitarra, piano ou outra linha musical. " +
                "O processamento acontece no próprio aparelho."
        )

        val input = card()

        input.addView(TextView(this).apply {
            text = "Ficheiro de áudio"
            textSize = 18f
            setTextColor(textColor)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })

        input.addView(
            bodyText(
                "MP3, WAV, M4A, OGG e outros formatos que o Android consiga descodificar."
            ).apply {
                setPadding(0, dp(6), 0, dp(12))
            }
        )

        input.addView(
            button("Escolher áudio", true).apply {
                setOnClickListener {
                    analytics.event("audio_to_midi_picker")

                    startActivityForResult(
                        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            type = "audio/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            )
                        },
                        REQUEST_PICK
                    )
                }
            }
        )

        add(input)

        val power = card()
        power.addView(title("O que esta versão faz", 17f))

        listOf(
            "4 perfis de deteção: Voz, Melodia, Baixo e Amplo",
            "BPM configurável e quantização 1/8, 1/16 ou 1/32",
            "Transposição de -12 a +12 semitons",
            "Limpeza e união de notas para um MIDI mais editável"
        ).forEach { feature ->
            power.addView(
                bodyText("• " + feature).apply {
                    setPadding(0, dp(7), 0, 0)
                }
            )
        }

        add(power)

        add(
            infoCard(
                "PROCESSAMENTO LOCAL",
                "O áudio é descodificado e analisado no aparelho. O ficheiro original não é enviado para um servidor."
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

        if (requestCode == REQUEST_SAVE) {
            if (
                resultCode == RESULT_OK &&
                data?.data != null &&
                resultFile != null
            ) {
                try {
                    contentResolver.openOutputStream(
                        data.data!!
                    ).use { output ->
                        if (output == null) {
                            throw IllegalStateException(
                                "Não foi possível abrir o destino."
                            )
                        }

                        resultFile!!
                            .inputStream()
                            .use { input ->
                                input.copyTo(output)
                            }
                    }

                    Toast.makeText(
                        this,
                        "MIDI exportado com sucesso.",
                        Toast.LENGTH_SHORT
                    ).show()

                    analytics.event(
                        "audio_to_midi_exported"
                    )
                } catch (error: Exception) {
                    Toast.makeText(
                        this,
                        error.message
                            ?: "Não foi possível exportar o MIDI.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            return
        }

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
        buildBase("Configuração")

        addTitle("Audio → MIDI Studio")
        addText(
            "Escolha como o ToolNexa deve interpretar a gravação. As opções avançadas afetam o MIDI final."
        )

        val fileCard = card()
        fileCard.addView(TextView(this).apply {
            text = "ÁUDIO PRONTO"
            textSize = 11.5f
            setTextColor(audioAccent)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        })
        fileCard.addView(
            title(selectedName, 18f).apply {
                setPadding(0, dp(7), 0, dp(3))
            }
        )
        fileCard.addView(
            bodyText("Até " + MAX_SECONDS + "s • processamento local")
        )
        add(fileCard)

        val engineCard = card()
        engineCard.addView(
            title(I18n.t(this, "Motor de conversão"), 17f)
        )
        engineCard.addView(
            bodyText(
                I18n.t(
                    this,
                    "A IA Neural faz transcrição polifónica. O modelo fica guardado no aparelho depois do primeiro download."
                )
            ).apply {
                setPadding(0, dp(5), 0, dp(10))
            }
        )
        engineCard.addView(
            choiceRow(
                listOf(
                    "neural" to "IA Neural • PRO",
                    "local" to I18n.t(this, "Local rápido")
                ),
                conversionEngine,
                onSelect = {
                    key ->
                    conversionEngine =
                        key
                },
                proKeys =
                    setOf("neural")
            )
        )
        add(engineCard)

        val profileCard = card()
        profileCard.addView(title("Perfil de deteção", 17f))
        profileCard.addView(
            bodyText("Escolha o intervalo musical mais adequado ao seu áudio.")
                .apply { setPadding(0, dp(5), 0, dp(10)) }
        )
        profileCard.addView(
            choiceRow(
                listOf(
                    "voice" to "Voz",
                    "melody" to "Melodia",
                    "bass" to "Baixo",
                    "wide" to "Amplo • PRO"
                ),
                detectionProfile,
                onSelect = {
                    key ->
                    detectionProfile =
                        key
                },
                proKeys =
                    setOf("wide")
            )
        )
        add(profileCard)

        val sensitivityCard = card()
        sensitivityLabel = title(
            "Sensibilidade: " + sensitivity + "%",
            17f
        )
        sensitivityCard.addView(sensitivityLabel)
        sensitivityCard.addView(
            bodyText("Controla quanto áudio fraco é ignorado durante a deteção.")
                .apply { setPadding(0, dp(5), 0, dp(7)) }
        )

        sensitivityCard.addView(
            SeekBar(this).apply {
                max = 60
                progress = (sensitivity - 20).coerceIn(0, 60)
                setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            sensitivity = (progress + 20).coerceIn(20, 80)
                            sensitivityLabel?.text =
                                I18n.t(this@AudioToMidiActivity, "Sensibilidade") +
                                    ": " + sensitivity + "%"
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                    }
                )
            }
        )
        add(sensitivityCard)

        val advanced = card()
        advanced.addView(title("Estúdio avançado", 17f))
        advanced.addView(
            bodyText("Configure o andamento e a forma como as notas serão organizadas no MIDI.")
                .apply { setPadding(0, dp(5), 0, dp(10)) }
        )

        advanced.addView(
            title(
                I18n.t(this, "BPM") + ": " + bpm,
                15.5f
            ).apply { tag = "bpm_label" }
        )

        advanced.addView(
            SeekBar(this).apply {
                max = 120
                progress = (bpm - 60).coerceIn(0, 120)
                setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            bpm = (progress + 60).coerceIn(60, 180)
                            advanced.findViewWithTag<TextView>("bpm_label")?.text =
                                I18n.t(this@AudioToMidiActivity, "BPM") +
                                    ": " + bpm
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                    }
                )
            }
        )

        advanced.addView(
            title(I18n.t(this, "Quantização"), 15.5f).apply {
                setPadding(0, dp(8), 0, dp(5))
            }
        )
        advanced.addView(
            choiceRow(
                listOf(
                    "0" to "Desligada",
                    "8" to "1/8",
                    "16" to "1/16 • PRO",
                    "32" to "1/32 • PRO"
                ),
                quantizeGrid.toString(),
                onSelect = {
                    key ->
                    quantizeGrid =
                        key.toIntOrNull() ?: 0
                },
                proKeys =
                    setOf(
                        "16",
                        "32"
                    )
            )
        )

        advanced.addView(
            title(
                I18n.t(this, "Transposição") + ": " +
                    transposeSemitones.toSignedString() + " st",
                15.5f
            ).apply {
                tag = "transpose_label"
                setPadding(0, dp(10), 0, dp(4))
            }
        )

        advanced.addView(
            SeekBar(this).apply {
                max = 24
                progress = (transposeSemitones + 12).coerceIn(0, 24)
                setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            transposeSemitones = (progress - 12).coerceIn(-12, 12)
                            advanced.findViewWithTag<TextView>("transpose_label")?.text =
                                I18n.t(this@AudioToMidiActivity, "Transposição") +
                                    ": " +
                                    transposeSemitones.toSignedString() +
                                    " st"
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                    }
                )
            }
        )

        val cleanup =
            android.widget.CheckBox(this).apply {
                text =
                    "Limpeza inteligente • PRO"
                textSize =
                    15f
                setTextColor(
                    textColor
                )
                isChecked =
                    cleanupEnabled
                buttonTintList =
                    android.content.res.ColorStateList.valueOf(
                        audioAccent
                    )
                setOnClickListener {
                    if (
                        isChecked
                    ) {
                        isChecked =
                            false

                        ProGate.runOrUpgrade(
                            this@AudioToMidiActivity,
                            "Limpeza inteligente"
                        ) {
                            isChecked =
                                true
                            cleanupEnabled =
                                true
                        }
                    } else {
                        cleanupEnabled =
                            false
                    }
                }
            }
        advanced.addView(cleanup)
        advanced.addView(
            bodyText(
                "Remove notas muito curtas, junta repetições e evita sobreposição desnecessária."
            ).apply {
                setPadding(dp(2), 0, 0, 0)
            }
        )

        add(advanced)

        val action = button("Converter para MIDI", true).apply {
            setOnClickListener {
                startConversion()
            }
        }
        convertButton = action
        add(action)

        add(
            infoCard(
                "DICA",
                "Para voz use Voz. Para linhas graves use Baixo. Para um instrumento com notas altas e baixas use Amplo."
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
        val options = currentOptions()

        convertButton?.isEnabled = false

        buildConversionScreen(options.engine)

        if (options.engine == "neural") {
            startNeuralConversion(uri, options)
        } else {
            startLocalConversion(uri, options, false)
        }
    }

    private fun buildConversionScreen(
        engine: String
    ) {
        buildBase("A converter")

        val statusCard = card()
        statusCard.addView(
            TextView(this).apply {
                text =
                    if (engine == "neural") {
                        "TOOLNEXA NEURAL"
                    } else {
                        "TOOLNEXA LOCAL"
                    }
                textSize = 11.5f
                setTextColor(audioAccent)
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
                letterSpacing = 0.08f
            }
        )
        statusCard.addView(
            title(
                if (engine == "neural") {
                    I18n.t(this, "Transcrição neural")
                } else {
                    I18n.t(this, "A criar o seu MIDI")
                },
                24f
            ).apply {
                setPadding(0, dp(7), 0, dp(2))
            }
        )
        statusCard.addView(
            bodyText(
                if (engine == "neural") {
                    I18n.t(
                        this,
                        "Modelo neural → notas → pitch bends → limpeza → MIDI"
                    )
                } else {
                    I18n.t(
                        this,
                        "Descodificação → análise → limpeza → exportação"
                    )
                }
            )
        )
        add(statusCard)

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
            12
        )

        progressText =
            bodyText(
                I18n.t(this, "A preparar o áudio...")
            )

        add(progressText!!)

        add(
            infoCard(
                if (engine == "neural") {
                    I18n.t(this, "IA NEURAL")
                } else {
                    I18n.t(this, "LOCAL RÁPIDO")
                },
                if (engine == "neural") {
                    I18n.t(
                        this,
                        "O modelo Basic Pitch é baixado uma vez e guardado no aparelho. A inferência neural acontece localmente."
                    )
                } else {
                    I18n.t(
                        this,
                        "Este modo não precisa de descarregar um modelo."
                    )
                }
            )
        )

        root()?.post {
            if (!isFinishing) {
                I18n.localizeWindow(this)
            }
        }
    }

    private fun startNeuralConversion(
        uri: Uri,
        options: ConversionOptions
    ) {
        if (
            !BasicPitchModelManager.isInstalled(
                this
            )
        ) {
            showModelSetup()
            return
        }

        buildConversionScreen(
            "neural"
        )

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
                                    (
                                        progress * 0.14f
                                    ).roundToInt()

                                progressText?.text =
                                    I18n.t(
                                        this,
                                        "A analisar áudio..."
                                    ) +
                                        " " +
                                        progress +
                                        "%"
                            }
                        }
                    }

                val range =
                    detectionRange(
                        options.profile
                    )

                val normalized =
                    (
                        options.sensitivity -
                            20
                        ).coerceIn(
                            0,
                            60
                        ) / 60f

                val onsetThreshold =
                    (
                        0.68f -
                            normalized *
                            0.28f
                        ).coerceIn(
                            0.35f,
                            0.68f
                        )

                val frameThreshold =
                    (
                        0.46f -
                            normalized *
                            0.20f
                        ).coerceIn(
                            0.24f,
                            0.46f
                        )

                val minNoteFrames =
                    if (
                        options.cleanup
                    ) {
                        5
                    } else {
                        3
                    }

                neuralEngine?.close()

                val engine =
                    BasicPitchNativeEngine(
                        context = this,
                        minMidi = range.first,
                        maxMidi = range.second,
                        onsetThreshold = onsetThreshold,
                        frameThreshold = frameThreshold,
                        minNoteLengthFrames = minNoteFrames,
                        maxSeconds = MAX_SECONDS,
                        onProgress = {
                            progress,
                            message ->
                            runOnUiThread {
                                if (!isFinishing) {
                                    progressBar?.progress =
                                        progress

                                    progressText?.text =
                                        message +
                                            " " +
                                            progress +
                                            "%"
                                }
                            }
                        },
                        onSuccess = {
                            notes,
                            durationSeconds ->
                            neuralEngine = null

                            executor.execute {
                                try {
                                    val baseNotes =
                                        notes.map {
                                            note ->
                                            NoteEvent(
                                                startSeconds =
                                                    note.startSeconds,
                                                durationSeconds =
                                                    note.durationSeconds,
                                                pitch =
                                                    note.pitch,
                                                velocity =
                                                    (
                                                        35 +
                                                            note.amplitude *
                                                            83.0
                                                        ).roundToInt()
                                                            .coerceIn(
                                                                35,
                                                                118
                                                            ),
                                                pitchBends =
                                                    note.pitchBends
                                            )
                                        }

                                    val enhanced =
                                        enhanceNotes(
                                            baseNotes,
                                            options,
                                            durationSeconds.coerceAtLeast(
                                                MIN_SECONDS
                                            )
                                        )

                                    if (
                                        enhanced.isEmpty()
                                    ) {
                                        throw IllegalStateException(
                                            I18n.t(
                                                this@AudioToMidiActivity,
                                                "A IA não encontrou notas musicais claras."
                                            )
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
                                        enhanced,
                                        options.bpm
                                    )

                                    resultFile =
                                        file

                                    runOnUiThread {
                                        if (!isFinishing) {
                                            showResult(
                                                file,
                                                TranscriptionResult(
                                                    enhanced
                                                ),
                                                durationSeconds,
                                                options,
                                                "neural"
                                            )
                                        }
                                    }
                                } catch (
                                    error: Exception
                                ) {
                                    runOnUiThread {
                                        if (
                                            !isFinishing
                                        ) {
                                            Toast.makeText(
                                                this,
                                                error.message
                                                    ?: "Não foi possível gerar o MIDI neural.",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            startLocalConversion(
                                                uri,
                                                options,
                                                true
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onError = {
                            message ->
                            neuralEngine =
                                null

                            if (
                                !isFinishing
                            ) {
                                Toast.makeText(
                                    this,
                                    message +
                                        " " +
                                        I18n.t(
                                            this,
                                            "A usar o motor local como fallback."
                                        ),
                                    Toast.LENGTH_LONG
                                ).show()

                                startLocalConversion(
                                    uri,
                                    options,
                                    true
                                )
                            }
                        }
                    )

                neuralEngine =
                    engine

                engine.start(
                    samples =
                        decoded.samples,
                    sampleRate =
                        decoded.sampleRate,
                    durationSeconds =
                        decoded.durationSeconds
                )
            } catch (
                error: Exception
            ) {
                runOnUiThread {
                    if (!isFinishing) {
                        Toast.makeText(
                            this,
                            error.message
                                ?: "Não foi possível preparar a IA neural.",
                            Toast.LENGTH_LONG
                        ).show()

                        startLocalConversion(
                            uri,
                            options,
                            true
                        )
                    }
                }
            }
        }
    }

    private fun startLocalConversion(
        uri: Uri,
        options: ConversionOptions,
        fallbackFromNeural: Boolean
    ) {
        buildConversionScreen(
            "local"
        )

        if (fallbackFromNeural) {
            progressText?.text =
                I18n.t(
                    this,
                    "A IA Neural não está disponível. A usar o motor local..."
                )
        }

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
                                    I18n.t(
                                        this,
                                        "A analisar áudio..."
                                    ) +
                                        " " +
                                        progress +
                                        "%"
                            }
                        }
                    }

                val range =
                    detectionRange(
                        options.profile
                    )

                val threshold =
                    PitchTranscriber
                        .thresholdForSensitivity(
                            options.sensitivity
                        )

                val detected =
                    PitchTranscriber.transcribe(
                        decoded.samples,
                        decoded.sampleRate,
                        threshold,
                        range.first,
                        range.second
                    ) { progress ->
                        runOnUiThread {
                            if (!isFinishing) {
                                progressBar?.progress =
                                    progress

                                progressText?.text =
                                    I18n.t(
                                        this,
                                        "A detetar notas..."
                                    ) +
                                        " " +
                                        progress +
                                        "%"
                            }
                        }
                    }

                val enhanced =
                    enhanceNotes(
                        detected.notes,
                        options,
                        decoded.durationSeconds
                    )

                if (enhanced.isEmpty()) {
                    throw IllegalStateException(
                        "Não foram encontradas notas musicais claras. " +
                            "Tente outro perfil, ajuste a sensibilidade ou use uma gravação mais limpa."
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
                    enhanced,
                    options.bpm
                )

                resultFile =
                    file

                runOnUiThread {
                    if (!isFinishing) {
                        showResult(
                            file,
                            TranscriptionResult(
                                enhanced
                            ),
                            decoded.durationSeconds,
                            options,
                            "local"
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
        durationSeconds: Double,
        options: ConversionOptions,
        engineName: String
    ) {
        buildBase(
            "Resultado"
        )

        val notes =
            result.notes.map {
                it.copy()
            }

        val duration =
            durationSeconds.coerceAtLeast(
                MIN_SECONDS
            )

        val hero =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(20),
                    dp(20),
                    dp(20)
                )

                background =
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(
                            success
                        )
                        cornerRadius =
                            dp(24).toFloat()
                    }
            }

        hero.addView(
            TextView(this).apply {
                text =
                    "CONVERSÃO CONCLUÍDA"
                textSize =
                    11.5f
                setTextColor(
                    android.graphics.Color.WHITE
                )
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
                letterSpacing =
                    0.08f
            }
        )

        hero.addView(
            TextView(this).apply {
                text =
                    "MIDI pronto"
                textSize =
                    29f
                setTextColor(
                    android.graphics.Color.WHITE
                )
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
                setPadding(
                    0,
                    dp(7),
                    0,
                    dp(3)
                )
            }
        )

        hero.addView(
            TextView(this).apply {
                text =
                    "A conversão terminou. Use a prévia para ouvir o resultado antes de exportar."
                textSize =
                    14.5f
                setTextColor(
                    android.graphics.Color.WHITE
                )
                setLineSpacing(
                    0f,
                    1.1f
                )
            }
        )

        add(hero)

        val stats =
            card()

        stats.addView(
            title(
                "Resumo da análise",
                17f
            )
        )

        stats.addView(
            bodyText(
                "Notas: " +
                    notes.size +
                    "   •   Duração: " +
                    formatDuration(
                        duration
                    )
            )
        )

        if (
            notes.isNotEmpty()
        ) {
            val minPitch =
                notes.minOf {
                    it.pitch
                }

            val maxPitch =
                notes.maxOf {
                    it.pitch
                }

            stats.addView(
                bodyText(
                    "Extensão: " +
                        noteName(
                            minPitch
                        ) +
                        " – " +
                        noteName(
                            maxPitch
                        )
                )
            )

            stats.addView(
                bodyText(
                    "Tonalidade estimada: " +
                        estimateKey(
                            notes
                        ) +
                        " • BPM: " +
                        options.bpm
                )
            )

            stats.addView(
                bodyText(
                    "Motor: " +
                        engineLabel(
                            engineName
                        ) +
                        " • Pitch bends: " +
                        notes.count {
                            it.pitchBends.isNotEmpty()
                        }
                )
            )
        }

        add(stats)

        val previewCard =
            card()

        previewCard.addView(
            title(
                "Prévia MIDI",
                19f
            )
        )

        previewCard.addView(
            bodyText(
                "Ouça o MIDI sintetizado antes de exportar. A prévia usa o áudio gerado localmente e não altera o ficheiro original."
            ).apply {
                setPadding(
                    0,
                    dp(5),
                    0,
                    dp(10)
                )
            }
        )

        val visual =
            MidiPreviewView(
                this,
                notes,
                duration,
                audioAccent,
                audioAccent2
            )

        previewCard.addView(
            visual,
            LinearLayout.LayoutParams(
                -1,
                dp(145)
            ).apply {
                bottomMargin =
                    dp(12)
            }
        )

        val status =
            bodyText(
                "Prévia pronta para preparar..."
            ).apply {
                setTextColor(
                    textColor
                )
            }

        previewCard.addView(
            status
        )

        val seek =
            SeekBar(this).apply {
                max =
                    1000
                progress =
                    0
            }

        previewCard.addView(
            seek,
            LinearLayout.LayoutParams(
                -1,
                dp(44)
            )
        )

        val time =
            bodyText(
                "0:00 / " +
                    formatDuration(
                        duration
                    )
            ).apply {
                setPadding(
                    0,
                    0,
                    0,
                    dp(8)
                )
            }

        previewCard.addView(
            time
        )

        var ready =
            false

        var seeking =
            false

        val playButton =
            button(
                "▶ Reproduzir prévia",
                true
            )

        val stopButton =
            button(
                "■ Parar",
                false
            )

        val controls =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    android.view.Gravity.CENTER_VERTICAL
            }

        controls.addView(
            playButton,
            LinearLayout.LayoutParams(
                0,
                dp(50),
                1f
            ).apply {
                rightMargin =
                    dp(6)
            }
        )

        controls.addView(
            stopButton,
            LinearLayout.LayoutParams(
                0,
                dp(50),
                1f
            ).apply {
                leftMargin =
                    dp(0)
            }
        )

        previewCard.addView(
            controls
        )

        add(previewCard)

        playButton.setOnClickListener {
            val player =
                previewPlayer

            if (
                player == null
            ) {
                return@setOnClickListener
            }

            if (
                player.isPlaying()
            ) {
                player.pause()

                playButton.text =
                    "▶ Continuar"
                return@setOnClickListener
            }

            if (
                ready
            ) {
                player.resume()

                playButton.text =
                    "❚❚ Pausar"
                return@setOnClickListener
            }

            playButton.isEnabled =
                false

            playButton.text =
                "A preparar..."

            status.text =
                "A gerar o áudio da prévia..."

            player.play(
                notes.map {
                    MidiPreviewPlayer.PreviewNote(
                        startSeconds =
                            it.startSeconds,
                        durationSeconds =
                            it.durationSeconds,
                        pitch =
                            it.pitch,
                        velocity =
                            it.velocity,
                        pitchBends =
                            it.pitchBends
                    )
                },
                duration,
                onPrepared = { totalMs ->
                    ready =
                        true
                    playButton.isEnabled =
                        true
                    playButton.text =
                        "❚❚ Pausar"
                    status.text =
                        "Prévia em reprodução"

                    seek.max =
                        totalMs.coerceAtLeast(
                            1
                        )

                    time.text =
                        "0:00 / " +
                            formatTimeMs(
                                totalMs
                            )
                },
                onProgress = { currentMs, totalMs ->
                    if (
                        !seeking
                    ) {
                        seek.max =
                            totalMs.coerceAtLeast(
                                1
                            )

                        seek.progress =
                            currentMs.coerceIn(
                                0,
                                totalMs.coerceAtLeast(
                                    1
                                )
                            )

                        time.text =
                            formatTimeMs(
                                currentMs
                            ) +
                                " / " +
                                formatTimeMs(
                                    totalMs
                                )
                    }
                },
                onStopped = {
                    playButton.isEnabled =
                        true
                    playButton.text =
                        "▶ Reproduzir prévia"
                    status.text =
                        "Prévia parada"
                    seek.progress =
                        0
                    time.text =
                        "0:00 / " +
                            formatDuration(
                                duration
                            )
                }
            )
        }

        stopButton.setOnClickListener {
            previewPlayer?.stop()
        }

        seek.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                    seeking =
                        true
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                    seeking =
                        false

                    previewPlayer?.seekTo(
                        seekBar?.progress
                            ?: 0
                    )
                }

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (
                        fromUser &&
                        ready &&
                        seeking
                    ) {
                        time.text =
                            formatTimeMs(
                                progress
                            ) +
                                " / " +
                                formatTimeMs(
                                    seekBar?.max
                                        ?: 1
                                )
                    }
                }
            }
        )

        add(
            infoCard(
                "RECURSOS PRO",
                "IA Neural, Perfil Amplo, Quantização 1/16 e 1/32 e Limpeza inteligente estão protegidos pelo plano Pro. Toque neles no ecrã de configuração para ver o upgrade."
            )
        )

        if (
            result.notes.isEmpty()
        ) {
            add(
                infoCard(
                    "SEM NOTAS",
                    "Não foram encontradas notas suficientes nesta conversão."
                )
            )
        }

        add(
            button(
                "Exportar MIDI",
                true
            ).apply {
                setOnClickListener {
                    exportMidi(
                        file
                    )
                }
            }
        )

        add(
            button(
                "Guardar no histórico",
                false
            ).apply {
                setOnClickListener {
                    saveMidi(
                        file
                    )
                }
            }
        )

        add(
            button(
                "Partilhar MIDI",
                false
            ).apply {
                setOnClickListener {
                    shareMidi(
                        file
                    )
                }
            }
        )

        add(
            button(
                "Converter outro áudio",
                false
            ).apply {
                setOnClickListener {
                    previewPlayer?.stop()
                    selectedUri =
                        null
                    resultFile =
                        null
                    showStage1()
                }
            }
        )

        add(
            infoCard(
                "PRÉVIA ANTES DA EXPORTAÇÃO",
                "O Piano Roll foi removido. O ToolNexa agora mostra uma visualização simples das notas e uma prévia de áudio mais rápida, com reprodução, pausa, avanço e paragem."
            )
        )

        root()?.post {
            if (
                !isFinishing
            ) {
                I18n.localizeWindow(
                    this
                )
            }
        }
    }

    private fun exportMidi(
        file: File
    ) {
        resultFile = file

        try {
            startActivityForResult(
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "audio/midi"
                    putExtra(
                        Intent.EXTRA_TITLE,
                        safeFileName(
                            selectedName.substringBeforeLast(".")
                        ) + ".mid"
                    )
                    addCategory(
                        Intent.CATEGORY_OPENABLE
                    )
                },
                REQUEST_SAVE
            )

            analytics.event(
                "audio_to_midi_export_picker"
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Não foi possível abrir o exportador.",
                Toast.LENGTH_SHORT
            ).show()
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

    private fun pill(
        text: String,
        fillColor: Int,
        textColor: Int
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 10.5f
            setTextColor(textColor)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
            background =
                android.graphics.drawable.GradientDrawable().apply {
                    setColor(
                        (
                            fillColor and
                                0x00FFFFFF
                            ) or
                            (
                                0x22 shl 24
                            )
                    )
                    setStroke(dp(1), fillColor)
                    cornerRadius = dp(18).toFloat()
                }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(32)
            )
        }
    }

    private fun choiceRow(
        options: List<Pair<String, String>>,
        selectedKey: String,
        onSelect: (String) -> Unit,
        proKeys: Set<String> = emptySet()
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val buttons = linkedMapOf<String, Button>()
        val selectedKeyHolder = arrayOf(selectedKey)

        fun refresh() {
            buttons.forEach { (key, button) ->
                val selected = key == selectedKeyHolder[0]
                button.background =
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(
                            if (selected) {
                                audioAccent
                            } else {
                                0xFFF6F8FC.toInt()
                            }
                        )
                        setStroke(
                            dp(1),
                            if (selected) {
                                audioAccent
                            } else {
                                border
                            }
                        )
                        cornerRadius = dp(12).toFloat()
                    }
                button.setTextColor(
                    if (selected) {
                        android.graphics.Color.WHITE
                    } else {
                        textColor
                    }
                )
            }
        }

        options.forEachIndexed { index, pair ->
            val button = Button(this).apply {
                text = pair.second
                textSize = 12.5f
                isAllCaps = false
                minHeight = dp(44)
                stateListAnimator = null
                setPadding(dp(7), 0, dp(7), 0)
                setOnClickListener {
                    if (
                        pair.first in
                            proKeys
                    ) {
                        ProGate.runOrUpgrade(
                            this@AudioToMidiActivity,
                            pair.second
                        ) {
                            selectedKeyHolder[0] =
                                pair.first
                            onSelect(
                                pair.first
                            )
                            refresh()
                        }
                    } else {
                        selectedKeyHolder[0] =
                            pair.first
                        onSelect(
                            pair.first
                        )
                        refresh()
                    }
                }
            }

            val params = LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            ).apply {
                if (index > 0) {
                    leftMargin = dp(6)
                }
            }
            row.addView(button, params)
            buttons[pair.first] = button
        }

        refresh()
        return row
    }

    private fun formatTimeMs(
        milliseconds: Int
    ): String {
        val safe =
            milliseconds.coerceAtLeast(
                0
            )

        val totalSeconds =
            safe / 1000

        val minutes =
            totalSeconds / 60

        val seconds =
            totalSeconds % 60

        return String.format(
            Locale.US,
            "%d:%02d",
            minutes,
            seconds
        )
    }

    private fun engineLabel(
        engine: String
    ): String {
        return I18n.t(
            this,
            if (engine == "neural") {
                "IA Neural"
            } else {
                "Local rápido"
            }
        )
    }

    private fun currentOptions(): ConversionOptions {
        return ConversionOptions(
            detectionProfile,
            sensitivity,
            bpm,
            transposeSemitones,
            quantizeGrid,
            cleanupEnabled,
            conversionEngine
        )
    }

    private fun detectionRange(profile: String): Pair<Int, Int> {
        return when (profile) {
            "voice" -> 48 to 88
            "bass" -> 28 to 60
            "wide" -> 24 to 108
            else -> 36 to 96
        }
    }

    private fun profileLabel(profile: String): String {
        val key = when (profile) {
            "voice" -> "Voz"
            "bass" -> "Baixo"
            "wide" -> "Amplo"
            else -> "Melodia"
        }
        return I18n.t(this, key)
    }

    private fun quantizeLabel(grid: Int): String {
        return I18n.t(
            this,
            when (grid) {
                8 -> "1/8"
                16 -> "1/16"
                32 -> "1/32"
                else -> "Desligada"
            }
        )
    }

    private fun enhanceNotes(
        input: List<NoteEvent>,
        options: ConversionOptions,
        durationSeconds: Double
    ): List<NoteEvent> {
        if (input.isEmpty()) return emptyList()

        var notes = input.sortedBy { it.startSeconds }

        if (options.cleanup) {
            notes = notes
                .filter { it.durationSeconds >= 0.055 }
                .fold(mutableListOf()) { acc, note ->
                    val previous = acc.lastOrNull()

                    if (
                        previous != null &&
                        previous.pitch == note.pitch &&
                        note.startSeconds -
                            (
                                previous.startSeconds +
                                    previous.durationSeconds
                            ) < 0.11
                    ) {
                        acc[acc.lastIndex] = previous.copy(
                            durationSeconds = max(
                                previous.durationSeconds,
                                (
                                    note.startSeconds +
                                        note.durationSeconds
                                ) - previous.startSeconds
                            ),
                            velocity = max(
                                previous.velocity,
                                note.velocity
                            )
                        )
                    } else {
                        acc.add(note)
                    }

                    acc
                }
        }

        val transposed = notes.map { note ->
            note.copy(
                pitch = (
                    note.pitch +
                        options.transposeSemitones
                    ).coerceIn(0, 127)
            )
        }

        val quantized =
            if (options.quantizeGrid > 0) {
                val beatSeconds =
                    60.0 /
                        options.bpm.toDouble()

                val gridSeconds =
                    beatSeconds *
                        (
                            4.0 /
                                options.quantizeGrid.toDouble()
                            )

                transposed.map { note ->
                    val rawStart =
                        note.startSeconds

                    val rawEnd =
                        note.startSeconds +
                            note.durationSeconds

                    val start =
                        (
                            (
                                rawStart /
                                    gridSeconds
                                ).roundToInt() *
                                gridSeconds
                            ).coerceIn(
                                0.0,
                                durationSeconds
                            )

                    val desiredEnd =
                        (
                            (
                                rawEnd /
                                    gridSeconds
                                ).roundToInt() *
                                gridSeconds
                            )

                    val minDuration =
                        max(
                            0.04,
                            gridSeconds * 0.5
                        )

                    val end =
                        max(
                            start +
                                minDuration,
                            desiredEnd
                        ).coerceAtMost(
                            durationSeconds
                        )

                    note.copy(
                        startSeconds = start,
                        durationSeconds = max(
                            0.04,
                            end - start
                        )
                    )
                }
            } else {
                transposed
            }

        val sorted =
            quantized
                .filter {
                    it.startSeconds <
                        durationSeconds
                }
                .sortedBy {
                    it.startSeconds
                }

        if (!options.cleanup) {
            return sorted
        }

        val result =
            mutableListOf<NoteEvent>()

        for (note in sorted) {
            val previous =
                result.lastOrNull()

            if (
                previous != null &&
                note.startSeconds <
                    previous.startSeconds +
                        previous.durationSeconds &&
                note.pitch != previous.pitch
            ) {
                val safeStart =
                    max(
                        note.startSeconds,
                        previous.startSeconds +
                            0.01
                    )

                val safeEnd =
                    note.startSeconds +
                        note.durationSeconds

                if (safeEnd > safeStart) {
                    result.add(
                        note.copy(
                            startSeconds = safeStart,
                            durationSeconds =
                                safeEnd - safeStart
                        )
                    )
                }
            } else {
                result.add(note)
            }
        }

        return result
    }

    private fun estimateKey(
        notes: List<NoteEvent>
    ): String {
        if (notes.isEmpty()) return "—"

        val histogram = DoubleArray(12)

        notes.forEach { note ->
            histogram[note.pitch % 12] +=
                max(0.1, note.durationSeconds) *
                    (
                        note.velocity /
                            127.0
                        )
        }

        val major = doubleArrayOf(
            6.35, 2.23, 3.48, 2.33,
            4.38, 4.09, 2.52, 5.19,
            2.39, 3.66, 2.29, 2.88
        )

        val minor = doubleArrayOf(
            6.33, 2.68, 3.52, 5.38,
            2.60, 3.53, 2.54, 4.75,
            3.98, 2.69, 3.34, 3.17
        )

        val names = arrayOf(
            "C", "C#", "D", "D#", "E", "F",
            "F#", "G", "G#", "A", "A#", "B"
        )

        var bestScore =
            Double.NEGATIVE_INFINITY

        var bestName =
            "—"

        for (root in 0 until 12) {
            var majorScore = 0.0
            var minorScore = 0.0

            for (i in 0 until 12) {
                val value =
                    histogram[
                        (root + i) % 12
                    ]

                majorScore +=
                    value * major[i]

                minorScore +=
                    value * minor[i]
            }

            if (majorScore > bestScore) {
                bestScore = majorScore
                bestName =
                    names[root] +
                        " " +
                        I18n.t(this, "maior")
            }

            if (minorScore > bestScore) {
                bestScore = minorScore
                bestName =
                    names[root] +
                        " " +
                        I18n.t(this, "menor")
            }
        }

        return bestName
    }

    private fun noteName(midi: Int): String {
        val names = arrayOf(
            "C", "C#", "D", "D#", "E", "F",
            "F#", "G", "G#", "A", "A#", "B"
        )

        val pitch =
            midi.coerceIn(0, 127)

        val octave =
            (pitch / 12) - 1

        return names[pitch % 12] +
            octave
    }

    private fun Int.toSignedString(): String {
        return if (this > 0) {
            "+$this"
        } else {
            this.toString()
        }
    }

    private data class ConversionOptions(
        val profile: String,
        val sensitivity: Int,
        val bpm: Int,
        val transposeSemitones: Int,
        val quantizeGrid: Int,
        val cleanup: Boolean,
        val engine: String
    )

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
        val velocity: Int,
        val pitchBends: List<Int> = emptyList(),
        val channel: Int = 0
    )

    private data class TranscriptionResult(
        val notes: List<NoteEvent>
    )

    private class MidiPreviewView(
        context: Context,
        private val notes: List<NoteEvent>,
        private val durationSeconds: Double,
        private val primaryColor: Int,
        private val secondaryColor: Int
    ) : View(context) {

        private val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            )

        override fun onDraw(
            canvas: android.graphics.Canvas
        ) {
            super.onDraw(canvas)

            val width =
                width.toFloat()

            val height =
                height.toFloat()

            paint.style =
                android.graphics.Paint.Style.FILL

            paint.color =
                0xFFF7F9FD.toInt()

            canvas.drawRect(
                0f,
                0f,
                width,
                height,
                paint
            )

            paint.color =
                0xFFDCE5F2.toInt()

            paint.strokeWidth =
                dp(1).toFloat()

            for (i in 1 until 8) {
                val y =
                    height * i / 8f

                canvas.drawLine(
                    0f,
                    y,
                    width,
                    y,
                    paint
                )
            }

            for (i in 1 until 10) {
                val x =
                    width * i / 10f

                canvas.drawLine(
                    x,
                    0f,
                    x,
                    height,
                    paint
                )
            }

            if (
                notes.isEmpty() ||
                durationSeconds <= 0.0
            ) {
                return
            }

            val minPitch =
                (
                    notes.minOf {
                        it.pitch
                    } - 3
                    ).coerceAtLeast(0)

            val maxPitch =
                (
                    notes.maxOf {
                        it.pitch
                    } + 3
                    ).coerceAtMost(127)

            val pitchRange =
                max(
                    1,
                    maxPitch - minPitch
                )

            notes.forEachIndexed { index, note ->
                val x =
                    (
                        note.startSeconds /
                            durationSeconds
                        ).toFloat() *
                        width

                val noteWidth =
                    max(
                        dp(5).toFloat(),
                        (
                            note.durationSeconds /
                                durationSeconds
                            ).toFloat() *
                            width
                    )

                val normalized =
                    (
                        note.pitch -
                            minPitch
                        ).toFloat() /
                        pitchRange.toFloat()

                val y =
                    height -
                        (
                            normalized *
                                (
                                    height -
                                        dp(10)
                                    )
                            ) -
                        dp(7)

                val alpha =
                    (
                        110 +
                            (
                                note.velocity.coerceIn(
                                    35,
                                    118
                                ) - 35
                            ) * 2
                        ).coerceIn(
                            110,
                            255
                        )

                val base =
                    if (index % 2 == 0) {
                        primaryColor
                    } else {
                        secondaryColor
                    }

                paint.color =
                    (
                        base and
                            0x00FFFFFF
                        ) or
                        (
                            alpha shl 24
                            )

                canvas.drawRoundRect(
                    x,
                    y - dp(6),
                    min(
                        width,
                        x + noteWidth
                    ),
                    y + dp(6),
                    dp(6).toFloat(),
                    dp(6).toFloat(),
                    paint
                )
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
    }

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
                        MediaFormat.KEY_SAMPLE_RATE
                    )

                var channels =
                    format.getInteger(
                        MediaFormat.KEY_CHANNEL_COUNT
                    )

                var pcmEncoding =
                    if (
                        format.containsKey(
                            MediaFormat.KEY_PCM_ENCODING
                        )
                    ) {
                        format.getInteger(
                            MediaFormat.KEY_PCM_ENCODING
                        )
                    } else {
                        android.media.AudioFormat.ENCODING_PCM_16BIT
                    }

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
                                if (
                                    outputFormat.containsKey(
                                        MediaFormat.KEY_SAMPLE_RATE
                                    )
                                ) {
                                    outputFormat.getInteger(
                                        MediaFormat.KEY_SAMPLE_RATE
                                    )
                                } else {
                                    sampleRate
                                }

                            channels =
                                if (
                                    outputFormat.containsKey(
                                        MediaFormat.KEY_CHANNEL_COUNT
                                    )
                                ) {
                                    outputFormat.getInteger(
                                        MediaFormat.KEY_CHANNEL_COUNT
                                    )
                                } else {
                                    channels
                                }

                            pcmEncoding =
                                if (
                                    outputFormat.containsKey(
                                        MediaFormat.KEY_PCM_ENCODING
                                    )
                                ) {
                                    outputFormat.getInteger(
                                        MediaFormat.KEY_PCM_ENCODING
                                    )
                                } else {
                                    pcmEncoding
                                }
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
                                    if (
                                        format.containsKey(
                                            MediaFormat.KEY_DURATION
                                        )
                                    ) {
                                        format.getLong(
                                            MediaFormat.KEY_DURATION
                                        )
                                    } else {
                                        1L
                                    }
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

                val prepared =
                    prepareSamples(samples)

                val durationSeconds =
                    prepared.size.toDouble() /
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
                    prepared,
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

        private fun prepareSamples(
            source: FloatArray
        ): FloatArray {
            if (source.isEmpty()) return source

            var mean = 0.0

            for (sample in source) {
                mean += sample.toDouble()
            }

            mean /=
                source.size.toDouble()

            val cleaned =
                FloatArray(
                    source.size
                )

            var peak = 0.0

            for (i in source.indices) {
                val value =
                    (
                        source[i].toDouble() -
                            mean
                        ).coerceIn(
                            -1.0,
                            1.0
                        )

                cleaned[i] =
                    value.toFloat()

                peak =
                    max(
                        peak,
                        abs(value)
                    )
            }

            if (peak <= 0.0001) {
                return cleaned
            }

            val gain =
                min(
                    1.85,
                    0.92 / peak
                )

            for (i in cleaned.indices) {
                cleaned[i] =
                    (
                        cleaned[i] *
                            gain.toFloat()
                        ).coerceIn(
                            -1f,
                            1f
                        )
            }

            return cleaned
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
            minMidi: Int,
            maxMidi: Int,
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
                            sampleRate,
                            minMidi,
                            maxMidi
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
                sampleRate: Int,
                minMidi: Int,
                maxMidi: Int
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
                    minMidi..maxMidi
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
                    kotlin.math.floor(
                        position
                    )
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
        private const val BEND_RANGE_SEMITONES = 2.0

        private val midiChannels =
            (0..15)
                .filter { it != 9 }

        fun write(
            file: File,
            notes: List<NoteEvent>,
            bpm: Int
        ) {
            val safeBpm =
                bpm.coerceIn(
                    30,
                    300
                )

            val prepared =
                assignChannels(
                    notes
                )

            val tempoUs =
                (
                    60_000_000.0 /
                        safeBpm.toDouble()
                    ).roundToInt().coerceIn(
                        1,
                        0xFFFFFF
                    )

            val events =
                mutableListOf<MidiEvent>()

            events.add(
                MidiEvent(
                    tick = 0,
                    priority = 0,
                    data = byteArrayOf(
                        0xFF.toByte(),
                        0x51.toByte(),
                        0x03,
                        (
                            tempoUs shr 16
                        ).toByte(),
                        (
                            tempoUs shr 8
                        ).toByte(),
                        tempoUs.toByte()
                    )
                )
            )

            val bendChannels =
                prepared
                    .filter {
                        it.pitchBends.isNotEmpty()
                    }
                    .map {
                        it.channel
                    }
                    .distinct()

            for (channel in bendChannels) {
                addPitchBendRangeEvents(
                    events,
                    channel
                )
            }

            val usedChannels =
                prepared
                    .map {
                        it.channel
                    }
                    .distinct()

            for (channel in usedChannels) {
                events.add(
                    MidiEvent(
                        tick = 0,
                        priority = 1,
                        data = byteArrayOf(
                            (
                                0xC0 or
                                    channel
                                ).toByte(),
                            0
                        )
                    )
                )
            }

            for (note in prepared) {
                val startTick =
                    max(
                        0L,
                        (
                            note.startSeconds *
                                safeBpm.toDouble() /
                                60.0 *
                                PPQ
                            )
                                .roundToInt()
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
                                safeBpm.toDouble() /
                                60.0 *
                                PPQ
                            )
                                .roundToInt()
                                .toLong()
                    )

                if (
                    note.pitchBends.isNotEmpty()
                ) {
                    addPitchBendEvents(
                        events,
                        note,
                        startTick,
                        endTick
                    )
                }

                events.add(
                    MidiEvent(
                        tick = startTick,
                        priority = 4,
                        data = byteArrayOf(
                            (
                                0x90 or
                                    note.channel
                                ).toByte(),
                            note.pitch.toByte(),
                            note.velocity.toByte()
                        )
                    )
                )

                events.add(
                    MidiEvent(
                        tick = endTick,
                        priority = 2,
                        data = byteArrayOf(
                            (
                                0x80 or
                                    note.channel
                                ).toByte(),
                            note.pitch.toByte(),
                            0
                        )
                    )
                )

                if (
                    note.pitchBends.isNotEmpty()
                ) {
                    events.add(
                        MidiEvent(
                            tick = endTick,
                            priority = 3,
                            data =
                                pitchBendData(
                                    note.channel,
                                    0.0
                                )
                        )
                    )
                }
            }

            val sorted =
                events.sortedWith(
                    compareBy<MidiEvent> {
                        it.tick
                    }.thenBy {
                        it.priority
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

            track.write(0)
            track.write(0xFF)
            track.write(0x2F)
            track.write(0)

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
                    intToBytes(6)
                )

                output.write(
                    shortToBytes(0)
                )

                output.write(
                    shortToBytes(1)
                )

                output.write(
                    shortToBytes(PPQ)
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

        private fun assignChannels(
            notes: List<NoteEvent>
        ): List<NoteEvent> {
            if (notes.isEmpty()) {
                return emptyList()
            }

            data class Active(
                val channel: Int,
                val endSeconds: Double
            )

            val active =
                mutableListOf<Active>()

            return notes
                .sortedBy {
                    it.startSeconds
                }
                .map { note ->
                    active.removeAll {
                        it.endSeconds <=
                            note.startSeconds +
                                0.0005
                    }

                    val used =
                        active
                            .map {
                                it.channel
                            }
                            .toSet()

                    val channel =
                        midiChannels.firstOrNull {
                            !used.contains(it)
                        } ?: midiChannels[
                            active.size %
                                midiChannels.size
                        ]

                    active.add(
                        Active(
                            channel,
                            note.startSeconds +
                                note.durationSeconds
                        )
                    )

                    note.copy(
                        channel = channel
                    )
                }
        }

        private fun addPitchBendRangeEvents(
            events: MutableList<MidiEvent>,
            channel: Int
        ) {
            val status =
                (
                    0xB0 or
                        channel
                    ).toByte()

            fun cc(
                number: Int,
                value: Int
            ) {
                events.add(
                    MidiEvent(
                        tick = 0,
                        priority = 0,
                        data = byteArrayOf(
                            status,
                            number.toByte(),
                            value.toByte()
                        )
                    )
                )
            }

            cc(101, 0)
            cc(100, 0)
            cc(6, BEND_RANGE_SEMITONES.roundToInt())
            cc(38, 0)
            cc(101, 127)
            cc(100, 127)
        }

        private fun addPitchBendEvents(
            events: MutableList<MidiEvent>,
            note: NoteEvent,
            startTick: Long,
            endTick: Long
        ) {
            val bends =
                simplifyBends(
                    note.pitchBends
                )

            if (bends.isEmpty()) {
                return
            }

            val span =
                max(
                    1L,
                    endTick -
                        startTick
                )

            bends.forEachIndexed { index, value ->
                val position =
                    if (bends.size == 1) {
                        0.0
                    } else {
                        index.toDouble() /
                            (
                                bends.lastIndex
                            ).toDouble()
                    }

                val tick =
                    startTick +
                        (
                            span.toDouble() *
                                position
                            ).roundToInt()
                                .toLong()

                val semitones =
                    value.toDouble() /
                        3.0

                events.add(
                    MidiEvent(
                        tick = tick,
                        priority = 3,
                        data =
                            pitchBendData(
                                note.channel,
                                semitones
                            )
                    )
                )
            }
        }

        private fun simplifyBends(
            input: List<Int>
        ): List<Int> {
            if (input.isEmpty()) {
                return emptyList()
            }

            val maxPoints = 32

            if (
                input.size <=
                    maxPoints
            ) {
                return input
            }

            return List(maxPoints) { index ->
                val sourceIndex =
                    (
                        index.toDouble() /
                            (
                                maxPoints - 1
                                ).toDouble()
                        *
                        (
                            input.lastIndex
                                ).toDouble()
                    )
                        .roundToInt()
                        .coerceIn(
                            0,
                            input.lastIndex
                        )

                input[sourceIndex]
            }
        }

        private fun pitchBendData(
            channel: Int,
            semitones: Double
        ): ByteArray {
            val normalized =
                (
                    semitones /
                        BEND_RANGE_SEMITONES
                    ).coerceIn(
                        -1.0,
                        1.0
                    )

            val value =
                (
                    8192.0 +
                        normalized *
                        8191.0
                    )
                    .roundToInt()
                    .coerceIn(
                        0,
                        16383
                    )

            return byteArrayOf(
                (
                    0xE0 or
                        channel
                    ).toByte(),
                (
                    value and
                        0x7F
                    ).toByte(),
                (
                    (
                        value shr
                            7
                        ) and
                        0x7F
                    ).toByte()
            )
        }

        private data class MidiEvent(
            val tick: Long,
            val priority: Int,
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

            while (true) {
                current =
                    current shr 7

                if (current == 0L) {
                    break
                }

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

    companion object {
        private const val REQUEST_PICK = 1201
        private const val REQUEST_SAVE = 1202
        private const val TIMEOUT_US = 10_000L
        private const val MIN_SECONDS = 0.8
        private const val MAX_SECONDS = 120
    }
}
