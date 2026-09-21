package com.toolnexa.app

import android.app.Activity
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

/**
 * Registry of downloadable category packages.
 *
 * Each package maps to one Play Feature Delivery dynamic-feature module.
 * New tools are released by increasing the package version together with
 * the normal app release. Google Play updates installed feature modules
 * automatically when a new App Bundle is published.
 */
object ToolPackageCatalog {

    data class Package(
        val category: String,
        val module: String,
        val version: Int
    ) {
        val toolCount: Int
            get() = ToolRegistry.countFor(category)
    }

    /*
     * Only categories that already have a real dynamic-feature module
     * are enabled here. As each new category is migrated to its own
     * feature module, add one entry below. This prevents the app from
     * showing a download button for a module that does not exist yet.
     */
    private val packages = listOf(
        Package(
            category = "Business",
            module = "feature_business",
            version = 1
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

    fun all(): List<Package> {
        return packages
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
                com.google.android.play.core.splitinstall
                    .SplitInstallStateUpdatedListener {

                override fun onStateUpdate(
                    state: SplitInstallSessionState
                ) {
                    if (
                        !state.moduleNames()
                            .contains(module)
                    ) {
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
                                        ).toInt()
                                            .coerceIn(
                                                0,
                                                100
                                            )
                                } else {
                                    0
                                }

                            onProgress(percent)
                        }

                        SplitInstallSessionStatus.INSTALLED -> {
                            manager.unregisterListener(
                                this
                            )

                            SplitCompat.installActivity(
                                activity
                            )

                            onProgress(100)
                            onInstalled()
                        }

                        SplitInstallSessionStatus.FAILED,
                        SplitInstallSessionStatus.CANCELED -> {
                            manager.unregisterListener(
                                this
                            )

                            onError(
                                "Não foi possível instalar o pacote. " +
                                    "Verifique a internet e tente novamente."
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
                manager.unregisterListener(
                    listener
                )

                onError(
                    "O pacote ainda não está disponível " +
                        "nesta versão da app."
                )
            }
    }
}
