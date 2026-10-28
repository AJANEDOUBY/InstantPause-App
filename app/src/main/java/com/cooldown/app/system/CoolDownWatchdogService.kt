package com.cooldown.app.system

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat

/**
 * Service permanent qui maintient CoolDown en vie en arrière-plan.
 * Nécessaire sur certains téléphones (itel, Tecno, Infinix) qui tuent
 * agressivement les apps dès qu'elles sont fermées.
 */
class CoolDownWatchdogService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
        return START_STICKY
    }

    private fun createChannel() {
        val manager = NotificationManagerCompat.from(this)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MIN)
                .setName("CoolDown veille")
                .setDescription("Maintient la protection active en arrière-plan")
                .setShowBadge(false)
                .build()
        )
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pending = launchIntent?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("CoolDown veille")
            .setContentText("Votre protection est active.")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "cooldown_watchdog"
        private const val NOTIF_ID = 4205

        /** Démarre le service uniquement sur les téléphones problématiques. */
        fun startIfNeeded(context: Context) {
            val brand = Build.BRAND.lowercase()
            val needsWatchdog = brand in setOf(
                "itel", "tecno", "infinix", "xiaomi", "redmi", "poco",
                "huawei", "honor", "oppo", "realme", "vivo",
                "samsung", "oneplus", "meizu", "letv", "nokia"
            )
            if (needsWatchdog) start(context)
        }

        fun start(context: Context) {
            val intent = Intent(context, CoolDownWatchdogService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CoolDownWatchdogService::class.java))
        }
    }
}