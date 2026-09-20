package com.toolnexa.app

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class BackgroundRemoverActivity : Activity() {

    private val analytics by lazy {
        AnalyticsTracker(this)
    }

    private var selectedUri: Uri? = null
    private var originalBitmap: Bitmap? = null
    private var resultBitmap: Bitmap? = null

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
        analytics.screen("background_remover")
        showPicker()
    }

    private fun showPicker() {
        val root = base()
        root.addView(
            title(
                "Background Remover",
                29f
            )
        )
        root.addView(
            text(
                "Remova o fundo da imagem com segmentação por IA e receba um PNG transparente."
            )
        )

        val hero = card()
        hero.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(18)
        )

        hero.addView(
            label("CLOUDFLARE AI • BIRE FNET")
        )
        hero.addView(
            title(
                "Processamento real no servidor",
                20f
            )
        )
        hero.addView(
            text(
                "A imagem é enviada ao Worker ToolNexa e processada pelo pipeline de segmentação da Cloudflare Images, que usa BiRefNet."
            )
        )

        val facts = LinearLayout(this)
        facts.orientation = LinearLayout.VERTICAL

        listOf(
            "Entrada: JPG, PNG, WebP e formatos suportados",
            "Limite: 20 MB por imagem",
            "Saída: PNG com transparência",
            "Pré-visualização antes de guardar"
        ).forEach {
            facts.addView(
                text("• " + it)
            )
        }

        hero.addView(facts)

        root.addView(
            hero,
            margin(12)
        )

        val choose = button(
            "Escolher imagem",
            true
        )

        choose.setOnClickListener {
            analytics.event(
                "background_remover_pick"
            )

            startActivityForResult(
                Intent(
                    Intent.ACTION_OPEN_DOCUMENT
                ).apply {
                    type = "image/*"
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

        root.addView(
            choose,
            margin(12)
        )

        root.addView(
            text(
                "Dica: imagens com o sujeito bem separado do fundo tendem a produzir resultados mais limpos."
            ),
            margin(8)
        )

        setContentView(root)
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
            "background_remover_selected"
        )

        thread {
            val bitmap =
                contentResolver.openInputStream(uri)
                    ?.use {
                        BitmapFactory.decodeStream(it)
                    }

            runOnUiThread {
                if (bitmap == null) {
                    toast(
                        "Não foi possível ler a imagem."
                    )
                    return@runOnUiThread
                }

                originalBitmap = bitmap
                showEditor()
            }
        }
    }

    private fun showEditor() {
        val bitmap =
            originalBitmap ?: return

        val root = base()

        root.addView(
            progressHeader(
                "1 / 3",
                "Imagem selecionada"
            )
        )

        root.addView(
            title(
                "Prepare o processamento",
                25f
            )
        )

        root.addView(
            text(
                "Confira a imagem antes de enviar. O original não é alterado."
            )
        )

        val preview = imagePreview(bitmap)
        root.addView(
            preview,
            margin(10)
        )

        val meta = card()
        meta.setPadding(
            dp(16),
            dp(14),
            dp(16),
            dp(14)
        )
        meta.addView(
            label("ARQUIVO")
        )
        meta.addView(
            text(
                "Imagem selecionada • " +
                    bitmap.width +
                    " × " +
                    bitmap.height +
                    " px"
            )
        )
        root.addView(meta, margin(8))

        val process = button(
            "Remover fundo com IA",
            true
        )

        process.setOnClickListener {
            process.isEnabled = false
            process.text =
                "A processar com Cloudflare AI..."
            processImage()
        }

        root.addView(process, margin(12))

        val back = button(
            "Escolher outra imagem",
            false
        )
        back.setOnClickListener {
            showPicker()
        }

        root.addView(back, margin(8))
        setContentView(root)
    }

    private fun processImage() {
        val bitmap =
            originalBitmap ?: return

        analytics.event(
            "background_remover_start"
        )

        val dialog =
            android.app.AlertDialog.Builder(this)
                .setTitle(
                    "ToolNexa está a trabalhar"
                )
                .setMessage(
                    "A enviar a imagem e a executar a segmentação por IA..."
                )
                .setCancelable(false)
                .create()

        dialog.show()

        thread {
            val result =
                try {
                    CloudflareApi.removeBackground(
                        bitmap
                    )
                } catch (_: Exception) {
                    null
                }

            runOnUiThread {
                dialog.dismiss()

                if (result == null) {
                    analytics.event(
                        "background_remover_failed"
                    )
                    toast(
                        "Não foi possível remover o fundo. Verifique a ligação e tente novamente."
                    )
                    return@runOnUiThread
                }

                resultBitmap = result
                analytics.event(
                    "background_remover_success"
                )
                showResult()
            }
        }
    }

    private fun showResult() {
        val original =
            originalBitmap ?: return
        val result =
            resultBitmap ?: return

        val root = base()

        root.addView(
            progressHeader(
                "3 / 3",
                "Resultado"
            )
        )

        root.addView(
            title(
                "Fundo removido",
                27f
            )
        )

        root.addView(
            text(
                "O resultado está em PNG transparente. Compare o original e o resultado antes de guardar."
            )
        )

        val compare = LinearLayout(this)
        compare.orientation =
            LinearLayout.VERTICAL

        compare.addView(
            compareCard(
                "ORIGINAL",
                original
            ),
            margin(8)
        )

        compare.addView(
            compareCard(
                "RESULTADO • TRANSPARENTE",
                result
            ),
            margin(8)
        )

        root.addView(compare)

        val save = button(
            "Salvar PNG",
            true
        )
        save.setOnClickListener {
            saveResult(result)
        }
        root.addView(save, margin(10))

        val share = button(
            "Partilhar resultado",
            false
        )
        share.setOnClickListener {
            shareResult(result)
        }
        root.addView(share, margin(8))

        val again = button(
            "Processar outra imagem",
            false
        )
        again.setOnClickListener {
            selectedUri = null
            originalBitmap = null
            resultBitmap = null
            showPicker()
        }
        root.addView(again, margin(8))

        setContentView(root)
    }

    private fun saveResult(
        bitmap: Bitmap
    ) {
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "ToolNexa-background-removed-" +
                    System.currentTimeMillis() +
                    ".png"
            )
            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/png"
            )
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/ToolNexa"
            )
            put(
                MediaStore.Images.Media.IS_PENDING,
                1
            )
        }

        val uri =
            contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )

        if (uri == null) {
            toast(
                "Não foi possível criar o arquivo."
            )
            return
        }

        try {
            contentResolver.openOutputStream(uri)
                ?.use {
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        it
                    )
                }

            values.clear()
            values.put(
                MediaStore.Images.Media.IS_PENDING,
                0
            )
            contentResolver.update(
                uri,
                values,
                null,
                null
            )

            analytics.event(
                "background_remover_saved"
            )
            toast(
                "PNG salvo em Pictures/ToolNexa."
            )
        } catch (_: Exception) {
            contentResolver.delete(
                uri,
                null,
                null
            )
            toast(
                "Falha ao salvar o resultado."
            )
        }
    }

    private fun shareResult(
        bitmap: Bitmap
    ) {
        val file =
            File(
                cacheDir,
                "toolnexa-background-removed.png"
            )

        FileOutputStream(file).use {
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                it
            )
        }

        val uri =
            androidx.core.content.FileProvider
                .getUriForFile(
                    this,
                    "com.toolnexa.app.fileprovider",
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
                "Partilhar imagem"
            )
        )
    }

    private fun compareCard(
        name: String,
        bitmap: Bitmap
    ): LinearLayout {
        val box = card()
        box.setPadding(
            dp(12),
            dp(12),
            dp(12),
            dp(12)
        )

        box.addView(label(name))

        val image =
            ImageView(this).apply {
                setImageBitmap(bitmap)
                scaleType =
                    ImageView.ScaleType.CENTER_INSIDE
                background =
                    checkerboard()
                setPadding(
                    dp(8),
                    dp(8),
                    dp(8),
                    dp(8)
                )
            }

        box.addView(
            image,
            LinearLayout.LayoutParams(
                -1,
                dp(240)
            )
        )

        return box
    }

    private fun progressHeader(
        step: String,
        value: String
    ): LinearLayout {
        val row = LinearLayout(this)
        row.orientation =
            LinearLayout.HORIZONTAL
        row.gravity =
            Gravity.CENTER_VERTICAL
        row.setPadding(
            dp(14),
            dp(12),
            dp(14),
            dp(12)
        )
        row.background =
            GradientDrawable().apply {
                setColor(
                    android.graphics.Color.parseColor(
                        "#EAF0FF"
                    )
                )
                cornerRadius =
                    dp(14).toFloat()
            }

        val left = TextView(this)
        left.text = step
        left.textSize = 13f
        left.typeface =
            android.graphics.Typeface.DEFAULT_BOLD
        left.setTextColor(blue)

        val right = TextView(this)
        right.text = value
        right.textSize = 13f
        right.setTextColor(muted)
        right.setPadding(
            dp(10),
            0,
            0,
            0
        )

        row.addView(left)
        row.addView(
            right,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        return row
    }

    private fun imagePreview(
        bitmap: Bitmap
    ): ImageView {
        return ImageView(this).apply {
            setImageBitmap(bitmap)
            scaleType =
                ImageView.ScaleType.CENTER_INSIDE
            background = solid(
                android.graphics.Color.WHITE
            )
            setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )
        }
    }

    private fun base(): LinearLayout {
        val root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setBackgroundColor(bg)
                setPadding(
                    dp(16),
                    dp(18),
                    dp(16),
                    dp(30)
                )
            }

        val scroll = ScrollView(this)
        scroll.addView(root)
        return root
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            background =
                GradientDrawable().apply {
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

    private fun title(
        value: String,
        size: Float
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            typeface =
                android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            setPadding(
                0,
                dp(7),
                0,
                dp(7)
            )
        }
    }

    private fun label(
        value: String
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = 11f
            typeface =
                android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(blue)
            setPadding(
                0,
                dp(4),
                0,
                dp(5)
            )
        }
    }

    private fun text(
        value: String
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = 15f
            setTextColor(muted)
            setPadding(
                0,
                dp(3),
                0,
                dp(9)
            )
        }
    }

    private fun button(
        value: String,
        primary: Boolean
    ): Button {
        return Button(this).apply {
            text = value
            textSize = 15f
            minHeight = dp(52)
            stateListAnimator = null
            background =
                GradientDrawable().apply {
                    if (primary) {
                        setColor(blue)
                        setStroke(
                            dp(1),
                            blue
                        )
                        setTextColor(
                            android.graphics.Color.WHITE
                        )
                    } else {
                        setColor(
                            android.graphics.Color.TRANSPARENT
                        )
                        setStroke(
                            dp(1),
                            blue
                        )
                    }
                    cornerRadius =
                        dp(14).toFloat()
                }
            if (!primary) {
                setTextColor(blue)
            }
        }
    }

    private fun solid(
        color: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius =
                dp(14).toFloat()
        }
    }

    private fun checkerboard():
        GradientDrawable {
        return GradientDrawable().apply {
            setColor(
                android.graphics.Color.WHITE
            )
            setStroke(
                dp(1),
                border
            )
        }
    }

    private fun margin(
        top: Int
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            -1,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(
                0,
                dp(top),
                0,
                0
            )
        }
    }

    private fun toast(
        value: String
    ) {
        Toast.makeText(
            this,
            value,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun dp(
        value: Int
    ): Int {
        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }

    companion object {
        private const val REQUEST_PICK =
            701
    }
}
