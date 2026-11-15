package com.instantpause.app.domain.dnd

import com.instantpause.app.domain.protection.ProtectionReason
import com.instantpause.app.domain.protection.ProtectionWindow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Le filtre « Ne pas déranger » doit suivre la protection sans jamais écraser
 * le réglage d'origine de l'utilisateur. Ces tests couvrent les six règles de
 * `docs/DND-1.6.0.md` qui ne demandent pas d'appareil réel.
 */
class DndControllerTest {

    private val zone: ZoneId = ZoneId.of("Europe/Paris")
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 3, 10, 23, 30, 0, 0, zone)
    private val clock: Clock = Clock.fixed(now.toInstant(), zone)

    private class FakePrefs(var state: DndState = DndState()) : DndPreferences {
        override suspend fun readDnd(): DndState = state
        override suspend fun writeDnd(state: DndState) {
            this.state = state
        }
    }

    private class FakeGateway(
        var granted: Boolean = true,
        var filter: Int = RING,
        var acceptChanges: Boolean = true
    ) : DndGateway {
        val applied = mutableListOf<Int>()
        override fun isAccessGranted(): Boolean = granted
        override fun currentFilter(): Int = filter
        override fun setFilter(filter: Int): Boolean {
            if (!granted || !acceptChanges) return false
            this.filter = filter
            applied += filter
            return true
        }

        companion object {
            /** `NotificationManager.INTERRUPTION_FILTER_NONE` (sonnerie normale). */
            const val RING = 1
        }
    }

    private class FakeProtection(var reason: ProtectionReason) : ProtectionWindow {
        override suspend fun activeReason(now: ZonedDateTime): ProtectionReason = reason
    }

    private fun controller(
        prefs: FakePrefs,
        gateway: FakeGateway,
        protection: FakeProtection
    ) = DndController(protection, prefs, gateway, clock)

    @Test
    fun `activation memorise le filtre precedent et pose PRIORITY`() = runBlocking {
        val prefs = FakePrefs(DndState(enabled = true))
        val gateway = FakeGateway(filter = FakeGateway.RING)
        val dnd = controller(prefs, gateway, FakeProtection(ProtectionReason.SCHEDULE_WINDOW))

        dnd.sync(now)

        assertEquals(DndController.FILTER_PRIORITY, gateway.filter)
        assertTrue(prefs.state.active)
        assertEquals(FakeGateway.RING, prefs.state.previousFilter)
    }

    @Test
    fun `une seconde synchronisation n ecrase pas le filtre precedent`() = runBlocking {
        val prefs = FakePrefs(DndState(enabled = true))
        val gateway = FakeGateway(filter = FakeGateway.RING)
        val dnd = controller(prefs, gateway, FakeProtection(ProtectionReason.EMERGENCY_MODE))

        dnd.sync(now)
        dnd.sync(now)

        assertEquals(FakeGateway.RING, prefs.state.previousFilter)
        assertEquals(listOf(DndController.FILTER_PRIORITY), gateway.applied)
    }

    @Test
    fun `fin de plage restaure exactement le filtre precedent`() = runBlocking {
        val prefs = FakePrefs(DndState(enabled = true))
        val gateway = FakeGateway(filter = FakeGateway.RING)
        val protection = FakeProtection(ProtectionReason.SCHEDULE_WINDOW)
        val dnd = controller(prefs, gateway, protection)

        dnd.sync(now)
        protection.reason = ProtectionReason.NONE
        dnd.sync(now)

        assertEquals(FakeGateway.RING, gateway.filter)
        assertFalse(prefs.state.active)
    }

    @Test
    fun `sans acces systeme aucun filtre n est pose`() = runBlocking {
        val prefs = FakePrefs(DndState(enabled = true))
        val gateway = FakeGateway(granted = false)
        val dnd = controller(prefs, gateway, FakeProtection(ProtectionReason.SCHEDULE_WINDOW))

        dnd.sync(now)

        assertTrue(gateway.applied.isEmpty())
        assertFalse(prefs.state.active)
    }

    @Test
    fun `au demarrage une plage finie rend le son immediatement`() = runBlocking {
        // État laissé par un arrêt brutal : actif, plage désormais terminée.
        val prefs = FakePrefs(DndState(enabled = true, active = true, previousFilter = FakeGateway.RING))
        val gateway = FakeGateway(filter = DndController.FILTER_PRIORITY)
        val dnd = controller(prefs, gateway, FakeProtection(ProtectionReason.NONE))

        dnd.sync(now)

        assertEquals(FakeGateway.RING, gateway.filter)
        assertFalse(prefs.state.active)
    }

    @Test
    fun `desactivation manuelle depuis le volet n est pas reappliquee`() = runBlocking {
        val prefs = FakePrefs(DndState(enabled = true, active = true, previousFilter = FakeGateway.RING))
        // L'utilisateur a remis la sonnerie : le filtre n'est plus PRIORITY.
        val gateway = FakeGateway(filter = FakeGateway.RING)
        val dnd = controller(prefs, gateway, FakeProtection(ProtectionReason.SCHEDULE_WINDOW))

        dnd.sync(now)

        assertFalse(prefs.state.active)
        assertTrue(prefs.state.overridden)
        assertTrue(gateway.applied.isEmpty())
        assertEquals(FakeGateway.RING, gateway.filter)
    }

    @Test
    fun `un second passage ne repose pas le silence retire par l utilisateur`() = runBlocking {
        // Cas observé sur tablette : le retour dans l'écran Sécurité relançait
        // sync(), qui réappliquait PRIORITY juste après une coupure manuelle.
        val prefs = FakePrefs(DndState(enabled = true, active = true, previousFilter = FakeGateway.RING))
        val gateway = FakeGateway(filter = FakeGateway.RING)
        val dnd = controller(prefs, gateway, FakeProtection(ProtectionReason.SCHEDULE_WINDOW))

        dnd.sync(now)
        dnd.sync(now)
        dnd.sync(now)

        assertTrue(gateway.applied.isEmpty())
        assertEquals(FakeGateway.RING, gateway.filter)
        assertTrue(prefs.state.overridden)
    }

    @Test
    fun `la plage suivante repart sans le refus precedent`() = runBlocking {
        val prefs = FakePrefs(
            DndState(enabled = true, active = false, previousFilter = FakeGateway.RING, overridden = true)
        )
        val gateway = FakeGateway(filter = FakeGateway.RING)
        val protection = FakeProtection(ProtectionReason.NONE)
        val dnd = controller(prefs, gateway, protection)

        // Fin de plage : le refus est oublié.
        dnd.sync(now)
        assertFalse(prefs.state.overridden)

        // Nouvelle plage : le silence revient, comme prévu par l'option.
        protection.reason = ProtectionReason.SCHEDULE_WINDOW
        dnd.sync(now)
        assertTrue(prefs.state.active)
        assertEquals(DndController.FILTER_PRIORITY, gateway.filter)
    }

    @Test
    fun `un geste sur l interrupteur efface le refus memorise`() = runBlocking {
        val prefs = FakePrefs(
            DndState(enabled = true, active = false, previousFilter = FakeGateway.RING, overridden = true)
        )
        val gateway = FakeGateway(filter = FakeGateway.RING)
        val dnd = controller(prefs, gateway, FakeProtection(ProtectionReason.SCHEDULE_WINDOW))

        // L'utilisateur réactive explicitement : sa décision remplace l'ancienne.
        dnd.setEnabled(true)

        assertFalse(prefs.state.overridden)
        assertEquals(DndController.FILTER_PRIORITY, gateway.filter)
    }

    @Test
    fun `couper l option restaure le filtre`() = runBlocking {
        val prefs = FakePrefs(DndState(enabled = true, active = true, previousFilter = FakeGateway.RING))
        val gateway = FakeGateway(filter = DndController.FILTER_PRIORITY)
        val dnd = controller(prefs, gateway, FakeProtection(ProtectionReason.SCHEDULE_WINDOW))

        dnd.setEnabled(false)

        assertEquals(FakeGateway.RING, gateway.filter)
        assertFalse(prefs.state.active)
        assertFalse(prefs.state.enabled)
    }

    @Test
    fun `acces retire en cours de route ne pretend pas avoir restaure`() = runBlocking {
        val prefs = FakePrefs(DndState(enabled = true, active = true, previousFilter = FakeGateway.RING))
        val gateway = FakeGateway(granted = false, filter = DndController.FILTER_PRIORITY)
        val dnd = controller(prefs, gateway, FakeProtection(ProtectionReason.NONE))

        dnd.sync(now)

        assertTrue(prefs.state.active)
        assertTrue(gateway.applied.isEmpty())
    }

    @Test
    fun `l horloge injectee sert quand l appelant ne precise pas l instant`() = runBlocking {
        val prefs = FakePrefs(DndState(enabled = true))
        val gateway = FakeGateway(filter = FakeGateway.RING)
        val protection = FakeProtection(ProtectionReason.SCHEDULE_WINDOW)
        val dnd = controller(prefs, gateway, protection)

        dnd.sync() // instant fourni par Clock.fixed

        assertTrue(prefs.state.active)
        assertEquals(DndController.FILTER_PRIORITY, gateway.filter)
    }
}
