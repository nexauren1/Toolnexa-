package com.toolnexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupportActivity : Activity() {

    private val analytics by lazy {
        AnalyticsTracker(this)
    }

    private val bg by lazy {
        getColor(R.color.toolnexa_bg)
    }

    private val surface by lazy {
        getColor(R.color.toolnexa_surface)
    }

    private val text by lazy {
        getColor(R.color.toolnexa_text)
    }

    private val muted by lazy {
        getColor(R.color.toolnexa_muted)
    }

    private val blue by lazy {
        getColor(R.color.toolnexa_blue)
    }

    override fun onCreate(
        state: Bundle?
    ) {
        super.onCreate(state)
        analytics.screen("support")
        build()
    }

    private fun build() {
        val root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setBackgroundColor(bg)
            }

        val scroll =
            android.widget.ScrollView(this)

        val body =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(32)
                )
            }

        scroll.addView(body)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(root)

        body.addView(
            TextView(this).apply {
                text = "‹  ToolNexa"
                textSize = 15f
                setTextColor(blue)
                setOnClickListener {
                    finish()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(44)
            )
        )

        body.addView(
            TextView(this).apply {
                text = "Suporte, sugestões e reclamações"
                textSize = 27f
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(text)
            }
        )

        body.addView(
            TextView(this).apply {
                text =
                    "Escolhe o tipo de contacto e a ferramenta relacionada. Ao tocar em Enviar, o teu aplicativo de email abre com o relatório preenchido para reveres antes de enviar."
                textSize = 14f
                setTextColor(muted)
                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(18)
                )
            }
        )

        val type =
            Spinner(this)

        type.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                arrayOf(
                    "Suporte técnico",
                    "Reclamação",
                    "Sugestão",
                    "Problema de pagamento",
                    "Problema de conta"
                )
            )

        body.addView(
            label("Tipo de contacto")
        )
        body.addView(
            type,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        val tool =
            Spinner(this)

        tool.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                arrayOf(
                    "Geral",
                    "Background Remover",
                    "Image Compressor",
                    "Image Resizer",
                    "Image Converter",
                    "Planos / PayPal",
                    "Login / Conta",
                    "Histórico",
                    "Outra ferramenta"
                )
            )

        body.addView(
            label("Ferramenta ou área")
        )
        body.addView(
            tool,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        val message =
            EditText(this).apply {
                hint =
                    "Explica o problema, a ideia ou a sugestão..."
                minLines = 6
                gravity = Gravity.TOP
                setPadding(
                    dp(14),
                    dp(14),
                    dp(14),
                    dp(14)
                )
                background =
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(surface)
                        setStroke(
                            dp(1),
                            getColor(
                                R.color.toolnexa_border
                            )
                        )
                        cornerRadius =
                            dp(16).toFloat()
                    }
            }

        body.addView(
            label("Mensagem")
        )
        body.addView(
            message,
            LinearLayout.LayoutParams(
                -1,
                dp(150)
            )
        )

        val includeDiagnostics =
            android.widget.CheckBox(this).apply {
                text =
                    "Incluir diagnóstico técnico (modelo do aparelho, Android, versão da app e idioma)"
                isChecked = true
                setTextColor(text)
            }

        body.addView(
            includeDiagnostics,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        )

        body.addView(
            TextView(this).apply {
                text =
                    "Não incluímos palavras-passe, tokens, chaves privadas ou dados secretos. O email abre para poderes rever tudo antes de enviar."
                textSize = 12f
                setTextColor(muted)
                setPadding(
                    4,
                    0,
                    4,
                    14
                )
            }
        )

        val send =
            Button(this).apply {
                text = "Enviar para o suporte"
                isAllCaps = false
                setTextColor(Color.WHITE)
                background =
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(blue)
                        cornerRadius =
                            dp(14).toFloat()
                    }
                minHeight = dp(54)
                stateListAnimator = null
            }

        send.setOnClickListener {
            val textValue =
                message.text
                    .toString()
                    .trim()

            if (textValue.isBlank()) {
                message.error =
                    "Escreve a tua mensagem."
                return@setOnClickListener
            }

            val subject =
                "[ToolNexa] " +
                    type.selectedItem
                        .toString() +
                    " • " +
                    tool.selectedItem
                        .toString()

            val bodyText =
                buildReport(
                    type.selectedItem
                        .toString(),
                    tool.selectedItem
                        .toString(),
                    textValue,
                    includeDiagnostics.isChecked
                )

            analytics.event(
                "support_email_opened",
                "type" to
                    type.selectedItem
                        .toString(),
                "tool" to
                    tool.selectedItem
                        .toString()
            )

            composeEmail(
                subject,
                bodyText
            )
        }

        body.addView(
            send,
            LinearLayout.LayoutParams(
                -1,
                dp(54)
            ).apply {
                topMargin = dp(6)
            }
        )
    }

    private fun buildReport(
        type: String,
        tool: String,
        message: String,
        diagnostics: Boolean
    ): String {
        val user =
            com.google.firebase.auth
                .FirebaseAuth
                .getInstance()
                .currentUser

        val date =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.US
            ).format(Date())

        return buildString {
            appendLine("TOOLNEXA SUPPORT REPORT")
            appendLine("========================")
            appendLine()
            appendLine("Tipo: $type")
            appendLine("Área/ferramenta: $tool")
            appendLine("Data: $date")
            appendLine("Mensagem:")
            appendLine(message)
            appendLine()

            appendLine("Conta")
            appendLine("------")
            appendLine(
                "Nome: " +
                    (user?.displayName
                        ?: "Não disponível")
            )
            appendLine(
                "Email da conta: " +
                    (user?.email
                        ?: "Não disponível")
            )
            appendLine()

            if (diagnostics) {
                appendLine("Diagnóstico técnico")
                appendLine("------------------")
                appendLine(
                    "App: ToolNexa " +
                        BuildConfig.VERSION_NAME
                )
                appendLine(
                    "Version code: " +
                        BuildConfig.VERSION_CODE
                )
                appendLine(
                    "Android: " +
                        android.os.Build.VERSION.RELEASE
                )
                appendLine(
                    "SDK: " +
                        android.os.Build.VERSION.SDK_INT
                )
                appendLine(
                    "Fabricante: " +
                        android.os.Build.MANUFACTURER
                )
                appendLine(
                    "Modelo: " +
                        android.os.Build.MODEL
                )
                appendLine(
                    "Idioma: " +
                        Locale.getDefault().toLanguageTag()
                )
                appendLine(
                    "País/região: " +
                        Locale.getDefault().country
                )
            }

            appendLine()
            appendLine(
                "Nota: este relatório não contém palavra-passe, token de autenticação ou chave privada."
            )
        }
    }

    private fun composeEmail(
        subject: String,
        body: String
    ) {
        val intent =
            Intent(Intent.ACTION_SENDTO).apply {
                data =
                    Uri.parse(
                        "mailto:nexaurenstore@gmail.com"
                    )
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    subject
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    body
                )
            }

        if (
            intent.resolveActivity(
                packageManager
            ) != null
        ) {
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                "Não foi encontrada uma aplicação de email.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun label(
        value: String
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = 13f
            typeface =
                android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(text)
            setPadding(
                2,
                dp(14),
                2,
                dp(6)
            )
        }

    private fun dp(
        value: Int
    ): Int =
        (
            value *
                resources.displayMetrics.density
            ).toInt()
}
