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
     * Active le mode Zen pendant [ZEN_DURATION_MS].
     *
     * Auparavant une plage récurrente « tous les jours » était créée : la protection
     * se réactivait chaque nuit à la même heure, et il fallait le code pour s'en
     * débarrasser. On utilise désormais l'échéance ponctuelle `emergencyUntil`, qui
     * expire d'elle-même sans laisser de trace dans la liste des plages.
     */
    fun activateZenMode() {
        val end = System.currentTimeMillis() + ZEN_DURATION_MS
        viewModelScope.launch { prefs.setEmergencyUntil(end) }
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
    }
}
