package com.instantpause.app.ui.schedule

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.instantpause.app.MainDispatcherRule
import com.instantpause.app.data.datastore.ProtectionPrefs
import com.instantpause.app.domain.dnd.DndController
import com.instantpause.app.domain.dnd.DndGateway
import com.instantpause.app.domain.model.Schedule
import com.instantpause.app.domain.protection.ProtectionAlarms
import com.instantpause.app.domain.protection.ProtectionState
import com.instantpause.app.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalTime

class ScheduleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: ProtectionPrefs

    @Before
    fun setUp() {
        dataStore = FakeDataStore(emptyPreferences())
        prefs = ProtectionPrefs(dataStore)
    }

    @After
    fun tearDown() {
        // Rien à libérer : le DataStore de test est en mémoire.
    }

    @Test
    fun `le mode Zen ne cree aucune plage recurrente`() = runTest {
        val repository = RecordingScheduleRepository()
        val viewModel = ScheduleViewModel(repository, prefs, dndController(repository), NoopAlarms)
        val before = System.currentTimeMillis()

        viewModel.activateZenMode()

        assertEquals(emptyList<Schedule>(), repository.upserted)
        assertEquals(0, repository.upsertCount)

        val until = prefs.getEmergencyUntil()
        assertTrue("échéance absente", until > 0L)
        assertTrue(
            "échéance trop courte",
            until >= before + ScheduleViewModel.ZEN_DURATION_MS
        )
        assertTrue(
            "échéance aberrante",
            until <= System.currentTimeMillis() + ScheduleViewModel.ZEN_DURATION_MS
        )
    }

    @Test
    fun `une duree personnalisee est respectee`() = runTest {
        val viewModel = ScheduleViewModel(RecordingScheduleRepository(), prefs, dndController(RecordingScheduleRepository()), NoopAlarms)
        val before = System.currentTimeMillis()

        viewModel.activateZenMode(4 * 60 * 60 * 1000L)

        val until = prefs.getEmergencyUntil()
        assertTrue(until >= before + 4 * 60 * 60 * 1000L)
        assertTrue(until <= System.currentTimeMillis() + 4 * 60 * 60 * 1000L)
    }

    @Test
    fun `une duree hors bornes est ramenee dans l'intervalle autorise`() = runTest {
        val viewModel = ScheduleViewModel(RecordingScheduleRepository(), prefs, dndController(RecordingScheduleRepository()), NoopAlarms)
        val before = System.currentTimeMillis()

        viewModel.activateZenMode(48 * 60 * 60 * 1000L)

        val until = prefs.getEmergencyUntil()
        assertTrue(until <= System.currentTimeMillis() + ScheduleViewModel.MAX_ZEN_MS)
        assertTrue(until >= before + ScheduleViewModel.MAX_ZEN_MS - 1_000L)
    }

    @Test
    fun `annuler le mode Zen remet l'echeance a zero`() = runTest {
        val viewModel = ScheduleViewModel(RecordingScheduleRepository(), prefs, dndController(RecordingScheduleRepository()), NoopAlarms)
        viewModel.activateZenMode()
        assertTrue(prefs.getEmergencyUntil() > 0L)

        viewModel.cancelZenMode()

        assertEquals(0L, prefs.getEmergencyUntil())
    }

    @Test
    fun `reduire une periode Zen est identifie comme un affaiblissement`() {
        val now = 1_000_000_000L
        val tenHours = 10 * 60 * 60 * 1000L

        assertTrue(
            ScheduleViewModel.shortensCurrentZen(now, now + tenHours, 4 * 60 * 60 * 1000L)
        )
        assertTrue(
            ScheduleViewModel.shortensCurrentZen(now, now + tenHours, 6 * 60 * 60 * 1000L)
        )
    }

    @Test
    fun `prolonger ou rechoisir la meme duree reste libre`() {
        val now = 1_000_000_000L
        val fourHours = 4 * 60 * 60 * 1000L

        assertFalse(
            ScheduleViewModel.shortensCurrentZen(now, now + fourHours, 10 * 60 * 60 * 1000L)
        )
        assertFalse(
            ScheduleViewModel.shortensCurrentZen(now, now + fourHours, fourHours)
        )
        assertFalse(
            ScheduleViewModel.shortensCurrentZen(now, now - 1_000L, 4 * 60 * 60 * 1000L)
        )
    }

    @Test
    fun `une plage ajoutee passe bien par le depot`() = runTest {
        val repository = RecordingScheduleRepository()
        val viewModel = ScheduleViewModel(repository, prefs, dndController(repository), NoopAlarms)

        viewModel.addSchedule(LocalTime.of(22, 0), LocalTime.of(7, 0), Schedule.ALL_DAYS)

        assertEquals(1, repository.upsertCount)
        assertEquals(LocalTime.of(22, 0), repository.upserted.single().start)
        assertEquals(LocalTime.of(7, 0), repository.upserted.single().end)
    }

    /**
     * Contrôleur DND réel mais inerte : l'accès système est refusé, donc
     * `sync()` ne touche à rien. Les tests du filtre lui-même vivent dans
     * `DndControllerTest`.
     */
    private fun dndController(repository: ScheduleRepository): DndController =
        DndController(
            protection = ProtectionState(repository, prefs),
            prefs = prefs,
            gateway = DeniedDndGateway,
            clock = Clock.systemUTC()
        )
}

private object DeniedDndGateway : DndGateway {
    override fun isAccessGranted(): Boolean = false
    override fun currentFilter(): Int = 0
    override fun setFilter(filter: Int): Boolean = false
}

/** Les alarmes n'ont pas de sens dans un test JVM : on ne vérifie que les mutations. */
private object NoopAlarms : ProtectionAlarms {
    override suspend fun reschedule() = Unit
    override fun cancel() = Unit
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

private class RecordingScheduleRepository : ScheduleRepository {
    val upserted = mutableListOf<Schedule>()
    val upsertCount: Int get() = upserted.size

    override fun observeAll(): Flow<List<Schedule>> = MutableStateFlow(upserted.toList())

    override suspend fun activeSchedules(): List<Schedule> = upserted.filter { it.isActive }

    override suspend fun upsert(schedule: Schedule) {
        upserted += schedule
    }

    override suspend fun setActive(id: Long, active: Boolean) = Unit
    override suspend fun delete(id: Long) = Unit
}
