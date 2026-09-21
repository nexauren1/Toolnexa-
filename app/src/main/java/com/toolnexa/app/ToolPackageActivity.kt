package com.toolnexa.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class ToolPackageActivity : Activity() {

    private lateinit var packageInfo:
        ToolPackageCatalog.Package

    private lateinit var progress:
        ProgressBar

    private lateinit var status:
        TextView

    private var targetToolId: String? = null

    private val packageManager by lazy {
        ToolPackageManager(this)
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        LanguageManager.apply(this)
        packageManager.prepareActivity()

        val category =
            intent.getStringExtra("category")
                ?: "Business"

        targetToolId =
            intent.getStringExtra("tool_id")

        packageInfo =
            ToolPackageCatalog.forCategory(
                category
            ) ?: run {
                finish()
                return
            }

        build()
    }

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                dp(24),
                dp(30),
                dp(24),
                dp(30)
            )
            setBackgroundColor(
                getColor(R.color.toolnexa_bg)
            )
        }

        root.addView(
            TextView(this).apply {
                text =
                    "Pacote " + packageInfo.category
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(
                    getColor(R.color.toolnexa_text)
                )
                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )
            }
        )

        root.addView(
            TextView(this).apply {
                text =
                    packageInfo.toolCount.toString() +
                        " ferramentas profissionais"
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(
                    getColor(R.color.toolnexa_muted)
                )
                setPadding(
                    0,
                    dp(10),
                    0,
                    dp(4)
                )
            }
        )

        root.addView(
            TextView(this).apply {
                text =
                    "Para manter o ToolNexa leve, esta categoria é baixada separadamente. Você só baixa o que pretende usar."
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(
                    getColor(R.color.toolnexa_muted)
                )
                setPadding(
                    0,
                    dp(4),
                    0,
                    dp(20)
                )
            }
        )

        progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = 0
        }

        root.addView(
            progress,
            LinearLayout.LayoutParams(
                -1,
                dp(12)
            )
        )

        status = TextView(this).apply {
            text = "Verificando pacote..."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(
                getColor(R.color.toolnexa_muted)
            )
            setPadding(
                0,
                dp(12),
                0,
                dp(16)
            )
        }
        root.addView(status)

        val button = Button(this).apply {
            text = "Baixar pacote"
        }

        root.addView(
            button,
            LinearLayout.LayoutParams(
                -1,
                dp(54)
            )
        )

        button.setOnClickListener {
            button.isEnabled = false
            status.text =
                "Baixando pacote..."
            packageManager.download(
                packageInfo.module,
                onProgress = {
                    runOnUiThread {
                        progress.progress = it
                        status.text =
                            "Baixando... " + it + "%"
                    }
                },
                onInstalled = {
                    runOnUiThread {
                        status.text =
                            "Pacote instalado com sucesso."
                        openTarget()
                    }
                },
                onError = {
                    runOnUiThread {
                        button.isEnabled = true
                        status.text = it
                        Toast.makeText(
                            this,
                            it,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }

        if (
            packageManager.isInstalled(
                packageInfo.module
            )
        ) {
            progress.progress = 100
            status.text =
                "Pacote já instalado. Abrindo..."
            button.text = "Abrir ferramentas"
            button.setOnClickListener {
                openTarget()
            }
            root.postDelayed(
                { openTarget() },
                220
            )
        }

        setContentView(root)
    }

    private fun openTarget() {
        packageManager.prepareActivity()

        val intent =
            if (targetToolId != null) {
                Intent().apply {
                    setClassName(
                        this@ToolPackageActivity,
                        "com.toolnexa.business.BusinessToolActivity"
                    )
                    putExtra(
                        "tool_id",
                        targetToolId
                    )
                }
            } else {
                Intent(
                    this,
                    CategoryActivity::class.java
                ).apply {
                    putExtra(
                        "category",
                        packageInfo.category
                    )
                    putExtra(
                        "skip_package",
                        true
                    )
                }
            }

        startActivity(intent)
        finish()
    }

    private fun dp(value: Int): Int {
        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }
}
