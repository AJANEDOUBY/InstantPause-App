package com.cooldown.app.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Ce récepteur est réveillé par le système toutes les 15 minutes.
 * Si le processus de CoolDown a été tué (par exemple quand l'écran s'est éteint),
 * cette alarme relance le Watchdog pour maintenir la protection active.
 */
class WatchdogAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Réveil automatique : vérification de la protection.")

        // 1. On relance le Watchdog (si l'app est morte, ça la ressuscite)
        CoolDownWatchdogService.startIfNeeded(context)

        // 2. On reprogramme la prochaine alarme (dans 15 minutes)
        scheduleNextAlarm(context)
    }

    companion object {
        private const val TAG = "WatchdogAlarm"
        private const val INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
        private const val ALARM_REQUEST_CODE = 9000

        fun startAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = createPendingIntent(context)

            // On utilise setAndAllowWhileIdle : fonctionne même quand le téléphone est en veille
            val triggerAt = System.currentTimeMillis() + INTERVAL_MS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            Log.d(TAG, "Alarme de sécurité programmée dans 15 minutes.")
        }

        fun scheduleNextAlarm(context: Context) {
            startAlarm(context) // On boucle indéfiniment
        }

        private fun createPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WatchdogAlarmReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}