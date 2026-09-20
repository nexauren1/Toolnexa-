package com.toolnexa.app

import android.app.Activity
import android.app.Application
import android.os.Bundle

class ToolNexaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(
            object :
                ActivityLifecycleCallbacks {
                override fun onActivityResumed(
                    activity: Activity
                ) {
                    val name =
                        activity
                            .javaClass
                            .simpleName

                    val user =
                        com.google.firebase.auth.FirebaseAuth
                            .getInstance()
                            .currentUser

                    if (user != null) {
                        AnalyticsTracker(activity)
                            .setUser(user)
                    }

                    AnalyticsTracker(
                        activity
                    ).screen(
                        "page_" +
                            name
                                .removeSuffix(
                                    "Activity"
                                )
                                .replace(
                                    Regex(
                                        "([a-z])([A-Z])"
                                    ),
                                    "$1_$2"
                                )
                                .lowercase()
                    )
                }

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?
                ) = Unit

                override fun onActivityStarted(
                    activity: Activity
                ) = Unit

                override fun onActivityPaused(
                    activity: Activity
                ) = Unit

                override fun onActivityStopped(
                    activity: Activity
                ) = Unit

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle
                ) = Unit

                override fun onActivityDestroyed(
                    activity: Activity
                ) = Unit
            }
        )
    }
}
