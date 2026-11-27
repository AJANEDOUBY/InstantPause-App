package com.instantpause.app.domain.protection

import com.instantpause.app.domain.model.Schedule
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Prochain instant où l'état de protection peut basculer.
 *
 * Une plage n'a que deux transitions utiles : son début et sa fin. Les connaître
 * permet de programmer **une seule alarme ponctuelle** au lieu de réveiller l'app
 * toutes les quinze minutes pour vérifier.
 *
 * Aucune alarme n'est nécessaire pour intercepter un appel : c'est Telecom qui
 * démarre `CallRedirectionService` à froid. La seule raison de réveiller l'app est
 * le filtre « Ne pas déranger », qui doit suivre les plages.
 */
fun Schedule.nextBoundaryAfter(now: ZonedDateTime): ZonedDateTime? {
    if (!isActive || start == end || daysMask == 0) return null

    var best: ZonedDateTime? = null
    val today = now.toLocalDate()

    // On part de la veille : une plage qui a commencé hier et franchit minuit se
    // termine peut-être aujourd'hui, et cette fin est plus proche que tout le reste.
    for (offset in -1..LOOKAHEAD_DAYS) {
        val date = today.plusDays(offset.toLong())
        if (daysMask and Schedule.dayBit(date.dayOfWeek) == 0) continue

        val startAt = date.atTime(start).atZone(now.zone)
        val endAt = date.atTime(end).atZone(now.zone).let {
            if (crossesMidnight) it.plusDays(1) else it
        }

        for (candidate in listOf(startAt, endAt)) {
            if (!candidate.isAfter(now)) continue
            if (best == null || candidate.isBefore(best)) best = candidate
        }
    }

    return best
}

/**
 * Le plus proche des transitions : début ou fin de plage, ou expiration du mode Zen.
 *
 * `null` signifie qu'aucune protection n'est programmée : aucune alarme à poser.
 */
fun nextProtectionBoundary(
    now: ZonedDateTime,
    schedules: List<Schedule>,
    emergencyUntilMs: Long
): ZonedDateTime? {
    val zenEnd = emergencyUntilMs
        .takeIf { it > now.toInstant().toEpochMilli() }
        ?.let { Instant.ofEpochMilli(it).atZone(now.zone) }

    val fromSchedules = schedules.mapNotNull { it.nextBoundaryAfter(now) }.minOrNull()

    return listOfNotNull(zenEnd, fromSchedules).minOrNull()
}

/** Une plage hebdomadaire ne peut pas avoir de transition au-delà de 8 jours. */
private const val LOOKAHEAD_DAYS = 8
