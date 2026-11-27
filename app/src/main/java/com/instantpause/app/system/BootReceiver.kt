package com.instantpause.app.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.instantpause.app.di.DndEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recalcule la protection après un redémarrage ou une mise à jour de l'app.
 *
 * Attention : sur certaines surcouches (itel/Transsion, MIUI…), le système peut
 * ne jamais délivrer BOOT_COMPLETED tant que l'app n'est pas autorisée au
 * démarrage automatique. Un échec ici n'est donc pas fatal : Telecom relance
 * l'app à froid au premier appel sortant.
 *
 * Il n'y a plus de service permanent à relancer : seules deux choses doivent
 * survivre au redémarrage, et ce récepteur les couvre. Le filtre « Ne pas
 * déranger » est recalé tout de suite (une plage a pu se terminer pendant
 * l'arrêt) et l'alarme de transition est reprogrammée.
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

        // Une plage a pu se terminer pendant l'arrêt : le filtre « Ne pas
        // déranger » doit être retiré même si l'app n'est jamais rouverte.
        DndRestore.request(context)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EntryPointAccessors
                    .fromApplication(context.applicationContext, DndEntryPoint::class.java)
                    .protectionAlarms()
                    .reschedule()
            } catch (error: Throwable) {
                Log.w(TAG, "Alarme de transition non reprogrammée", error)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "CoolDownBoot"
    }
}
