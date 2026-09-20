package com.toolnexa.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class BackgroundRemoverActivity : Activity() {

    private val analytics by lazy { AnalyticsTracker(this) }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var selectedUri: Uri? = null
    private var resultFile: File? = null
    private var originalBitmap: Bitmap? = null

    private var editorForeground: Bitmap? = null
    private var baseForeground: Bitmap? = null
    private var customBackground: Bitmap? = null
    private var backgroundKey = "transparent"

    private var brightness = 0
    private var contrast = 0
    private var saturation = 0
    private var opacity = 100
    private var scalePercent = 100
    private var verticalPercent = 0
    private var blurLevel = 0
    private var brushMode = BrushMode.ERASE
    private var brushSize = 70

    private var editorImage: ImageView? = null
    private var editorSurface: EditorSurface? = null
    private var root: LinearLayout? = null

    private val blue by lazy { getColor(R.color.toolnexa_blue) }
    private val bg by lazy { getColor(R.color.toolnexa_bg) }
    private val surface by lazy { getColor(R.color.toolnexa_surface) }
    private val textColor by lazy { getColor(R.color.toolnexa_text) }
    private val muted by lazy { getColor(R.color.toolnexa_muted) }
    private val border by lazy { getColor(R.color.toolnexa_border) }

    enum class BrushMode {
        ERASE,
        RESTORE
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        analytics.screen("background_remover_stage_1")
        showStage1()
    }

    private fun showStage1() {
        buildBase("1 / 4  •  Escolher imagem")
        addTitle("Background Remover")
        addText(
            "Remova o fundo automaticamente e depois edite " +
                "a imagem com novos cenários, cores e ajustes."
        )

        add(
            infoCard(
                "PROCESSAMENTO REAL",
                "Cloudflare Images + BiRefNet • processamento " +
                    "no backend do ToolNexa."
            )
        )

        val input = card()
        input.addView(title("Começar com uma imagem", 18f))
        input.addView(
            bodyText(
                "JPG, PNG, WebP e formatos compatíveis. " +
                    "Limite de 20 MB."
            )
        )

        val choose = button("Escolher imagem", true)
        choose.setOnClickListener {
            analytics.event("background_remover_picker")
            startImagePicker(REQUEST_PICK)
        }
        input.addView(choose)
        add(input)

        add(
            infoCard(
                "PRIVACIDADE",
                "A imagem é enviada somente para o processamento " +
                    "solicitado e o resultado é devolvido ao aplicativo."
            )
        )
    }

    private fun startImagePicker(requestCode: Int) {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            },
            requestCode
        )
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
            resultCode != RESULT_OK ||
            data?.data == null
        ) {
            return
        }

        val uri = data.data!!

        if (requestCode == REQUEST_PICK) {
            selectedUri = uri

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            analytics.event(
                "background_remover_image_selected"
            )
            showStage2()
            return
        }

        if (requestCode == REQUEST_BACKGROUND) {
            loadBitmap(uri)?.let {
                customBackground = fitBackground(
                    it,
                    editorCanvasSize()
                )
                backgroundKey = "custom"
                renderEditor()
                analytics.event(
                    "background_remover_custom_background"
                )
            } ?: toast(
                "Não foi possível carregar este fundo."
            )
        }
    }

    private fun showStage2() {
        buildBase("2 / 4  •  Preparar processamento")
        addTitle("Confirmar imagem")
        addText(
            "Revise a imagem antes de enviar para o motor " +
                "de remoção de fundo."
        )

        val uri = selectedUri
            ?: return showStage1()

        val previewCard = card()

        val imagePreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            adjustViewBounds = true
            background = checkerboard()
            contentDescription =
                "Prévia da imagem selecionada"
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        previewCard.addView(
            imagePreview,
            LinearLayout.LayoutParams(-1, dp(310))
        )

        previewCard.addView(
            TextView(this).apply {
                text = displayName(uri)
                textSize = 14f
                setTextColor(textColor)
                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(2)
                )
            }
        )

        previewCard.addView(
            TextView(this).apply {
                text =
                    "Tamanho: " +
                        formatBytes(querySize(uri))
                textSize = 13f
                setTextColor(muted)
            }
        )

        add(previewCard)

        Thread {
            val bitmap = loadBitmap(uri)
            runOnUiThread {
                if (bitmap != null) {
                    imagePreview.setImageBitmap(bitmap)
                }
            }
        }.start()

        add(
            infoCard(
                "MOTOR",
                "Segmentação de foreground no Cloudflare Images. " +
                    "O resultado é gerado como PNG transparente."
            )
        )

        val process = button("Remover fundo", true)
        process.setOnClickListener { process() }
        add(process)

        val back = button(
            "Escolher outra imagem",
            false
        )
        back.setOnClickListener {
            selectedUri = null
            showStage1()
        }
        add(back)
    }

    private fun process() {
        val uri = selectedUri ?: return
        val user = auth.currentUser

        if (user == null) {
            toast("Inicie sessão para usar esta ferramenta.")
            finish()
            return
        }

        analytics.event(
            "background_remover_process_start"
        )

        val dialog = processingDialog()

        user.getIdToken(false)
            .addOnSuccessListener { result ->
                val token = result.token

                if (token.isNullOrBlank()) {
                    dialog.dismiss()
                    toast(
                        "A sessão não está disponível. Entre novamente."
                    )
                    return@addOnSuccessListener
                }

                Thread {
                    try {
                        val source = loadBitmap(uri)
                        val file =
                            CloudflareApi.removeBackground(
                                this,
                                uri,
                                token
                            )

                        if (source == null) {
                            throw IllegalStateException(
                                "Não foi possível preparar a imagem original."
                            )
                        }

                        runOnUiThread {
                            dialog.dismiss()
                            originalBitmap = source
                            resultFile = file
                            editorForeground = decodeEditorForeground(
                                file
                            )
                            baseForeground =
                                editorForeground?.copy(
                                    Bitmap.Config.ARGB_8888,
                                    true
                                )

                            analytics.event(
                                "background_remover_process_success"
                            )
                            showStage3()
                        }
                    } catch (error: Exception) {
                        runOnUiThread {
                            dialog.dismiss()
                            analytics.event(
                                "background_remover_process_failed",
                                "error" to
                                    error.javaClass.simpleName
                            )

                            AlertDialog.Builder(this)
                                .setTitle(
                                    "Não foi possível remover o fundo"
                                )
                                .setMessage(
                                    error.message
                                        ?: "O processamento falhou."
                                )
                                .setPositiveButton(
                                    "Tentar novamente"
                                ) { _, _ ->
                                    process()
                                }
                                .setNegativeButton(
                                    "Fechar",
                                    null
                                )
                                .show()
                        }
                    }
                }.start()
            }
            .addOnFailureListener {
                dialog.dismiss()
                toast(
                    "Não foi possível validar a sessão."
                )
            }
    }

    private fun showStage3() {
        val file =
            resultFile ?: return showStage2()

        buildBase("3 / 4  •  Resultado")
        addTitle("Fundo removido")
        addText(
            "O recorte ficou pronto. Agora pode editar o cenário, " +
                "as cores e o posicionamento antes de exportar."
        )

        val resultCard = card()

        resultCard.addView(
            TextView(this).apply {
                text = "✓ PROCESSADO NO CLOUDFLARE"
                textSize = 12f
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(
                    getColor(R.color.toolnexa_green)
                )
                setPadding(
                    0,
                    0,
                    0,
                    dp(10)
                )
            }
        )

        val image = ImageView(this).apply {
            scaleType =
                ImageView.ScaleType.CENTER_INSIDE
            adjustViewBounds = true
            background = checkerboard()
            contentDescription =
                "Resultado com fundo removido"
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        resultCard.addView(
            image,
            LinearLayout.LayoutParams(-1, dp(330))
        )

        BitmapFactory.decodeFile(file.absolutePath)
            ?.let { image.setImageBitmap(it) }

        add(resultCard)

        add(
            infoCard(
                "DETALHES",
                "Original: " +
                    formatBytes(
                        selectedUri?.let {
                            querySize(it)
                        } ?: 0L
                    ) +
                    "\nResultado: " +
                    formatBytes(file.length()) +
                    "\nFormato: PNG com alpha/transparência" +
                    "\nMotor: Cloudflare Images / BiRefNet"
            )
        )

        val edit = button(
            "Editar imagem",
            true
        )
        edit.setOnClickListener {
            analytics.event(
                "background_remover_editor_open"
            )
            prepareEditor()
        }
        add(edit)

        val save = button(
            "Salvar PNG transparente",
            false
        )
        save.setOnClickListener {
            saveResult(file)
        }
        add(save)

        val share = button(
            "Partilhar resultado",
            false
        )
        share.setOnClickListener {
            shareResult(file)
        }
        add(share)

        val again = button(
            "Processar outra imagem",
            false
        )
        again.setOnClickListener {
            resetEditor()
            selectedUri = null
            resultFile = null
            showStage1()
        }
        add(again)
    }

    private fun prepareEditor() {
        if (
            editorForeground == null ||
            baseForeground == null
        ) {
            editorForeground =
                resultFile?.let {
                    decodeEditorForeground(it)
                }
            baseForeground =
                editorForeground?.copy(
                    Bitmap.Config.ARGB_8888,
                    true
                )
        }

        resetEditorValues()
        showEditor()
    }

    private fun resetEditorValues() {
        backgroundKey = "transparent"
        customBackground = null
        brightness = 0
        contrast = 0
        saturation = 0
        opacity = 100
        scalePercent = 100
        verticalPercent = 0
        blurLevel = 0
        brushMode = BrushMode.ERASE
        brushSize = 70
    }

    private fun resetEditor() {
        editorForeground?.recycleIfSafe()
        baseForeground?.recycleIfSafe()
        customBackground?.recycleIfSafe()
        editorForeground = null
        baseForeground = null
        customBackground = null
    }

    private fun showEditor() {
        buildBase("4 / 4  •  Editar imagem")
        addTitle("Editor de Fundo")
        addText(
            "Troque o cenário, ajuste o recorte e exporte " +
                "uma versão de alta qualidade."
        )

        val previewCard = card()

        editorImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = checkerboard()
            contentDescription =
                "Prévia editável da imagem"
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        previewCard.addView(
            editorImage,
            LinearLayout.LayoutParams(-1, dp(390))
        )

        editorSurface = EditorSurface(
            this
        )

        editorImage?.setOnTouchListener { _, event ->
            editorSurface?.handleTouch(event) ?: false
        }

        add(previewCard)

        add(
            sectionTitle(
                "Fundo"
            )
        )
        add(
            bodyText(
                "Escolha transparente, uma cor, um cenário " +
                    "ou uma imagem da sua galeria."
            )
        )
        add(backgroundChoices())

        add(
            sectionTitle(
                "Ajustes"
            )
        )
        add(adjustmentPanel())

        add(
            sectionTitle(
                "Recorte"
            )
        )
        add(refinePanel())

        add(
            sectionTitle(
                "Exportação"
            )
        )
        add(
            infoCard(
                "QUALIDADE",
                "PNG conserva transparência e é sem perdas. " +
                    "JPG pode ser usado quando o fundo foi preenchido."
            )
        )

        val save = button(
            "Salvar imagem editada",
            true
        )
        save.setOnClickListener {
            showExportDialog()
        }
        add(save)

        val share = button(
            "Partilhar imagem editada",
            false
        )
        share.setOnClickListener {
            shareEdited()
        }
        add(share)

        val back = button(
            "Voltar ao resultado",
            false
        )
        back.setOnClickListener {
            showStage3()
        }
        add(back)

        renderEditor()
    }

    private fun backgroundChoices(): View {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val rows = listOf(
            listOf(
                "transparent" to "Transparente",
                "original" to "Original",
                "black" to "Preto",
                "white" to "Branco"
            ),
            listOf(
                "red" to "Vermelho",
                "blue" to "Azul",
                "green" to "Verde",
                "purple" to "Roxo"
            ),
            listOf(
                "orange" to "Laranja",
                "yellow" to "Amarelo",
                "pink" to "Rosa",
                "cyan" to "Ciano"
            ),
            listOf(
                "rainbow" to "Arco-íris",
                "sky" to "Céu",
                "sunset" to "Pôr do sol",
                "inferno" to "Inferno"
            ),
            listOf(
                "beach" to "Praia",
                "tropical" to "Tropical",
                "boat" to "Barco",
                "mountains" to "Montanhas"
            ),
            listOf(
                "city" to "Cidade",
                "forest" to "Floresta",
                "studio" to "Estúdio",
                "space" to "Espaço"
            ),
            listOf(
                "neon" to "Neon",
                "pastel" to "Pastel",
                "gold" to "Dourado",
                "silver" to "Prata"
            )
        )

        rows.forEach { row ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            row.forEach { item ->
                val button = smallChoiceButton(
                    item.second
                )
                button.setOnClickListener {
                    backgroundKey = item.first
                    customBackground = null
                    renderEditor()
                    analytics.event(
                        "background_remover_background",
                        "background" to item.first
                    )
                }
                line.addView(
                    button,
                    LinearLayout.LayoutParams(
                        0,
                        dp(46),
                        1f
                    ).apply {
                        setMargins(
                            dp(3),
                            dp(3),
                            dp(3),
                            dp(3)
                        )
                    }
                )
            }

            outer.addView(line)
        }

        val own = smallChoiceButton(
            "Minha imagem"
        )
        own.setOnClickListener {
            startImagePicker(REQUEST_BACKGROUND)
        }
        outer.addView(
            own,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            ).apply {
                setMargins(
                    dp(3),
                    dp(5),
                    dp(3),
                    dp(3)
                )
            }
        )

        return outer
    }

    private fun adjustmentPanel(): View {
        val panel = card()

        addSeekRow(
            panel,
            "Brilho",
            -100,
            100,
            brightness
        ) {
            brightness = it
            renderEditor()
        }

        addSeekRow(
            panel,
            "Contraste",
            -100,
            100,
            contrast
        ) {
            contrast = it
            renderEditor()
        }

        addSeekRow(
            panel,
            "Saturação",
            -100,
            100,
            saturation
        ) {
            saturation = it
            renderEditor()
        }

        addSeekRow(
            panel,
            "Opacidade",
            20,
            100,
            opacity
        ) {
            opacity = it
            renderEditor()
        }

        val filters = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        listOf(
            "Natural" to 0,
            "Vivo" to 25,
            "P&B" to -100
        ).forEach { item ->
            val b = smallChoiceButton(item.first)
            b.setOnClickListener {
                saturation = item.second
                brightness = 0
                contrast = 0
                renderEditor()
            }
            filters.addView(
                b,
                LinearLayout.LayoutParams(
                    0,
                    dp(44),
                    1f
                ).apply {
                    setMargins(
                        dp(3),
                        dp(4),
                        dp(3),
                        dp(3)
                    )
                }
            )
        }

        panel.addView(filters)
        return panel
    }

    private fun refinePanel(): View {
        val panel = card()

        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val erase = smallChoiceButton("Apagar")
        erase.setOnClickListener {
            brushMode = BrushMode.ERASE
            toast("Pincel: apagar")
        }

        val restore = smallChoiceButton("Restaurar")
        restore.setOnClickListener {
            brushMode = BrushMode.RESTORE
            toast("Pincel: restaurar")
        }

        modeRow.addView(
            erase,
            LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
            ).apply {
                setMargins(
                    0,
                    0,
                    dp(4),
                    dp(4)
                )
            }
        )
        modeRow.addView(
            restore,
            LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
            ).apply {
                setMargins(
                    dp(4),
                    0,
                    0,
                    dp(4)
                )
            }
        )

        panel.addView(modeRow)

        addSeekRow(
            panel,
            "Tamanho do pincel",
            20,
            220,
            brushSize
        ) {
            brushSize = it
            editorSurface?.brushSize = it
        }

        addSeekRow(
            panel,
            "Tamanho do recorte",
            70,
            140,
            scalePercent
        ) {
            scalePercent = it
            renderEditor()
        }

        addSeekRow(
            panel,
            "Posição vertical",
            -30,
            30,
            verticalPercent
        ) {
            verticalPercent = it
            renderEditor()
        }

        return panel
    }

    private fun addSeekRow(
        panel: LinearLayout,
        label: String,
        minValue: Int,
        maxValue: Int,
        initial: Int,
        onChanged: (Int) -> Unit
    ) {
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val labelView = TextView(this).apply {
            textSize = 14f
            setTextColor(textColor)
        }

        val valueView = TextView(this).apply {
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.END
        }

        titleRow.addView(
            labelView,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )
        titleRow.addView(
            valueView,
            LinearLayout.LayoutParams(
                dp(72),
                -2
            )
        )

        val seek = SeekBar(this).apply {
            max = maxValue - minValue
            progress = initial - minValue
        }

        fun renderValue() {
            val value = minValue + seek.progress
            labelView.text = label
            valueView.text = "$value"
        }

        renderValue()

        seek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    bar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val value =
                        minValue + progress
                    valueView.text = "$value"
                    if (fromUser) {
                        onChanged(value)
                    }
                }

                override fun onStartTrackingTouch(
                    bar: SeekBar?
                ) = Unit

                override fun onStopTrackingTouch(
                    bar: SeekBar?
                ) {
                    renderEditor()
                }
            }
        )

        panel.addView(titleRow)
        panel.addView(seek)
    }

    private fun emptySpacer(): View =
        View(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                1,
                dp(1)
            )
        }

    private fun sectionTitle(value: String): TextView =
        TextView(this).apply {
            text = value
            textSize = 18f
            typeface =
                android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            setPadding(
                0,
                dp(10),
                0,
                dp(3)
            )
        }

    private fun renderEditor() {
        val foreground =
            editorForeground ?: return

        val width = foreground.width
        val height = foreground.height

        Thread {
            val canvasBitmap =
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
                )

            val canvas = Canvas(canvasBitmap)

            drawEditorBackground(
                canvas,
                width,
                height
            )

            val paint = foregroundPaint()

            val scaledWidth =
                (width *
                    scalePercent /
                    100f
                ).roundToInt()
            val scaledHeight =
                (height *
                    scalePercent /
                    100f
                ).roundToInt()

            val left =
                (width - scaledWidth) / 2f
            val top =
                (height - scaledHeight) /
                    2f +
                    (height *
                        verticalPercent /
                        100f)

            val sourceRect =
                android.graphics.Rect(
                    0,
                    0,
                    foreground.width,
                    foreground.height
                )

            val targetRect =
                android.graphics.RectF(
                    left,
                    top,
                    left + scaledWidth,
                    top + scaledHeight
                )

            canvas.drawBitmap(
                foreground,
                sourceRect,
                targetRect,
                paint
            )

            runOnUiThread {
                editorImage?.setImageBitmap(canvasBitmap)
            }
        }.start()
    }

    private fun drawEditorBackground(
        canvas: Canvas,
        width: Int,
        height: Int
    ) {
        val key = backgroundKey

        if (key == "transparent") {
            drawCheckerboard(
                canvas,
                width,
                height
            )
            return
        }

        if (key == "original") {
            originalBitmap?.let {
                drawBackgroundBitmap(
                    canvas,
                    it,
                    width,
                    height,
                    blurLevel
                )
            } ?: drawColor(
                canvas,
                Color.WHITE
            )
            return
        }

        if (key == "custom") {
            customBackground?.let {
                drawBackgroundBitmap(
                    canvas,
                    it,
                    width,
                    height,
                    blurLevel
                )
            } ?: drawColor(
                canvas,
                Color.WHITE
            )
            return
        }

        val background =
            createPresetBackground(
                key,
                width,
                height
            )

        if (blurLevel > 0) {
            val blurred =
                fastBlur(
                    background,
                    blurLevel
                )
            canvas.drawBitmap(
                blurred,
                0f,
                0f,
                null
            )
            blurred.recycleIfSafe()
        } else {
            canvas.drawBitmap(
                background,
                0f,
                0f,
                null
            )
        }

        background.recycleIfSafe()
    }

    private fun drawColor(
        canvas: Canvas,
        color: Int
    ) {
        canvas.drawColor(color)
    }

    private fun drawCheckerboard(
        canvas: Canvas,
        width: Int,
        height: Int
    ) {
        val size = dp(24)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        var y = 0
        var row = 0

        while (y < height) {
            var x = 0
            var col = 0

            while (x < width) {
                paint.color =
                    if ((row + col) % 2 == 0) {
                        0xFFE9EDF3.toInt()
                    } else {
                        Color.WHITE
                    }

                canvas.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    (x + size).toFloat(),
                    (y + size).toFloat(),
                    paint
                )

                x += size
                col++
            }

            y += size
            row++
        }
    }

    private fun createPresetBackground(
        key: String,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap =
            Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (key) {
            "black" -> canvas.drawColor(Color.BLACK)
            "white" -> canvas.drawColor(Color.WHITE)

            "red" ->
                canvas.drawColor(
                    Color.rgb(
                        220,
                        38,
                        38
                    )
                )

            "blue" ->
                canvas.drawColor(
                    Color.rgb(
                        37,
                        99,
                        235
                    )
                )

            "green" ->
                canvas.drawColor(
                    Color.rgb(
                        22,
                        163,
                        74
                    )
                )

            "purple" ->
                canvas.drawColor(
                    Color.rgb(
                        122,
                        85,
                        232
                    )
                )

            "orange" ->
                canvas.drawColor(
                    Color.rgb(
                        242,
                        138,
                        56
                    )
                )

            "yellow" ->
                canvas.drawColor(
                    Color.rgb(
                        245,
                        158,
                        11
                    )
                )

            "pink" ->
                canvas.drawColor(
                    Color.rgb(
                        236,
                        72,
                        153
                    )
                )

            "cyan" ->
                canvas.drawColor(
                    Color.rgb(
                        15,
                        157,
                        149
                    )
                )

            "rainbow" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        width.toFloat(),
                        height.toFloat(),
                        intArrayOf(
                            0xFFEF4444.toInt(),
                            0xFFF59E0B.toInt(),
                            0xFFEAB308.toInt(),
                            0xFF22C55E.toInt(),
                            0xFF06B6D4.toInt(),
                            0xFF3B82F6.toInt(),
                            0xFF8B5CF6.toInt()
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
            }

            "pastel" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        width.toFloat(),
                        height.toFloat(),
                        0xFFFFD6E7.toInt(),
                        0xFFD8F3FF.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
            }

            "gold" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        width.toFloat(),
                        height.toFloat(),
                        0xFF5C3B00.toInt(),
                        0xFFFFD56A.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
            }

            "silver" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        width.toFloat(),
                        height.toFloat(),
                        0xFF64748B.toInt(),
                        0xFFE2E8F0.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
            }

            "sky" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        0f,
                        height.toFloat(),
                        0xFF6FD6FF.toInt(),
                        0xFFEFFBFF.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
                paint.color = 0xFFFFFFFF.toInt()
                drawCloud(
                    canvas,
                    width * 0.23f,
                    height * 0.28f,
                    width * 0.15f,
                    paint
                )
                drawCloud(
                    canvas,
                    width * 0.70f,
                    height * 0.20f,
                    width * 0.12f,
                    paint
                )
                paint.color = 0xFFFFE79B.toInt()
                canvas.drawCircle(
                    width * 0.82f,
                    height * 0.20f,
                    width * 0.07f,
                    paint
                )
            }

            "sunset" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        0f,
                        height.toFloat(),
                        0xFF2B1055.toInt(),
                        0xFFFF7A59.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
                paint.color = 0xFFFFD166.toInt()
                canvas.drawCircle(
                    width * 0.72f,
                    height * 0.48f,
                    width * 0.11f,
                    paint
                )
                drawHills(
                    canvas,
                    width,
                    height,
                    0xFF4C1D45.toInt(),
                    0.65f
                )
            }

            "inferno" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        height.toFloat(),
                        0f,
                        0f,
                        0xFF3A0800.toInt(),
                        0xFFFF6A00.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
                paint.color = 0xFFFFC107.toInt()
                drawFlame(
                    canvas,
                    width * 0.18f,
                    height * 0.92f,
                    width * 0.14f,
                    height * 0.42f,
                    paint
                )
                drawFlame(
                    canvas,
                    width * 0.50f,
                    height * 0.98f,
                    width * 0.18f,
                    height * 0.55f,
                    paint
                )
                drawFlame(
                    canvas,
                    width * 0.82f,
                    height * 0.92f,
                    width * 0.13f,
                    height * 0.38f,
                    paint
                )
            }

            "beach" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        0f,
                        height * 0.58f,
                        0xFF70C7FF.toInt(),
                        0xFFE9FBFF.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height * 0.58f,
                    paint
                )
                paint.shader = null
                paint.color = 0xFF39B7D8.toInt()
                canvas.drawRect(
                    0f,
                    height * 0.58f,
                    width.toFloat(),
                    height * 0.78f,
                    paint
                )
                paint.color = 0xFFFFDCA8.toInt()
                canvas.drawRect(
                    0f,
                    height * 0.78f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.color = 0xFFFFEFAF.toInt()
                canvas.drawCircle(
                    width * 0.78f,
                    height * 0.19f,
                    width * 0.08f,
                    paint
                )
            }

            "tropical" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        0f,
                        height.toFloat(),
                        0xFF25C2A0.toInt(),
                        0xFF0A6E4D.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
                drawPalm(
                    canvas,
                    width * 0.12f,
                    height * 0.94f,
                    width * 0.22f,
                    paint
                )
                drawPalm(
                    canvas,
                    width * 0.88f,
                    height * 0.96f,
                    width * 0.26f,
                    paint
                )
                paint.color = 0xFFFFE083.toInt()
                canvas.drawCircle(
                    width * 0.76f,
                    height * 0.18f,
                    width * 0.09f,
                    paint
                )
            }

            "boat" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        0f,
                        height.toFloat(),
                        0xFF7DD3FC.toInt(),
                        0xFF0EA5E9.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
                paint.color = 0xFF0F766E.toInt()
                canvas.drawRect(
                    0f,
                    height * 0.68f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.color = 0xFF78350F.toInt()
                val hull = Path()
                hull.moveTo(
                    width * 0.25f,
                    height * 0.69f
                )
                hull.lineTo(
                    width * 0.74f,
                    height * 0.69f
                )
                hull.lineTo(
                    width * 0.62f,
                    height * 0.82f
                )
                hull.lineTo(
                    width * 0.35f,
                    height * 0.82f
                )
                hull.close()
                canvas.drawPath(
                    hull,
                    paint
                )
                paint.color = Color.WHITE
                canvas.drawRect(
                    width * 0.49f,
                    height * 0.35f,
                    width * 0.51f,
                    height * 0.70f,
                    paint
                )
                val sail = Path()
                sail.moveTo(
                    width * 0.50f,
                    height * 0.37f
                )
                sail.lineTo(
                    width * 0.72f,
                    height * 0.64f
                )
                sail.lineTo(
                    width * 0.51f,
                    height * 0.64f
                )
                sail.close()
                paint.color = 0xFFFFFBEB.toInt()
                canvas.drawPath(
                    sail,
                    paint
                )
            }

            "mountains" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        0f,
                        height.toFloat(),
                        0xFFB8E1FF.toInt(),
                        0xFFEAF5EA.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
                paint.color = 0xFF4B5563.toInt()
                drawMountain(
                    canvas,
                    width * 0.10f,
                    height,
                    width * 0.45f,
                    height * 0.36f,
                    paint
                )
                drawMountain(
                    canvas,
                    width * 0.50f,
                    height,
                    width * 0.48f,
                    height * 0.42f,
                    paint
                )
                paint.color = 0xFF86EFAC.toInt()
                canvas.drawRect(
                    0f,
                    height * 0.72f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
            }

            "city" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        0f,
                        height.toFloat(),
                        0xFF111827.toInt(),
                        0xFF4B5563.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
                drawCity(
                    canvas,
                    width,
                    height,
                    paint
                )
            }

            "forest" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        0f,
                        height.toFloat(),
                        0xFFBBF7D0.toInt(),
                        0xFF14532D.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
                drawForest(
                    canvas,
                    width,
                    height,
                    paint
                )
            }

            "studio" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        0f,
                        height.toFloat(),
                        0xFFF8FAFC.toInt(),
                        0xFFCBD5E1.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
                paint.color = Color.WHITE
                canvas.drawOval(
                    width * 0.15f,
                    height * 0.62f,
                    width * 0.85f,
                    height * 1.15f,
                    paint
                )
            }

            "space" -> {
                paint.shader =
                    RadialGradient(
                        width * 0.50f,
                        height * 0.42f,
                        width * 0.80f,
                        0xFF312E81.toInt(),
                        0xFF020617.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
                paint.color = Color.WHITE
                for (i in 0 until 36) {
                    val x =
                        ((i * 97) %
                            100) *
                            width / 100f
                    val y =
                        ((i * 53) %
                            100) *
                            height / 100f
                    val radius =
                        1f +
                            ((i * 7) % 4)
                    canvas.drawCircle(
                        x,
                        y,
                        radius,
                        paint
                    )
                }
                paint.color = 0xFF60A5FA.toInt()
                canvas.drawCircle(
                    width * 0.76f,
                    height * 0.25f,
                    width * 0.10f,
                    paint
                )
            }

            "neon" -> {
                paint.shader =
                    LinearGradient(
                        0f,
                        0f,
                        width.toFloat(),
                        height.toFloat(),
                        0xFF0F172A.toInt(),
                        0xFF581C87.toInt(),
                        Shader.TileMode.CLAMP
                    )
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.strokeWidth =
                    max(8f, width * 0.012f)
                paint.color = 0xFF22D3EE.toInt()
                canvas.drawCircle(
                    width * 0.30f,
                    height * 0.45f,
                    width * 0.17f,
                    paint
                )
                paint.color = 0xFFF472B6.toInt()
                canvas.drawCircle(
                    width * 0.70f,
                    height * 0.56f,
                    width * 0.20f,
                    paint
                )
                paint.style = Paint.Style.FILL
            }
        }

        return bitmap
    }

    private fun drawCloud(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        paint: Paint
    ) {
        canvas.drawCircle(
            cx,
            cy,
            size * 0.45f,
            paint
        )
        canvas.drawCircle(
            cx + size * 0.42f,
            cy + size * 0.03f,
            size * 0.35f,
            paint
        )
        canvas.drawCircle(
            cx - size * 0.42f,
            cy + size * 0.06f,
            size * 0.33f,
            paint
        )
        canvas.drawRect(
            cx - size * 0.62f,
            cy + size * 0.05f,
            cx + size * 0.62f,
            cy + size * 0.42f,
            paint
        )
    }

    private fun drawHills(
        canvas: Canvas,
        width: Int,
        height: Int,
        color: Int,
        level: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        val path = Path()
        path.moveTo(
            0f,
            height.toFloat()
        )
        path.lineTo(
            0f,
            height * level
        )
        path.quadTo(
            width * 0.25f,
            height * 0.47f,
            width * 0.50f,
            height * level
        )
        path.quadTo(
            width * 0.76f,
            height * 0.46f,
            width.toFloat(),
            height * level
        )
        path.lineTo(
            width.toFloat(),
            height.toFloat()
        )
        path.close()
        canvas.drawPath(
            path,
            paint
        )
    }

    private fun drawFlame(
        canvas: Canvas,
        cx: Float,
        base: Float,
        width: Float,
        height: Float,
        paint: Paint
    ) {
        val path = Path()
        path.moveTo(
            cx,
            base
        )
        path.cubicTo(
            cx - width,
            base - height * 0.25f,
            cx - width * 0.65f,
            base - height * 0.72f,
            cx,
            base - height
        )
        path.cubicTo(
            cx + width * 0.10f,
            base - height * 0.63f,
            cx + width,
            base - height * 0.53f,
            cx,
            base
        )
        path.close()
        canvas.drawPath(
            path,
            paint
        )
    }

    private fun drawPalm(
        canvas: Canvas,
        x: Float,
        base: Float,
        size: Float,
        paint: Paint
    ) {
        paint.color = 0xFF5B3A29.toInt()
        paint.strokeWidth = max(
            6f,
            size * 0.07f
        )
        canvas.drawLine(
            x,
            base,
            x + size * 0.10f,
            base - size,
            paint
        )

        paint.color = 0xFF166534.toInt()
        val topX =
            x + size * 0.10f
        val topY =
            base - size
        repeat(6) { i ->
            val angle =
                -150f + i * 32f
            val rad =
                Math.toRadians(angle.toDouble())
            canvas.drawLine(
                topX,
                topY,
                topX +
                    (
                        kotlin.math.cos(rad) *
                            size *
                            0.55f
                        ).toFloat(),
                topY +
                    (
                        kotlin.math.sin(rad) *
                            size *
                            0.55f
                        ).toFloat(),
                paint
            )
        }
    }

    private fun drawMountain(
        canvas: Canvas,
        startX: Float,
        groundY: Int,
        width: Float,
        peakHeight: Float,
        paint: Paint
    ) {
        val path = Path()
        path.moveTo(
            startX,
            groundY.toFloat()
        )
        path.lineTo(
            startX + width * 0.50f,
            groundY - peakHeight
        )
        path.lineTo(
            startX + width,
            groundY.toFloat()
        )
        path.close()
        canvas.drawPath(
            path,
            paint
        )
    }

    private fun drawCity(
        canvas: Canvas,
        width: Int,
        height: Int,
        paint: Paint
    ) {
        val base = height * 0.88f
        for (i in 0 until 10) {
            val buildingWidth =
                width * (0.07f + (i % 3) * 0.02f)
            val x =
                width * i / 10f
            val top =
                base -
                    height *
                    (0.18f + (i % 4) * 0.10f)

            paint.color =
                when (i % 3) {
                    0 -> 0xFF1F2937.toInt()
                    1 -> 0xFF334155.toInt()
                    else -> 0xFF475569.toInt()
                }

            canvas.drawRect(
                x,
                top,
                x + buildingWidth,
                base,
                paint
            )

            paint.color = 0xFFFDE68A.toInt()
            for (w in 1..2) {
                for (h in 1..4) {
                    val wx =
                        x +
                            buildingWidth *
                            (0.20f + w * 0.25f)
                    val wy =
                        top +
                            (base - top) *
                            (0.16f + h * 0.18f)
                    canvas.drawRect(
                        wx,
                        wy,
                        wx + width * 0.008f,
                        wy + height * 0.014f,
                        paint
                    )
                }
            }
        }
    }

    private fun drawForest(
        canvas: Canvas,
        width: Int,
        height: Int,
        paint: Paint
    ) {
        paint.color = 0xFF166534.toInt()
        for (i in 0 until 11) {
            val x =
                width * i / 10f
            val treeHeight =
                height *
                    (0.32f + (i % 4) * 0.08f)

            canvas.drawRect(
                x - width * 0.012f,
                height * 0.76f,
                x + width * 0.012f,
                height.toFloat(),
                paint
            )

            val path = Path()
            path.moveTo(
                x,
                height * 0.12f +
                    height * 0.03f *
                    (i % 3)
            )
            path.lineTo(
                x - width * 0.10f,
                height * 0.72f
            )
            path.lineTo(
                x + width * 0.10f,
                height * 0.72f
            )
            path.close()
            canvas.drawPath(
                path,
                paint
            )

            paint.color =
                0xFF15803D.toInt()
            canvas.drawCircle(
                x,
                height * 0.76f -
                    treeHeight * 0.18f,
                width * 0.11f,
                paint
            )
        }
    }

    private fun drawBackgroundBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        width: Int,
        height: Int,
        blur: Int
    ) {
        val fitted =
            fitBackground(
                bitmap,
                Pair(
                    width,
                    height
                )
            )

        val output =
            if (blur > 0) {
                fastBlur(
                    fitted,
                    blur
                )
            } else {
                fitted
            }

        canvas.drawBitmap(
            output,
            0f,
            0f,
            null
        )

        if (output !== fitted) {
            output.recycleIfSafe()
        }
        fitted.recycleIfSafe()
    }

    private fun fitBackground(
        source: Bitmap,
        size: Pair<Int, Int>
    ): Bitmap {
        val width = size.first
        val height = size.second

        val scale =
            max(
                width.toFloat() /
                    source.width,
                height.toFloat() /
                    source.height
            )

        val scaledWidth =
            (source.width * scale)
                .roundToInt()

        val scaledHeight =
            (source.height * scale)
                .roundToInt()

        val scaled =
            Bitmap.createScaledBitmap(
                source,
                scaledWidth,
                scaledHeight,
                true
            )

        val left =
            max(
                0,
                (scaledWidth - width) / 2
            )
        val top =
            max(
                0,
                (scaledHeight - height) / 2
            )

        return Bitmap.createBitmap(
            scaled,
            left,
            top,
            width,
            height
        ).also {
            scaled.recycleIfSafe()
        }
    }

    private fun editorCanvasSize(): Pair<Int, Int> {
        val foreground =
            editorForeground
                ?: return Pair(
                    dp(1080),
                    dp(1080)
                )
        return Pair(
            foreground.width,
            foreground.height
        )
    }

    private fun fastBlur(
        source: Bitmap,
        level: Int
    ): Bitmap {
        if (level <= 0) {
            return source.copy(
                Bitmap.Config.ARGB_8888,
                false
            )
        }

        val sample =
            max(
                2,
                1 + level * 2
            )

        val smallWidth =
            max(
                32,
                source.width / sample
            )
        val smallHeight =
            max(
                32,
                source.height / sample
            )

        val small =
            Bitmap.createScaledBitmap(
                source,
                smallWidth,
                smallHeight,
                true
            )

        val output =
            Bitmap.createScaledBitmap(
                small,
                source.width,
                source.height,
                true
            )

        small.recycleIfSafe()
        return output
    }

    private fun foregroundPaint(): Paint {
        val saturationValue =
            1f +
                saturation /
                100f

        val scale =
            1f +
                contrast /
                100f

        val translate =
            brightness *
                2.55f +
                (1f - scale) *
                128f

        val matrix =
            ColorMatrix(
                floatArrayOf(
                    scale * saturationValue,
                    0f,
                    0f,
                    0f,
                    translate,
                    0f,
                    scale * saturationValue,
                    0f,
                    0f,
                    translate,
                    0f,
                    0f,
                    scale * saturationValue,
                    0f,
                    translate,
                    0f,
                    0f,
                    0f,
                    opacity / 100f,
                    0f
                )
            )

        return Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.FILTER_BITMAP_FLAG
        ).apply {
            colorFilter =
                ColorMatrixColorFilter(
                    matrix
                )
        }
    }

    private fun showExportDialog() {
        if (
            backgroundKey ==
            "transparent"
        ) {
            AlertDialog.Builder(this)
                .setTitle(
                    "Salvar transparente"
                )
                .setItems(
                    arrayOf(
                        "PNG • máxima qualidade • sem perdas"
                    )
                ) { _, _ ->
                    exportAndSave(
                        ExportFormat.PNG,
                        100
                    )
                }
                .show()
            return
        }

        val labels =
            arrayOf(
                "PNG • máxima qualidade",
                "JPG • 100% • máxima qualidade",
                "JPG • 90% • qualidade alta",
                "JPG • 80% • arquivo menor"
            )

        AlertDialog.Builder(this)
            .setTitle(
                "Como deseja salvar?"
            )
            .setItems(
                labels
            ) { _, which ->
                val format =
                    if (which == 0) {
                        ExportFormat.PNG
                    } else {
                        ExportFormat.JPG
                    }

                val quality =
                    when (which) {
                        0 -> 100
                        1 -> 100
                        2 -> 90
                        else -> 80
                    }

                exportAndSave(
                    format,
                    quality
                )
            }
            .show()
    }

    private fun exportAndSave(
        format: ExportFormat,
        quality: Int
    ) {
        Thread {
            try {
                val bitmap =
                    renderFinalBitmap()

                val extension =
                    if (
                        format ==
                        ExportFormat.PNG
                    ) {
                        "png"
                    } else {
                        "jpg"
                    }

                val temp =
                    File.createTempFile(
                        "toolnexa-edited-",
                        ".$extension",
                        cacheDir
                    )

                temp.outputStream().use {
                    bitmap.compress(
                        if (
                            format ==
                            ExportFormat.PNG
                        ) {
                            Bitmap.CompressFormat.PNG
                        } else {
                            Bitmap.CompressFormat.JPEG
                        },
                        quality,
                        it
                    )
                }

                bitmap.recycleIfSafe()

                runOnUiThread {
                    saveEditedTemp(
                        temp,
                        extension
                    )
                }
            } catch (error: Exception) {
                runOnUiThread {
                    toast(
                        error.message
                            ?: "Não foi possível exportar."
                    )
                }
            }
        }.start()
    }

    private fun saveEditedTemp(
        file: File,
        extension: String
    ) {
        if (!NexaurenStorage.hasRoot(this)) {
            AlertDialog.Builder(this)
                .setTitle(
                    "Diretório não definido"
                )
                .setMessage(
                    "Escolha o diretório Nexauren X em " +
                        "Definições antes de salvar."
                )
                .setNegativeButton(
                    "Fechar",
                    null
                )
                .setPositiveButton(
                    "Abrir Definições"
                ) { _, _ ->
                    finish()
                    startActivity(
                        Intent(
                            this,
                            MainActivity::class.java
                        ).apply {
                            putExtra(
                                "open_screen",
                                "settings"
                            )
                        }
                    )
                }
                .show()
            return
        }

        val name =
            "ToolNexa-Background-" +
                System.currentTimeMillis() +
                ".$extension"

        val saved =
            NexaurenStorage.save(
                this,
                "Imagem",
                "Background Remover",
                file,
                name
            )

        if (saved != null) {
            NexaurenHistory.add(
                this,
                "Background Remover",
                saved,
                name,
                file.length()
            )
            analytics.event(
                "background_remover_editor_save",
                "format" to extension
            )
            toast(
                "Imagem salva com qualidade selecionada."
            )
        } else {
            analytics.event(
                "background_remover_editor_save_failed"
            )
            toast(
                "Não foi possível salvar a imagem."
            )
        }

        file.delete()
    }

    private fun shareEdited() {
        Thread {
            try {
                val bitmap =
                    renderFinalBitmap()

                val file =
                    File.createTempFile(
                        "toolnexa-share-",
                        ".png",
                        cacheDir
                    )

                file.outputStream().use {
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        it
                    )
                }
                bitmap.recycleIfSafe()

                val uri =
                    NexaurenStorage.fileProviderUri(
                        this,
                        file
                    )

                runOnUiThread {
                    startActivity(
                        Intent.createChooser(
                            Intent(
                                Intent.ACTION_SEND
                            ).apply {
                                type = "image/png"
                                putExtra(
                                    Intent.EXTRA_STREAM,
                                    uri
                                )
                                addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            },
                            "Partilhar imagem"
                        )
                    )

                    analytics.event(
                        "background_remover_editor_share"
                    )
                }
            } catch (error: Exception) {
                runOnUiThread {
                    toast(
                        error.message
                            ?: "Não foi possível partilhar."
                    )
                }
            }
        }.start()
    }

    private fun renderFinalBitmap(): Bitmap {
        val foreground =
            editorForeground?.copy(
                Bitmap.Config.ARGB_8888,
                false
            ) ?: throw IllegalStateException(
                "O recorte não está disponível."
            )

        val width = foreground.width
        val height = foreground.height

        val output =
            Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )
        val canvas = Canvas(output)

        drawEditorBackground(
            canvas,
            width,
            height
        )

        val scaledWidth =
            (width *
                scalePercent /
                100f
            ).roundToInt()
        val scaledHeight =
            (height *
                scalePercent /
                100f
            ).roundToInt()

        val left =
            (width - scaledWidth) / 2f
        val top =
            (height - scaledHeight) /
                2f +
                (height *
                    verticalPercent /
                    100f)

        canvas.drawBitmap(
            foreground,
            android.graphics.Rect(
                0,
                0,
                foreground.width,
                foreground.height
            ),
            android.graphics.RectF(
                left,
                top,
                left + scaledWidth,
                top + scaledHeight
            ),
            foregroundPaint()
        )

        foreground.recycleIfSafe()
        return output
    }

    private fun decodeEditorForeground(
        file: File
    ): Bitmap {
        val bounds =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

        BitmapFactory.decodeFile(
            file.absolutePath,
            bounds
        )

        val sample =
            computeSample(
                bounds.outWidth,
                bounds.outHeight,
                3200
            )

        val options =
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig =
                    Bitmap.Config.ARGB_8888
            }

        return BitmapFactory.decodeFile(
            file.absolutePath,
            options
        ) ?: throw IllegalStateException(
            "Não foi possível carregar o resultado."
        )
    }

    private fun loadBitmap(
        uri: Uri
    ): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)
                ?.use {
                    BitmapFactory.decodeStream(it)
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun computeSample(
        width: Int,
        height: Int,
        maxDimension: Int
    ): Int {
        var sample = 1
        val longest =
            max(
                width,
                height
            )

        while (
            longest / sample >
            maxDimension
        ) {
            sample *= 2
        }

        return sample
    }

    private fun saveResult(file: File) {
        if (!NexaurenStorage.hasRoot(this)) {
            AlertDialog.Builder(this)
                .setTitle(
                    "Diretório não definido"
                )
                .setMessage(
                    "Escolha o diretório Nexauren X em " +
                        "Definições antes de salvar."
                )
                .setNegativeButton(
                    "Fechar",
                    null
                )
                .setPositiveButton(
                    "Abrir Definições"
                ) { _, _ ->
                    finish()
                    startActivity(
                        Intent(
                            this,
                            MainActivity::class.java
                        ).apply {
                            putExtra(
                                "open_screen",
                                "settings"
                            )
                        }
                    )
                }
                .show()
            return
        }

        val name =
            "ToolNexa-" +
                System.currentTimeMillis() +
                ".png"

        val saved =
            NexaurenStorage.save(
                this,
                "Imagem",
                "Background Remover",
                file,
                name
            )

        if (saved != null) {
            NexaurenHistory.add(
                this,
                "Background Remover",
                saved,
                name,
                file.length()
            )
            analytics.event(
                "background_remover_save_success"
            )
            toast("Resultado salvo.")
        } else {
            analytics.event(
                "background_remover_save_failed"
            )
            toast(
                "Não foi possível salvar o resultado."
            )
        }
    }

    private fun shareResult(file: File) {
        val uri =
            NexaurenStorage.fileProviderUri(
                this,
                file
            )

        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                },
                "Partilhar resultado"
            )
        )

        analytics.event(
            "background_remover_share"
        )
    }

    private fun processingDialog(): AlertDialog {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(24),
                dp(20),
                dp(24),
                dp(20)
            )
        }

        box.addView(
            TextView(this).apply {
                text = "ToolNexa AI"
                textSize = 18f
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(textColor)
            }
        )

        val label = TextView(this).apply {
            text = "A preparar a imagem..."
            textSize = 15f
            setTextColor(muted)
            setPadding(
                0,
                dp(8),
                0,
                dp(10)
            )
        }

        box.addView(label)
        box.addView(ProgressBar(this))

        val dialog =
            AlertDialog.Builder(this)
                .setView(box)
                .setCancelable(false)
                .create()

        dialog.setOnShowListener {
            val messages = arrayOf(
                "A validar a imagem...",
                "A enviar para o processamento seguro...",
                "A remover o fundo com BiRefNet...",
                "A preparar o PNG transparente..."
            )

            var index = 0
            val handler =
                Handler(Looper.getMainLooper())

            val task = object : Runnable {
                override fun run() {
                    if (!dialog.isShowing) {
                        return
                    }

                    label.text =
                        messages[
                            index % messages.size
                        ]

                    index++

                    handler.postDelayed(
                        this,
                        900
                    )
                }
            }

            handler.post(task)
        }

        dialog.show()
        return dialog
    }

    private fun infoCard(
        titleText: String,
        message: String
    ): LinearLayout {
        val box = card()
        val header = title(
            titleText,
            12f
        )
        header.setTextColor(blue)
        box.addView(header)
        box.addView(
            bodyText(
                message
            )
        )
        return box
    }

    private fun card(): LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
            )
            background =
                android.graphics.drawable
                    .GradientDrawable()
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

    private fun buildBase(label: String) {
        root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setBackgroundColor(bg)
            }

        val header =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(18),
                    dp(16),
                    dp(18),
                    dp(12)
                )
                setBackgroundColor(surface)
            }

        header.addView(
            title(
                "ToolNexa",
                20f
            )
        )

        header.addView(
            TextView(this).apply {
                text = label
                textSize = 13f
                setTextColor(blue)
            }
        )

        root!!.addView(header)

        val rail =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        listOf(
            R.color.toolnexa_green,
            R.color.toolnexa_blue,
            R.color.toolnexa_red,
            R.color.toolnexa_yellow
        ).forEach { res ->
            rail.addView(
                View(this).apply {
                    setBackgroundColor(
                        getColor(res)
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(4),
                    1f
                )
            )
        }

        root!!.addView(
            rail,
            LinearLayout.LayoutParams(
                -1,
                dp(4)
            )
        )

        val scroll = ScrollView(this)
        val body =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(16),
                    dp(14),
                    dp(16),
                    dp(30)
                )
            }

        scroll.addView(body)
        root!!.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun body(): LinearLayout? =
        (
            root?.getChildAt(2)
                as? ScrollView
            )?.getChildAt(0)
                as? LinearLayout

    private fun add(view: View) {
        body()?.addView(
            view,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    0,
                    dp(6),
                    0,
                    dp(6)
                )
            }
        )
    }

    private fun addTitle(value: String) {
        add(
            title(
                value,
                28f
            )
        )
    }

    private fun addText(value: String) {
        add(
            bodyText(
                value
            )
        )
    }

    private fun title(
        value: String,
        size: Float
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            typeface =
                android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(
                textColor
            )
            setPadding(
                0,
                dp(4),
                0,
                dp(6)
            )
        }

    private fun bodyText(
        value: String
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(
                muted
            )
            setPadding(
                0,
                dp(3),
                0,
                dp(6)
            )
        }

    private fun button(
        label: String,
        primary: Boolean
    ): Button =
        Button(this).apply {
            text = label
            textSize = 15f
            minHeight = dp(52)
            stateListAnimator = null
            setAllCaps(false)
            setTextColor(
                if (primary) {
                    Color.WHITE
                } else {
                    blue
                }
            )
            background =
                android.graphics.drawable
                    .GradientDrawable()
                    .apply {
                        setColor(
                            if (primary) {
                                blue
                            } else {
                                Color.TRANSPARENT
                            }
                        )

                        if (!primary) {
                            setStroke(
                                dp(1),
                                blue
                            )
                        }

                        cornerRadius =
                            dp(14).toFloat()
                    }
        }

    private fun smallChoiceButton(
        label: String
    ): Button =
        Button(this).apply {
            text = label
            textSize = 12f
            minHeight = dp(44)
            setAllCaps(false)
            stateListAnimator = null
            setTextColor(blue)
            background =
                android.graphics.drawable
                    .GradientDrawable()
                    .apply {
                        setColor(
                            Color.WHITE
                        )
                        setStroke(
                            dp(1),
                            border
                        )
                        cornerRadius =
                            dp(12).toFloat()
                    }
        }

    private fun displayName(
        uri: Uri
    ): String =
        try {
            contentResolver.query(
                uri,
                arrayOf(
                    android.provider
                        .OpenableColumns
                        .DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    "Imagem selecionada"
                }
            } ?: "Imagem selecionada"
        } catch (_: Exception) {
            "Imagem selecionada"
        }

    private fun querySize(
        uri: Uri
    ): Long =
        try {
            contentResolver.query(
                uri,
                arrayOf(
                    android.provider
                        .OpenableColumns
                        .SIZE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(0)
                } else {
                    0L
                }
            } ?: 0L
        } catch (_: Exception) {
            0L
        }

    private fun formatBytes(
        bytes: Long
    ): String {
        if (bytes <= 0) return "—"

        return when {
            bytes >=
                1024L * 1024L ->
                String.format(
                    Locale.US,
                    "%.2f MB",
                    bytes /
                        1024.0 /
                        1024.0
                )

            bytes >= 1024L ->
                String.format(
                    Locale.US,
                    "%.1f KB",
                    bytes /
                        1024.0
                )

            else ->
                "$bytes B"
        }
    }

    private fun checkerboard(): Drawable {
        val size = dp(18)

        return object : Drawable() {
            private val paint =
                Paint(
                    Paint.ANTI_ALIAS_FLAG
                )

            override fun draw(
                canvas: Canvas
            ) {
                var y = bounds.top
                var row = 0

                while (
                    y <
                    bounds.bottom
                ) {
                    var x = bounds.left
                    var col = 0

                    while (
                        x <
                        bounds.right
                    ) {
                        paint.color =
                            if (
                                (row + col) %
                                2 == 0
                            ) {
                                0xFFE9EDF3.toInt()
                            } else {
                                Color.WHITE
                            }

                        canvas.drawRect(
                            x.toFloat(),
                            y.toFloat(),
                            (
                                x + size
                            ).toFloat(),
                            (
                                y + size
                            ).toFloat(),
                            paint
                        )

                        x += size
                        col++
                    }

                    y += size
                    row++
                }
            }

            override fun setAlpha(
                alpha: Int
            ) = Unit

            override fun setColorFilter(
                colorFilter:
                    android.graphics
                        .ColorFilter?
            ) = Unit

            override fun getOpacity(): Int =
                PixelFormat.OPAQUE
        }
    }

    private fun dp(
        value: Int
    ): Int =
        (
            value *
                resources
                    .displayMetrics
                    .density
            ).roundToInt()

    private fun toast(
        message: String
    ) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private inner class EditorSurface(
        context: android.content.Context
    ) : View(context) {

        var brushSize: Int = 70

        private val brushPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )
        private var lastX = 0f
        private var lastY = 0f

        fun handleTouch(
            event: MotionEvent
        ): Boolean {
            if (
                editorForeground == null
            ) {
                return false
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    applyBrush(
                        event.x,
                        event.y
                    )
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx =
                        event.x - lastX
                    val dy =
                        event.y - lastY

                    val distance =
                        kotlin.math.sqrt(
                            dx * dx +
                                dy * dy
                        )

                    val steps =
                        max(
                            1,
                            (
                                distance /
                                    max(
                                        4f,
                                        brushSize /
                                            3f
                                    )
                                ).roundToInt()
                        )

                    for (
                        i in 1..steps
                    ) {
                        val x =
                            lastX +
                                dx *
                                i /
                                steps
                        val y =
                            lastY +
                                dy *
                                i /
                                steps

                        applyBrush(
                            x,
                            y
                        )
                    }

                    lastX = event.x
                    lastY = event.y
                    return true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    renderEditor()
                    return true
                }
            }

            return true
        }

        private fun applyBrush(
            x: Float,
            y: Float
        ) {
            val bitmap =
                editorForeground
                    ?: return

            val preview =
                editorImage
                    ?: return

            val contentWidth =
                (
                    preview.width -
                        preview.paddingLeft -
                        preview.paddingRight
                    ).toFloat()

            val contentHeight =
                (
                    preview.height -
                        preview.paddingTop -
                        preview.paddingBottom
                    ).toFloat()

            if (
                contentWidth <= 0f ||
                contentHeight <= 0f
            ) {
                return
            }

            val scale =
                min(
                    contentWidth /
                        bitmap.width,
                    contentHeight /
                        bitmap.height
                )

            val drawWidth =
                bitmap.width * scale
            val drawHeight =
                bitmap.height * scale

            val left =
                preview.paddingLeft +
                    (
                        contentWidth -
                            drawWidth
                        ) / 2f
            val top =
                preview.paddingTop +
                    (
                        contentHeight -
                            drawHeight
                        ) / 2f

            val bx =
                (
                    x - left
                    ) / scale
            val by =
                (
                    y - top
                    ) / scale

            if (
                bx < 0 ||
                by < 0 ||
                bx >= bitmap.width ||
                by >= bitmap.height
            ) {
                return
            }

            val canvas =
                Canvas(bitmap)

            val radius =
                brushSize /
                    scale /
                    2f

            if (
                brushMode ==
                BrushMode.ERASE
            ) {
                brushPaint.xfermode =
                    android.graphics
                        .PorterDuffXfermode(
                            android.graphics
                                .PorterDuff
                                .Mode.CLEAR
                        )

                canvas.drawCircle(
                    bx,
                    by,
                    radius,
                    brushPaint
                )

                brushPaint.xfermode = null
            } else {
                val original =
                    baseForeground
                        ?: return

                canvas.save()

                val path = Path()
                path.addCircle(
                    bx,
                    by,
                    radius,
                    Path.Direction.CW
                )

                canvas.clipPath(
                    path
                )

                canvas.drawBitmap(
                    original,
                    0f,
                    0f,
                    null
                )

                canvas.restore()
            }

            renderEditor()
        }
    }

    private enum class ExportFormat {
        PNG,
        JPG
    }

    private fun Bitmap.recycleIfSafe() {
        if (!isRecycled && !isMutable) {
            return
        }
        if (!isRecycled) {
            recycle()
        }
    }

    companion object {
        private const val REQUEST_PICK = 801
        private const val REQUEST_BACKGROUND = 802
    }
}
