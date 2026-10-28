package com.cooldown.app.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Ré-arme la protection après un redémarrage ou une mise à jour de l'app.
 *
 * Attention : sur certaines surcouches (itel/Transsion, MIUI…), le système peut
 * ne jamais délivrer BOOT_COMPLETED tant que l'app n'est pas autorisée au
 * démarrage automatique. Un échec ici n'est donc pas fatal : Telecom relance
 * l'app à froid au premier appel sortant.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        Log.i(TAG, "Réveil système ($action) : ré-armement de la protection.")

        // Chaque étape est isolée : un refus du système ne doit pas empêcher l'autre.
        runCatching { CoolDownWatchdogService.startIfNeeded(context) }
            .onSuccess { Log.i(TAG, "Watchdog relancé.") }
            .onFailure { Log.w(TAG, "Watchdog non relancé (restriction système ?)", it) }

        runCatching { WatchdogAlarmReceiver.startAlarm(context) }
            .onSuccess { Log.i(TAG, "Alarme de sécurité reprogrammée.") }
            .onFailure { Log.w(TAG, "Alarme non reprogrammée (restriction système ?)", it) }
    }

    private companion object {
        const val TAG = "CoolDownBoot"
    }
}
