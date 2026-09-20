package com.toolnexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.roundToInt

class CategoryActivity : Activity() {
    private val blue by lazy { getColor(R.color.toolnexa_blue) }
    private val bg by lazy { getColor(R.color.toolnexa_bg) }
    private val surface by lazy { getColor(R.color.toolnexa_surface) }
    private val textColor by lazy { getColor(R.color.toolnexa_text) }
    private val muted by lazy { getColor(R.color.toolnexa_muted) }
    private val border by lazy { getColor(R.color.toolnexa_border) }
    private val analytics by lazy { AnalyticsTracker(this) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val category = intent.getStringExtra("category") ?: "Imagem"
        analytics.screen("category_" + category.lowercase())
        build(category)
    }

    private fun build(category: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(12))
            setBackgroundColor(surface)
        }
        header.addView(TextView(this).apply {
            text = "ToolNexa"
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })
        header.addView(TextView(this).apply {
            text = category
            textSize = 13f
            setTextColor(blue)
        })
        root.addView(header)

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(30))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        addText(body, "Categoria $category", 27f, textColor, true)
        addText(
            body,
            "Ferramentas organizadas para encontrar rapidamente o que precisa.",
            15f,
            muted,
            false
        )

        if (category == "Imagem") {
            toolCard(
                body,
                "Image Compressor",
                "Reduza o tamanho mantendo a qualidade ajustável.",
                R.drawable.ic_tool_compress
            ) {
                open("compressor")
            }
            toolCard(
                body,
                "Image Resizer",
                "Defina a largura e preserve a proporção.",
                R.drawable.ic_tool_resize
            ) {
                open("resizer")
            }
            toolCard(
                body,
                "Image Converter",
                "Converta para JPG, PNG ou WebP com prévia do resultado.",
                R.drawable.ic_tool_resize
            ) {
                open("converter")
            }
        }
    }

    private fun toolCard(
        body: LinearLayout,
        name: String,
        description: String,
        icon: Int,
        action: () -> Unit
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(14), dp(12), dp(14))
            background = GradientDrawable().apply {
                setColor(surface)
                setStroke(dp(1), border)
                cornerRadius = dp(18).toFloat()
            }
            alpha = 0f
        }

        card.addView(ImageView(this).apply {
            setImageResource(icon)
            setColorFilter(blue)
            contentDescription = name
        }, LinearLayout.LayoutParams(dp(42), dp(42)))

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
        }
        addText(texts, name, 17f, textColor, true)
        addText(texts, description, 13f, muted, false)
        card.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))

        card.addView(TextView(this).apply {
            text = "›"
            textSize = 28f
            setTextColor(blue)
        })

        card.setOnClickListener {
            analytics.event("category_tool_open", "category" to "Imagem", "tool_name" to name)
            action()
        }

        body.addView(
            card,
            LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(10), 0, 0)
            }
        )
        card.animate().alpha(1f).setDuration(240).start()
    }

    private fun open(tool: String) {
        startActivity(
            Intent(this, ToolWorkflowActivity::class.java).apply {
                putExtra("tool", tool)
            }
        )
    }

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Float,
        color: Int,
        bold: Boolean
    ) {
        parent.addView(TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, dp(6))
        })
    }

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).roundToInt()
}
