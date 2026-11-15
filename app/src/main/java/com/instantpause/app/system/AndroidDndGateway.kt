package com.instantpause.app.system

import android.app.NotificationManager
import android.content.Context
import com.instantpause.app.domain.dnd.DndGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Passerelle réelle vers `NotificationManager`.
 *
 * `setInterruptionFilter` lève `SecurityException` sans accès à la politique de
 * notification : on renvoie `false` plutôt que de propager l'exception, le
 * contrôleur se contente alors de ne rien appliquer (fail-open : le téléphone
 * sonne normalement).
 */
@Singleton
class AndroidDndGateway @Inject constructor(
    @param:ApplicationContext context: Context
) : DndGateway {

    private val manager: NotificationManager? =
        context.getSystemService(NotificationManager::class.java)

    override fun isAccessGranted(): Boolean =
        manager?.isNotificationPolicyAccessGranted == true

    override fun currentFilter(): Int =
        manager?.currentInterruptionFilter ?: FILTER_UNKNOWN

    override fun setFilter(filter: Int): Boolean = runCatching {
        val nm = manager ?: return false
        nm.setInterruptionFilter(filter)
        true
    }.getOrDefault(false)

    private companion object {
        /** `NotificationManager.INTERRUPTION_FILTER_UNKNOWN`. */
        const val FILTER_UNKNOWN = 0
    }
}
