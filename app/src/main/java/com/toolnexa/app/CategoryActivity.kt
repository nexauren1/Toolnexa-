package com.toolnexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import kotlin.math.roundToInt

class CategoryActivity : Activity() {

    private val analytics by lazy { AnalyticsTracker(this) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        LanguageManager.apply(this)

        val categoryName =
            intent.getStringExtra("category")
                ?: "Imagem"

        analytics.screen(
            "category_" +
                categoryName.lowercase()
        )

        build(categoryName)
    }

    private fun build(
        categoryName: String
    ) {
        val definition =
            CategoryCatalog.all.firstOrNull {
                it.name == categoryName
            } ?: CategoryCatalog.all.first()

        val root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setBackgroundColor(
                    getColor(
                        R.color.toolnexa_bg
                    )
                )
            }

        val header =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(16),
                    dp(16),
                    dp(16),
                    dp(14)
                )
                background =
                    GradientDrawable().apply {
                        setColor(
                            getColor(
                                R.color.toolnexa_surface
                            )
                        )
                        setStroke(
                            dp(1),
                            getColor(
                                R.color.toolnexa_border
                            )
                        )
                    }
                elevation =
                    dp(3).toFloat()
            }

        val iconBox =
            LinearLayout(this).apply {
                gravity = Gravity.CENTER
                background =
                    GradientDrawable().apply {
                        setColor(
                            definition.softColor
                        )
                        cornerRadius =
                            dp(14).toFloat()
                    }
            }

        iconBox.addView(
            ImageView(this).apply {
                setImageResource(
                    definition.icon
                )
                setColorFilter(
                    definition.accent
                )
                contentDescription =
                    definition.name
            },
            LinearLayout.LayoutParams(
                dp(34),
                dp(34)
            )
        )

        header.addView(
            iconBox,
            LinearLayout.LayoutParams(
                dp(52),
                dp(52)
            )
        )

        val headerText =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(12),
                    0,
                    0,
                    0
                )
            }

        headerText.addView(
            TextView(this).apply {
                text = "ToolNexa"
                textSize = 18f
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(
                    getColor(
                        R.color.toolnexa_text
                    )
                )
            }
        )

        headerText.addView(
            TextView(this).apply {
                text = definition.name
                textSize = 13f
                setTextColor(
                    definition.accent
                )
            }
        )

        header.addView(
            headerText,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        root.addView(header)

        val brandRail =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
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

        val scroll =
            ScrollView(this)

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

        val tools =
            CategoryCatalog.toolsFor(
                definition.name
            )

        body.addView(
            TextView(this).apply {
                text =
                    "Categoria " +
                        definition.name
                textSize = 27f
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(
                    getColor(
                        R.color.toolnexa_text
                    )
                )
            }
        )

        body.addView(
            TextView(this).apply {
                text =
                    definition.description
                textSize = 15f
                setTextColor(
                    getColor(
                        R.color.toolnexa_muted
                    )
                )
                setPadding(
                    0,
                    dp(4),
                    0,
                    dp(8)
                )
            }
        )

        val search = EditText(this).apply {
            hint = "Pesquisar ferramenta"
            textSize = 15f
            setSingleLine(true)
            setTextColor(
                getColor(R.color.toolnexa_text)
            )
            setHintTextColor(
                getColor(R.color.toolnexa_muted)
            )
            setPadding(
                dp(14),
                dp(8),
                dp(14),
                dp(8)
            )
            background =
                GradientDrawable().apply {
                    setColor(
                        getColor(
                            R.color.toolnexa_surface
                        )
                    )
                    setStroke(
                        dp(1),
                        definition.accent
                    )
                    cornerRadius =
                        dp(14).toFloat()
                }
            setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_menu_search,
                0,
                0,
                0
            )
            compoundDrawablePadding = dp(10)
        }

        body.addView(
            search,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                setMargins(
                    0,
                    dp(12),
                    0,
                    dp(4)
                )
            }
        )

        val countLabel =
            TextView(this).apply {
                textSize = 12f
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(
                    definition.accent
                )
                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(4)
                )
            }

        body.addView(countLabel)

        val toolsContainer =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        body.addView(
            toolsContainer,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        fun renderTools(query: String) {
            toolsContainer.removeAllViews()

            val normalized =
                query.trim().lowercase()

            val filtered =
                if (normalized.isBlank()) {
                    tools
                } else {
                    tools.filter {
                        it.name.lowercase().contains(normalized) ||
                            it.description
                                .lowercase()
                                .contains(normalized)
                    }
                }

            if (tools.isEmpty()) {
                countLabel.text =
                    "0 ferramentas"

                val empty =
                    LinearLayout(this).apply {
                        orientation =
                            LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setPadding(
                            dp(24),
                            dp(32),
                            dp(24),
                            dp(32)
                        )
                        background =
                            GradientDrawable().apply {
                                setColor(
                                    getColor(
                                        R.color.toolnexa_surface
                                    )
                                )
                                setStroke(
                                    dp(1),
                                    getColor(
                                        R.color.toolnexa_border
                                    )
                                )
                                cornerRadius =
                                    dp(18).toFloat()
                            }
                    }

                empty.addView(
                    TextView(this).apply {
                        text =
                            "Categoria preparada"
                        textSize = 19f
                        typeface =
                            android.graphics.Typeface.DEFAULT_BOLD
                        setTextColor(
                            definition.accent
                        )
                        gravity = Gravity.CENTER
                    }
                )

                empty.addView(
                    TextView(this).apply {
                        text =
                            "As ferramentas desta categoria serão adicionadas em breve."
                        textSize = 14f
                        setTextColor(
                            getColor(
                                R.color.toolnexa_muted
                            )
                        )
                        gravity = Gravity.CENTER
                        setPadding(
                            0,
                            dp(8),
                            0,
                            0
                        )
                    }
                )

                toolsContainer.addView(empty)
                return
            }

            countLabel.text =
                filtered.size.toString() +
                    if (filtered.size == 1) {
                        " ferramenta"
                    } else {
                        " ferramentas"
                    }

            if (filtered.isEmpty()) {
                toolsContainer.addView(
                    TextView(this).apply {
                        text =
                            "Nenhuma ferramenta encontrada."
                        textSize = 14f
                        setTextColor(
                            getColor(
                                R.color.toolnexa_muted
                            )
                        )
                        gravity = Gravity.CENTER
                        setPadding(
                            0,
                            dp(28),
                            0,
                            dp(28)
                        )
                    }
                )
                return
            }

            for (index in filtered.indices step 2) {
                val row =
                    LinearLayout(this).apply {
                        orientation =
                            LinearLayout.HORIZONTAL
                    }

                addToolGridCard(
                    row,
                    definition,
                    filtered[index]
                )

                if (
                    index + 1 <
                        filtered.size
                ) {
                    addToolGridCard(
                        row,
                        definition,
                        filtered[index + 1]
                    )
                } else {
                    row.addView(
                        Space(this),
                        LinearLayout.LayoutParams(
                            0,
                            dp(166),
                            1f
                        ).apply {
                            setMargins(
                                dp(6),
                                dp(6),
                                dp(6),
                                dp(6)
                            )
                        }
                    )
                }

                toolsContainer.addView(
                    row,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(166)
                    )
                )
            }
        }

        search.addTextChangedListener(
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
                    analytics.event(
                        "category_tool_search",
                        "category" to definition.name,
                        "query_length" to
                            (s?.length ?: 0).toString()
                    )
                    renderTools(
                        s?.toString() ?: ""
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) = Unit
            }
        )

        renderTools("")

        root.post {
            if (!isFinishing) {
                I18n.localizeWindow(this)
            }
        }
    }

        private fun addToolGridCard(
        row: LinearLayout,
        definition: CategoryDefinition,
        tool: ToolDefinition
    ) {
        val item =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(14),
                    dp(13),
                    dp(12),
                    dp(12)
                )

                background =
                    GradientDrawable().apply {
                        setColor(
                            getColor(
                                R.color.toolnexa_surface
                            )
                        )
                        setStroke(
                            dp(1),
                            getColor(
                                R.color.toolnexa_border
                            )
                        )
                        cornerRadius =
                            dp(18).toFloat()
                    }

                alpha = 0f
                translationY =
                    dp(8).toFloat()
            }

        val top =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val iconBox =
            LinearLayout(this).apply {
                gravity = Gravity.CENTER
                background =
                    GradientDrawable().apply {
                        setColor(
                            definition.softColor
                        )
                        cornerRadius =
                            dp(13).toFloat()
                    }
            }

        iconBox.addView(
            ImageView(this).apply {
                setImageResource(
                    tool.icon
                )
                setColorFilter(
                    definition.accent
                )
                contentDescription =
                    tool.name
            },
            LinearLayout.LayoutParams(
                dp(34),
                dp(34)
            )
        )

        top.addView(
            iconBox,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        top.addView(
            Space(this),
            LinearLayout.LayoutParams(
                0,
                1,
                1f
            )
        )

        top.addView(
            TextView(this).apply {
                text = "›"
                textSize = 26f
                setTextColor(
                    definition.accent
                )
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                dp(22),
                dp(42)
            )
        )

        item.addView(
            top,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            )
        )

        item.addView(
            TextView(this).apply {
                text = tool.name
                textSize = 15.5f
                typeface =
                    android.graphics.Typeface
                        .DEFAULT_BOLD
                setTextColor(
                    getColor(
                        R.color.toolnexa_text
                    )
                )
                maxLines = 2
                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(2)
                )
            }
        )

        item.addView(
            TextView(this).apply {
                text = tool.description
                textSize = 12f
                setTextColor(
                    getColor(
                        R.color.toolnexa_muted
                    )
                )
                maxLines = 2
            }
        )

        item.setOnClickListener {
            analytics.event(
                "category_tool_open",
                "category" to
                    definition.name,
                "tool_name" to
                    tool.name
            )

            if (
                tool.name ==
                    "Background Remover"
            ) {
                startActivity(
                    Intent(
                        this,
                        BackgroundRemoverActivity::class.java
                    )
                )
            } else {
                startActivity(
                    Intent(
                        this,
                        ToolWorkflowActivity::class.java
                    ).apply {
                        putExtra(
                            "tool",
                            when (tool.name) {
                                "Image Compressor" ->
                                    "compressor"

                                "Image Resizer" ->
                                    "resizer"

                                else ->
                                    "converter"
                            }
                        )
                    }
                )
            }
        }

        row.addView(
            item,
            LinearLayout.LayoutParams(
                0,
                dp(166),
                1f
            ).apply {
                setMargins(
                    dp(6),
                    dp(6),
                    dp(6),
                    dp(6)
                )
            }
        )

        item.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(240)
            .start()
    }

    private fun dp(value: Int): Int {
        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }
}
