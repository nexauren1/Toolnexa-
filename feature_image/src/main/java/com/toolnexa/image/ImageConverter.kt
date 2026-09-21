package com.toolnexa.image

import com.toolnexa.app.*

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

data class ConversionResult(
    val file: File,
    val originalBytes: Long,
    val convertedBytes: Long,
    val width: Int,
    val height: Int,
    val format: String
)

class ImageConverter(private val context: Context) {
    fun convert(
        uri: Uri,
        format: String,
        quality: Int
    ): ConversionResult? {
        val bitmap =
            context.contentResolver
                .openInputStream(uri)
                ?.use { android.graphics.BitmapFactory.decodeStream(it) }
                ?: return null

        val extension = when (format.lowercase()) {
            "png" -> "png"
            "webp" -> "webp"
            else -> "jpg"
        }

        val compressFormat =
            when (extension) {
                "png" -> Bitmap.CompressFormat.PNG
                "webp" -> {
                    if (android.os.Build.VERSION.SDK_INT >= 30) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        Bitmap.CompressFormat.WEBP
                    }
                }
                else -> Bitmap.CompressFormat.JPEG
            }

        val output = File(
            context.cacheDir,
            "toolnexa-converted-" +
                System.currentTimeMillis() +
                "." +
                extension
        )

        FileOutputStream(output).use { stream ->
            if (
                !bitmap.compress(
                    compressFormat,
                    quality.coerceIn(1, 100),
                    stream
                )
            ) {
                bitmap.recycle()
                return null
            }
        }

        val result = ConversionResult(
            file = output,
            originalBytes = getSize(uri),
            convertedBytes = output.length(),
            width = bitmap.width,
            height = bitmap.height,
            format = extension.uppercase()
        )
        bitmap.recycle()
        return result
    }

    private fun getSize(uri: Uri): Long =
        try {
            context.contentResolver
                .openFileDescriptor(uri, "r")
                ?.use { it.statSize }
                ?: 0L
        } catch (_: Exception) {
            0L
        }
}
