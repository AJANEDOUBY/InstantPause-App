package com.instantpause.app.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantpause.app.data.datastore.ProtectionPrefs
import com.instantpause.app.domain.model.Schedule
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
    private val prefs: ProtectionPrefs
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
        viewModelScope.launch { prefs.setEmergencyUntil(end) }
    }

    /** Coupe le mode Zen avant son échéance (action qui affaiblit la protection). */
    fun cancelZenMode() {
        viewModelScope.launch { prefs.clearEmergency() }
    }

    fun toggleActive(schedule: Schedule) {
        viewModelScope.launch {
            repository.setActive(schedule.id, !schedule.isActive)
        }
    }

    fun delete(schedule: Schedule) {
        viewModelScope.launch {
            repository.delete(schedule.id)
        }
    }

    companion object {
        const val ZEN_DURATION_MS = 2 * 60 * 60 * 1000L
        const val MIN_ZEN_MS = 5 * 60 * 1000L
        const val MAX_ZEN_MS = 24 * 60 * 60 * 1000L

        /** Durées proposées d'un seul appui, de la plus courte à la plus longue. */
        val ZEN_PRESETS: List<Pair<String, Long>> = listOf(
            "3 h" to 3 * 60 * 60 * 1000L,
            "4 h" to 4 * 60 * 60 * 1000L,
            "6 h" to 6 * 60 * 60 * 1000L,
            "8 h" to 8 * 60 * 60 * 1000L,
            "12 h" to 12 * 60 * 60 * 1000L
        )
    }
}
