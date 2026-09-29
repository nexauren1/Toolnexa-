package com.toolnexa.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class TextCaseConverterActivity : Activity() {

    private val blue by lazy {
        getColor(R.color.toolnexa_blue)
    }

    private val red by lazy {
        getColor(R.color.toolnexa_red)
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

    private lateinit var input: EditText
    private lateinit var output: EditText

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        LanguageManager.apply(this)

        window.statusBarColor = bg
        window.navigationBarColor = surface

        build()
    }

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = toolbarBackground()
        }

        toolbar.addView(
            TextView(this).apply {
                text = "TOOLNEXA"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textColor)
            },
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        val close = Button(this).apply {
            text = "Voltar"
            isAllCaps = false
            setTextColor(blue)
            background = transparent()
            setOnClickListener { finish() }
        }

        toolbar.addView(
            close,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(48)
            )
        )

        root.addView(
            toolbar,
            LinearLayout.LayoutParams(-1, dp(72))
        )

        val rail = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        listOf(
            R.color.toolnexa_red,
            R.color.toolnexa_blue,
            R.color.toolnexa_purple,
            R.color.toolnexa_cyan
        ).forEach { res ->
            rail.addView(
                Space(this).apply {
                    setBackgroundColor(getColor(res))
                },
                LinearLayout.LayoutParams(0, dp(4), 1f)
            )
        }

        root.addView(
            rail,
            LinearLayout.LayoutParams(-1, dp(4))
        )

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(28))
        }

        scroll.addView(content)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        content.addView(
            TextView(this).apply {
                text = "TEXT / TRANSFORM"
                textSize = 10f
                letterSpacing = 0.14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(getColor(R.color.toolnexa_cyan))
            }
        )

        content.addView(
            TextView(this).apply {
                text = "Text Case Converter"
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textColor)
                setPadding(0, dp(4), 0, dp(6))
            }
        )

        content.addView(
            TextView(this).apply {
                text = "Transforme rapidamente o texto entre diferentes formatos. Tudo funciona offline."
                textSize = 14f
                setTextColor(muted)
                setPadding(0, 0, 0, dp(16))
            }
        )

        input = editor(
            hint = "Escreve ou cola o teu texto aqui..."
        )

        content.addView(
            TextView(this).apply {
                text = "TEXTO ORIGINAL"
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(muted)
                letterSpacing = 0.12f
                setPadding(0, 0, 0, dp(8))
            }
        )

        content.addView(
            input,
            LinearLayout.LayoutParams(-1, dp(190)).apply {
                setMargins(0, 0, 0, dp(12))
            }
        )

        val upper = actionButton("MAIÚSCULAS")
        val lower = actionButton("minúsculas")
        val title = actionButton("Capitalizar Palavras")
        val sentence = actionButton("Capitalizar Frases")

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row1.addView(
            upper,
            LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                setMargins(0, dp(4), dp(6), dp(4))
            }
        )
        row1.addView(
            lower,
            LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                setMargins(dp(6), dp(4), 0, dp(4))
            }
        )

        content.addView(
            row1,
            LinearLayout.LayoutParams(-1, dp(58))
        )

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row2.addView(
            title,
            LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                setMargins(0, dp(4), dp(6), dp(4))
            }
        )
        row2.addView(
            sentence,
            LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                setMargins(dp(6), dp(4), 0, dp(4))
            }
        )

        content.addView(
            row2,
            LinearLayout.LayoutParams(-1, dp(58)).apply {
                setMargins(0, 0, 0, dp(16))
            }
        )

        content.addView(
            TextView(this).apply {
                text = "RESULTADO"
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(muted)
                letterSpacing = 0.12f
                setPadding(0, 0, 0, dp(8))
            }
        )

        output = editor(
            hint = "O resultado aparecerá aqui..."
        )
        output.isFocusable = false
        output.isCursorVisible = false

        content.addView(
            output,
            LinearLayout.LayoutParams(-1, dp(190)).apply {
                setMargins(0, 0, 0, dp(12))
            }
        )

        val copy = actionButton("Copiar resultado")
        copy.setOnClickListener {
            copyOutput()
        }

        content.addView(
            copy,
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                setMargins(0, 0, 0, dp(10))
            }
        )

        val clear = Button(this).apply {
            text = "Limpar"
            isAllCaps = false
            setTextColor(blue)
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), blue)
                cornerRadius = dp(14).toFloat()
            }
            stateListAnimator = null
            setOnClickListener {
                input.text?.clear()
                output.text?.clear()
            }
        }

        content.addView(
            clear,
            LinearLayout.LayoutParams(-1, dp(52))
        )

        upper.setOnClickListener {
            transform { it.uppercase() }
        }

        lower.setOnClickListener {
            transform { it.lowercase() }
        }

        title.setOnClickListener {
            transform { toTitleCase(it) }
        }

        sentence.setOnClickListener {
            transform { toSentenceCase(it) }
        }

        input.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    if (s.isNullOrBlank()) {
                        output.setText("")
                    }
                }

                override fun afterTextChanged(
                    s: Editable?
                ) = Unit
            }
        )

        setContentView(root)
        I18n.localizeWindow(this)
    }

    private fun transform(
        operation: (String) -> String
    ) {
        val raw = input.text?.toString().orEmpty()

        if (raw.isBlank()) {
            toast("Escreve ou cola algum texto primeiro.")
            return
        }

        output.setText(operation(raw))
        output.setSelection(output.text.length)
    }

    private fun toTitleCase(
        value: String
    ): String {
        return value.lowercase().replace(
            Regex("\\b(\\p{L})")
        ) { match ->
            match.value.uppercase()
        }
    }

    private fun toSentenceCase(
        value: String
    ): String {
        val lower = value.lowercase()
        val chars = lower.toCharArray()
        var capitalizeNext = true

        for (index in chars.indices) {
            val current = chars[index]

            if (capitalizeNext && current.isLetter()) {
                chars[index] = current.uppercaseChar()
                capitalizeNext = false
            }

            if (
                current == '.' ||
                current == '!' ||
                current == '?' ||
                current == '\n'
            ) {
                capitalizeNext = true
            }
        }

        return String(chars)
    }

    private fun copyOutput() {
        val result = output.text?.toString().orEmpty()

        if (result.isBlank()) {
            toast("Não há resultado para copiar.")
            return
        }

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "ToolNexa Text Case Converter",
                result
            )
        )

        toast("Resultado copiado.")
    }

    private fun editor(
        hint: String
    ): EditText {
        return EditText(this).apply {
            this.hint = hint
            minLines = 6
            gravity = Gravity.TOP
            textSize = 15f
            setTextColor(textColor)
            setHintTextColor(muted)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(surface)
                setStroke(dp(1), border)
                cornerRadius = dp(18).toFloat()
            }
        }
    }

    private fun actionButton(
        label: String
    ): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(blue, red)
            ).apply {
                cornerRadius = dp(14).toFloat()
            }
            stateListAnimator = null
        }
    }

    private fun toolbarBackground(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.parseColor("#0B1120"),
                Color.parseColor("#10192D")
            )
        )
    }

    private fun transparent(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
        }
    }

    private fun toast(
        message: String
    ) {
        Toast.makeText(
            this,
            message,
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
}
