package com.instantpause.app.system

import android.content.Context
import com.instantpause.app.di.DndEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recale le filtre « Ne pas déranger » depuis un `BroadcastReceiver`.
 *
 * Les récepteurs ne sont pas injectés par Hilt : on passe par
 * [DndEntryPoint]. Le travail est court (deux lectures DataStore et un appel
 * système), d'où l'absence de `goAsync()`.
 */
internal object DndRestore {

    fun request(context: Context) {
        runCatching {
            val controller = EntryPointAccessors
                .fromApplication(context.applicationContext, DndEntryPoint::class.java)
                .dndController()
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { controller.sync() }
            }
        }
    }
}
