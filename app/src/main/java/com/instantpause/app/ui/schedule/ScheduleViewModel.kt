package com.instantpause.app.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantpause.app.R
import com.instantpause.app.data.datastore.ProtectionPrefs
import com.instantpause.app.domain.dnd.DndController
import com.instantpause.app.domain.model.Schedule
import com.instantpause.app.domain.protection.ProtectionAlarms
import com.instantpause.app.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    private val prefs: ProtectionPrefs,
    private val dnd: DndController,
    private val alarms: ProtectionAlarms
) : ViewModel() {

    val schedules: StateFlow<List<Schedule>> = repository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Échéance du mode Zen, en millisecondes murales (0 = inactif).
     * Lu par `ShouldBlockCallUseCase`, qui le traite comme une protection active.
     */
    val emergencyUntil: StateFlow<Long> = prefs.emergencyUntil
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0L
        )

    fun addSchedule(start: LocalTime, end: LocalTime, daysMask: Int) {
        if (start == end || daysMask == 0) return
        viewModelScope.launch {
            repository.upsert(
                Schedule(
                    start = start,
                    end = end,
                    daysMask = daysMask,
                    isActive = true
                )
            )
            syncDnd()
            rescheduleAlarms()
        }
    }

    /**
     * Active le mode Zen pour [durationMs].
     *
     * Auparavant une plage récurrente « tous les jours » était créée : la protection
     * se réactivait chaque nuit à la même heure, et il fallait le code pour s'en
     * débarrasser. On utilise désormais l'échéance ponctuelle `emergencyUntil`, qui
     * expire d'elle-même sans laisser de trace dans la liste des plages.
     */
    fun activateZenMode(durationMs: Long = ZEN_DURATION_MS) {
        val end = System.currentTimeMillis() + durationMs.coerceIn(MIN_ZEN_MS, MAX_ZEN_MS)
        viewModelScope.launch {
            prefs.setEmergencyUntil(end)
            syncDnd()
            rescheduleAlarms()
        }
    }

    /** Coupe le mode Zen avant son échéance (action qui affaiblit la protection). */
    fun cancelZenMode() {
        viewModelScope.launch {
            prefs.clearEmergency()
            syncDnd()
            rescheduleAlarms()
        }
    }

    fun toggleActive(schedule: Schedule) {
        viewModelScope.launch {
            repository.setActive(schedule.id, !schedule.isActive)
            syncDnd()
            rescheduleAlarms()
        }
    }

    fun delete(schedule: Schedule) {
        viewModelScope.launch {
            repository.delete(schedule.id)
            syncDnd()
            rescheduleAlarms()
        }
    }

    /**
     * Recale le filtre « Ne pas déranger » (plage qui se termine, Zen expiré,
     * retour des réglages système). Appelé aussi par la boucle d'horloge de
     * l'écran Plages, qui tourne toutes les 15 s.
     */
    suspend fun syncDnd() {
        runCatching { dnd.sync() }
    }

    /**
     * Une plage a changé : le filtre et l'alarme de transition doivent suivre.
     *
     * Séparé de [syncDnd] parce que la boucle d'horloge de l'écran Plages appelle
     * cette dernière toutes les 15 s — inutile de replanifier une alarme aussi souvent.
     */
    private suspend fun rescheduleAlarms() {
        runCatching { alarms.reschedule() }
    }

    /** Version non suspendue, pour les écrans qui n'ont pas de coroutine sous la main. */
    fun requestDndSync() {
        viewModelScope.launch { syncDnd() }
    }

    companion object {
        const val ZEN_DURATION_MS = 2 * 60 * 60 * 1000L
        const val MIN_ZEN_MS = 5 * 60 * 1000L
        const val MAX_ZEN_MS = 24 * 60 * 60 * 1000L

        /** Durées proposées d'un seul appui : libellé localisé puis durée en millisecondes. */
        val ZEN_PRESETS: List<Pair<Int, Long>> = listOf(
            R.string.zen_hours_4 to 4 * 60 * 60 * 1000L,
            R.string.zen_hours_6 to 6 * 60 * 60 * 1000L,
            R.string.zen_hours_8 to 8 * 60 * 60 * 1000L,
            R.string.zen_hours_10 to 10 * 60 * 60 * 1000L,
            R.string.zen_hours_12 to 12 * 60 * 60 * 1000L
        )

        private const val SAME_END_TOLERANCE_MS = 60_000L

        /**
         * Vrai si [durationMs] terminerait le Zen **avant** l'échéance en cours.
         *
         * Réduire une protection est une action qui affaiblit : elle exige le code.
         * Prolonger, en revanche, renforce la protection et reste libre.
         * La tolérance d'une minute évite de redemander le code quand on réappuie
         * sur la même durée quelques secondes plus tard.
         */
        fun shortensCurrentZen(
            nowMs: Long,
            currentUntilMs: Long,
            durationMs: Long
        ): Boolean {
            if (currentUntilMs <= nowMs) return false
            val newEnd = nowMs + durationMs.coerceIn(MIN_ZEN_MS, MAX_ZEN_MS)
            return newEnd < currentUntilMs - SAME_END_TOLERANCE_MS
        }
    }
}
