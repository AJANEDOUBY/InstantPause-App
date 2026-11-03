package com.instantpause.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.instantpause.app.domain.model.BlockStatus
import com.instantpause.app.domain.model.BlockedLog
import com.instantpause.app.domain.model.Schedule
import com.instantpause.app.domain.model.WhitelistEntry
import java.time.LocalTime

@Entity(tableName = "schedule", indices = [Index("isActive")])
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val daysOfWeek: Int,
    val isActive: Boolean = true
)

@Entity(tableName = "whitelist", indices = [Index(value = ["phoneE164"], unique = true)])
data class WhitelistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val phoneE164: String,
    val phoneDisplay: String,
    val regionCode: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_logs", indices = [Index("timestamp")])
data class BlockedLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val targetNumberBlocked: String,
    val status: BlockStatus
)

class BlockStatusConverters {
    @TypeConverter fun fromStatus(s: BlockStatus): String = s.name
    @TypeConverter fun toStatus(v: String): BlockStatus =
        runCatching { BlockStatus.valueOf(v) }.getOrDefault(BlockStatus.BLOCKED)
}

// ===== Mappers entité <-> domaine =====
private fun minutesToTime(m: Int): LocalTime =
    LocalTime.ofSecondOfDay(m.coerceIn(0, 1439) * 60L)
private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

fun ScheduleEntity.toDomain() = Schedule(
    id, minutesToTime(startMinuteOfDay), minutesToTime(endMinuteOfDay), daysOfWeek, isActive
)
fun Schedule.toEntity() = ScheduleEntity(
    id, start.toMinuteOfDay(), end.toMinuteOfDay(), daysMask, isActive
)
fun WhitelistEntity.toDomain() = WhitelistEntry(id, contactName, phoneE164, phoneDisplay)
fun BlockedLogEntity.toDomain() = BlockedLog(id, timestamp, targetNumberBlocked, status)