package com.instantpause.app.domain.protection

import com.instantpause.app.domain.model.Schedule
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * L'alarme de transition remplace l'ancien réveil toutes les quinze minutes.
 * Elle doit tomber exactement au prochain début ou à la prochaine fin de plage,
 * sans jamais en oublier une (plage de nuit, jour unique de la semaine, Zen).
 */
class ProtectionBoundariesTest {

    private val zone: ZoneId = ZoneId.of("Europe/Paris")

    private fun at(day: Int, hour: Int, minute: Int = 0): ZonedDateTime =
        ZonedDateTime.of(2026, 3, day, hour, minute, 0, 0, zone)

    private fun schedule(
        start: String,
        end: String,
        daysMask: Int = Schedule.ALL_DAYS,
        isActive: Boolean = true
    ) = Schedule(
        start = LocalTime.parse(start),
        end = LocalTime.parse(end),
        daysMask = daysMask,
        isActive = isActive
    )

    @Test
    fun `au milieu d'une plage de nuit, la fin du jour est retenue`() {
        // 3 h du matin : la plage a commencé la veille à 22 h et finit à 7 h.
        val next = schedule("22:00", "07:00").nextBoundaryAfter(at(day = 10, hour = 3))

        assertEquals(at(day = 10, hour = 7), next)
    }

    @Test
    fun `plage de nuit, le prochain changement est la fin du lendemain`() {
        val next = schedule("22:00", "07:00").nextBoundaryAfter(at(day = 10, hour = 23))

        assertEquals(at(day = 11, hour = 7), next)
    }

    @Test
    fun `avant le debut, le prochain changement est le debut du jour`() {
        val next = schedule("22:00", "07:00").nextBoundaryAfter(at(day = 10, hour = 12))

        assertEquals(at(day = 10, hour = 22), next)
    }

    @Test
    fun `plage de jour, debut puis fin`() {
        val schedule = schedule("09:00", "12:00")

        assertEquals(at(day = 10, hour = 9), schedule.nextBoundaryAfter(at(day = 10, hour = 8)))
        assertEquals(at(day = 10, hour = 12), schedule.nextBoundaryAfter(at(day = 10, hour = 10)))
    }

    @Test
    fun `un seul jour de la semaine est respecte`() {
        // Mardi 10 mars 2026 ; plage uniquement le lundi.
        val mondayOnly = schedule(
            start = "22:00",
            end = "07:00",
            daysMask = Schedule.dayBit(DayOfWeek.MONDAY)
        )

        assertEquals(at(day = 16, hour = 22), mondayOnly.nextBoundaryAfter(at(day = 10, hour = 12)))
    }

    @Test
    fun `plage inactive ou vide ne programme rien`() {
        assertNull(schedule("22:00", "07:00", isActive = false).nextBoundaryAfter(at(10, 12)))
        assertNull(schedule("22:00", "22:00").nextBoundaryAfter(at(10, 12)))
        assertNull(schedule("22:00", "07:00", daysMask = 0).nextBoundaryAfter(at(10, 12)))
    }

    @Test
    fun `le Zen en cours passe avant une plage plus lointaine`() {
        val now = at(day = 10, hour = 12)
        val zenEnd = at(day = 10, hour = 14).toInstant().toEpochMilli()

        val next = nextProtectionBoundary(
            now = now,
            schedules = listOf(schedule("22:00", "07:00")),
            emergencyUntilMs = zenEnd
        )

        assertEquals(at(day = 10, hour = 14), next)
    }

    @Test
    fun `une plage plus proche que le Zen est retenue`() {
        val now = at(day = 10, hour = 12)
        val zenEnd = at(day = 10, hour = 20).toInstant().toEpochMilli()

        val next = nextProtectionBoundary(
            now = now,
            schedules = listOf(schedule("13:00", "14:00")),
            emergencyUntilMs = zenEnd
        )

        assertEquals(at(day = 10, hour = 13), next)
    }

    @Test
    fun `un Zen expire et aucune plage ne programme rien`() {
        val now = at(day = 10, hour = 12)
        val zenEnd = at(day = 10, hour = 11).toInstant().toEpochMilli()

        assertNull(
            nextProtectionBoundary(
                now = now,
                schedules = emptyList(),
                emergencyUntilMs = zenEnd
            )
        )
    }
}
