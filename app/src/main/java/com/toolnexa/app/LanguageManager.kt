package com.toolnexa.app

import android.app.LocaleManager
import android.content.Context
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

    /*
     * Android 13+ already applies the selected application locale
     * to Activity resources. Calling applicationLocales from onCreate()
     * can trigger a second recreation and cause a black screen or loop.
     *
     * Therefore apply() is intentionally a no-op on Android 13+.
     * The system performs the configuration update when set() changes
     * applicationLocales.
     */
    fun apply(
        context: Context
    ) {
        if (Build.VERSION.SDK_INT >= 33) {
            return
        }

        val code =
            current(context)

        if (code.isBlank()) {
            return
        }

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

        if (Build.VERSION.SDK_INT >= 33) {
            val manager =
                context.getSystemService(
                    LocaleManager::class.java
                )

            if (
                manager.applicationLocales
                    .toLanguageTags() != code
            ) {
                /*
                 * Android recreates affected Activities automatically.
                 * Do not call Activity.recreate() afterwards.
                 */
                manager.applicationLocales =
                    LocaleList.forLanguageTags(code)
            }

            return
        }
    }

    fun current(
        context: Context
    ): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val manager =
                context.getSystemService(
                    LocaleManager::class.java
                )

            val tags =
                manager.applicationLocales
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
