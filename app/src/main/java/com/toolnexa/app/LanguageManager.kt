package com.toolnexa.app

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {

    const val PORTUGUESE = "pt"
    const val ENGLISH = "en"
    const val SPANISH = "es"
    const val FRENCH = "fr"
    const val ARABIC = "ar"

    val supported =
        listOf(
            PORTUGUESE,
            ENGLISH,
            SPANISH,
            FRENCH,
            ARABIC
        )

    fun apply(
        context: Context
    ) {
        val code =
            context
                .getSharedPreferences(
                    "toolnexa_language",
                    Context.MODE_PRIVATE
                )
                .getString(
                    "language",
                    ""
                )
                ?.trim()
                .orEmpty()

        if (code.isBlank()) {
            return
        }

        val locale =
            Locale.forLanguageTag(code)

        Locale.setDefault(locale)

        val configuration =
            Configuration(
                context.resources.configuration
            )

        configuration.setLocale(locale)
        configuration.setLayoutDirection(
            locale
        )

        context.resources.updateConfiguration(
            configuration,
            context.resources.displayMetrics
        )
    }

    fun set(
        context: Context,
        code: String
    ) {
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
    ): String =
        context
            .getSharedPreferences(
                "toolnexa_language",
                Context.MODE_PRIVATE
            )
            .getString(
                "language",
                "pt"
            ) ?: "pt"
}
