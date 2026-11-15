package com.instantpause.app.domain.protection

import com.instantpause.app.data.datastore.ProtectionPrefs
import com.instantpause.app.domain.repository.ScheduleRepository
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Pourquoi la protection est active à un instant donné.
 *
 * `NONE` signifie que rien ne bloque : ni mode Zen, ni plage en cours.
 */
enum class ProtectionReason { NONE, EMERGENCY_MODE, SCHEDULE_WINDOW }

/**
 * Réponse unique à la question « la protection est-elle active maintenant ? ».
 *
 * Trois endroits posaient la question avec trois calculs légèrement différents :
 * `ShouldBlockCallUseCase`, le bandeau de l'onglet Plages, et bientôt le
 * contrôleur « Ne pas déranger ». Une seule implémentation évite qu'ils
 * divergent (par exemple une plage oubliée dans le filtre DND).
 */
interface ProtectionWindow {
    suspend fun activeReason(now: ZonedDateTime): ProtectionReason
}

/**
 * Implémentation réelle : Zen d'abord (échéance ponctuelle), puis plages.
 *
 * L'ordre a une conséquence visible : un Zen actif reste actif même si toutes
 * les plages sont désactivées, et son échéance est respectée à la seconde.
 */
class ProtectionState @Inject constructor(
    private val schedules: ScheduleRepository,
    private val prefs: ProtectionPrefs
) : ProtectionWindow {

    override suspend fun activeReason(now: ZonedDateTime): ProtectionReason {
        if (prefs.getEmergencyUntil() > now.toInstant().toEpochMilli()) {
            return ProtectionReason.EMERGENCY_MODE
        }
        if (schedules.activeSchedules().any { it.isActiveAt(now) }) {
            return ProtectionReason.SCHEDULE_WINDOW
        }
        return ProtectionReason.NONE
    }
}
