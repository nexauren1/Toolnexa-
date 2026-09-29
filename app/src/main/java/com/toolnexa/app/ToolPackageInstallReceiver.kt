package com.toolnexa.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller

class ToolPackageInstallReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val module =
            intent.getStringExtra(EXTRA_MODULE)
                ?: return

        val revision =
            intent.getStringExtra(EXTRA_REVISION)
                ?: ""

        val prefs =
            context.getSharedPreferences(
                "tool_packages",
                Context.MODE_PRIVATE
            )

        val status =
            intent.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
            )

        if (
            status ==
                PackageInstaller.STATUS_SUCCESS
        ) {
            prefs.edit()
                .putString(
                    "pending_status_" + module,
                    "success"
                )
                .putString(
                    "revision_" + module,
                    revision
                )
                .putBoolean(
                    "ever_installed_" + module,
                    true
                )
                .apply()

            return
        }

        val message =
            intent.getStringExtra(
                PackageInstaller.EXTRA_STATUS_MESSAGE
            )
                ?: "O Android não conseguiu instalar o pacote."

        prefs.edit()
            .putString(
                "pending_status_" + module,
                "failure"
            )
            .putString(
                "pending_message_" + module,
                message
            )
            .apply()
    }

    companion object {
        const val EXTRA_MODULE =
            "tool_package_module"

        const val EXTRA_REVISION =
            "tool_package_revision"

        const val EXTRA_CATEGORY =
            "tool_package_category"
    }
}
