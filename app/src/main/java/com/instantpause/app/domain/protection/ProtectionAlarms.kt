package com.instantpause.app.domain.protection

/**
 * Alarme unique posée au prochain changement d'état de la protection.
 *
 * Le domaine ne sait pas comment Android programme une alarme : il demande
 * seulement de recalculer l'échéance après une modification de plage, du mode
 * Zen ou de l'option « Silence pendant la pause ». L'implémentation vit dans
 * `ProtectionAlarmScheduler`.
 */
interface ProtectionAlarms {
    /** Recalcule la prochaine transition et remplace l'alarme en place. */
    suspend fun reschedule()

    /** Retire toute alarme (option de silence coupée, aucune plage). */
    fun cancel()
}
