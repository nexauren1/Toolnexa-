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

    fun generateAiBackground(
        context: Context,
        prompt: String,
        firebaseIdToken: String
    ): File {
        val connection =
            (URL(
                BASE_URL +
                    "/api/tools/background-remover/ai-background"
            ).openConnection()
                as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 120_000
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
                .use { output ->
                    val body =
                        JSONObject()
                            .put(
                                "prompt",
                                prompt
                            )
                            .toString()

                    output.write(
                        body.toByteArray(
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
                        "A IA não conseguiu gerar o fundo."
                    )
                )
            }

            val dataUri =
                json.optString(
                    "data_uri"
                )

            val base64 =
                dataUri.substringAfter(
                    "base64,",
                    ""
                )

            if (base64.isBlank()) {
                throw IOException(
                    "A IA devolveu uma imagem inválida."
                )
            }

            val bytes =
                android.util.Base64.decode(
                    base64,
                    android.util.Base64.DEFAULT
                )

            if (bytes.isEmpty()) {
                throw IOException(
                    "A imagem gerada está vazia."
                )
            }

            val file =
                File.createTempFile(
                    "toolnexa-ai-bg-",
                    ".jpg",
                    context.cacheDir
                )

            FileOutputStream(file)
                .use {
                    it.write(bytes)
                }

            return file
        } finally {
            connection.disconnect()
        }
    }

    fun loadPlans():
        List<PlanInfo> {
        val connection =
            openConnection(
                "/api/plans",
                "GET",
                null
            )

        try {
            val result =
                readJsonResponse(
                    connection
                )

            val array =
                result.optJSONArray(
                    "plans"
                ) ?: JSONArray()

            val plans =
                mutableListOf<PlanInfo>()

            for (
                index in
                0 until array.length()
            ) {
                val item =
                    array.optJSONObject(
                        index
                    ) ?: continue

                plans.add(
                    PlanInfo(
                        code =
                            item.optString(
                                "id",
                                item.optString(
                                    "code"
                                )
                            ),
                        name =
                            item.optString(
                                "name"
                            ),
                        priceUsd =
                            item.optString(
                                "priceUsd",
                                item.optString(
                                    "price_usd"
                                )
                            ),
                        interval =
                            item.optString(
                                "billingInterval",
                                item.optString(
                                    "billing_interval"
                                )
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
    ): PayPalSubscriptionInfo {
        val connection =
            openConnection(
                "/api/paypal/create-subscription",
                "POST",
                firebaseIdToken
            )

        try {
            connection.outputStream
                .use {
                    it.write(
                        JSONObject()
                            .put(
                                "planId",
                                "PRO"
                            )
                            .toString()
                            .toByteArray(
                                Charsets.UTF_8
                            )
                    )
                }

            val json =
                readJsonResponse(
                    connection
                )

            val id =
                json.optString(
                    "subscriptionId"
                )

            val approvalUrl =
                json.optString(
                    "approvalUrl",
                    json.optString(
                        "approval_url"
                    )
                )

            if (
                id.isBlank() ||
                approvalUrl.isBlank()
            ) {
                throw IOException(
                    "O PayPal não devolveu uma assinatura válida."
                )
            }

            return PayPalSubscriptionInfo(
                subscriptionId = id,
                approvalUrl = approvalUrl
            )
        } finally {
            connection.disconnect()
        }
    }

    fun activateProSubscription(
        firebaseIdToken: String,
        subscriptionId: String
    ): AccountInfo {
        val connection =
            openConnection(
                "/api/paypal/activate-subscription",
                "POST",
                firebaseIdToken
            )

        try {
            connection.outputStream
                .use {
                    it.write(
                        JSONObject()
                            .put(
                                "subscriptionId",
                                subscriptionId
                            )
                            .toString()
                            .toByteArray(
                                Charsets.UTF_8
                            )
                    )
                }

            val json =
                readJsonResponse(
                    connection
                )

            return AccountInfo(
                uid = "",
                planCode =
                    json.optString(
                        "plan",
                        "FREE"
                    ),
                planName =
                    json.optString(
                        "planName",
                        json.optString(
                            "plan",
                            "Free"
                  fun loadAccount(
        firebaseIdToken: String
    ): AccountInfo {
        val connection =
            openConnection(
                "/api/entitlement",
                "GET",
                firebaseIdToken
            )

        try {
            val root =
                readJsonResponse(
                    connection
                )

            val planCode =
                root.optString(
                    "plan",
                    "FREE"
                ).uppercase()

            return AccountInfo(
                uid = "",
                planCode =
                    planCode,
                planName =
                    root.optString(
                        "planName",
                        if (
                            planCode ==
                            "PRO"
                        ) {
                            "Pro"
                        } else {
                            "Free"
                        }
                    ),
                priceUsd =
                    root.optString(
                        "priceUsd",
                        if (
                            planCode ==
                            "PRO"
                        ) {
                            "5.00"
                        } else {
                            "0.00"
                        }
                    ),
                subscriptionStatus =
                    root.optString(
                        "status"
                    ).ifBlank {
                        null
                    }
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(
        path: String,
        method: String,
        firebaseIdToken: String?
    ): HttpURLConnection {
        return (
            URL(
                BASE_URL + path
            ).openConnection()
                as HttpURLConnection
        ).apply {
            connectTimeout =
                if (
                    path.contains(
                        "/paypal/"
                    )
                ) {
                    30_000
                } else {
                    15_000
                }

            readTimeout =
                if (
                    path.contains(
                        "/paypal/"
                    )
                ) {
                    60_000
                } else {
                    30_000
                }

            requestMethod =
                method

            doInput =
                true

            if (
                method != "GET"
            ) {
                doOutput =
                    true
            }

            setRequestProperty(
                "Accept",
                "application/json"
            )

            setRequestProperty(
                "User-Agent",
                "ToolNexa-Android/" +
                    BuildConfig.VERSION_NAME
            )

            if (
                !firebaseIdToken
                    .isNullOrBlank()
            ) {
                setRequestProperty(
                    "Authorization",
                    "Bearer " +
                        firebaseIdToken
                )
            }

            if (
                method != "GET"
            ) {
                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
            }
        }
    }

    private fun readJsonResponse(
        connection: HttpURLConnection
    ): JSONObject {
        val status =
            connection.responseCode

        val stream =
            if (
                status in
                200..299
            ) {
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

        if (
            status !in
            200..299
        ) {
            throw IOException(
                serverMessage(
                    json,
                    "O servidor recusou o pedido."
                )
            )
        }

        return json
    }

    private fun serverMessage(
        json: JSONObject,
        fallback: String
    ): String {
        val message =
            json.optString(
                "message",
                ""
            ).trim()

        if (
            message.isNotBlank()
        ) {
            return message
        }

        val error =
            json.optString(
                "error",
                ""
            ).trim()

        if (
            error.isNotBlank()
        ) {
            return error
        }

        val details =
            json.optJSONArray(
                "details"
            )

        if (
            details != null
        ) {
            for (
                index in
                0 until
                details.length()
            ) {
                val item =
                    details.optJSONObject(
                        index
                    ) ?: continue

                val description =
                    item.optString(
                        "description",
                        ""
                    ).trim()

                if (
                    description.isNotBlank()
                ) {
                    return description
                }

                val issue =
                    item.optString(
                        "issue",
                        ""
                    ).trim()

                if (
                    issue.isNotBlank()
                ) {
                    return issue
                }
            }
        }

        return fallback
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

    data class PayPalSubscriptionInfo(
        val subscriptionId: String,
        val approvalUrl: String
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
