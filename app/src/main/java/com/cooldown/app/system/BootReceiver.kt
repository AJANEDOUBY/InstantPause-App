package com.cooldown.app.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i("CoolDownBoot", "Redémarrage détecté : relance de la protection.")

            // Relance le Watchdog et l'Alarme de sécurité
            CoolDownWatchdogService.startIfNeeded(context)
            WatchdogAlarmReceiver.startAlarm(context)
        }
    }
}