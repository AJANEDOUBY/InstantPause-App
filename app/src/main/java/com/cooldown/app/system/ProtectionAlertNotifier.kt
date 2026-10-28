package com.cooldown.app.system

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
import com.cooldown.app.ui.pause.PauseActivity
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
        runCatching { manager.notify(ID_BLOCKED_CALL, buildBlockedNotification(number)) }
            .onFailure { Log.e(TAG, "Notify failed", it) }
        vibrate()

        Log.d(TAG, "showBlockedCall done")
    }

    @SuppressLint("MissingPermission")
    fun dismissBlockedCall() {
        Log.d(TAG, "dismissBlockedCall")
        runCatching { manager.cancel(ID_BLOCKED_CALL) }
    }

    private fun buildBlockedNotification(number: String): Notification {
        val pauseIntent = PauseActivity.intent(context, number, fromUser = true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pending = PendingIntent.getActivity(
            context,
            REQ_PAUSE,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Appel bloque")
            .setContentText("Touchez ici pour reprendre votre souffle.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Cet appel peut attendre demain. Touchez pour ouvrir l'ecran de pause.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)
            .setVibrate(longArrayOf(0, 300, 150, 300, 150, 300))
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(500)
                }
            }
        }
    }

    companion object {
        private const val TAG = "CoolDownNotifier"
        private const val CHANNEL_ID = "cooldown_protection"
        private const val SERVICE_CHANNEL_ID = "cooldown_service"
        private const val ID_BLOCKED_CALL = 4203

        // Doit rester différent du request code de PauseForegroundService (4300) :
        // même code + même Intent filtré = même PendingIntent, donc extras écrasés.
        private const val REQ_PAUSE = 4301

        fun buildForegroundNotification(context: Context): Notification {
            return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Instant Pause")
                .setContentText("Preparation de la pause...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .build()
        }
    }
}