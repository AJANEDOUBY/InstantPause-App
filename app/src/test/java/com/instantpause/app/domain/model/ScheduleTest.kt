package com.instantpause.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleTest {

    private val zone: ZoneId = ZoneId.of("Europe/Paris")

    /** 2026-03-10 est un mardi. */
    private fun at(day: Int, hour: Int, minute: Int = 0): ZonedDateTime =
        ZonedDateTime.of(2026, 3, day, hour, minute, 0, 0, zone)

    private fun schedule(
        start: LocalTime,
        end: LocalTime,
        mask: Int = Schedule.ALL_DAYS,
        active: Boolean = true
    ) = Schedule(start = start, end = end, daysMask = mask, isActive = active)

    @Test
    fun `plage intra-jour active le bon jour et dans la fenetre`() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0), Schedule.dayBit(DayOfWeek.TUESDAY))

        assertTrue(s.isActiveAt(at(10, 12)))
    }

    @Test
    fun `borne de debut incluse`() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0))

        assertTrue(s.isActiveAt(at(10, 9)))
    }

    @Test
    fun `borne de fin exclue`() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0))

        assertFalse(s.isActiveAt(at(10, 17)))
    }

    @Test
    fun `hors fenetre horaire`() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0))

        assertFalse(s.isActiveAt(at(10, 18)))
    }

    @Test
    fun `jour non selectionne`() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0), Schedule.dayBit(DayOfWeek.TUESDAY))

        // Mercredi
        assertFalse(s.isActiveAt(at(11, 12)))
    }

    @Test
    fun `plage desactivee jamais active`() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(17, 0), active = false)

        assertFalse(s.isActiveAt(at(10, 12)))
    }

    @Test
    fun `debut egal a la fin jamais actif`() {
        val s = schedule(LocalTime.of(9, 0), LocalTime.of(9, 0))

        assertFalse(s.isActiveAt(at(10, 9)))
    }

    @Test
    fun `franchit minuit le soir`() {
        val s = schedule(LocalTime.of(22, 0), LocalTime.of(7, 0), Schedule.dayBit(DayOfWeek.TUESDAY))

        assertTrue(s.isActiveAt(at(10, 23, 30)))
    }

    @Test
    fun `franchit minuit le lendemain matin`() {
        val s = schedule(LocalTime.of(22, 0), LocalTime.of(7, 0), Schedule.dayBit(DayOfWeek.TUESDAY))

        // Mercredi 06:00 -> hier (mardi) sélectionné
        assertTrue(s.isActiveAt(at(11, 6)))
    }

    @Test
    fun `franchit minuit mais jour precedent non selectionne`() {
        val s = schedule(LocalTime.of(22, 0), LocalTime.of(7, 0), Schedule.dayBit(DayOfWeek.WEDNESDAY))

        // Mercredi 06:00 : hier = mardi, non sélectionné ; 06:00 < 22:00
        assertFalse(s.isActiveAt(at(11, 6)))
    }

    @Test
    fun `franchit minuit mais fenetre terminee`() {
        val s = schedule(LocalTime.of(22, 0), LocalTime.of(7, 0), Schedule.dayBit(DayOfWeek.TUESDAY))

        // Mercredi 08:00 : hors fenêtre
        assertFalse(s.isActiveAt(at(11, 8)))
    }
}
