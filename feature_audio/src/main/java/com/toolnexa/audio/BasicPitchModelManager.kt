package com.toolnexa.audio

import com.toolnexa.app.*

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object BasicPitchModelManager {

    const val MODEL_VERSION =
        "1.0.1"

    const val MODEL_SIZE_BYTES =
        207858L

    private const val MODEL_URL =
        "https://raw.githubusercontent.com/spotify/basic-pitch/" +
            "fa5997af0a8210982619003269994a1be25eddf3/" +
            "basic_pitch/saved_models/icassp_2022/nmp.tflite"

    private const val MODEL_DIR =
        "models"

    private const val MODEL_NAME =
        "basic_pitch_nmp.tflite"

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    fun modelFile(
        context: Context
    ): File {
        val directory =
            File(
                context.filesDir,
                MODEL_DIR
            )

        return File(
            directory,
            MODEL_NAME
        )
    }

    fun isInstalled(
        context: Context
    ): Boolean {
        val file =
            modelFile(
                context
            )

        return file.exists() &&
            file.length() >=
                MODEL_SIZE_BYTES * 9 / 10
    }

    fun installedSizeBytes(
        context: Context
    ): Long {
        return if (
            isInstalled(
                context
            )
        ) {
            modelFile(
                context
            ).length()
        } else {
            0L
        }
    }

    fun download(
        context: Context,
        onProgress:
            (downloaded: Long, total: Long, percent: Int) -> Unit,
        onComplete: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val appContext =
            context.applicationContext

        Thread {
            var connection:
                HttpURLConnection? =
                null

            val directory =
                File(
                    appContext.filesDir,
                    MODEL_DIR
                )

            val temporary =
                File(
                    directory,
                    "$MODEL_NAME.part"
                )

            try {
                directory.mkdirs()

                if (
                    temporary.exists()
                ) {
                    temporary.delete()
                }

                connection =
                    (
                        URL(
                            MODEL_URL
                        ).openConnection()
                            as HttpURLConnection
                        ).apply {
                            requestMethod =
                                "GET"
                            connectTimeout =
                                20_000
                            readTimeout =
                                30_000
                            instanceFollowRedirects =
                                true
                            setRequestProperty(
                                "User-Agent",
                                "ToolNexa/$MODEL_VERSION"
                            )
                        }

                connection.connect()

                val response =
                    connection.responseCode

                if (
                    response !in
                        200..299
                ) {
                    throw IllegalStateException(
                        "Servidor do modelo devolveu HTTP $response."
                    )
                }

                val total =
                    connection
                        .contentLengthLong
                        .takeIf {
                            it > 0
                        }
                        ?: MODEL_SIZE_BYTES

                var downloaded =
                    0L

                connection
                    .inputStream
                    .buffered(
                        32 * 1024
                    )
                    .use { input ->
                        temporary
                            .outputStream()
                            .buffered(
                                32 * 1024
                            )
                            .use { output ->
                                val buffer =
                                    ByteArray(
                                        32 * 1024
                                    )

                                while (true) {
                                    val read =
                                        input.read(
                                            buffer
                                        )

                                    if (
                                        read < 0
                                    ) {
                                        break
                                    }

                                    output.write(
                                        buffer,
                                        0,
                                        read
                                    )

                                    downloaded +=
                                        read

                                    val percent =
                                        (
                                            downloaded
                                                .toDouble() /
                                                total
                                                    .coerceAtLeast(
                                                        1L
                                                    )
                                                    .toDouble() *
                                                100.0
                                            )
                                            .toInt()
                                            .coerceIn(
                                                0,
                                                100
                                            )

                                    mainHandler.post {
                                        onProgress(
                                            downloaded,
                                            total,
                                            percent
                                        )
                                    }
                                }
                            }
                    }

                if (
                    downloaded <
                        MODEL_SIZE_BYTES * 9 / 10
                ) {
                    throw IllegalStateException(
                        "O ficheiro do modelo ficou incompleto."
                    )
                }

                val target =
                    modelFile(
                        appContext
                    )

                if (
                    target.exists()
                ) {
                    target.delete()
                }

                if (
                    !temporary.renameTo(
                        target
                    )
                ) {
                    throw IllegalStateException(
                        "Não foi possível instalar o modelo."
                    )
                }

                mainHandler.post {
                    onProgress(
                        target.length(),
                        target.length(),
                        100
                    )
                    onComplete(
                        target
                    )
                }
            } catch (error: Exception) {
                temporary.delete()

                mainHandler.post {
                    onError(
                        error.message
                            ?: "Não foi possível baixar o modelo."
                    )
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    fun displaySizeMb(): String {
        return String.format(
            java.util.Locale.US,
            "%.2f MB",
            MODEL_SIZE_BYTES /
                1_000_000.0
        )
    }
}
