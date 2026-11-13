package com.instantpause.app.system

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.instantpause.app.ui.pause.PauseActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtectionAlertNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val manager = NotificationManagerCompat.from(context)

    init {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Protection Instant Pause")
                .setDescription("Alertes de blocage d'appel")
                .setVibrationEnabled(true)
                .build()
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(SERVICE_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Service Instant Pause")
                .setDescription("Notification discrete pendant l'ouverture de l'ecran de pause")
                .build()
        )
        // Même notification, mais sans tête haute ni vibration : utilisée dès que
        // l'écran de pause est visible, pour ne pas recouvrir son titre.
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(QUIET_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Pause ouverte")
                .setDescription("Rappel discret pendant que l'écran de pause est affiché")
                .setVibrationEnabled(false)
                .build()
        )
    }

    /** Vrai tant que l'écran de pause est au premier plan. */
    @Volatile
    private var pauseVisible = false

    fun setPauseVisible(visible: Boolean) {
        pauseVisible = visible
    }

    /**
     * Remplace l'alerte sonore par un rappel discret, sans tête haute.
     *
     * Appelée quand l'écran de pause s'ouvre de lui-même : la notification reste
     * disponible dans le volet, mais ne vient plus masquer « Prenez un instant ».
     */
    @SuppressLint("MissingPermission")
    fun softenBlockedCall(number: String) {
        if (!manager.areNotificationsEnabled()) return
        runCatching {
            manager.cancel(ID_BLOCKED_CALL)
            manager.notify(ID_BLOCKED_CALL, buildBlockedNotification(number, quiet = true))
        }.onFailure { Log.e(TAG, "Soften failed", it) }
    }

    @SuppressLint("MissingPermission")
    fun showBlockedCall(number: String) {
        Log.d(TAG, "showBlockedCall called for $number")
        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled, skip")
            return
        }

        // Une seule alerte, au moment du blocage. Aucune répétition : l'appel est
        // déjà refusé, re-sonner toutes les 30 s n'apporte rien et finit par
        // pousser l'utilisateur à couper les notifications de l'app.
        val quiet = pauseVisible
        runCatching {
            manager.notify(ID_BLOCKED_CALL, buildBlockedNotification(number, quiet = quiet))
        }.onFailure { Log.e(TAG, "Notify failed", it) }
        if (!quiet) vibrate()

        Log.d(TAG, "showBlockedCall done")
    }

    @SuppressLint("MissingPermission")
    fun dismissBlockedCall() {
        Log.d(TAG, "dismissBlockedCall")
        pauseVisible = false
        runCatching { manager.cancel(ID_BLOCKED_CALL) }
    }

    private fun buildBlockedNotification(number: String, quiet: Boolean = false): Notification {
        val pauseIntent = PauseActivity.intent(context, number, fromUser = true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pending = PendingIntent.getActivity(
            context,
            REQ_PAUSE,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(
            context,
            if (quiet) QUIET_CHANNEL_ID else CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Appel bloqué")
            .setContentText("Touchez ici pour reprendre votre souffle.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Touchez pour ouvrir l'écran de pause et respirer un instant.")
            )
            .setPriority(
                if (quiet) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_MAX
            )
            .setSilent(quiet)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .apply {
                if (!quiet) {
                    setFullScreenIntent(pending, true)
                    setVibrate(longArrayOf(0, 300, 150, 300, 150, 300))
                }
            }
            .build()
    }

    private fun vibrate() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.let {
            if (it.hasVibrator()) {
                it.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    companion object {
        private const val TAG = "CoolDownNotifier"
        private const val CHANNEL_ID = "cooldown_protection"
        private const val QUIET_CHANNEL_ID = "cooldown_protection_quiet"
        private const val SERVICE_CHANNEL_ID = "cooldown_service"
        private const val ID_BLOCKED_CALL = 4203

        // Doit rester différent du request code de PauseForegroundService (4300) :
        // même code + même Intent filtré = même PendingIntent, donc extras écrasés.
        private const val REQ_PAUSE = 4301

        fun buildForegroundNotification(context: Context): Notification {
            return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Instant Pause")
                .setContentText("Préparation de la pause...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .build()
        }
    }
}