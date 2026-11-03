package com.instantpause.app.domain.repository

import com.instantpause.app.domain.model.BlockStatus
import com.instantpause.app.domain.model.BlockedLog
import com.instantpause.app.domain.model.Schedule
import com.instantpause.app.domain.model.WhitelistEntry
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeAll(): Flow<List<Schedule>>
    suspend fun activeSchedules(): List<Schedule>
    suspend fun upsert(schedule: Schedule)
    suspend fun setActive(id: Long, active: Boolean)
    suspend fun delete(id: Long)
}

enum class AddWhitelistResult { ADDED, DUPLICATE, LIMIT_REACHED, INVALID_NUMBER }

interface WhitelistRepository {
    fun observeAll(): Flow<List<WhitelistEntry>>
    suspend fun isWhitelisted(candidates: List<String>): Boolean
    suspend fun add(contactName: String, rawNumber: String, regionHint: String): AddWhitelistResult
    suspend fun remove(id: Long)

    companion object {
        /** Le cercle reste volontairement minuscule : 3 personnes, pas une liste. */
        const val MAX_ENTRIES = 3
    }
}

interface BlockedLogRepository {
    fun observeRecent(limit: Int = 200): Flow<List<BlockedLog>>
    suspend fun log(number: String, status: BlockStatus)
}