package com.cooldown.app.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooldown.app.domain.model.Schedule
import com.cooldown.app.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository
) : ViewModel() {

    /** Liste des plages, rechargée automatiquement à chaque changement en base. */
    val schedules: StateFlow<List<Schedule>> = repository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
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
}