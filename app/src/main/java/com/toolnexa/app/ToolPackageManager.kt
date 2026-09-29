package com.toolnexa.app

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Handler
import android.os.Looper
import com.google.android.play.core.splitcompat.SplitCompat
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ToolPackageCatalog {

    data class Package(
        val category: String,
        val module: String
    ) {
        val toolCount: Int
            get() = ToolRegistry.countFor(category)
    }

    private val packages = listOf(
        Package("Business", "feature_business"),
        Package("Imagem", "feature_image"),
        Package("Áudio", "feature_audio")
    )

    fun forCategory(category: String): Package? =
        packages.firstOrNull {
            it.category.equals(category, ignoreCase = true)
        }

    fun all(): List<Package> = packages

    fun activityClassFor(toolId: String): String? = when (toolId) {
        "business-invoice-maker",
        "business-receipt-maker",
        "business-quote-maker",
        "business-profit-calculator",
        "business-expense-tracker",
        "business-plan",
        "business-proposal",
        "business-contract-maker",
        "business-name-generator",
        "business-pricing-calculator" ->
            "com.toolnexa.business.BusinessToolActivity"

        "image-compressor",
        "image-resizer",
        "image-converter" ->
            "com.toolnexa.image.ToolWorkflowActivity"

        "background-remover" ->
            "com.toolnexa.image.BackgroundRemoverActivity"

        "audio-to-midi" ->
            "com.toolnexa.audio.AudioToMidiActivity"

        else -> null
    }
}

class ToolPackageManager(
    private val activity: Activity
) {

    private val preferences =
        activity.getSharedPreferences(
            "tool_packages",
            Activity.MODE_PRIVATE
        )

    private val handler =
        Handler(Looper.getMainLooper())

    fun prepareActivity() {
        try {
            SplitCompat.installActivity(activity)
        } catch (_: Exception) {
        }
    }

    fun isInstalled(module: String): Boolean = try {
        val info =
            activity.packageManager.getApplicationInfo(
                activity.packageName,
                0
            )

        info.splitNames?.contains(module) == true
    } catch (_: Exception) {
        false
    }

    fun installedRevision(
        packageInfo: ToolPackageCatalog.Package
    ): String {
        return preferences.getString(
            "revision_" + packageInfo.module,
            ""
        ).orEmpty()
    }

    fun markInstalled(
        packageInfo: ToolPackageCatalog.Package,
        revision: String
    ) {
        preferences.edit()
            .putString(
                "revision_" + packageInfo.module,
                revision
            )
            .putBoolean(
                "ever_installed_" + packageInfo.module,
                true
            )
            .apply()
    }

    fun hasEverInstalled(
        packageInfo: ToolPackageCatalog.Package
    ): Boolean {
        return preferences.getBoolean(
            "ever_installed_" + packageInfo.module,
            false
        ) || isInstalled(packageInfo.module)
    }

    fun needsUpdate(
        packageInfo: ToolPackageCatalog.Package
    ): Boolean = false

    fun download(
        packageInfo: ToolPackageCatalog.Package,
        onProgress: (Int) -> Unit,
        onInstalled: () -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val manifest = fetchManifest()
                val remote =
                    manifest.packages.optJSONObject(
                        packageInfo.module
                    ) ?: throw IllegalStateException(
                        "O pacote " +
                            packageInfo.category +
                            " não está disponível."
                    )

                if (
                    manifest.baseVersionCode !=
                        BuildConfig.VERSION_CODE
                ) {
                    throw IllegalStateException(
                        "Este pacote pertence a outra versão base da app. " +
                            "Atualize primeiro o ToolNexa."
                    )
                }

                val revision =
                    remote.optString("revision").trim()

                val asset =
                    remote.optString("asset").trim()

                if (
                    revision.isBlank() ||
                    asset.isBlank()
                ) {
                    throw IllegalStateException(
                        "Metadados do pacote incompletos."
                    )
                }

                val downloadUrl =
                    remote.optString("downloadUrl")
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?: TOOL_PACKAGE_ASSET_BASE + asset

                val file =
                    downloadAsset(
                        downloadUrl,
                        asset,
                        onProgress
                    )

                installSplit(
                    file,
                    packageInfo,
                    revision,
                    onInstalled,
                    onError
                )
            } catch (error: Exception) {
                activity.runOnUiThread {
                    onError(
                        error.message
                            ?: "Não foi possível preparar o pacote."
                    )
                }
            }
        }.start()
    }

    private fun fetchManifest(): PackageManifest {
        val manifestConnection =
            URL(TOOL_PACKAGE_MANIFEST_URL)
                .openConnection() as HttpURLConnection

        manifestConnection.connectTimeout = 10000
        manifestConnection.readTimeout = 15000
        manifestConnection.instanceFollowRedirects = true
        manifestConnection.requestMethod = "GET"
        manifestConnection.setRequestProperty(
            "Accept",
            "application/json"
        )
        manifestConnection.setRequestProperty(
            "User-Agent",
            "ToolNexa-Android"
        )

        val code = manifestConnection.responseCode

        if (code !in 200..299) {
            manifestConnection.disconnect()
            throw IllegalStateException(
                "Não foi possível consultar o pacote de ferramentas (HTTP " +
                    code +
                    ")."
            )
        }

        val manifestJson =
            manifestConnection.inputStream
                .bufferedReader()
                .use { it.readText() }

        manifestConnection.disconnect()

        val json =
            JSONObject(manifestJson)

        return PackageManifest(
            baseVersionCode =
                json.optInt(
                    "baseVersionCode",
                    0
                ),
            packages =
                json.optJSONObject(
                    "packages"
                ) ?: JSONObject()
        )
    }

    private fun downloadAsset(
        downloadUrl: String,
        assetName: String,
        onProgress: (Int) -> Unit
    ): File {
        val connection =
            URL(downloadUrl)
                .openConnection() as HttpURLConnection

        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.requestMethod = "GET"
        connection.setRequestProperty(
            "User-Agent",
            "ToolNexa-Android"
        )
        connection.connect()

        val total =
            connection.contentLengthLong

        val directory =
            File(
                activity.filesDir,
                "tool_packages"
            )

        directory.mkdirs()

        val file =
            File(
                directory,
                assetName
            )

        FileOutputStream(file).use { output ->
            connection.inputStream.use { input ->
                val buffer =
                    ByteArray(16 * 1024)

                var downloaded = 0L

                while (true) {
                    val read =
                        input.read(buffer)

                    if (read == -1) {
                        break
                    }

                    output.write(
                        buffer,
                        0,
                        read
                    )

                    downloaded += read

                    if (total > 0L) {
                        val percent =
                            (
                                downloaded * 100L /
                                    total
                                )
                                .toInt()
                                .coerceIn(
                                    0,
                                    100
                                )

                        activity.runOnUiThread {
                            onProgress(percent)
                        }
                    }
                }
            }
        }

        connection.disconnect()

        if (
            !file.exists() ||
            file.length() == 0L
        ) {
            throw IllegalStateException(
                "O download do pacote ficou vazio."
            )
        }

        return file
    }

    private fun installSplit(
        file: File,
        packageInfo: ToolPackageCatalog.Package,
        revision: String,
        onInstalled: () -> Unit,
        onError: (String) -> Unit
    ) {
        val installer =
            activity.packageManager.packageInstaller

        val params =
            PackageInstaller.SessionParams(
                PackageInstaller.SessionParams
                    .MODE_INHERIT_EXISTING
            ).apply {
                setAppPackageName(
                    activity.packageName
                )
            }

        val sessionId =
            installer.createSession(params)

        preferences.edit()
            .putString(
                "pending_revision_" +
                    packageInfo.module,
                revision
            )
            .putString(
                "pending_status_" +
                    packageInfo.module,
                "pending"
            )
            .apply()

        installer.openSession(
            sessionId
        ).use { session ->

            file.inputStream().use { input ->

                session.openWrite(
                    packageInfo.module,
                    0,
                    file.length()
                ).use { output ->

                    val buffer =
                        ByteArray(32 * 1024)

                    while (true) {
                        val read =
                            input.read(buffer)

                        if (read == -1) {
                            break
                        }

                        output.write(
                            buffer,
                            0,
                            read
                        )
                    }

                    output.flush()
                    session.fsync(output)
                }
            }

            val callbackIntent =
                Intent(
                    activity,
                    ToolPackageInstallReceiver::class.java
                ).apply {

                    putExtra(
                        ToolPackageInstallReceiver
                            .EXTRA_MODULE,
                        packageInfo.module
                    )

                    putExtra(
                        ToolPackageInstallReceiver
                            .EXTRA_REVISION,
                        revision
                    )

                    putExtra(
                        ToolPackageInstallReceiver
                            .EXTRA_CATEGORY,
                        packageInfo.category
                    )
                }

            val pendingIntent =
                PendingIntent.getBroadcast(
                    activity,
                    sessionId,
                    callbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )

            session.commit(
                pendingIntent.intentSender
            )
        }

        activity.runOnUiThread {
            pollInstallResult(
                packageInfo,
                revision,
                onInstalled,
                onError
            )
        }
    }

    private fun pollInstallResult(
        packageInfo: ToolPackageCatalog.Package,
        revision: String,
        onInstalled: () -> Unit,
        onError: (String) -> Unit
    ) {
        val deadline =
            System.currentTimeMillis() +
                INSTALL_TIMEOUT_MS

        fun poll() {
            when (
                preferences.getString(
                    "pending_status_" +
                        packageInfo.module,
                    "pending"
                )
            ) {
                "success" -> {
                    if (
                        isInstalled(
                            packageInfo.module
                        )
                    ) {
                        try {
                            SplitCompat.installActivity(
                                activity
                            )
                        } catch (_: Exception) {
                        }

                        markInstalled(
                            packageInfo,
                            revision
                        )

                        preferences.edit()
                            .remove(
                                "pending_status_" +
                                    packageInfo.module
                            )
                            .remove(
                                "pending_message_" +
                                    packageInfo.module
                            )
                            .remove(
                                "pending_revision_" +
                                    packageInfo.module
                            )
                            .apply()

                        onInstalled()
                        return
                    }
                }

                "failure" -> {
                    val message =
                        preferences.getString(
                            "pending_message_" +
                                packageInfo.module,
                            "Não foi possível instalar o pacote."
                        )
                            ?: "Não foi possível instalar o pacote."

                    preferences.edit()
                        .remove(
                            "pending_status_" +
                                packageInfo.module
                        )
                        .remove(
                            "pending_message_" +
                                packageInfo.module
                        )
                        .apply()

                    onError(message)
                    return
                }
            }

            if (
                System.currentTimeMillis() >
                    deadline
            ) {
                onError(
                    "A instalação do pacote demorou demasiado. " +
                        "Tente novamente."
                )
                return
            }

            handler.postDelayed(
                { poll() },
                700L
            )
        }

        poll()
    }

    companion object {
        private const val TOOL_PACKAGE_MANIFEST_URL =
            "https://github.com/nexauren1/Toolnexa-/releases/download/tool-packages/toolnexa-packages.json"

        private const val TOOL_PACKAGE_ASSET_BASE =
            "https://github.com/nexauren1/Toolnexa-/releases/download/tool-packages/"

        private const val INSTALL_TIMEOUT_MS =
            120_000L
    }

    private data class PackageManifest(
        val baseVersionCode: Int,
        val packages: JSONObject
    )
}
