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
import kotlin.math.roundToInt

data class ResizeResult(
    val file: File,
    val originalBytes: Long,
    val resizedBytes: Long,
    val width: Int,
    val height: Int
)

class ImageResizer(
    private val context: Context
) {

    fun resize(
        uri: Uri,
        targetWidth: Int
    ): ResizeResult? {
        val source =
            decodeForWidth(
                uri,
                targetWidth
            ) ?: return null

        if (source.width <= 0 || source.height <= 0) {
            source.recycle()
            return null
        }

        val targetHeight =
            (
                source.height.toDouble() *
                    targetWidth.toDouble() /
                    source.width.toDouble()
            )
                .roundToInt()
                .coerceAtLeast(1)

        val resized =
            Bitmap.createScaledBitmap(
                source,
                targetWidth,
                targetHeight,
                true
            )

        if (resized !== source) {
            source.recycle()
        }

        val output = File(
            context.cacheDir,
            "toolnexa-resized-" +
                System.currentTimeMillis() +
                ".jpg"
        )

        FileOutputStream(output).use { stream ->
            if (
                !resized.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    stream
                )
            ) {
                resized.recycle()
                return null
            }
        }

        val result = ResizeResult(
            file = output,
            originalBytes = getSize(uri),
            resizedBytes = output.length(),
            width = targetWidth,
            height = targetHeight
        )

        resized.recycle()
        return result
    }

    fun saveToGallery(
        file: File
    ): Uri? {
        val name =
            "ToolNexa-Resized-" +
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

            if (
                Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
            ) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES +
                        "/ToolNexa"
                )
            }
        }

        val resolver =
            context.contentResolver

        val uri =
            resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: return null

        return try {
            resolver.openOutputStream(
                uri
            )?.use { output ->
                file.inputStream().use {
                    input ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException(
                "Unable to open output"
            )

            uri
        } catch (_: Exception) {
            resolver.delete(
                uri,
                null,
                null
            )
            null
        }
    }

    private fun decodeForWidth(
        uri: Uri,
        targetWidth: Int
    ): Bitmap? {
        val bounds =
            BitmapFactory.Options().apply {
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

        if (
            bounds.outWidth <= 0 ||
            bounds.outHeight <= 0
        ) {
            return null
        }

        val desired =
            (targetWidth * 2)
                .coerceAtLeast(2048)

        var sample = 1

        while (
            bounds.outWidth / sample > desired ||
            bounds.outHeight / sample > desired
        ) {
            sample *= 2
        }

        val options =
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig =
                    Bitmap.Config.ARGB_8888
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

    private fun getSize(
        uri: Uri
    ): Long {
        return try {
            context.contentResolver
                .openFileDescriptor(
                    uri,
                    "r"
                )
                ?.use { it.statSize }
                ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
