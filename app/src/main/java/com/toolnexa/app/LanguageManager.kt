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

    private const val PREFS =
        "toolnexa_language"

    private const val KEY =
        "language"

    /*
     * ToolNexa uses its own I18n layer for runtime text.
     * Do not mutate Resources or trigger Activity recreation here.
     */
    fun apply(
        context: Context
    ) {
        // Runtime translation is handled by I18n.
        // Do not mutate the process-wide Locale while Activities run.
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
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY,
                code
            )
            .commit()
    }

    fun current(
        context: Context
    ): String {
        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY,
                PORTUGUESE
            )
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf {
                supported.contains(it)
            }
            ?: PORTUGUESE
    }
}
