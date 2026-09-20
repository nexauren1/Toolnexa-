package com.toolnexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class HistoryActivity : Activity() {
    private val analytics by lazy { AnalyticsTracker(this) }
    private val blue by lazy { getColor(R.color.toolnexa_blue) }
    private val bg by lazy { getColor(R.color.toolnexa_bg) }
    private val surface by lazy { getColor(R.color.toolnexa_surface) }
    private val textColor by lazy { getColor(R.color.toolnexa_text) }
    private val muted by lazy { getColor(R.color.toolnexa_muted) }
    private val border by lazy { getColor(R.color.toolnexa_border) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        LanguageManager.apply(this)
        analytics.screen("history")
        build()
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(12))
            background = GradientDrawable().apply {
                setColor(surface)
                setStroke(dp(1), border)
            }
        }

        header.addView(
            TextView(this).apply {
                text = "ToolNexa"
                textSize = 20f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(textColor)
            }
        )
        header.addView(
            TextView(this).apply {
                text = "Histórico"
                textSize = 13f
                setTextColor(blue)
            }
        )
        root.addView(header)

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

        root.addView(
            brandRail,
            LinearLayout.LayoutParams(
                -1,
                dp(4)
            )
        )

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(30))
        }

        val scroll = ScrollView(this)
        scroll.addView(body)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(root)
        I18n.localizeWindow(this)

        val entries = NexaurenHistory.list(this)

        body.addView(
            TextView(this).apply {
                text = "Resultados guardados"
                textSize = 27f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(textColor)
            }
        )

        body.addView(
            TextView(this).apply {
                text =
                    if (entries.isEmpty()) {
                        "Ainda não existem resultados guardados."
                    } else {
                        entries.size.toString() +
                            " resultado(s) recente(s)"
                    }
                textSize = 14f
                setTextColor(muted)
                setPadding(0, dp(4), 0, dp(10))
            }
        )

        entries.forEach { entry ->
            addEntry(body, entry)
        }

        if (entries.isNotEmpty()) {
            val clear = Button(this).apply {
                text = "Limpar histórico"
            }
            styleSecondary(clear)
            clear.setOnClickListener {
                analytics.event("history_clear")
                NexaurenHistory.clear(this)
                Toast.makeText(
                    this,
                    "Histórico limpo.",
                    Toast.LENGTH_SHORT
                ).show()
                build()
            }

            body.addView(
                clear,
                LinearLayout.LayoutParams(
                    -1,
                    dp(50)
                ).apply {
                    setMargins(0, dp(14), 0, 0)
                }
            )
        }
    }

    private fun addEntry(
        body: LinearLayout,
        entry: HistoryEntry
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(surface)
                setStroke(dp(1), border)
                cornerRadius = dp(18).toFloat()
            }
            alpha = 0f
            translationY = dp(7).toFloat()
        }

        box.addView(
            TextView(this).apply {
                text = entry.tool
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(blue)
            }
        )

        box.addView(
            TextView(this).apply {
                text = entry.name
                textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(textColor)
            }
        )

        box.addView(
            TextView(this).apply {
                text =
                    formatBytes(entry.size) +
                        " • " +
                        SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            Locale.getDefault()
                        ).format(Date(entry.timestamp))
                textSize = 13f
                setTextColor(muted)
                setPadding(0, dp(4), 0, dp(8))
            }
        )

        val open = Button(this).apply {
            text = "Abrir"
        }
        styleSecondary(open)

        open.setOnClickListener {
            analytics.event(
                "history_item_open",
                "tool" to entry.tool
            )
            try {
                val uri = Uri.parse(entry.uri)
                startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(
                            uri,
                            "image/*"
                        )
                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                )
            } catch (_: Exception) {
                Toast.makeText(
                    this,
                    "Não foi possível abrir este resultado.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        box.addView(open)

        body.addView(
            box,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(8), 0, 0)
            }
        )

        box.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .start()
    }

    private fun styleSecondary(button: Button) {
        button.setTextColor(blue)
        button.textSize = 14f
        button.background = GradientDrawable().apply {
            setColor(android.graphics.Color.TRANSPARENT)
            setStroke(dp(1), blue)
            cornerRadius = dp(14).toFloat()
        }
        button.minHeight = dp(48)
        button.stateListAnimator = null
    }

    private fun formatBytes(size: Long): String {
        if (size < 1024L) {
            return size.toString() + " B"
        }

        val kb = size / 1024.0

        if (kb < 1024.0) {
            return String.format(
                Locale.US,
                "%.1f KB",
                kb
            )
        }

        return String.format(
            Locale.US,
            "%.1f MB",
            kb / 1024.0
        )
    }

    private fun dp(value: Int): Int {
        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }
}
