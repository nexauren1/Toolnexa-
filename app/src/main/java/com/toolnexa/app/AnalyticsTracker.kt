package com.toolnexa.app

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseUser

class AnalyticsTracker(
    context: Context
) {

    private val analytics =
        FirebaseAnalytics.getInstance(context)

    fun screen(name: String) {
        val bundle = Bundle().apply {
            putString(
                FirebaseAnalytics.Param.SCREEN_NAME,
                name
            )
            putString(
                FirebaseAnalytics.Param.SCREEN_CLASS,
                contextClassName()
            )
        }

        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            bundle
        )
    }

    fun event(
        name: String,
        vararg params: Pair<String, String>
    ) {
        val safeName = name
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .take(40)
            .ifBlank { "toolnexa_event" }

        val bundle = Bundle()

        for ((key, value) in params) {
            bundle.putString(
                key.take(40),
                value.take(100)
            )
        }

        analytics.logEvent(
            safeName,
            bundle
        )
    }

    fun setUser(user: FirebaseUser) {
        analytics.setUserId(user.uid)
        analytics.setUserProperty(
            "auth_provider",
            user.providerData
                .firstOrNull()
                ?.providerId
                ?: "unknown"
        )
    }

    fun clearUser() {
        analytics.setUserId(null)
    }

    private fun contextClassName(): String {
        return "MainActivity"
    }
}
