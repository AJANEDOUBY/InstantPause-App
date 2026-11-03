package com.instantpause.app.domain.usecase

import android.telecom.PhoneAccountHandle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.instantpause.app.data.datastore.ProtectionPrefs
import com.instantpause.app.domain.model.Schedule
import com.instantpause.app.domain.phone.PhoneNumberAnalyzer
import com.instantpause.app.domain.repository.AddWhitelistResult
import com.instantpause.app.domain.repository.ScheduleRepository
import com.instantpause.app.domain.repository.WhitelistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ShouldBlockCallUseCaseTest {

    private val zone: ZoneId = ZoneId.of("Europe/Paris")
    private val tuesdayNoon: ZonedDateTime = ZonedDateTime.of(2026, 3, 10, 12, 0, 0, 0, zone)
    private val tuesdayLate: ZonedDateTime = ZonedDateTime.of(2026, 3, 10, 23, 30, 0, 0, zone)

    private fun nightSchedule(): Schedule =
        Schedule(
            start = LocalTime.of(22, 0),
            end = LocalTime.of(7, 0),
            daysMask = Schedule.ALL_DAYS,
            isActive = true
        )

    private fun useCase(
        schedules: List<Schedule> = emptyList(),
        whitelisted: Boolean = false,
        normalized: String? = null,
        emergencyNumbers: Set<String> = emptySet(),
        emergencyUntil: Long = 0L
    ): ShouldBlockCallUseCase {
        val prefs = ProtectionPrefs(FakeDataStore(emptyPreferences()))
        if (emergencyUntil != 0L) {
            runBlocking { prefs.setEmergencyUntil(emergencyUntil) }
        }
        return ShouldBlockCallUseCase(
            schedules = FakeScheduleRepository(schedules),
            whitelist = FakeWhitelistRepository(whitelisted),
            prefs = prefs,
            normalizer = FakeAnalyzer(emergencyNumbers, normalized)
        )
    }

    @Test
    fun `urgence autorisee meme pendant une plage active`() {
        val uc = useCase(
            schedules = listOf(nightSchedule()),
            emergencyNumbers = setOf("112")
        )

        val decision = runBlocking { uc("112", listOf("FR"), tuesdayLate) }

        assertEquals(AllowReason.EMERGENCY_NUMBER, (decision as CallDecision.Allow).reason)
    }

    @Test
    fun `protection inactive hors plage`() {
        val uc = useCase(schedules = emptyList())

        val decision = runBlocking { uc("0612345678", listOf("FR"), tuesdayNoon) }

        assertTrue(decision is CallDecision.Allow)
        assertEquals(AllowReason.PROTECTION_INACTIVE, (decision as CallDecision.Allow).reason)
        assertNull(decision.normalizedNumber)
    }

    @Test
    fun `mode urgence emotionnelle bloque`() {
        val uc = useCase(
            schedules = emptyList(),
            emergencyUntil = tuesdayNoon.toInstant().toEpochMilli() + 3_600_000
        )

        val decision = runBlocking { uc("0612345678", listOf("FR"), tuesdayNoon) }

        assertEquals(ProtectionReason.EMERGENCY_MODE, (decision as CallDecision.Block).reason)
    }

    @Test
    fun `mode urgence expire ne bloque plus`() {
        val uc = useCase(
            schedules = emptyList(),
            emergencyUntil = tuesdayNoon.toInstant().toEpochMilli() - 1_000
        )

        val decision = runBlocking { uc("0612345678", listOf("FR"), tuesdayNoon) }

        assertEquals(AllowReason.PROTECTION_INACTIVE, (decision as CallDecision.Allow).reason)
    }

    @Test
    fun `plage active bloque et remonte le numero normalise`() {
        val uc = useCase(
            schedules = listOf(nightSchedule()),
            normalized = "+33612345678"
        )

        val decision = runBlocking { uc("0612345678", listOf("FR"), tuesdayLate) }

        assertEquals(ProtectionReason.SCHEDULE_WINDOW, (decision as CallDecision.Block).reason)
        assertEquals("+33612345678", decision.normalizedNumber)
    }

    @Test
    fun `whitelist autorise pendant une plage active`() {
        val uc = useCase(
            schedules = listOf(nightSchedule()),
            whitelisted = true,
            normalized = "+33612345678"
        )

        val decision = runBlocking { uc("0612345678", listOf("FR"), tuesdayLate) }

        assertEquals(AllowReason.WHITELISTED, (decision as CallDecision.Allow).reason)
        assertEquals("+33612345678", decision.normalizedNumber)
    }

    @Test
    fun `whitelist autorise aussi en mode urgence`() {
        val uc = useCase(
            whitelisted = true,
            normalized = "+33612345678",
            emergencyUntil = tuesdayNoon.toInstant().toEpochMilli() + 3_600_000
        )

        val decision = runBlocking { uc("0612345678", listOf("FR"), tuesdayNoon) }

        assertEquals(AllowReason.WHITELISTED, (decision as CallDecision.Allow).reason)
    }

    @Test
    fun `regions vides retombent sur la region par defaut`() {
        val uc = useCase(
            schedules = listOf(nightSchedule()),
            normalized = "+33612345678"
        )

        val decision = runBlocking { uc("0612345678", emptyList(), tuesdayLate) }

        assertTrue(decision is CallDecision.Block)
    }
}

// ===== Fakes =====

private class FakeDataStore<T>(initial: T) : DataStore<T> {
    private val state = MutableStateFlow(initial)

    override val data: Flow<T> = state

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

private class FakeScheduleRepository(
    private val items: List<Schedule>
) : ScheduleRepository {
    override fun observeAll(): Flow<List<Schedule>> = MutableStateFlow(items)
    override suspend fun activeSchedules(): List<Schedule> = items.filter { it.isActive }
    override suspend fun upsert(schedule: Schedule) = Unit
    override suspend fun setActive(id: Long, active: Boolean) = Unit
    override suspend fun delete(id: Long) = Unit
}

private class FakeWhitelistRepository(
    private val whitelisted: Boolean
) : WhitelistRepository {
    override fun observeAll(): Flow<List<com.instantpause.app.domain.model.WhitelistEntry>> =
        MutableStateFlow(emptyList())

    override suspend fun isWhitelisted(candidates: List<String>): Boolean = whitelisted

    override suspend fun add(
        contactName: String,
        rawNumber: String,
        regionHint: String
    ): AddWhitelistResult = AddWhitelistResult.ADDED

    override suspend fun remove(id: Long) = Unit
}

private class FakeAnalyzer(
    private val emergencyNumbers: Set<String>,
    private val normalized: String?
) : PhoneNumberAnalyzer {
    override fun resolveRegions(handle: PhoneAccountHandle?): List<String> = listOf("FR")
    override fun toE164(raw: String, region: String): String? = normalized
    override fun candidates(raw: String, regions: List<String>): List<String> =
        listOfNotNull(normalized)

    override fun isEmergency(raw: String, regions: List<String>): Boolean =
        raw in emergencyNumbers

    override fun defaultRegion(): String = "FR"
}
