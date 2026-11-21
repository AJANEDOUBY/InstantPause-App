package com.instantpause.app.domain.security

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Le verrou est fait pour résister à soi-même : changer l'heure du téléphone
 * ne doit ni raccourcir une temporisation, ni la contourner par un redémarrage.
 */
class AppLockManagerClockTest {

    /** Horloge murale : l'utilisateur peut la déplacer dans les réglages. */
    private class WallClock(var now: Instant = Instant.parse("2026-01-01T00:00:00Z")) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
        fun shiftMs(ms: Long) {
            now = now.plusMillis(ms)
        }
    }

    /** Temps monotone : ne bouge pas quand l'heure est modifiée, repart à zéro au redémarrage. */
    private class Mono(var elapsed: Long = 1_000_000L) : MonotonicClock {
        override fun elapsedMillis(): Long = elapsed
    }

    private class MemoryStore : PassphraseStore {
        var stored: StoredPassphrase? = null
        var lockout: LockoutState = LockoutState()
        override suspend fun read(): StoredPassphrase? = stored
        override suspend fun write(stored: StoredPassphrase) {
            this.stored = stored
        }

        override suspend fun clear() {
            stored = null
        }

        override suspend fun readLockout(): LockoutState = lockout
        override suspend fun writeLockout(state: LockoutState) {
            lockout = state
        }
    }

    private val store = MemoryStore()
    private val wall = WallClock()
    private val mono = Mono()
    private val hasher = PassphraseHasher(iterations = 1_000)
    private val code = "Cooldown2026"
    private val wrong = "Mauvais2026"

    private fun newManager() = AppLockManager(store, hasher, wall, mono)

    /** Crée le code puis enchaîne trois échecs : 30 s de temporisation. */
    private suspend fun lockedOutManager(): AppLockManager {
        val manager = newManager()
        manager.setPassphrase(code)
        repeat(3) { manager.unlock(wrong) }
        assertTrue(manager.unlock(code) is AppLockManager.Result.RateLimited)
        return manager
    }

    @Test
    fun `avancer l heure du telephone ne raccourcit pas la temporisation`() = runBlocking {
        val manager = lockedOutManager()

        wall.shiftMs(60 * 60_000L) // +1 h sur l'horloge murale, rien sur le temps monotone

        assertTrue(manager.unlock(code) is AppLockManager.Result.RateLimited)
        assertTrue(manager.remainingLockoutMs() > 0L)
    }

    @Test
    fun `reculer l heure du telephone ne prolonge pas la temporisation`() = runBlocking {
        val manager = lockedOutManager()

        wall.shiftMs(-24 * 60 * 60_000L) // -1 jour
        mono.elapsed += 31_000L // 31 s réellement écoulées

        assertEquals(AppLockManager.Result.Success, manager.unlock(code))
    }

    @Test
    fun `tuer l app puis avancer l heure ne contourne pas la temporisation`() = runBlocking {
        lockedOutManager()

        wall.shiftMs(60 * 60_000L)
        val restarted = newManager() // nouveau processus, même stockage

        assertTrue(restarted.unlock(code) is AppLockManager.Result.RateLimited)
    }

    @Test
    fun `apres un redemarrage la temporisation n est pas remise a zero`() = runBlocking {
        lockedOutManager()

        // Redémarrage du téléphone : le temps monotone repart de quelques secondes.
        mono.elapsed = 5_000L
        val restarted = newManager()

        assertTrue(restarted.unlock(code) is AppLockManager.Result.RateLimited)
        assertTrue(restarted.remainingLockoutMs() in 1L..30_000L)

        mono.elapsed = 31_000L
        assertEquals(AppLockManager.Result.Success, restarted.unlock(code))
    }

    @Test
    fun `un ancien etat sans delai enregistre est plafonne a quinze minutes`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        // Format de la 1.6.0 : échéance murale seule, ici absurde (un an).
        store.lockout = LockoutState(
            failedAttempts = 3,
            lockedUntil = wall.millis() + 365L * 24 * 60 * 60_000L
        )

        val restarted = newManager()
        restarted.ensureLoaded()

        assertEquals(15 * 60_000L, restarted.remainingLockoutMs())
    }

    @Test
    fun `changer l heure pendant l arriere-plan ne contourne pas le reverrouillage`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.unlock(code)

        manager.onAppBackgrounded()
        wall.shiftMs(-10 * 60_000L) // l'heure recule : l'ancienne version voyait un temps négatif
        mono.elapsed += 120_000L // 2 min réellement passées
        manager.onAppForegrounded()

        assertTrue(manager.state.value.isLocked)
    }

    @Test
    fun `un passage rapide en arriere-plan ne reverrouille toujours pas`() = runBlocking {
        val manager = newManager()
        manager.setPassphrase(code)
        manager.unlock(code)

        manager.onAppBackgrounded()
        mono.elapsed += 5_000L
        manager.onAppForegrounded()

        assertTrue(!manager.state.value.isLocked)
    }
}
