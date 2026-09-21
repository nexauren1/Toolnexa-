package com.toolnexa.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

object ProGate {

    private const val PREFS =
        "toolnexa_pro_access"

    private const val KEY_IS_PRO =
        "is_pro"

    private const val KEY_CHECKED =
        "checked"

    fun isCachedPro(
        context: Context
    ): Boolean {
        return context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).getBoolean(
            KEY_IS_PRO,
            false
        )
    }

    fun clearCache(
        context: Context
    ) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .clear()
            .apply()
    }

    fun runOrUpgrade(
        activity: Activity,
        featureName: String,
        onPro: () -> Unit
    ) {
        val auth =
            FirebaseAuth.getInstance()

        val user =
            auth.currentUser

        if (
            user == null
        ) {
            showUpgradeDialog(
                activity,
                featureName,
                requireLogin = true
            )
            return
        }

        if (
            isCachedPro(
                activity
            )
        ) {
            onPro()
            return
        }

        Toast.makeText(
            activity,
            "A verificar o teu plano Pro...",
            Toast.LENGTH_SHORT
        ).show()

        user.getIdToken(
            false
        ).addOnCompleteListener { task ->
            val token =
                task.result?.token

            if (
                !task.isSuccessful ||
                token.isNullOrBlank()
            ) {
                showUpgradeDialog(
                    activity,
                    featureName
                )
                return@addOnCompleteListener
            }

            Thread {
                try {
                    val account =
                        CloudflareApi.loadAccount(
                            token
                        )

                    val isPro =
                        account.planCode.equals(
                            "PRO",
                            ignoreCase = true
                        )

                    activity.runOnUiThread {
                        activity.getSharedPreferences(
                            PREFS,
                            Context.MODE_PRIVATE
                        ).edit()
                            .putBoolean(
                                KEY_IS_PRO,
                                isPro
                            )
                            .putBoolean(
                                KEY_CHECKED,
                                true
                            )
                            .apply()

                        if (
                            isPro
                        ) {
                            onPro()
                        } else {
                            showUpgradeDialog(
                                activity,
                                featureName
                            )
                        }
                    }
                } catch (_: Exception) {
                    activity.runOnUiThread {
                        showUpgradeDialog(
                            activity,
                            featureName
                        )
                    }
                }
            }.start()
        }
    }

    private fun showUpgradeDialog(
        activity: Activity,
        featureName: String,
        requireLogin: Boolean = false
    ) {
        if (
            activity.isFinishing
        ) {
            return
        }

        val title =
            TextView(activity).apply {
                text =
                    if (
                        requireLogin
                    ) {
                        "Inicia sessão"
                    } else {
                        "Recurso Pro"
                    }

                textSize =
                    22f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    activity.getColor(
                        R.color.toolnexa_text
                    )
                )
            }

        val message =
            TextView(activity).apply {
                text =
                    if (
                        requireLogin
                    ) {
                        "Para usar "$featureName", inicia sessão e verifica o teu acesso ao plano Pro."
                    } else {
                        ""$featureName" está disponível no plano Pro."
                    }

                textSize =
                    15f

                setTextColor(
                    activity.getColor(
                        R.color.toolnexa_muted
                    )
                )

                setPadding(
                    0,
                    dp(activity, 8),
                    0,
                    dp(activity, 4)
                )
            }

        val box =
            LinearLayout(activity).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(activity, 22),
                    dp(activity, 18),
                    dp(activity, 22),
                    dp(activity, 8)
                )
            }

        box.addView(
            title
        )

        box.addView(
            message
        )

        val upgrade =
            Button(activity).apply {
                text =
                    if (
                        requireLogin
                    ) {
                        "Abrir conta"
                    } else {
                        "Fazer upgrade para Pro"
                    }

                isAllCaps =
                    false

                minHeight =
                    dp(activity, 50)

                setTextColor(
                    android.graphics.Color.WHITE
                )

                background =
                    GradientDrawable().apply {
                        setColor(
                            activity.getColor(
                                R.color.toolnexa_blue
                            )
                        )
                        cornerRadius =
                            dp(activity, 14).toFloat()
                    }

                setOnClickListener {
                    activity.startActivity(
                        Intent(
                            activity,
                            PlansActivity::class.java
                        )
                    )
                }
            }

        box.addView(
            upgrade,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 50)
            ).apply {
                topMargin =
                    dp(activity, 12)
            }
        )

        val cancel =
            Button(activity).apply {
                text =
                    "Agora não"

                isAllCaps =
                    false

                setOnClickListener {
                    dialog?.dismiss()
                }
            }

        box.addView(
            cancel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 46)
            )
        )

        var dialog:
            AlertDialog? = null

        dialog =
            AlertDialog.Builder(
                activity
            )
                .setView(box)
                .create()

        dialog.show()
    }

    private fun dp(
        context: Context,
        value: Int
    ): Int {
        return (
            value *
                context.resources
                    .displayMetrics
                    .density
            ).toInt()
    }
}
