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
 * Réveil au début ou à la fin d'une plage (ou à l'expiration du mode Zen).
 *
 * Deux choses seulement : recaler le filtre « Ne pas déranger » sur l'état réel,
 * puis programmer la transition suivante. Si l'option de silence est coupée, le
 * recalcul annule toute alarme et le téléphone redevient parfaitement calme.
 */
class ProtectionAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        DndRestore.request(context)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EntryPointAccessors
                    .fromApplication(context.applicationContext, DndEntryPoint::class.java)
                    .protectionAlarms()
                    .reschedule()
            } catch (error: Throwable) {
                Log.w(TAG, "Prochaine transition non programmée", error)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ProtectionAlarm"
    }
}
