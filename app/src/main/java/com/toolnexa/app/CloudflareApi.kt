package com.toolnexa.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object CloudflareApi {
    private const val BASE_URL =
        "https://toolnexa.nexaurenstore.workers.dev"

    fun removeBackground(
        bitmap: Bitmap
    ): Bitmap? {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            bytes
        )

        val connection =
            URL(
                BASE_URL +
                    "/api/tools/background-remover"
            ).openConnection()
                as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 30_000
            connection.readTimeout = 120_000
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "image/png"
            )
            connection.setRequestProperty(
                "X-ToolNexa-Version",
                BuildConfig.VERSION_NAME
            )

            connection.outputStream.use { output ->
                output.write(bytes.toByteArray())
            }

            if (
                connection.responseCode !in
                200..299
            ) {
                return null
            }

            connection.inputStream.use {
                BitmapFactory.decodeStream(it)
            }
        } finally {
            connection.disconnect()
        }
    }
}
