package com.instantpause.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule ORDER BY startMinuteOfDay")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedule WHERE isActive = 1")
    suspend fun getActive(): List<ScheduleEntity>

    @Upsert
    suspend fun upsert(entity: ScheduleEntity): Long

    @Query("UPDATE schedule SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM schedule WHERE id = :id")
    suspend fun deleteById(id: Long)
}

enum class InsertOutcome { INSERTED, DUPLICATE, LIMIT_REACHED }

@Dao
abstract class WhitelistDao {
    @Query("SELECT * FROM whitelist ORDER BY contactName COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<WhitelistEntity>>

    @Query("SELECT COUNT(*) FROM whitelist WHERE phoneE164 IN (:candidates)")
    abstract suspend fun countMatching(candidates: List<String>): Int

    @Query("SELECT COUNT(*) FROM whitelist")
    abstract suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnore(entity: WhitelistEntity): Long

    @Query("DELETE FROM whitelist WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Transaction
    open suspend fun insertWithLimit(entity: WhitelistEntity, max: Int): InsertOutcome {
        if (countMatching(listOf(entity.phoneE164)) > 0) return InsertOutcome.DUPLICATE
        if (count() >= max) return InsertOutcome.LIMIT_REACHED
        return if (insertIgnore(entity) == -1L) InsertOutcome.DUPLICATE else InsertOutcome.INSERTED
    }
}

@Dao
interface BlockedLogDao {
    @Insert suspend fun insert(entity: BlockedLogEntity): Long

    @Query("SELECT * FROM blocked_logs ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<BlockedLogEntity>>

    @Query("DELETE FROM blocked_logs WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}