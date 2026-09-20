package com.toolnexa.app

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object CloudflareApi {

    private const val BASE_URL =
        "https://toolnexa.nexaurenstore.workers.dev"

    fun removeBackground(
        context: Context,
        uri: Uri,
        firebaseIdToken: String
    ): File {
        val size = querySize(
            context,
            uri
        )

        if (size > MAX_UPLOAD_BYTES) {
            throw IOException(
                "A imagem deve ter no máximo 20 MB."
            )
        }

        val connection =
            (URL(
                BASE_URL +
                    "/api/tools/background-remover"
            ).openConnection()
                as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 120_000
                requestMethod = "POST"
                doOutput = true
                setChunkedStreamingMode(
                    64 * 1024
                )
                setRequestProperty(
                    "Authorization",
                    "Bearer " +
                        firebaseIdToken
                )
                setRequestProperty(
                    "Content-Type",
                    context.contentResolver
                        .getType(uri)
                        ?: "image/jpeg"
                )
                setRequestProperty(
                    "Accept",
                    "image/png"
                )
                setRequestProperty(
                    "User-Agent",
                    "ToolNexa-Android/" +
                        BuildConfig.VERSION_NAME
                )
            }

        try {
            context.contentResolver
                .openInputStream(uri)
                ?.use { input ->
                    connection.outputStream
                        .use { output ->
                            val buffer =
                                ByteArray(
                                    64 * 1024
                                )

                            while (true) {
                                val read =
                                    input.read(
                                        buffer
                                    )

                                if (read <= 0) {
                                    break
                                }

                                output.write(
                                    buffer,
                                    0,
                                    read
                                )
                            }
                        }
                }
                ?: throw IOException(
                    "Não foi possível abrir a imagem."
                )

            val status =
                connection.responseCode

            if (status !in 200..299) {
                val message =
                    readError(
                        connection
                    )

                throw IOException(
                    message.ifBlank {
                        "O servidor não conseguiu processar a imagem."
                    }
                )
            }

            val file =
                File.createTempFile(
                    "toolnexa-bg-",
                    ".png",
                    context.cacheDir
                )

            FileOutputStream(file)
                .use { output ->
                    connection.inputStream
                        .use { input ->
                            input.copyTo(
                                output,
                                64 * 1024
                            )
                        }
                }

            if (
                !file.exists() ||
                file.length() == 0L
            ) {
                file.delete()
                throw IOException(
                    "O resultado ficou vazio."
                )
            }

            return file
        } finally {
            connection.disconnect()
        }
    }

    fun loadPlans():
        List<PlanInfo> {
        val connection =
            (URL(
                BASE_URL +
                    "/api/plans"
            ).openConnection()
                as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                requestMethod = "GET"
                setRequestProperty(
                    "Accept",
                    "application/json"
                )
                setRequestProperty(
                    "User-Agent",
                    "ToolNexa-Android/" +
                        BuildConfig.VERSION_NAME
                )
            }

        try {
            val status =
                connection.responseCode

            if (status !in 200..299) {
                throw IOException(
                    "Não foi possível carregar os planos."
                )
            }

            val body =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            val root =
                JSONObject(body)

            val array =
                root.optJSONArray(
                    "plans"
                ) ?: JSONArray()

            val plans =
                mutableListOf<PlanInfo>()

            for (
                index in
                0 until array.length()
            ) {
                val item =
                    array.getJSONObject(
                        index
                    )

                plans.add(
                    PlanInfo(
                        code =
                            item.optString(
                                "code"
                            ),
                        name =
                            item.optString(
                                "name"
                            ),
                        priceUsd =
                            item.optString(
                                "price_usd"
                            ),
                        interval =
                            item.optString(
                                "billing_interval"
                            ),
                        paypalPlanId =
                            item.optString(
                                "paypal_plan_id"
                            ).ifBlank {
                                null
                            }
                    )
                )
            }

            return plans
        } finally {
            connection.disconnect()
        }
    }

    fun createProSubscription(
        firebaseIdToken: String
    ): String {
        val connection =
            (URL(
                BASE_URL +
                    "/api/paypal/create-subscription"
            ).openConnection()
                as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 45_000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty(
                    "Authorization",
                    "Bearer " +
                        firebaseIdToken
                )
                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
                setRequestProperty(
                    "Accept",
                    "application/json"
                )
            }

        try {
            connection.outputStream
                .use {
                    it.write(
                        """{"plan_code":"pro"}"""
                            .toByteArray(
                                Charsets.UTF_8
                            )
                    )
                }

            val status =
                connection.responseCode

            val stream =
                if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val body =
                stream
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
                    ?: ""

            val json =
                JSONObject(
                    body.ifBlank {
                        "{}"
                    }
                )

            if (status !in 200..299) {
                throw IOException(
                    json.optString(
                        "message",
                        "Não foi possível iniciar o PayPal Sandbox."
                    )
                )
            }

            return json.optString(
                "approval_url"
            ).ifBlank {
                throw IOException(
                    "O PayPal não devolveu o link de aprovação."
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    fun loadAccount(
        firebaseIdToken: String
    ): AccountInfo {
        val connection =
            (URL(
                BASE_URL +
                    "/api/account"
            ).openConnection()
                as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                requestMethod = "GET"
                setRequestProperty(
                    "Authorization",
                    "Bearer " +
                        firebaseIdToken
                )
                setRequestProperty(
                    "Accept",
                    "application/json"
                )
            }

        try {
            val status =
                connection.responseCode

            val stream =
                if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val body =
                stream
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
                    ?: ""

            val root =
                JSONObject(
                    body.ifBlank {
                        "{}"
                    }
                )

            if (status !in 200..299) {
                throw IOException(
                    root.optString(
                        "message",
                        "Não foi possível verificar o plano."
                    )
                )
            }

            val account =
                root.optJSONObject(
                    "account"
                ) ?: throw IOException(
                    "Resposta de conta inválida."
                )

            val plan =
                account.optJSONObject(
                    "plan"
                ) ?: throw IOException(
                    "Plano não encontrado."
                )

            val subscription =
                account.optJSONObject(
                    "subscription"
                )

            return AccountInfo(
                uid =
                    account.optString(
                        "uid"
                    ),
                planCode =
                    plan.optString(
                        "code",
                        "free"
                    ),
                planName =
                    plan.optString(
                        "name",
                        "Free"
                    ),
                priceUsd =
                    plan.optString(
                        "price_usd",
                        "0.00"
                    ),
                subscriptionStatus =
                    subscription
                        ?.optString(
                            "status"
                        )
                        ?.ifBlank {
                            null
                        }
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun readError(
        connection: HttpURLConnection
    ): String {
        return try {
            val body =
                connection.errorStream
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
                    ?: ""

            if (body.isBlank()) {
                return ""
            }

            JSONObject(body)
                .optString(
                    "message",
                    ""
                )
        } catch (_: Exception) {
            ""
        }
    }

    private fun querySize(
        context: Context,
        uri: Uri
    ): Long {
        var cursor: Cursor? = null

        return try {
            cursor =
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        OpenableColumns.SIZE
                    ),
                    null,
                    null,
                    null
                )

            if (
                cursor != null &&
                cursor.moveToFirst()
            ) {
                cursor.getLong(0)
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        } finally {
            cursor?.close()
        }
    }

    data class PlanInfo(
        val code: String,
        val name: String,
        val priceUsd: String,
        val interval: String,
        val paypalPlanId: String?
    )

    data class AccountInfo(
        val uid: String,
        val planCode: String,
        val planName: String,
        val priceUsd: String,
        val subscriptionStatus: String?
    )

    private const val MAX_UPLOAD_BYTES =
        20L * 1024L * 1024L
}
