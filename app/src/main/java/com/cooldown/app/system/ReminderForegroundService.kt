package com.cooldown.app.system

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderForegroundService : Service() {

    @Inject lateinit var notifier: ProtectionAlertNotifier

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var reminderJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== onCreate called ===")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "=== onStartCommand called ===")
        val number = intent?.getStringExtra(EXTRA_NUMBER).orEmpty()
        Log.d(TAG, "Number: '$number'")

        if (number.isEmpty()) {
            Log.w(TAG, "Empty number, stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildServiceNotification()
        try {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
            Log.d(TAG, "startForeground OK")
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground FAILED", t)
            stopSelf()
            return START_NOT_STICKY
        }

        reminderJob?.cancel()
        reminderJob = scope.launch {
            Log.d(TAG, "Reminder loop STARTED (interval=${REMINDER_INTERVAL_MS}ms, count=$REMINDER_COUNT)")
            try {
                repeat(REMINDER_COUNT) { index ->
                    Log.d(TAG, "Iteration ${index + 1}: sleeping ${REMINDER_INTERVAL_MS}ms")
                    delay(REMINDER_INTERVAL_MS)
                    if (!isActive) {
                        Log.w(TAG, "Job cancelled at iteration ${index + 1}")
                        return@launch
                    }
                    Log.d(TAG, "Posting reminder ${index + 1}/$REMINDER_COUNT")
                    runCatching { notifier.postReminder(number) }
                        .onFailure { Log.e(TAG, "postReminder failed at iteration ${index + 1}", it) }
                }
                Log.d(TAG, "Reminder loop FINISHED normally")
            } finally {
                Log.d(TAG, "Stopping self")
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun buildServiceNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pending = launchIntent?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("CoolDown")
            .setContentText("Rappels de pause actifs")
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }

    override fun onDestroy() {
        Log.d(TAG, "=== onDestroy ===")
        reminderJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ReminderFGS"
        private const val EXTRA_NUMBER = "extra_reminder_number"
        private const val NOTIF_ID = 4206
        private const val SERVICE_CHANNEL_ID = "cooldown_service"
        private const val REMINDER_INTERVAL_MS = 30_000L
        private const val REMINDER_COUNT = 10

        fun start(context: Context, number: String) {
            Log.d(TAG, "start() called")
            val intent = Intent(context, ReminderForegroundService::class.java)
                .putExtra(EXTRA_NUMBER, number)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            Log.d(TAG, "stop() called")
            context.stopService(Intent(context, ReminderForegroundService::class.java))
        }
    }
}