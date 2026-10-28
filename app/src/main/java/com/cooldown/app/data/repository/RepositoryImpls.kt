package com.cooldown.app.data.repository

import com.cooldown.app.data.local.BlockedLogDao
import com.cooldown.app.data.local.BlockedLogEntity
import com.cooldown.app.data.local.InsertOutcome
import com.cooldown.app.data.local.ScheduleDao
import com.cooldown.app.data.local.WhitelistDao
import com.cooldown.app.data.local.WhitelistEntity
import com.cooldown.app.data.local.toDomain
import com.cooldown.app.data.local.toEntity
import com.cooldown.app.domain.model.BlockStatus
import com.cooldown.app.domain.model.BlockedLog
import com.cooldown.app.domain.model.Schedule
import com.cooldown.app.domain.model.WhitelistEntry
import com.cooldown.app.domain.phone.PhoneNumberNormalizer
import com.cooldown.app.domain.repository.AddWhitelistResult
import com.cooldown.app.domain.repository.BlockedLogRepository
import com.cooldown.app.domain.repository.ScheduleRepository
import com.cooldown.app.domain.repository.WhitelistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val dao: ScheduleDao
) : ScheduleRepository {
    override fun observeAll(): Flow<List<Schedule>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun activeSchedules(): List<Schedule> =
        dao.getActive().map { it.toDomain() }

    override suspend fun upsert(schedule: Schedule) {
        dao.upsert(schedule.toEntity())
    }

    override suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active)

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

@Singleton
class WhitelistRepositoryImpl @Inject constructor(
    private val dao: WhitelistDao,
    private val normalizer: PhoneNumberNormalizer
) : WhitelistRepository {

    override fun observeAll(): Flow<List<WhitelistEntry>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun isWhitelisted(candidates: List<String>): Boolean =
        candidates.isNotEmpty() && dao.countMatching(candidates) > 0

    override suspend fun add(
        contactName: String,
        rawNumber: String,
        regionHint: String
    ): AddWhitelistResult {
        val e164 = normalizer.candidates(rawNumber, listOf(regionHint)).firstOrNull()
            ?: return AddWhitelistResult.INVALID_NUMBER

        val outcome = dao.insertWithLimit(
            WhitelistEntity(
                contactName = contactName.trim(),
                phoneE164 = e164,
                phoneDisplay = rawNumber.trim(),
                regionCode = regionHint
            ),
            max = WHITELIST_MAX
        )
        return when (outcome) {
            InsertOutcome.INSERTED -> AddWhitelistResult.ADDED
            InsertOutcome.DUPLICATE -> AddWhitelistResult.DUPLICATE
            InsertOutcome.LIMIT_REACHED -> AddWhitelistResult.LIMIT_REACHED
        }
    }

    override suspend fun remove(id: Long) = dao.deleteById(id)

    private companion object {
        const val WHITELIST_MAX = 5
    }
}

@Singleton
class BlockedLogRepositoryImpl @Inject constructor(
    private val dao: BlockedLogDao
) : BlockedLogRepository {
    override fun observeRecent(limit: Int): Flow<List<BlockedLog>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun log(number: String, status: BlockStatus) {
        dao.insert(
            BlockedLogEntity(
                timestamp = System.currentTimeMillis(),
                targetNumberBlocked = number,
                status = status
            )
        )
        // Purge opportuniste : évite la croissance illimitée de la table.
        dao.deleteOlderThan(System.currentTimeMillis() - MAX_LOG_AGE_MS)
    }

    private companion object {
        const val MAX_LOG_AGE_MS = 90L * 24 * 60 * 60 * 1000 // 90 jours
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindSchedule(impl: ScheduleRepositoryImpl): ScheduleRepository
    @Binds abstract fun bindWhitelist(impl: WhitelistRepositoryImpl): WhitelistRepository
    @Binds abstract fun bindBlockedLog(impl: BlockedLogRepositoryImpl): BlockedLogRepository
}