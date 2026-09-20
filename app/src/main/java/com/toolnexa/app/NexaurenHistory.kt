package com.toolnexa.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val tool: String,
    val uri: String,
    val name: String,
    val size: Long,
    val timestamp: Long
)

object NexaurenHistory {
    private const val PREFS = "toolnexa_history"
    private const val KEY = "entries"
    private const val MAX = 50

    fun add(
        context: Context,
        tool: String,
        uri: Uri,
        name: String,
        size: Long
    ) {
        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val current =
            try {
                JSONArray(
                    prefs.getString(
                        KEY,
                        "[]"
                    ) ?: "[]"
                )
            } catch (_: Exception) {
                JSONArray()
            }

        val next = JSONArray()

        next.put(
            JSONObject().apply {
                put("tool", tool)
                put("uri", uri.toString())
                put("name", name)
                put("size", size)
                put(
                    "timestamp",
                    System.currentTimeMillis()
                )
            }
        )

        for (i in 0 until current.length()) {
            if (i >= MAX - 1) break
            next.put(current.getJSONObject(i))
        }

        prefs.edit()
            .putString(KEY, next.toString())
            .apply()
    }

    fun list(context: Context): List<HistoryEntry> {
        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val array =
            try {
                JSONArray(
                    prefs.getString(
                        KEY,
                        "[]"
                    ) ?: "[]"
                )
            } catch (_: Exception) {
                JSONArray()
            }

        val result =
            mutableListOf<HistoryEntry>()

        for (i in 0 until array.length()) {
            val item =
                array.optJSONObject(i) ?: continue

            result.add(
                HistoryEntry(
                    tool = item.optString("tool"),
                    uri = item.optString("uri"),
                    name = item.optString("name"),
                    size = item.optLong("size"),
                    timestamp = item.optLong("timestamp")
                )
            )
        }

        return result
    }

    fun clear(context: Context) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .remove(KEY)
            .apply()
    }
}
