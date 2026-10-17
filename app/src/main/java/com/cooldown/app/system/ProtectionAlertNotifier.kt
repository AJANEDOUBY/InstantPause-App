package com.cooldown.app.system

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cooldown.app.ui.pause.PauseActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtectionAlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = NotificationManagerCompat.from(context)

    init {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Protection CoolDown")
                .setDescription("Alertes de protection et appels bloqués")
                .build()
        )
    }

    /** Notification affichée quand un appel est bloqué. */
    fun showBlockedCall(number: String) {
        if (!manager.areNotificationsEnabled()) return

        val pauseIntent = PauseActivity.intent(context, number)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pending = PendingIntent.getActivity(
            context,
            REQ_PAUSE,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Prenons un moment.")
            .setContentText("Cet appel peut attendre demain.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)  // ⭐ NOUVEAU
            .build()

        runCatching { manager.notify(ID_BLOCKED_CALL, notification) }
    }

    companion object {
        private const val CHANNEL_ID = "cooldown_protection"
        private const val ID_BLOCKED_CALL = 4203
        private const val REQ_PAUSE = 4300

        /**
         * Notification minimale utilisée par PauseForegroundService.
         * Doit être appelée dans les 5 s après startForegroundService().
         */
        fun buildForegroundNotification(context: Context): Notification {
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Prenons un moment.")
                .setContentText("Cet appel peut attendre demain.")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .build()
        }
    }
}