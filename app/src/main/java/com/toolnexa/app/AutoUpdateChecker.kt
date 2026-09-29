package com.toolnexa.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AutoUpdateChecker {

    private const val BASE_RELEASE_API =
        "https://api.github.com/repos/nexauren1/Toolnexa-/releases/latest"

    private const val PACKAGE_RELEASE_API =
        "https://api.github.com/repos/nexauren1/Toolnexa-/releases/tags/tool-packages"

    private const val PACKAGE_MANIFEST_NAME =
        "toolnexa-packages.json"

    private const val CHANNEL_ID =
        "toolnexa_updates"

    private const val BASE_NOTIFICATION_ID =
        8401

    fun check(context: Context) {
        val appContext = context.applicationContext

        Thread {
            checkBaseRelease(appContext)
            checkToolPackages(appContext)
        }.start()
    }

    private fun checkBaseRelease(
        context: Context
    ) {
        try {
            val json =
                fetchJson(
                    BASE_RELEASE_API
                )

            val version =
                json.optString(
                    "tag_name"
                )
                    .removePrefix("v")
                    .trim()

            if (
                version.isBlank() ||
                !isNewer(
                    version,
                    BuildConfig.VERSION_NAME
                )
            ) {
                return
            }

            val prefs =
                context.getSharedPreferences(
                    "toolnexa_update_notifications",
                    Context.MODE_PRIVATE
                )

            if (
                prefs.getString(
                    "last_base_notified",
                    ""
                ) == version
            ) {
                return
            }

            prefs.edit()
                .putString(
                    "last_base_notified",
                    version
                )
                .apply()

            ensureChannel(context)

            val intent =
                Intent(
                    context,
                    MainActivity::class.java
                ).apply {
                    putExtra(
                        "open_screen",
                        "update_base"
                    )
                    addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }

            val pending =
                PendingIntent.getActivity(
                    context,
                    BASE_NOTIFICATION_ID,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )

            val notification =
                NotificationCompat.Builder(
                    context,
                    CHANNEL_ID
                )
                    .setSmallIcon(
                        android.R.drawable.stat_sys_download_done
                    )
                    .setContentTitle(
                        "Nova versão do ToolNexa"
                    )
                    .setContentText(
                        "A versão " +
                            version +
                            " está disponível."
                    )
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(
                                "Toque para abrir a atualização. " +
                                    "O APK base só é necessário quando a própria aplicação mudou."
                            )
                    )
                    .setAutoCancel(true)
                    .setContentIntent(pending)
                    .build()

            context.getSystemService(
                NotificationManager::class.java
            ).notify(
                BASE_NOTIFICATION_ID,
                notification
            )
        } catch (_: Exception) {
        }
    }

    private fun checkToolPackages(
        context: Context
    ) {
        try {
            val release =
                fetchJson(
                    PACKAGE_RELEASE_API
                )

            val assets =
                release.optJSONArray(
                    "assets"
                )
                    ?: return

            var manifestUrl: String? = null

            for (
                index in 0 until assets.length()
            ) {
                val item =
                    assets.optJSONObject(index)
                        ?: continue

                if (
                    item.optString("name") ==
                        PACKAGE_MANIFEST_NAME
                ) {
                    manifestUrl =
                        item.optString(
                            "browser_download_url"
                        )
                    break
                }
            }

            if (
                manifestUrl.isNullOrBlank()
            ) {
                return
            }

            val manifest =
                fetchJson(
                    manifestUrl
                )

            if (
                manifest.optInt(
                    "baseVersionCode",
                    -1
                ) != BuildConfig.VERSION_CODE
            ) {
                return
            }

            val packages =
                manifest.optJSONObject(
                    "packages"
                )
                    ?: return

            val prefs =
                context.getSharedPreferences(
                    "tool_packages",
                    Context.MODE_PRIVATE
                )

            for (
                packageInfo
                in ToolPackageCatalog.all()
            ) {
                if (
                    !isInstalled(
                        context,
                        packageInfo.module
                    )
                ) {
                    continue
                }

                val remote =
                    packages.optJSONObject(
                        packageInfo.module
                    )
                        ?: continue

                val revision =
                    remote.optString(
                        "revision"
                    )
                        .trim()

                if (revision.isBlank()) {
                    continue
                }

                val localRevision =
                    prefs.getString(
                        "revision_" +
                            packageInfo.module,
                        ""
                    )
                        .orEmpty()

                if (
                    localRevision == revision
                ) {
                    continue
                }

                notifyPackageUpdate(
                    context,
                    packageInfo.category,
                    packageInfo.module,
                    revision
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun notifyPackageUpdate(
        context: Context,
        category: String,
        module: String,
        revision: String
    ) {
        val prefs =
            context.getSharedPreferences(
                "toolnexa_update_notifications",
                Context.MODE_PRIVATE
            )

        val key =
            "package_" +
                module

        if (
            prefs.getString(
                key,
                ""
            ) == revision
        ) {
            return
        }

        prefs.edit()
            .putString(
                key,
                revision
            )
            .apply()

        ensureChannel(context)

        val notificationId =
            8500 +
                kotlin.math.abs(
                    module.hashCode() % 500
                )

        val intent =
            Intent(
                context,
                ToolPackageActivity::class.java
            ).apply {
                putExtra(
                    "category",
                    category
                )
                putExtra(
                    "force_update",
                    true
                )
                putExtra(
                    "package_module",
                    module
                )
            }

        val pending =
            PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.stat_sys_download_done
                )
                .setContentTitle(
                    "Atualização de ferramenta"
                )
                .setContentText(
                    "Novo pacote disponível: " +
                        category
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "O pacote " +
                                category +
                                " foi atualizado. " +
                                "Toque para baixar somente esse pacote."
                        )
                )
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()

        context.getSystemService(
            NotificationManager::class.java
        ).notify(
            notificationId,
            notification
        )
    }

    private fun isInstalled(
        context: Context,
        module: String
    ): Boolean {
        return try {
            val info =
                context.packageManager
                    .getApplicationInfo(
                        context.packageName,
                        0
                    )

            info.splitNames
                ?.contains(module)
                == true
        } catch (_: Exception) {
            false
        }
    }

    private fun ensureChannel(
        context: Context
    ) {
        if (
            Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O
        ) {
            return
        }

        val manager =
            context.getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Atualizações ToolNexa",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description =
                    "Avisos sobre novas versões e pacotes de ferramentas."
            }
        )
    }

    private fun fetchJson(
        urlString: String
    ): JSONObject {
        val connection =
            URL(urlString)
                .openConnection() as HttpURLConnection

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

        val code =
            connection.responseCode

        if (
            code !in 200..299
        ) {
            connection.disconnect()
            throw IllegalStateException(
                "HTTP " + code
            )
        }

        val body =
            connection.inputStream
                .bufferedReader()
                .use { it.readText() }

        connection.disconnect()

        return JSONObject(body)
    }

    private fun isNewer(
        remote: String,
        local: String
    ): Boolean {
        val a =
            remote.split(".")
                .map {
                    it.filter(
                        Char::isDigit
                    )
                        .toIntOrNull()
                        ?: 0
                }

        val b =
            local.split(".")
                .map {
                    it.filter(
                        Char::isDigit
                    )
                        .toIntOrNull()
                        ?: 0
                }

        val size =
            maxOf(
                a.size,
                b.size
            )

        for (
            index in 0 until size
        ) {
            val remotePart =
                a.getOrElse(index) { 0 }

            val localPart =
                b.getOrElse(index) { 0 }

            if (
                remotePart != localPart
            ) {
                return remotePart > localPart
            }
        }

        return false
    }
}
