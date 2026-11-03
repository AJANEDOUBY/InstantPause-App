package com.instantpause.app.domain.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

enum class BlockStatus { BLOCKED, ALLOWED_WHITELIST, USER_CLOSED }

data class Schedule(
    val id: Long = 0,
    val start: LocalTime,
    val end: LocalTime,
    val daysMask: Int,
    val isActive: Boolean = true
) {
    val crossesMidnight: Boolean get() = end < start

    fun isActiveAt(now: ZonedDateTime): Boolean {
        if (!isActive || start == end) return false
        val t = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
        val today = dayBit(now.dayOfWeek)
        val yesterday = dayBit(now.dayOfWeek.minus(1))
        return if (!crossesMidnight) {
            (daysMask and today) != 0 && t >= start && t < end
        } else {
            ((daysMask and today) != 0 && t >= start) ||
                    ((daysMask and yesterday) != 0 && t < end)
        }
    }

    companion object {
        const val ALL_DAYS = 0b1111111
        fun dayBit(day: DayOfWeek): Int = 1 shl (day.value - 1)
    }
}

data class WhitelistEntry(
    val id: Long,
    val contactName: String,
    val phoneE164: String,
    val phoneDisplay: String
)

data class BlockedLog(
    val id: Long,
    val timestamp: Long,
    val number: String,
    val status: BlockStatus
)