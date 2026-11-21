package com.instantpause.app.domain.security

/**
 * Accès persistant au code et au compteur d'échecs.
 *
 * Interface volontairement étroite : elle permet de tester toute la logique de
 * verrouillage sans Android ni DataStore.
 */
interface PassphraseStore {
    suspend fun read(): StoredPassphrase?
    suspend fun write(stored: StoredPassphrase)
    suspend fun clear()
    suspend fun readLockout(): LockoutState
    suspend fun writeLockout(state: LockoutState)
}

data class LockoutState(
    val failedAttempts: Int = 0,
    /** Échéance en epoch ms : affichage et ancien format uniquement. */
    val lockedUntil: Long = 0L,
    /** Temps monotone (elapsedRealtime) au moment de la temporisation. */
    val lockedAtElapsed: Long = 0L,
    /** Durée de la temporisation en ms (0 = aucune, ou ancien format). */
    val lockDelayMs: Long = 0L
)

/**
 * Plus on se trompe, plus on attend.
 *
 * Trois essais libres (une faute de frappe arrive à tout le monde), puis 30 s,
 * 1 min, 2 min… jusqu'à 15 min. Le but n'est pas de bloquer un attaquant
 * patient mais de faire passer l'impulsivité.
 */
object LockoutPolicy {

    const val FREE_ATTEMPTS = 3
    private const val BASE_MS = 30_000L
    private const val MAX_MS = 15 * 60_000L

    fun delayMs(failedAttempts: Int): Long {
        if (failedAttempts < FREE_ATTEMPTS) return 0L
        val step = (failedAttempts - FREE_ATTEMPTS).coerceIn(0, 10)
        return (BASE_MS shl step).coerceAtMost(MAX_MS)
    }

    fun attemptsLeft(failedAttempts: Int): Int =
        (FREE_ATTEMPTS - failedAttempts).coerceAtLeast(0)
}
