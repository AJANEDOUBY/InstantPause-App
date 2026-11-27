package com.instantpause.app.domain.dnd

import com.instantpause.app.domain.protection.ProtectionReason
import com.instantpause.app.domain.protection.ProtectionWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Collections

/**
 * Régressions de la 1.6.1 sur `DndController` :
 * - plusieurs `sync()` simultanés (écran, `BootReceiver`, `ProtectionAlarmReceiver`)
 *   ne doivent jamais mémoriser `PRIORITY` comme « filtre précédent » ;
 * - un téléphone déjà en `PRIORITY` n'est pas modifié ni « restauré » ;
 * - un état défectueux enregistré par une version antérieure ne bloque plus le DND.
 */
class DndControllerConcurrencyTest {

    private val zone: ZoneId = ZoneId.of("Europe/Paris")
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 3, 10, 23, 30, 0, 0, zone)
    private val clock: Clock = Clock.fixed(now.toInstant(), zone)

    /** Les `delay` forcent l'entrelacement des coroutines entre lecture et écriture. */
    private class SlowPrefs(@Volatile var state: DndState = DndState()) : DndPreferences {
        override suspend fun readDnd(): DndState {
            delay(5)
            return state
        }

        override suspend fun writeDnd(state: DndState) {
            delay(5)
            this.state = state
        }
    }

    private class RecordingGateway(@Volatile var filter: Int) : DndGateway {
        val applied: MutableList<Int> = Collections.synchronizedList(mutableListOf())
        override fun isAccessGranted(): Boolean = true
        override fun currentFilter(): Int = filter
        override fun setFilter(filter: Int): Boolean {
            this.filter = filter
            applied += filter
            return true
        }
    }

    private class FakeProtection(@Volatile var reason: ProtectionReason) : ProtectionWindow {
        override suspend fun activeReason(now: ZonedDateTime): ProtectionReason = reason
    }

    private fun controller(
        prefs: DndPreferences,
        gateway: DndGateway,
        protection: ProtectionWindow
    ) = DndController(protection, prefs, gateway, clock)

    @Test
    fun `des sync simultanes posent PRIORITY une seule fois et gardent le filtre d origine`() =
        runBlocking(Dispatchers.Default) {
            val prefs = SlowPrefs(DndState(enabled = true))
            val gateway = RecordingGateway(filter = DndController.FILTER_ALL)
            val protection = FakeProtection(ProtectionReason.SCHEDULE_WINDOW)
            val dnd = controller(prefs, gateway, protection)

            (1..8).map { async { dnd.sync(now) } }.awaitAll()

            assertEquals(listOf(DndController.FILTER_PRIORITY), gateway.applied.toList())
            assertTrue(prefs.state.active)
            assertEquals(DndController.FILTER_ALL, prefs.state.previousFilter)

            // Fin de plage : le téléphone doit revenir au mode normal, pas rester bloqué.
            protection.reason = ProtectionReason.NONE
            (1..8).map { async { dnd.sync(now) } }.awaitAll()

            assertEquals(DndController.FILTER_ALL, gateway.filter)
            assertFalse(prefs.state.active)
        }

    @Test
    fun `un telephone deja en PRIORITY n est ni modifie ni restaure`() = runBlocking {
        val prefs = SlowPrefs(DndState(enabled = true))
        val gateway = RecordingGateway(filter = DndController.FILTER_PRIORITY)
        val protection = FakeProtection(ProtectionReason.SCHEDULE_WINDOW)
        val dnd = controller(prefs, gateway, protection)

        dnd.sync(now)

        assertTrue(gateway.applied.isEmpty())
        assertFalse(prefs.state.active)

        protection.reason = ProtectionReason.NONE
        dnd.sync(now)

        // Le réglage de l'utilisateur est intact après la plage.
        assertTrue(gateway.applied.isEmpty())
        assertEquals(DndController.FILTER_PRIORITY, gateway.filter)
    }

    @Test
    fun `un etat herite avec PRIORITY comme filtre precedent revient au mode normal`() = runBlocking {
        // Laissé par la 1.6.0 après la course : actif, « précédent » = PRIORITY.
        val prefs = SlowPrefs(
            DndState(enabled = true, active = true, previousFilter = DndController.FILTER_PRIORITY)
        )
        val gateway = RecordingGateway(filter = DndController.FILTER_PRIORITY)
        val dnd = controller(prefs, gateway, FakeProtection(ProtectionReason.NONE))

        dnd.sync(now)

        assertEquals(DndController.FILTER_ALL, gateway.filter)
        assertFalse(prefs.state.active)
    }

    @Test
    fun `setEnabled et sync simultanes ne se bloquent pas et restent coherents`() =
        runBlocking(Dispatchers.Default) {
            val prefs = SlowPrefs(DndState(enabled = false))
            val gateway = RecordingGateway(filter = DndController.FILTER_ALL)
            val dnd = controller(prefs, gateway, FakeProtection(ProtectionReason.EMERGENCY_MODE))

            listOf(
                async { dnd.setEnabled(true) },
                async { dnd.sync(now) },
                async { dnd.sync(now) }
            ).awaitAll()

            assertEquals(DndController.FILTER_PRIORITY, gateway.filter)
            assertEquals(DndController.FILTER_ALL, prefs.state.previousFilter)

            dnd.setEnabled(false)

            assertEquals(DndController.FILTER_ALL, gateway.filter)
            assertFalse(prefs.state.active)
        }
}
