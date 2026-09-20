package com.toolnexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
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

    private val textColor by lazy {
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
        LanguageManager.apply(this)
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
                text = getString(R.string.support_back)
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
                text = getString(R.string.support_title)
                textSize = 27f
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(textColor)
            }
        )

        body.addView(
            TextView(this).apply {
                text =
                    getString(
                        R.string.support_description
                    )
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
            Spinner(this).apply {
                adapter =
                    ArrayAdapter(
                        this@SupportActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        resources.getStringArray(
                            R.array.support_types
                        )
                    )
            }

        body.addView(
            label(
                getString(
                    R.string.support_type_label
                )
            )
        )
        body.addView(
            type,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        val tool =
            Spinner(this).apply {
                adapter =
                    ArrayAdapter(
                        this@SupportActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        resources.getStringArray(
                            R.array.support_tools
                        )
                    )
            }

        body.addView(
            label(
                getString(
                    R.string.support_tool_label
                )
            )
        )
        body.addView(
            tool,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        val problem =
            Spinner(this).apply {
                adapter =
                    ArrayAdapter(
                        this@SupportActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        resources.getStringArray(
                            R.array.support_problems
                        )
                    )
            }

        body.addView(
            label(
                getString(
                    R.string.support_problem_label
                )
            )
        )
        body.addView(
            problem,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        val message =
            EditText(this).apply {
                hint =
                    getString(
                        R.string.support_message_hint
                    )
                minLines = 6
                gravity = Gravity.TOP
                setTextColor(textColor)
                setHintTextColor(muted)
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
            label(
                getString(
                    R.string.support_message_label
                )
            )
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
                    getString(
                        R.string.support_diagnostics
                    )
                isChecked = true
                setTextColor(textColor)
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
                    getString(
                        R.string.support_privacy_note
                    )
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
                text =
                    getString(
                        R.string.support_send
                    )
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
            val messageText =
                message.text
                    .toString()
                    .trim()

            if (messageText.isBlank()) {
                message.error =
                    getString(
                        R.string.support_message_required
                    )
                return@setOnClickListener
            }

            val subject =
                getString(
                    R.string.support_subject_prefix
                ) +
                    " " +
                    type.selectedItem
                        .toString() +
                    " • " +
                    tool.selectedItem
                        .toString() +
                    " • " +
                    problem.selectedItem
                        .toString()

            val bodyText =
                buildReport(
                    type.selectedItem
                        .toString(),
                    tool.selectedItem
                        .toString(),
                    problem.selectedItem
                        .toString(),
                    messageText,
                    includeDiagnostics.isChecked
                )

            analytics.event(
                "support_email_opened",
                "type" to
                    type.selectedItem
                        .toString(),
                "tool" to
                    tool.selectedItem
                        .toString(),
                "problem" to
                    problem.selectedItem
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
        problem: String,
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
            appendLine("Type: $type")
            appendLine("Area/tool: $tool")
            appendLine("Problem: $problem")
            appendLine("Date: $date")
            appendLine("Message:")
            appendLine(message)
            appendLine()

            appendLine("Account")
            appendLine("-------")
            appendLine(
                "Name: " +
                    (user?.displayName
                        ?: "Not available")
            )
            appendLine(
                "Account email: " +
                    (user?.email
                        ?: "Not available")
            )
            appendLine()

            if (diagnostics) {
                appendLine("Technical diagnostics")
                appendLine("---------------------")
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
                    "Manufacturer: " +
                        android.os.Build.MANUFACTURER
                )
                appendLine(
                    "Model: " +
                        android.os.Build.MODEL
                )
                appendLine(
                    "Language: " +
                        Locale.getDefault().toLanguageTag()
                )
                appendLine(
                    "Country/region: " +
                        Locale.getDefault().country
                )
            }

            appendLine()
            appendLine(
                "The email opens for review before sending. No password, auth token or private key is included."
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
                getString(
                    R.string.support_no_email_app
                ),
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
            setTextColor(textColor)
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
