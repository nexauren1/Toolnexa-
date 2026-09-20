package com.toolnexa.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

class ToolWorkflowActivity : Activity() {
    private val analytics by lazy { AnalyticsTracker(this) }
    private val compressor by lazy { ImageCompressor(this) }
    private val resizer by lazy { ImageResizer(this) }
    private val converter by lazy { ImageConverter(this) }
    private var tool = "compressor"
    private var selectedUri: Uri? = null
    private var resultFile: File? = null
    private var preview: ImageView? = null
    private var compressionQuality = 82
    private var resizeWidth = 1080
    private var resizeQuality = 90
    private var converterFormat = "JPG"
    private var converterQuality = 92
    private var root: LinearLayout? = null
    private val blue by lazy { getColor(R.color.toolnexa_blue) }
    private val bg by lazy { getColor(R.color.toolnexa_bg) }
    private val surface by lazy { getColor(R.color.toolnexa_surface) }
    private val textColor by lazy { getColor(R.color.toolnexa_text) }
    private val muted by lazy { getColor(R.color.toolnexa_muted) }
    private val border by lazy { getColor(R.color.toolnexa_border) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        LanguageManager.apply(this)
        tool = intent.getStringExtra("tool") ?: "compressor"
        analytics.screen("tool_" + tool + "_stage_1")
        showStage1()
    }

    private fun showStage1() {
        buildBase("1 / 3  •  Escolher arquivo")
        addTitle(
            when (tool) {
                "compressor" -> "Image Compressor"
                "resizer" -> "Image Resizer"
                else -> "Image Converter"
            }
        )
        addText("Escolha uma imagem. O processamento é feito no próprio dispositivo.")
        val pick = button("Escolher imagem", true)
        pick.setOnClickListener {
            analytics.event("tool_file_picker", "tool" to tool)
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }, REQUEST_PICK)
        }
        add(pick)
        addText("Depois poderá ajustar as opções e confirmar o resultado antes de guardar.")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK && resultCode == RESULT_OK && data?.data != null) {
            selectedUri = data.data
            try {
                contentResolver.takePersistableUriPermission(
                    selectedUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            analytics.event("tool_file_selected", "tool" to tool)
            showStage2()
        }
    }

    private fun showStage2() {
        buildBase("2 / 3  •  Personalizar")
        addTitle(
            when (tool) {
                "compressor" -> "Personalizar compressão"
                "resizer" -> "Personalizar tamanho"
                else -> "Escolher formato"
            }
        )
        val uri = selectedUri ?: return showStage1()
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        add(preview!!, 250)
        Thread {
            val bitmap = compressor.decodeSampled(uri, 1600)
            runOnUiThread { if (bitmap != null) preview?.setImageBitmap(bitmap) }
        }.start()
        when (tool) {
            "compressor" -> addCompressorOptions()
            "resizer" -> addResizerOptions(uri)
            "converter" -> addConverterOptions()
        }
        val process = button("Processar e ver resultado", true)
        process.setOnClickListener { processTool() }
        add(process)
        val back = button("Voltar e escolher outra imagem", false)
        back.setOnClickListener { showStage1() }
        add(back)
    }

    private fun addCompressorOptions() {
        val label = TextView(this).apply { text = "Qualidade: " + compressionQuality + "%"; textSize = 16f; setTextColor(textColor) }
        add(label)
        val seek = SeekBar(this).apply {
            max = 90
            progress = compressionQuality - 10
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b: SeekBar?, p: Int, fromUser: Boolean) {
                    compressionQuality = p + 10
                    label.text = "Qualidade: " + compressionQuality + "%"
                    if (fromUser) analytics.event("compress_quality_changed", "value" to compressionQuality.toString())
                }
                override fun onStartTrackingTouch(b: SeekBar?) = Unit
                override fun onStopTrackingTouch(b: SeekBar?) = Unit
            })
        }
        add(seek)
        addText("A qualidade pode ser ajustada antes de gerar o resultado.")
    }

    private fun addResizerOptions(uri: Uri) {
        addText("Largura de saída em pixels")
        val width = EditText(this).apply {
            setText(resizeWidth.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }
        styleEdit(width)
        add(width)
        val label = TextView(this).apply { text = "Qualidade: " + resizeQuality + "%"; textSize = 16f; setTextColor(textColor) }
        add(label)
        val seek = SeekBar(this).apply {
            max = 90
            progress = resizeQuality - 10
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b: SeekBar?, p: Int, fromUser: Boolean) {
                    resizeQuality = p + 10
                    label.text = "Qualidade: " + resizeQuality + "%"
                }
                override fun onStartTrackingTouch(b: SeekBar?) = Unit
                override fun onStopTrackingTouch(b: SeekBar?) = Unit
            })
        }
        add(seek)
        addText("A proporção original é preservada pelo motor atual do Resizer.")
    }

    private fun addConverterOptions() {
        addText("Formato de saída")
        val formats = listOf("JPG", "PNG", "WebP")
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        for (format in formats) {
            val b = button(format, format == converterFormat)
            b.setOnClickListener {
                converterFormat = format
                analytics.event(
                    "convert_format_changed",
                    "format" to format
                )
                showStage2()
            }
            row.addView(
                b,
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f
                ).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
            )
        }
        add(row)

        val label = TextView(this).apply {
            text = "Qualidade: $converterQuality%"
            textSize = 16f
            setTextColor(textColor)
        }
        add(label)

        val seek = SeekBar(this).apply {
            max = 90
            progress = converterQuality - 10
            setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        b: SeekBar?,
                        p: Int,
                        fromUser: Boolean
                    ) {
                        converterQuality = p + 10
                        label.text =
                            "Qualidade: $converterQuality%"
                    }

                    override fun onStartTrackingTouch(
                        b: SeekBar?
                    ) = Unit

                    override fun onStopTrackingTouch(
                        b: SeekBar?
                    ) = Unit
                }
            )
        }
        add(seek)
        addText(
            "PNG preserva transparência. JPG e WebP usam qualidade ajustável."
        )
    }

    private fun processTool() {
        val uri = selectedUri ?: return
        if (tool == "resizer") {
            val fields = findViews(EditText::class.java)
            resizeWidth = fields.firstOrNull()?.text?.toString()?.toIntOrNull()?.coerceIn(16, 4096) ?: 1080
        }
        analytics.event("tool_process_start", "tool" to tool)
        val dialog = processing(
            when (tool) {
                "compressor" -> "A comprimir..."
                "resizer" -> "A redimensionar..."
                else -> "A converter..."
            }
        )
        Thread {
            try {
                val result = when (tool) {
                    "compressor" -> compressor.compress(uri, compressionQuality)
                    "resizer" -> resizer.resize(
                        uri,
                        resizeWidth,
                        resizeQuality
                    )
                    else -> converter.convert(
                        uri,
                        converterFormat,
                        converterQuality
                    )
                }
                runOnUiThread {
                    dialog.dismiss()
                    if (result == null) {
                        analytics.event("tool_process_failed", "tool" to tool)
                        toast("Não foi possível processar o arquivo.")
                    } else {
                        analytics.event("tool_process_success", "tool" to tool)
                        when (tool) {
                            "compressor" -> {
                                val r = result as CompressionResult
                                resultFile = r.file
                                showStage3(
                                    r.file,
                                    r.originalBytes,
                                    r.compressedBytes,
                                    r.width,
                                    r.height
                                )
                            }
                            "resizer" -> {
                                val r = result as ResizeResult
                                resultFile = r.file
                                showStage3(
                                    r.file,
                                    r.originalBytes,
                                    r.resizedBytes,
                                    r.width,
                                    r.height
                                )
                            }
                            else -> {
                                val r = result as ConversionResult
                                resultFile = r.file
                                showStage3(
                                    r.file,
                                    r.originalBytes,
                                    r.convertedBytes,
                                    r.width,
                                    r.height
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { dialog.dismiss(); analytics.event("tool_process_exception", "tool" to tool, "error" to e.javaClass.simpleName); toast("O processamento falhou. Tente novamente.") }
            }
        }.start()
    }

    private fun showStage3(file: File, original: Long, output: Long, width: Int, height: Int) {
        buildBase("3 / 3  •  Resultado")
        addTitle("Resultado pronto")
        addText("Veja o resultado e salve no diretório escolhido.")
        val image = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE; setBackgroundColor(android.graphics.Color.WHITE); setPadding(dp(8), dp(8), dp(8), dp(8)) }
        preview = image
        add(image, 280)
        Thread {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            runOnUiThread { if (bitmap != null) image.setImageBitmap(bitmap) }
        }.start()
        val delta = if (original > 0) ((original - output).toDouble() / original * 100).roundToInt() else 0
        addText("Dimensões: " + width + " × " + height + " px\nOriginal: " + formatBytes(original) + "\nResultado: " + formatBytes(output) + "\nVariação: " + delta + "%")
        val save = button("Salvar", true)
        save.setOnClickListener { saveResult(file) }
        add(save)
        val share = button("Partilhar", false)
        share.setOnClickListener { shareResult(file) }
        add(share)
        val again = button("Processar outro arquivo", false)
        again.setOnClickListener { selectedUri = null; resultFile = null; showStage1() }
        add(again)
    }

    private fun saveResult(file: File) {
        if (!NexaurenStorage.hasRoot(this)) {
            analytics.event(
                "tool_save_blocked_no_directory",
                "tool" to tool
            )

            AlertDialog.Builder(this)
                .setTitle("Diretório não definido")
                .setMessage(
                    "Escolha o diretório em Definições antes de salvar."
                )
                .setNegativeButton(
                    "Agora não",
                    null
                )
                .setPositiveButton(
                    "Abrir Definições"
                ) { _, _ ->
                    analytics.event(
                        "tool_open_settings_for_storage",
                        "tool" to tool
                    )

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

        analytics.event(
            "tool_save_start",
            "tool" to tool
        )

        val toolFolder =
            when (tool) {
                "compressor" -> "Compressor"
                "resizer" -> "Resizer"
                else -> "Converter"
            }

        val fileName =
            "ToolNexa-" +
                System.currentTimeMillis() +
                "." +
                file.extension

        val saved =
            NexaurenStorage.save(
                this,
                "Imagem",
                toolFolder,
                file,
                fileName
            )

        if (saved != null) {
            analytics.event(
                "tool_save_success",
                "tool" to tool
            )

            NexaurenHistory.add(
                this,
                toolFolder,
                saved,
                fileName,
                file.length()
            )

            toast("Arquivo salvo.")
        } else {
            analytics.event(
                "tool_save_failed",
                "tool" to tool
            )

            toast(
                "Não foi possível salvar no diretório escolhido."
            )
        }
    }

    private fun shareResult(file: File) {
        val uri = NexaurenStorage.fileProviderUri(this, file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Partilhar resultado"))
        analytics.event("tool_share", "tool" to tool)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun buildBase(label: String) {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(10)); setBackgroundColor(surface) }
        header.addView(TextView(this).apply { text = "ToolNexa"; textSize = 20f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(textColor) })
        header.addView(TextView(this).apply { text = label; textSize = 13f; setTextColor(blue) })
        root!!.addView(header)
        val brandRail =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        listOf(
            R.color.toolnexa_green,
            R.color.toolnexa_blue,
            R.color.toolnexa_red,
            R.color.toolnexa_yellow
        ).forEach { colorRes ->
            brandRail.addView(
                View(this).apply {
                    setBackgroundColor(
                        getColor(colorRes)
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
            brandRail,
            LinearLayout.LayoutParams(
                -1,
                dp(4)
            )
        )

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(30)) }
        scroll.addView(body)
        root!!.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun body(): LinearLayout? {
        val scroll =
            root?.childrenSequence()
                ?.filterIsInstance<ScrollView>()
                ?.firstOrNull()

        return scroll
            ?.getChildAt(0)
            as? LinearLayout
    }

    private fun add(view: View, height: Int? = null) { body()?.addView(view, LinearLayout.LayoutParams(-1, height ?: ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, dp(6)) }) }
    private fun addTitle(value: String) { add(TextView(this).apply { text = value; textSize = 27f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(textColor) }); addSpacer(2) }
    private fun addText(value: String) { add(TextView(this).apply { text = value; textSize = 14f; setTextColor(muted) }) }
    private fun addSpacer(size: Int) { add(View(this), size) }

    private fun findViews(clazz: Class<out View>): List<EditText> {
        val result = mutableListOf<EditText>()
        fun scan(v: View?) {
            if (v is EditText) result.add(v)
            if (v is ViewGroup) for (i in 0 until v.childCount) scan(v.getChildAt(i))
        }
        scan(root)
        return result
    }

    private fun button(label: String, primary: Boolean) = Button(this).apply {
        text = label; textSize = 15f; minHeight = dp(50); stateListAnimator = null
        setTextColor(if (primary) android.graphics.Color.WHITE else blue)
        background = android.graphics.drawable.GradientDrawable().apply { setColor(if (primary) blue else android.graphics.Color.TRANSPARENT); if (!primary) setStroke(dp(1), blue); cornerRadius = dp(14).toFloat() }
    }

    private fun styleEdit(edit: EditText) {
        edit.setTextColor(textColor); edit.setHintTextColor(muted); edit.setPadding(dp(14), dp(10), dp(14), dp(10))
        edit.background = android.graphics.drawable.GradientDrawable().apply { setColor(surface); setStroke(dp(1), border); cornerRadius = dp(14).toFloat() }
    }

    private fun processing(message: String): AlertDialog {
        val box = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(14), dp(20), dp(14)) }
        val gear = TextView(this).apply { text = "⚙"; textSize = 30f; setTextColor(blue) }
        box.addView(gear, LinearLayout.LayoutParams(dp(46), dp(46)))
        box.addView(TextView(this).apply { text = message; textSize = 15f; setTextColor(textColor); setPadding(dp(14), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        val dialog = AlertDialog.Builder(this).setView(box).setCancelable(false).create()
        dialog.setOnShowListener {
            fun spin() { if (!dialog.isShowing) return; gear.animate().rotationBy(360f).setDuration(850).withEndAction { spin() }.start() }
            spin()
        }
        dialog.show()
        return dialog
    }

    private fun formatBytes(v: Long): String {
        if (v < 1024) return v.toString() + " B"
        val kb = v / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        return String.format(Locale.US, "%.1f MB", kb / 1024.0)
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        const val REQUEST_PICK = 1001
    }
}