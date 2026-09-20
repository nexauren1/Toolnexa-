package com.toolnexa.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.app.AlertDialog
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import android.graphics.drawable.GradientDrawable

class PlansActivity : Activity() {

    private val auth by lazy {
        FirebaseAuth.getInstance()
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
        showPlans()
    }

    private fun showPlans() {
        val root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setBackgroundColor(bg)
                setPadding(18, 24, 18, 30)
            }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        root.addView(
            title(
                "Planos do ToolNexa",
                29f
            )
        )

        root.addView(
            body(
                "Escolha o nível de utilização que corresponde ao que precisa."
            )
        )

        root.addView(
            planCard(
                "Free",
                "Grátis",
                "Acesso às ferramentas disponíveis no nível Free."
            )
        )

        root.addView(
            planCard(
                "Pro",
                "US$ 5 / mês",
                "Experiência Pro do ToolNexa. O pagamento será processado pelo sistema de subscrição conectado ao backend."
            )
        )

        root.addView(
            body(
                "Estado de pagamento: Sandbox durante esta fase de desenvolvimento."
            )
        )
    }

    private fun planCard(
        name: String,
        price: String,
        description: String
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            background =
                GradientDrawable().apply {
                    setColor(surface)
                    setStroke(1, border)
                    cornerRadius = 18f
                }

            addView(title(name, 21f))
            addView(body(price))
            addView(body(description))

            if (name == "Pro") {
                val subscribe =
                    Button(this@PlansActivity).apply {
                        text = "Assinar Pro"
                        minHeight = 54
                        setTextColor(
                            android.graphics.Color.WHITE
                        )
                        background =
                            GradientDrawable().apply {
                                setColor(
                                    getColor(
                                        R.color.toolnexa_blue
                                    )
                                )
                                cornerRadius = 14f
                            }
                    }

                subscribe.setOnClickListener {
                    startProSubscription()
                }

                addView(
                    subscribe,
                    LinearLayout.LayoutParams(
                        -1,
                        54
                    )
                )
            }
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
            setPadding(0, 6, 0, 8)
        }

    private fun body(
        value: String
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(muted)
            setPadding(0, 4, 0, 12)
        }
}
