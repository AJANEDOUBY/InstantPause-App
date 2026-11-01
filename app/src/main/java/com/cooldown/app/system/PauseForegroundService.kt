package com.cooldown.app.system

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.cooldown.app.ui.pause.PauseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Service ultra-court pour ouvrir PauseActivity depuis l'arrière-plan.
 *
 * Sur Android 14+, startActivity() depuis un FGS démarré par un BroadcastReceiver est bloqué.
 * Solution : envoyer un PendingIntent avec le mode BAL explicite autorisé.
 */
class PauseForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    // Voir CoolDownWatchdogService : constante recopiée à la compilation, sans danger
    // sur les Android antérieurs à 14 (ServiceCompat ignore le type).
    @SuppressLint("InlinedApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val number = intent?.getStringExtra(EXTRA_NUMBER).orEmpty()

        // 1. Passer en foreground immédiatement (obligatoire <5s sur Android 12+).
        val notification = ProtectionAlertNotifier.buildForegroundNotification(this)
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        // 2. Ouvrir PauseActivity via PendingIntent + mode BAL autorisé (Android 14+).
        runCatching { launchPauseActivity(number) }
            .onFailure { /* log éventuel */ }

        // 3. S'arrêter après 3 secondes.
        serviceScope.launch {
            delay(3_000L)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    /**
     * Sur Android 14+, seul un PendingIntent envoyé avec
     * MODE_BACKGROUND_ACTIVITY_START_ALLOWED peut ouvrir une activité depuis un FGS
     * démarré en arrière-plan.
     */
    @Suppress("DEPRECATION")
    private fun launchPauseActivity(number: String) {
        val targetIntent = PauseActivity.intent(this, number)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = PendingIntent.getActivity(
            this,
            REQ_PAUSE,
            targetIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            options.setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            )
        }

        try {
            pendingIntent.send(this, 0, null, null, null, null, options.toBundle())
        } catch (e: PendingIntent.CanceledException) {
            @Suppress("DEPRECATION")
            startActivity(targetIntent)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_NUMBER = "extra_target_number"
        private const val NOTIF_ID = 4204
        private const val REQ_PAUSE = 4300

        fun start(context: Context, number: String) {
            val intent = Intent(context, PauseForegroundService::class.java)
                .putExtra(EXTRA_NUMBER, number)
            context.startForegroundService(intent)
        }
    }
}