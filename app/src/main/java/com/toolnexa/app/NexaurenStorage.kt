package com.toolnexa.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import java.io.File

object NexaurenStorage {

    private const val PREFS = "toolnexa_storage"
    private const val ROOT_URI = "root_uri"

    fun setRoot(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(ROOT_URI, uri.toString())
            .apply()
    }

    fun save(
        context: Context,
        category: String,
        tool: String,
        file: File,
        displayName: String
    ): Uri? {
        val rawRoot = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).getString(ROOT_URI, null) ?: return null

        return try {
            val root = Uri.parse(rawRoot)
            val nexauren = findOrCreateDirectory(
                context,
                root,
                "Nexauren X"
            )
            val categoryDir = findOrCreateDirectory(
                context,
                nexauren,
                safeName(category)
            )
            val toolDir = findOrCreateDirectory(
                context,
                categoryDir,
                safeName(tool)
            )

            val mime = when (file.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }

            val target = DocumentsContract.createDocument(
                context.contentResolver,
                toolDir,
                mime,
                safeName(displayName)
            ) ?: return null

            context.contentResolver.openOutputStream(target)?.use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: return null

            target
        } catch (_: Exception) {
            null
        }
    }

    private fun findOrCreateDirectory(
        context: Context,
        parent: Uri,
        name: String
    ): Uri {
        val existing = DocumentsContract.buildChildDocumentsUriUsingTree(
            parent,
            DocumentsContract.getTreeDocumentId(parent)
        )

        context.contentResolver.query(
            existing,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(1)
                val mime = cursor.getString(2)
                if (
                    displayName == name &&
                    mime == DocumentsContract.Document.MIME_TYPE_DIR
                ) {
                    val id = cursor.getString(0)
                    return DocumentsContract.buildDocumentUriUsingTree(
                        parent,
                        id
                    )
                }
            }
        }

        return DocumentsContract.createDocument(
            context.contentResolver,
            parent,
            DocumentsContract.Document.MIME_TYPE_DIR,
            name
        ) ?: throw IllegalStateException(
            "Unable to create directory"
        )
    }

    private fun safeName(value: String): String =
        value
            .replace("/", "_")
            .replace("\\", "_")
            .trim()
            .ifBlank { "arquivo" }

    fun fileProviderUri(
        context: Context,
        file: File
    ): Uri =
        FileProvider.getUriForFile(
            context,
            "com.toolnexa.app.fileprovider",
            file
        )
}
