package com.instantpause.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.instantpause.app.domain.security.AppLockManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CoolDownApplication : Application() {

    @Inject
    lateinit var appLock: AppLockManager

    /** Nombre d'activités démarrées : 0 ⇒ l'app est en arrière-plan. */
    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()

        // On compte les activités de l'app plutôt que d'utiliser ProcessLifecycleOwner :
        // pas de dépendance supplémentaire, et PauseActivity est comptée comme
        // « app au premier plan » (on ne se reverrouille pas pendant un appel bloqué).
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (startedActivities == 0) appLock.onAppForegrounded()
                startedActivities++
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                if (startedActivities == 0) appLock.onAppBackgrounded()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
