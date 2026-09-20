package com.toolnexa.app

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
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
            current(context)

        if (code.isBlank()) {
            return
        }

        if (Build.VERSION.SDK_INT >= 33) {
            val manager =
                context.getSystemService(
                    LocaleManager::class.java
                )

            val currentTags =
                manager.applicationLocales
                    .toLanguageTags()

            if (currentTags != code) {
                manager.applicationLocales =
                    LocaleList.forLanguageTags(code)
            }

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

        if (Build.VERSION.SDK_INT >= 33) {
            val manager =
                context.getSystemService(
                    LocaleManager::class.java
                )

            manager.applicationLocales =
                LocaleList.forLanguageTags(code)

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
        if (Build.VERSION.SDK_INT >= 33) {
            val tags =
                context
                    .getSystemService(
                        LocaleManager::class.java
                    )
                    .applicationLocales
                    .toLanguageTags()

            val first =
                tags
                    .split(",")
                    .firstOrNull()
                    ?.trim()
                    ?.lowercase()
                    ?.substringBefore("-")
                    .orEmpty()

            if (supported.contains(first)) {
                return first
            }
        }

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
