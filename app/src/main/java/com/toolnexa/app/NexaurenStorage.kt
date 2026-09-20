package com.toolnexa.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import java.io.File

object NexaurenStorage {

    private const val PREFS = "toolnexa_storage"
    private const val ROOT_URI = "root_uri"
    private const val NEXAUREN_FOLDER = "Nexauren X"

    fun setRoot(
        context: Context,
        uri: Uri
    ) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .putString(
                ROOT_URI,
                uri.toString()
            )
            .apply()
    }

    fun clearRoot(
        context: Context
    ) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .remove(ROOT_URI)
            .apply()
    }

    fun prepareRoot(
        context: Context,
        uri: Uri
    ): Boolean {
        return try {
            if (!DocumentsContract.isTreeUri(uri)) {
                return false
            }

            val treeDocument =
                documentUriFromTree(uri)

            val folder =
                findOrCreateDirectory(
                    context,
                    uri,
                    treeDocument,
                    NEXAUREN_FOLDER
                )

            if (folder == null) {
                return false
            }

            setRoot(context, uri)
            true
        } catch (_: Exception) {
            clearRoot(context)
            false
        }
    }

    private fun rootTreeUri(
        context: Context
    ): Uri? {
        val raw =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            ).getString(
                ROOT_URI,
                null
            ) ?: return null

        return try {
            val uri = Uri.parse(raw)

            if (
                DocumentsContract.isTreeUri(uri)
            ) {
                uri
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun documentUriFromTree(
        treeUri: Uri
    ): Uri {
        val documentId =
            DocumentsContract.getTreeDocumentId(
                treeUri
            )

        require(
            documentId.isNotBlank()
        )

        return DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            documentId
        )
    }

    private fun ensureNexaurenFolder(
        context: Context
    ): Uri? {
        val treeUri =
            rootTreeUri(context)
                ?: return null

        val rootDocument =
            documentUriFromTree(treeUri)

        return try {
            findOrCreateDirectory(
                context,
                treeUri,
                rootDocument,
                NEXAUREN_FOLDER
            )
        } catch (_: Exception) {
            null
        }
    }

    fun rootName(
        context: Context
    ): String? {
        val treeUri =
            rootTreeUri(context)
                ?: return null

        return try {
            val documentUri =
                documentUriFromTree(treeUri)

            context.contentResolver.query(
                documentUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun hasRoot(
        context: Context
    ): Boolean =
        !rootName(context).isNullOrBlank()

    fun save(
        context: Context,
        category: String,
        tool: String,
        file: File,
        displayName: String
    ): Uri? {
        val treeUri =
            rootTreeUri(context)
                ?: return null

        return try {
            val nexauren =
                ensureNexaurenFolder(context)
                    ?: return null

            val categoryDir =
                findOrCreateDirectory(
                    context,
                    treeUri,
                    nexauren,
                    safeName(category)
                ) ?: return null

            val toolDir =
                findOrCreateDirectory(
                    context,
                    treeUri,
                    categoryDir,
                    safeName(tool)
                ) ?: return null

            val mime =
                when (file.extension.lowercase()) {
                    "png" ->
                        "image/png"

                    "webp" ->
                        "image/webp"

                    else ->
                        "image/jpeg"
                }

            val target =
                DocumentsContract.createDocument(
                    context.contentResolver,
                    toolDir,
                    mime,
                    safeName(displayName)
                ) ?: return null

            context.contentResolver
                .openOutputStream(target)
                ?.use { output ->
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
        treeUri: Uri,
        parentDocument: Uri,
        name: String
    ): Uri? {
        val parentId =
            DocumentsContract.getDocumentId(
                parentDocument
            )

        val children =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                parentId
            )

        context.contentResolver.query(
            children,
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
                val displayName =
                    cursor.getString(1)

                val mime =
                    cursor.getString(2)

                if (
                    displayName == name &&
                    mime ==
                        DocumentsContract.Document.MIME_TYPE_DIR
                ) {
                    val id =
                        cursor.getString(0)

                    return DocumentsContract
                        .buildDocumentUriUsingTree(
                            treeUri,
                            id
                        )
                }
            }
        }

        val created =
            DocumentsContract.createDocument(
                context.contentResolver,
                parentDocument,
                DocumentsContract.Document.MIME_TYPE_DIR,
                safeName(name)
            ) ?: return null

        return created
    }

    private fun safeName(
        value: String
    ): String =
        value
            .replace("/", "_")
            .replace("\\", "_")
            .trim()
            .ifBlank {
                "arquivo"
            }

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
