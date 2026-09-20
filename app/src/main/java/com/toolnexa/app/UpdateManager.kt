package com.toolnexa.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

class UpdateManager(private val activity: Activity) {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingInstallFile: File? = null

    fun checkForUpdate(showErrors: Boolean = false) {
        val prefs = activity.getSharedPreferences(
            "toolnexa_settings",
            Context.MODE_PRIVATE
        )
        if (!prefs.getBoolean("auto_update_check", true)) return

        Thread {
            try {
                val connection =
                    URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"
                connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json"
                )
                connection.setRequestProperty(
                    "User-Agent",
                    "ToolNexa-Android"
                )

                val code = connection.responseCode
                if (code !in 200..299) {
                    connection.disconnect()
                    throw IllegalStateException("GitHub HTTP " + code)
                }

                val body = connection.inputStream.bufferedReader().use {
                    it.readText()
                }
                connection.disconnect()

                val release = JSONObject(body)
                val latestVersion = release
                    .optString("tag_name")
                    .removePrefix("v")
                    .trim()
                val notes = release
                    .optString("body")
                    .trim()
                val apkUrl = findApkAsset(
                    release.optJSONArray("assets")
                )

                if (
                    apkUrl != null &&
                    isNewer(
                        latestVersion,
                        BuildConfig.VERSION_NAME
                    )
                ) {
                    handler.post {
                        showUpdateDialog(
                            latestVersion,
                            notes,
                            apkUrl
                        )
                    }
                }
            } catch (_: Exception) {
                if (showErrors) {
                    handler.post {
                        Toast.makeText(
                            activity,
                            "Não foi possível verificar atualizações agora.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }.start()
    }

    fun retryInstallAfterSettings() {
        val file = pendingInstallFile ?: return
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            return
        }
        pendingInstallFile = null
        installApk(file)
    }

    private fun findApkAsset(assets: JSONArray?): String? {
        if (assets == null) return null
        for (index in 0 until assets.length()) {
            val item = assets.optJSONObject(index) ?: continue
            val name = item.optString("name")
            val download = item.optString(
                "browser_download_url"
            )
            if (
                name.endsWith(".apk", ignoreCase = true) &&
                download.isNotBlank()
            ) {
                return download
            }
        }
        return null
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val a = remote.split(".").map {
            it.filter(Char::isDigit).toIntOrNull() ?: 0
        }
        val b = local.split(".").map {
            it.filter(Char::isDigit).toIntOrNull() ?: 0
        }
        val size = max(a.size, b.size)

        for (index in 0 until size) {
            val remotePart = a.getOrElse(index) { 0 }
            val localPart = b.getOrElse(index) { 0 }
            if (remotePart != localPart) {
                return remotePart > localPart
            }
        }
        return false
    }

    private fun showUpdateDialog(
        version: String,
        notes: String,
        apkUrl: String
    ) {
        val box = LinearLayout(activity)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(48, 18, 48, 8)

        val message = TextView(activity)
        message.text = buildString {
            append("Uma nova versão do ToolNexa está disponível.\n\n")
            append("Versão: ")
            append(version)
            if (notes.isNotBlank()) {
                append("\n\n")
                append(notes.take(700))
            }
        }
        message.textSize = 16f
        message.setTextColor(
            activity.getColor(R.color.toolnexa_text)
        )
        box.addView(message)

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Atualização necessária")
            .setView(box)
            .setCancelable(false)
            .setPositiveButton("Baixar") { _, _ ->
                downloadApk(apkUrl)
            }
            .create()

        dialog.show()
    }

    private fun downloadApk(downloadUrl: String) {
        val overlay = AlertDialog.Builder(activity)
            .setTitle("Baixando ToolNexa")
            .setCancelable(false)
            .create()

        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 12, 48, 28)

        val label = TextView(activity)
        label.text = "0%"
        label.gravity = Gravity.CENTER
        label.textSize = 18f
        label.setTextColor(
            activity.getColor(R.color.toolnexa_text)
        )

        val progress = ProgressBar(
            activity,
            null,
            android.R.attr.progressBarStyleHorizontal
        )
        progress.max = 100

        layout.addView(
            progress,
            LinearLayout.LayoutParams(-1, 20)
        )
        layout.addView(
            label,
            LinearLayout.LayoutParams(-1, 60)
        )

        overlay.setView(layout)
        overlay.show()

        Thread {
            try {
                val connection =
                    URL(downloadUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.requestMethod = "GET"
                connection.setRequestProperty(
                    "User-Agent",
                    "ToolNexa-Android"
                )
                connection.connect()

                val total = connection.contentLengthLong
                val file = File(
                    activity.getExternalFilesDir(
                        Environment.DIRECTORY_DOWNLOADS
                    ),
                    "toolnexa-update.apk"
                )

                FileOutputStream(file).use { output ->
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        var downloaded = 0L

                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break

                            output.write(buffer, 0, read)
                            downloaded += read

                            if (total > 0) {
                                val percent =
                                    ((downloaded * 100L) / total)
                                        .toInt()
                                        .coerceIn(0, 100)

                                handler.post {
                                    progress.progress = percent
                                    label.text =
                                        percent.toString() + "%"
                                }
                            }
                        }
                    }
                }
                connection.disconnect()

                handler.post {
                    progress.progress = 100
                    label.text = "100%"
                    overlay.dismiss()
                    pendingInstallFile = file
                    startInstall(file)
                }
            } catch (_: Exception) {
                handler.post {
                    overlay.dismiss()
                    Toast.makeText(
                        activity,
                        "Falha ao baixar a atualização.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun startInstall(file: File) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            pendingInstallFile = file

            AlertDialog.Builder(activity)
                .setTitle("Permissão necessária")
                .setMessage(
                    "O Android precisa permitir que o ToolNexa instale a atualização baixada."
                )
                .setPositiveButton("Continuar") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.packageName)
                    )
                    activity.startActivity(intent)
                }
                .setNegativeButton("Agora não", null)
                .show()
            return
        }

        installApk(file)
    }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(
            activity,
            "com.toolnexa.app.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                uri,
                "application/vnd.android.package-archive"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        activity.startActivity(intent)
    }

    companion object {
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/nexauren1/Toolnexa-/releases/latest"
    }
}
