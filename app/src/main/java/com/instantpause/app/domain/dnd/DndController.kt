package com.instantpause.app.domain.dnd

import com.instantpause.app.domain.protection.ProtectionReason
import com.instantpause.app.domain.protection.ProtectionWindow
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * État persistant du filtre « Ne pas déranger » posé par l'application.
 *
 * [previousFilter] est la valeur exacte de `interruptionFilter` avant notre
 * passage : c'est elle qu'on restaure, jamais une valeur supposée. Sans cela,
 * l'application écraserait le réglage de l'utilisateur (sonnerie, vibreur…).
 *
 * [overridden] retient que l'utilisateur a coupé le silence lui-même pendant la
 * plage en cours. Sans ce drapeau, la synchronisation suivante (retour dans
 * l'app, boucle de 15 s) reposerait le filtre qu'il vient de retirer : le
 * téléphone se remettrait à sonner tout seul, puis se tairait tout seul.
 */
data class DndState(
    val enabled: Boolean = false,
    val active: Boolean = false,
    val previousFilter: Int = 0,
    val overridden: Boolean = false
)

/** Lecture/écriture de [DndState], implémentée par `ProtectionPrefs`. */
interface DndPreferences {
    suspend fun readDnd(): DndState
    suspend fun writeDnd(state: DndState)
}

/**
 * Accès système au filtre d'interruption, isolé pour rester testable sans Android.
 *
 * Les valeurs numériques correspondent à `NotificationManager.INTERRUPTION_FILTER_*`.
 */
interface DndGateway {
    fun isAccessGranted(): Boolean
    fun currentFilter(): Int

    /** @return vrai si le filtre a bien été appliqué. */
    fun setFilter(filter: Int): Boolean
}

/**
 * Pose et retire le filtre `PRIORITY` pendant les plages et le mode Zen.
 *
 * Règles (voir `docs/DND-1.6.0.md`) :
 * - `PRIORITY` est le seul filtre compatible avec la promesse « les contacts de
 *   confiance restent joignables » : alarmes et conversations prioritaires passent
 *   selon les réglages Android de l'utilisateur.
 * - Si l'utilisateur coupe le mode depuis le volet rapide, on ne le réapplique
 *   pas : il reprend la main.
 * - Si l'accès est retiré, on ne prétend pas avoir restauré : `active` reste vrai
 *   et l'interface le signale.
 * - Si le téléphone est déjà en `PRIORITY` au début de la plage, l'application ne
 *   prend pas la main : elle n'a rien posé, donc rien à restaurer.
 *
 * Toutes les opérations passent par [mutex] : `sync` est appelé depuis les
 * écrans, `BootReceiver` et `WatchdogAlarmReceiver`. Sans sérialisation, deux
 * appels simultanés pouvaient mémoriser `PRIORITY` (posé par le premier) comme
 * « filtre précédent » et laisser le téléphone bloqué en Ne pas déranger.
 */
@Singleton
class DndController @Inject constructor(
    private val protection: ProtectionWindow,
    private val prefs: DndPreferences,
    private val gateway: DndGateway,
    private val clock: Clock
) {
    private val mutex = Mutex()

    /** Aligne le filtre système sur l'état réel de la protection. */
    suspend fun sync(now: ZonedDateTime = ZonedDateTime.now(clock)) {
        mutex.withLock {
            syncLocked(now)
        }
    }

    /**
     * Active ou désactive l'option, puis applique immédiatement.
     *
     * La désactivation restaure le filtre d'origine : c'est une action qui
     * rend le téléphone plus bruyant, donc elle ne demande pas le code.
     *
     * Un geste explicite sur cet interrupteur efface aussi le refus mémorisé :
     * l'utilisateur vient de dire ce qu'il veut pour la plage en cours.
     */
    suspend fun setEnabled(enabled: Boolean) {
        mutex.withLock {
            val state = prefs.readDnd()
            prefs.writeDnd(state.copy(enabled = enabled, overridden = false))
            syncLocked(ZonedDateTime.now(clock))
        }
    }

    /** Restaure le filtre si l'application en avait posé un. */
    suspend fun restoreNow() {
        mutex.withLock {
            val state = prefs.readDnd()
            if (state.active) restoreLocked(state)
        }
    }

    /** À n'appeler que mutex pris : le `Mutex` n'est pas réentrant. */
    private suspend fun syncLocked(now: ZonedDateTime) {
        val state = prefs.readDnd()

        if (state.active) {
            val stillProtected = isProtected(now)
            if (!state.enabled || !stillProtected) {
                restoreLocked(state)
                return
            }
            // Filtre modifié entre-temps (volet rapide, autre app) : l'utilisateur
            // a repris la main. On le note, sinon la synchronisation suivante
            // reposerait le silence qu'il vient de retirer.
            if (gateway.isAccessGranted() && gateway.currentFilter() != FILTER_PRIORITY) {
                prefs.writeDnd(state.copy(active = false, overridden = true))
            }
            return
        }

        if (!state.enabled) return
        if (!isProtected(now)) {
            // La plage est finie : le refus de l'utilisateur ne vaut que pour
            // elle. La prochaine plage repart d'un silence normal.
            if (state.overridden) prefs.writeDnd(state.copy(overridden = false))
            return
        }
        if (state.overridden) return
        if (!gateway.isAccessGranted()) return

        val rawPrevious = gateway.currentFilter()
        // Déjà en « Prioritaires seulement » : le silence existe, l'utilisateur
        // l'a choisi. On n'en prend pas la propriété, sinon la fin de plage
        // écraserait son réglage.
        if (rawPrevious == FILTER_PRIORITY) return

        // Valeur inconnue : on se rabat sur le mode normal plutôt que de
        // mémoriser 0, qui n'est pas restaurable de façon fiable.
        val previous = if (rawPrevious == FILTER_UNKNOWN) FILTER_ALL else rawPrevious

        if (!gateway.setFilter(FILTER_PRIORITY)) return
        prefs.writeDnd(state.copy(active = true, previousFilter = previous))
    }

    private suspend fun isProtected(now: ZonedDateTime): Boolean =
        protection.activeReason(now) != ProtectionReason.NONE

    private suspend fun restoreLocked(state: DndState) {
        // Migration : un état enregistré par une version défectueuse (PRIORITY ou
        // 0 mémorisé comme « précédent ») est ramené au mode normal, sinon le
        // téléphone resterait bloqué en Ne pas déranger.
        val target = if (state.previousFilter == FILTER_PRIORITY || state.previousFilter == FILTER_UNKNOWN) {
            FILTER_ALL
        } else {
            state.previousFilter
        }
        if (!gateway.setFilter(target)) return
        prefs.writeDnd(state.copy(active = false))
    }

    companion object {
        /** `NotificationManager.INTERRUPTION_FILTER_UNKNOWN`. */
        const val FILTER_UNKNOWN = 0

        /** `NotificationManager.INTERRUPTION_FILTER_ALL` (mode normal). */
        const val FILTER_ALL = 1

        /** `NotificationManager.INTERRUPTION_FILTER_PRIORITY`. */
        const val FILTER_PRIORITY = 2
    }
}