package com.cooldown.app.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Reçoit les événements système au démarrage ou après mise à jour de l'app.
 * Pour l'instant, simple trace : les plages horaires se recalculent à chaque appel.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> Log.i(TAG, "Démarrage détecté")
            Intent.ACTION_MY_PACKAGE_REPLACED -> Log.i(TAG, "Mise à jour de l'app détectée")
        }
    }

    private companion object {
        const val TAG = "CoolDownBoot"
    }
}