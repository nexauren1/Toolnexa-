package com.toolnexa.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import kotlin.math.roundToInt

class BackgroundRemoverActivity : Activity() {

    private val analytics by lazy { AnalyticsTracker(this) }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var selectedUri: Uri? = null
    private var resultFile: File? = null
    private var root: LinearLayout? = null
    private var preview: ImageView? = null

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
        analytics.screen("background_remover_stage_1")
        showStage1()
    }

    private fun showStage1() {
        buildBase("1 / 3  •  Escolher imagem")
        addTitle("Background Remover")
        addText(
            "Remova o fundo automaticamente e receba " +
                "um PNG com transparência real."
        )

        add(infoCard(
            "PROCESSAMENTO REAL",
            "Cloudflare Images + BiRefNet • processamento " +
                "no backend do ToolNexa."
        ))

        val input = card()
        input.addView(title("Começar com uma imagem", 18f))
        input.addView(bodyText(
            "JPG, PNG, WebP e formatos compatíveis. " +
                "Limite de 20 MB."
        ))

        val choose = button("Escolher imagem", true)
        choose.setOnClickListener {
            analytics.event("background_remover_picker")
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
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

        add(infoCard(
            "PRIVACIDADE",
            "A imagem é enviada somente para o processamento " +
                "solicitado e o resultado é devolvido ao aplicativo."
        ))
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
            resultCode != RESULT_OK
        ) {
            return
        }

        val uri = data?.data ?: return
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
    }

    private fun showStage2() {
        buildBase("2 / 3  •  Preparar processamento")
        addTitle("Confirmar imagem")
        addText(
            "Revise a imagem antes de enviar para o motor " +
                "de remoção de fundo."
        )

        val uri = selectedUri ?: return showStage1()
        val previewCard = card()

        preview = ImageView(this).apply {
            scaleType =
                ImageView.ScaleType.CENTER_INSIDE
            adjustViewBounds = true
            background = checkerboard()
            contentDescription =
                "Prévia da imagem selecionada"
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        previewCard.addView(
            preview,
            LinearLayout.LayoutParams(
                -1,
                dp(310)
            )
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
            val bitmap =
                contentResolver
                    .openInputStream(uri)
                    ?.use {
                        BitmapFactory.decodeStream(it)
                    }

            runOnUiThread {
                if (bitmap != null) {
                    preview?.setImageBitmap(bitmap)
                }
            }
        }.start()

        add(infoCard(
            "MOTOR",
            "Segmentação de foreground no Cloudflare Images. " +
                "O resultado é gerado como PNG transparente."
        ))

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
                        val file =
                            CloudflareApi.removeBackground(
                                this,
                                uri,
                                token
                            )

                        runOnUiThread {
                            dialog.dismiss()
                            resultFile = file
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

        buildBase("3 / 3  •  Resultado")
        addTitle("Fundo removido")
        addText(
            "O processamento terminou. O resultado é um " +
                "PNG com transparência."
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
            LinearLayout.LayoutParams(
                -1,
                dp(330)
            )
        )

        BitmapFactory.decodeFile(file.absolutePath)
            ?.let { image.setImageBitmap(it) }

        add(resultCard)

        add(infoCard(
            "DETALHES",
            "Original: " +
                formatBytes(
                    selectedUri?.let { querySize(it) } ?: 0L
                ) +
                "\nResultado: " +
                formatBytes(file.length()) +
                "\nFormato: PNG com alpha/transparência" +
                "\nMotor: Cloudflare Images / BiRefNet"
        ))

        val save = button("Salvar em Nexauren X", true)
        save.setOnClickListener { saveResult(file) }
        add(save)

        val share = button("Partilhar resultado", false)
        share.setOnClickListener { shareResult(file) }
        add(share)

        val again = button(
            "Processar outra imagem",
            false
        )
        again.setOnClickListener {
            selectedUri = null
            resultFile = null
            showStage1()
        }
        add(again)
    }

    private fun saveResult(file: File) {
        if (!NexaurenStorage.hasRoot(this)) {
            AlertDialog.Builder(this)
                .setTitle("Diretório não definido")
                .setMessage(
                    "Escolha o diretório Nexauren X em " +
                        "Definições antes de salvar."
                )
                .setNegativeButton("Fechar", null)
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
                    putExtra(Intent.EXTRA_STREAM, uri)
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
        val header = title(titleText, 12f)
        header.setTextColor(blue)
        box.addView(header)
        box.addView(bodyText(message))
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

        header.addView(title("ToolNexa", 20f))
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
                    setBackgroundColor(getColor(res))
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
            LinearLayout.LayoutParams(-1, dp(4))
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
            root?.getChildAt(2) as? ScrollView
            )?.getChildAt(0) as? LinearLayout

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
        add(title(value, 28f))
    }

    private fun addText(value: String) {
        add(bodyText(value))
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
            setTextColor(textColor)
            setPadding(
                0,
                dp(4),
                0,
                dp(6)
            )
        }

    private fun bodyText(value: String): TextView =
        TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(muted)
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
            setTextColor(
                if (primary) {
                    android.graphics.Color.WHITE
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
                                android.graphics.Color.TRANSPARENT
                            }
                        )

                        if (!primary) {
                            setStroke(dp(1), blue)
                        }

                        cornerRadius =
                            dp(14).toFloat()
                    }
        }

    private fun displayName(uri: Uri): String =
        try {
            contentResolver.query(
                uri,
                arrayOf(
                    android.provider.OpenableColumns
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

    private fun querySize(uri: Uri): Long =
        try {
            contentResolver.query(
                uri,
                arrayOf(
                    android.provider.OpenableColumns.SIZE
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

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "—"

        return when {
            bytes >= 1024L * 1024L ->
                String.format(
                    java.util.Locale.US,
                    "%.2f MB",
                    bytes / 1024.0 / 1024.0
                )

            bytes >= 1024L ->
                String.format(
                    java.util.Locale.US,
                    "%.1f KB",
                    bytes / 1024.0
                )

            else ->
                "$bytes B"
        }
    }

    private fun checkerboard(): Drawable {
        val size = dp(18)

        return object : Drawable() {
            private val paint =
                Paint(Paint.ANTI_ALIAS_FLAG)

            override fun draw(canvas: Canvas) {
                var y = bounds.top
                var row = 0

                while (y < bounds.bottom) {
                    var x = bounds.left
                    var col = 0

                    while (x < bounds.right) {
                        paint.color =
                            if ((row + col) % 2 == 0) {
                                0xFFE9EDF3.toInt()
                            } else {
                                0xFFFFFFFF.toInt()
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

            override fun setAlpha(alpha: Int) = Unit

            override fun setColorFilter(
                colorFilter:
                    android.graphics.ColorFilter?
            ) = Unit

            override fun getOpacity(): Int =
                PixelFormat.OPAQUE
        }
    }

    private fun dp(value: Int): Int =
        (
            value *
                resources.displayMetrics.density
            ).roundToInt()

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        private const val REQUEST_PICK = 801
    }
}
