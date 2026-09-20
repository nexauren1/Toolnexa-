package com.toolnexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class StorageActivity : Activity() {

    private val analytics by lazy { AnalyticsTracker(this) }
    private val blue by lazy { getColor(R.color.toolnexa_blue) }
    private val bg by lazy { getColor(R.color.toolnexa_bg) }
    private val surface by lazy { getColor(R.color.toolnexa_surface) }
    private val textColor by lazy { getColor(R.color.toolnexa_text) }
    private val muted by lazy { getColor(R.color.toolnexa_muted) }
    private val border by lazy { getColor(R.color.toolnexa_border) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        analytics.screen("storage")
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
                text = "Nexauren X"
                textSize = 13f
                setTextColor(blue)
            }
        )

        root.addView(header)

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(30))
        }

        body.addView(
            TextView(this).apply {
                text = "Pasta de armazenamento"
                textSize = 27f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(textColor)
            }
        )

        val current = NexaurenStorage.rootName(this)
            ?: "Nenhuma pasta definida"

        val folderCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = GradientDrawable().apply {
                setColor(surface)
                setStroke(dp(1), border)
                cornerRadius = dp(18).toFloat()
            }
        }

        folderCard.addView(
            TextView(this).apply {
                text = "Destino atual"
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(blue)
            }
        )

        folderCard.addView(
            TextView(this).apply {
                text = current
                textSize = 18f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(textColor)
                setPadding(0, dp(6), 0, dp(12))
            }
        )

        folderCard.addView(
            TextView(this).apply {
                text =
                    "Resultados: Nexauren X / Imagem / ferramenta"
                textSize = 13f
                setTextColor(muted)
            }
        )

        body.addView(
            folderCard,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(12), 0, dp(12))
            }
        )

        val choose = Button(this).apply {
            text =
                if (NexaurenStorage.hasRoot(this@StorageActivity)) {
                    "Alterar pasta"
                } else {
                    "Escolher pasta"
                }
        }
        stylePrimary(choose)
        choose.setOnClickListener {
            analytics.event("storage_change_start")
            startActivityForResult(
                Intent(
                    Intent.ACTION_OPEN_DOCUMENT_TREE
                ).apply {
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    )
                },
                REQUEST_FOLDER
            )
        }

        body.addView(
            choose,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        body.addView(
            TextView(this).apply {
                text =
                    "A escolha é guardada e reutilizada nos próximos resultados."
                textSize = 14f
                setTextColor(muted)
                setPadding(
                    0,
                    dp(14),
                    0,
                    0
                )
            }
        )

        root.addView(
            body,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
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
            requestCode == REQUEST_FOLDER &&
            resultCode == RESULT_OK &&
            data?.data != null
        ) {
            val uri = data.data!!

            try {
                val flags =
                    data.flags and
                        (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                if (flags != 0) {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        flags
                    )
                }
            } catch (_: Exception) {
            }

            NexaurenStorage.setRoot(
                this,
                uri
            )

            analytics.event(
                "storage_changed",
                "folder" to (
                    NexaurenStorage.rootName(
                        this
                    ) ?: "Nexauren X"
                    )
            )

            Toast.makeText(
                this,
                "Pasta atualizada.",
                Toast.LENGTH_SHORT
            ).show()

            build()
        }
    }

    private fun stylePrimary(button: Button) {
        button.setTextColor(Color.WHITE)
        button.textSize = 15f
        button.background =
            GradientDrawable().apply {
                setColor(blue)
                cornerRadius = dp(14).toFloat()
            }
        button.minHeight = dp(52)
        button.stateListAnimator = null
    }

    private fun dp(value: Int): Int {
        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }

    companion object {
        private const val REQUEST_FOLDER = 1101
    }
}
