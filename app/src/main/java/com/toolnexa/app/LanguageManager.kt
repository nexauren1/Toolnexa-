package com.toolnexa.app

import android.content.Context
import java.util.Locale

object LanguageManager {

    const val PORTUGUESE = "pt"
    const val ENGLISH = "en"
    val supported =
        listOf(
            PORTUGUESE,
            ENGLISH
        )
    fun apply(
        context: Context
    ) {
        val code =
            current(context)

        val locale =
            Locale.forLanguageTag(code)

        Locale.setDefault(locale)

        val configuration =
            android.content.res.Configuration(
                context.resources.configuration
            )

        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(
            configuration,
            context.resources.displayMetrics
        )
    }

    fun set(
        context: Context,
        code: String
    ) {
        if (!supported.contains(code)) {
            return
        }

        context
            .getSharedPreferences(
                "toolnexa_language",
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                "language",
                code
            )
            .apply()
    }

    fun current(
        context: Context
    ): String {
        return context
            .getSharedPreferences(
                "toolnexa_language",
                Context.MODE_PRIVATE
            )
            .getString(
                "language",
                PORTUGUESE
            )
            ?.trim()
            ?.lowercase()
            ?.takeIf {
                supported.contains(it)
            }
            ?: PORTUGUESE
    }
}
