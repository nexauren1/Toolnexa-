package com.toolnexa.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

data class CompressionResult(
    val file: File,
    val originalBytes: Long,
    val compressedBytes: Long,
    val width: Int,
    val height: Int
)

class ImageCompressor(private val context: Context) {

    fun decodeSampled(
        uri: Uri,
        maxDimension: Int = 2048
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver
            .openInputStream(uri)
            ?.use {
                BitmapFactory.decodeStream(
                    it,
                    null,
                    bounds
                )
            }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        var sample = 1
        while (
            bounds.outWidth / sample > maxDimension ||
            bounds.outHeight / sample > maxDimension
        ) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return context.contentResolver
            .openInputStream(uri)
            ?.use {
                BitmapFactory.decodeStream(
                    it,
                    null,
                    options
                )
            }
    }

    fun compress(
        uri: Uri,
        quality: Int
    ): CompressionResult? {
        val bitmap = decodeSampled(uri) ?: return null

        val output = File(
            context.cacheDir,
            "toolnexa-" +
                System.currentTimeMillis() +
                ".jpg"
        )

        FileOutputStream(output).use { stream ->
            if (
                !bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality,
                    stream
                )
            ) {
                bitmap.recycle()
                return null
            }
        }

        val result = CompressionResult(
            file = output,
            originalBytes = getSize(uri),
            compressedBytes = output.length(),
            width = bitmap.width,
            height = bitmap.height
        )

        bitmap.recycle()
        return result
    }

    fun saveToGallery(file: File): Uri? {
        val name =
            "ToolNexa-" +
                System.currentTimeMillis() +
                ".jpg"

        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                name
            )
            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES +
                        "/ToolNexa"
                )
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException(
                "Unable to open output"
            )

            uri
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun getSize(uri: Uri): Long {
        return try {
            context.contentResolver
                .openFileDescriptor(uri, "r")
                ?.use { it.statSize }
                ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
