package com.cooldown.app.domain.repository

import com.cooldown.app.domain.model.BlockStatus
import com.cooldown.app.domain.model.BlockedLog
import com.cooldown.app.domain.model.Schedule
import com.cooldown.app.domain.model.WhitelistEntry
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
}

interface BlockedLogRepository {
    fun observeRecent(limit: Int = 200): Flow<List<BlockedLog>>
    suspend fun log(number: String, status: BlockStatus)
}