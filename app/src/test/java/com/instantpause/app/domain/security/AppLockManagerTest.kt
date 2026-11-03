package com.instantpause.app.domain.security

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Horloge pilotable : indispensable pour tester les temporisations sans attendre. */
private class MutableClock(var now: Instant = Instant.parse("2026-01-01T00:00:00Z")) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = now

    fun advance(ms: Long) {
        now = now.plusMillis(ms)
    }
}

private class FakeStore : PassphraseStore {
    var stored: StoredPassphrase? = null
    var lockout: LockoutState = LockoutState()
    var failWrite = false
    var failClear = false

    override suspend fun read(): StoredPassphrase? = stored
    override suspend fun write(stored: StoredPassphrase) {
        if (failWrite) error("stockage indisponible")
        this.stored = stored
    }

    override suspend fun clear() {
        if (failClear) error("stockage indisponible")
        stored = null
    }

    override suspend fun readLockout(): LockoutState = lockout
    override suspend fun writeLockout(state: LockoutState) {
        lockout = state
    }
}

class AppLockManagerTest {

    private val store = FakeStore()
    private val clock = MutableClock()
    private val hasher = PassphraseHasher(iterations = 1_000)

    private fun newManager() = AppLockManager(store, hasher, clock)

    private val code = "Cooldown2026"

    // --- Cycle de vie ---

    @Test
    fun `sans code le verrou est desactive`() = runBlocking {
        val manager = newManager()
        manager.ensureLoaded()
        assertEquals(AppLockManager.Status.DISABLED, manager.state.value.status)
        assertFalse(manager.state.value.isConfigured)
    }

    @Test
    fun `definir un code laisse la session verrouillee`() = runBlocking {
        val manager = newManager()
        assertEquals(AppLockManager.Result.Success, manager.setPassphrase(code))
        // Créer le code ne doit pas offrir une fenêtre de gratuité : la première
        // action qui affaiblit la protection redemande immédiatement le code.
        assertEquals(AppLockManager.Status.LOCKED, manager.state.value.status)
        assertTrue(manager.state.value.isLocked)
        assertNotNull(store.stored)
        // Le sel et l'empreinte sont bien deux valeurs distinctes.
        assertFalse(store.stored!!.salt == store.stored!!.hash)
    }

    @Test
    fun `le code cree permet ensuite de deverrouiller`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        assertEquals(AppLockManager.Result.Success, manager.unlock(code))
        assertEquals(AppLockManager.Status.UNLOCKED, manager.state.value.status)
    }

    @Test
    fun `un code trop faible est refuse et rien n est ecrit`() = runBlocking {
        val manager = newManager()
        val result = manager.setPassphrase("abc")
        assertTrue(result is AppLockManager.Result.Rejected)
        assertNull(store.stored)
    }

    @Test
    fun `impossible d ecraser un code existant sans l ancien`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        assertEquals(
            AppLockManager.Result.AlreadyConfigured,
            manager.setPassphrase("Autre2026")
        )
    }

    @Test
    fun `apres redemarrage le verrou est ferme`() = runBlocking {
        newManager().setPassphrase(code)
        val restarted = newManager()
        restarted.ensureLoaded()
        assertEquals(AppLockManager.Status.LOCKED, restarted.state.value.status)
        assertTrue(restarted.state.value.isLocked)
    }

    // --- Déverrouillage ---

    @Test
    fun `le bon code deverrouille`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.lock()
        assertEquals(AppLockManager.Result.Success, manager.unlock(code))
        assertEquals(AppLockManager.Status.UNLOCKED, manager.state.value.status)
    }

    @Test
    fun `un code faux ne deverrouille pas`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.lock()
        val result = manager.unlock("Mauvais2026")
        assertTrue(result is AppLockManager.Result.Wrong)
        assertTrue(manager.state.value.isLocked)
    }

    @Test
    fun `trois echecs declenchent une temporisation`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.lock()

        manager.unlock("Mauvais2026")
        manager.unlock("Mauvais2026")
        // Le troisième échec déclenche directement la temporisation de 30 s.
        val third = manager.unlock("Mauvais2026")
        assertTrue(third is AppLockManager.Result.RateLimited)
        assertEquals(clock.millis() + 30_000L, (third as AppLockManager.Result.RateLimited).until)

        // Même le bon code est refusé pendant l'attente.
        val blocked = manager.unlock(code)
        assertTrue(blocked is AppLockManager.Result.RateLimited)
        assertTrue(manager.state.value.isLocked)
    }

    @Test
    fun `le compteur d echecs survit a un redemarrage`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.lock()
        repeat(3) { manager.unlock("Mauvais2026") }

        val restarted = newManager()
        restarted.ensureLoaded()
        assertTrue(restarted.state.value.lockedUntil > clock.millis())

        val blocked = restarted.unlock(code)
        assertTrue(blocked is AppLockManager.Result.RateLimited)
    }

    @Test
    fun `la temporisation expire et le bon code repasse`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.lock()
        repeat(3) { manager.unlock("Mauvais2026") }

        clock.advance(LockoutPolicy.delayMs(3) + 1)
        assertEquals(0L, manager.remainingLockoutMs())
        assertEquals(AppLockManager.Result.Success, manager.unlock(code))
    }

    @Test
    fun `un succes remet le compteur d echecs a zero`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.lock()
        manager.unlock("Mauvais2026")
        manager.unlock("Mauvais2026")
        manager.unlock(code)

        assertEquals(0, store.lockout.failedAttempts)
        assertEquals(0L, store.lockout.lockedUntil)
    }

    // --- Changement / suppression ---

    @Test
    fun `changer le code exige l ancien`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.lock()

        assertTrue(manager.changePassphrase("Faux2026", "Nouveau2026") is AppLockManager.Result.Wrong)
        assertEquals(AppLockManager.Result.Success, manager.changePassphrase(code, "Nouveau2026"))

        manager.lock()
        assertEquals(AppLockManager.Result.Success, manager.unlock("Nouveau2026"))
    }

    @Test
    fun `supprimer le code exige le bon code et efface l empreinte`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.lock()

        assertTrue(manager.removePassphrase("Faux2026") is AppLockManager.Result.Wrong)
        assertNotNull(store.stored)

        assertEquals(AppLockManager.Result.Success, manager.removePassphrase(code))
        assertNull(store.stored)
        assertEquals(AppLockManager.Status.DISABLED, manager.state.value.status)
    }

    @Test
    fun `sans code configure les operations renvoient NotConfigured`() = runBlocking {
        val manager = newManager()
        manager.ensureLoaded()
        assertEquals(AppLockManager.Result.NotConfigured, manager.unlock(code))
        assertEquals(AppLockManager.Result.NotConfigured, manager.removePassphrase(code))
    }

    // --- Défaillances du stockage ---

    @Test
    fun `un echec d ecriture n annonce pas un code enregistre`() = runBlocking {
        store.failWrite = true
        val manager = newManager()
        assertEquals(AppLockManager.Result.StorageError, manager.setPassphrase(code))
        assertEquals(AppLockManager.Status.DISABLED, manager.state.value.status)
        assertNull(store.stored)
    }

    @Test
    fun `un echec de suppression laisse le verrou actif`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.lock()
        store.failClear = true

        assertEquals(AppLockManager.Result.StorageError, manager.removePassphrase(code))
        assertTrue(manager.state.value.isLocked)
        assertNotNull(store.stored)
    }

    // --- Reverrouillage automatique ---

    @Test
    fun `un passage rapide en arriere-plan ne reverrouille pas`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.unlock(code)
        manager.onAppBackgrounded()
        clock.advance(30_000)
        manager.onAppForegrounded()
        assertEquals(AppLockManager.Status.UNLOCKED, manager.state.value.status)
    }

    @Test
    fun `plus de soixante secondes en arriere-plan reverrouille`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.unlock(code)
        manager.onAppBackgrounded()
        clock.advance(61_000)
        manager.onAppForegrounded()
        assertEquals(AppLockManager.Status.LOCKED, manager.state.value.status)
    }

    @Test
    fun `verrouiller manuellement ne fait rien sans code`() = runBlocking {
        val manager = newManager()
        manager.ensureLoaded()
        manager.lock()
        assertEquals(AppLockManager.Status.DISABLED, manager.state.value.status)
    }
}
