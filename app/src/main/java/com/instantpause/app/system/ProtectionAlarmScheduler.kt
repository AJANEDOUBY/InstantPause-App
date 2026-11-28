package com.instantpause.app.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.instantpause.app.data.datastore.ProtectionPrefs
import com.instantpause.app.domain.protection.ProtectionAlarms
import com.instantpause.app.domain.protection.nextProtectionBoundary
import com.instantpause.app.domain.repository.ScheduleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Programme **une** alarme au prochain changement d'état de la protection.
 *
 * Remplace l'ancien réveil toutes les quinze minutes : on ne dérange le système
 * qu'au début et à la fin d'une plage, et uniquement si l'option « Silence pendant
 * la pause » est active — c'est la seule chose qui doive être ajustée sans que
 * l'utilisateur ouvre l'app. L'interception des appels, elle, ne dépend d'aucune
 * alarme : Telecom démarre le service de redirection à froid.
 */
@Singleton
class ProtectionAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val schedules: ScheduleRepository,
    private val prefs: ProtectionPrefs
) : ProtectionAlarms {

    /** Recalcule la prochaine transition et remplace l'alarme en place. */
    override suspend fun reschedule() {
        val now = ZonedDateTime.now()
        val next = if (prefs.isDndEnabled()) {
            nextProtectionBoundary(
                now = now,
                schedules = schedules.activeSchedules(),
                emergencyUntilMs = prefs.getEmergencyUntil()
            )
        } else {
            null
        }

        if (next == null) {
            cancel()
            return
        }

        set(next.toInstant().toEpochMilli())
    }

    override fun cancel() {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching { manager.cancel(pendingIntent()) }
    }

    private fun set(triggerAtMs: Long) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent()

        // Exact quand le système l'autorise (fin de plage à la minute près), sinon
        // repli inexact : la restauration du son peut alors glisser de quelques
        // minutes, ce qui reste préférable à un refus de programmer l'alarme.
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            manager.canScheduleExactAlarms()

        runCatching {
            if (exactAllowed) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMs, pending
                )
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMs, pending
                )
            }
        }.onFailure { error ->
            Log.w(TAG, "Alarme exacte refusée, repli inexact", error)
            runCatching {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMs, pending
                )
            }.onFailure { Log.w(TAG, "Aucune alarme programmable", it) }
        }
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ProtectionAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private companion object {
        const val TAG = "ProtectionAlarms"
        const val REQUEST_CODE = 9000
    }
}
