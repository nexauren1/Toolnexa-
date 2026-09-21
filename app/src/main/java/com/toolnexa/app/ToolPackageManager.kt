package com.toolnexa.app

import android.app.Activity
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

object ToolPackageCatalog {

    data class Package(
        val category: String,
        val module: String,
        val version: Int,
        val toolCount: Int
    )

    private val packages = listOf(
        Package(
            category = "Business",
            module = "feature_business",
            version = 1,
            toolCount = 10
        )
    )

    fun forCategory(category: String): Package? {
        return packages.firstOrNull {
            it.category.equals(
                category,
                ignoreCase = true
            )
        }
    }
}

class ToolPackageManager(
    private val activity: Activity
) {
    private val manager: SplitInstallManager =
        SplitInstallManagerFactory.create(activity)

    fun prepareActivity() {
        SplitCompat.installActivity(activity)
    }

    fun isInstalled(module: String): Boolean {
        return manager.installedModules.contains(module)
    }

    fun download(
        module: String,
        onProgress: (Int) -> Unit,
        onInstalled: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isInstalled(module)) {
            onProgress(100)
            onInstalled()
            return
        }

        val listener =
            object :
                com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener {

                override fun onStateUpdate(
                    state: SplitInstallSessionState
                ) {
                    if (!state.moduleNames().contains(module)) {
                        return
                    }

                    when (state.status()) {
                        SplitInstallSessionStatus.DOWNLOADING,
                        SplitInstallSessionStatus.INSTALLING -> {
                            val total =
                                state.totalBytesToDownload()
                            val downloaded =
                                state.bytesDownloaded()
                            val percent =
                                if (total > 0L) {
                                    (
                                        downloaded * 100L /
                                            total
                                        ).toInt().coerceIn(
                                            0,
                                            100
                                        )
                                } else {
                                    0
                                }
                            onProgress(percent)
                        }

                        SplitInstallSessionStatus.INSTALLED -> {
                            manager.unregisterListener(this)
                            SplitCompat.installActivity(
                                activity
                            )
                            onProgress(100)
                            onInstalled()
                        }

                        SplitInstallSessionStatus.FAILED,
                        SplitInstallSessionStatus.CANCELED -> {
                            manager.unregisterListener(this)
                            onError(
                                "Não foi possível instalar o pacote. Verifique a internet e tente novamente."
                            )
                        }
                    }
                }
            }

        manager.registerListener(listener)

        val request =
            SplitInstallRequest
                .newBuilder()
                .addModule(module)
                .build()

        manager.startInstall(request)
            .addOnFailureListener {
                manager.unregisterListener(listener)
                onError(
                    "O pacote ainda não está disponível nesta versão da app."
                )
            }
    }
}
