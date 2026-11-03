package com.instantpause.app.domain.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verrou de configuration.
 *
 * Le but n'est pas de protéger un secret : c'est de résister à **soi-même**.
 * Quand la tentation de couper la protection arrive (colère, alcool), il faut
 * pouvoir retaper un code alphanumérique mémorisé à jeun. Tout le reste est
 * secondaire.
 *
 * Deux principes :
 *  - le verrou ne garde que les actions qui **affaiblissent** la protection ;
 *    bloquer un appel, ajouter une plage ou activer le Zen reste toujours libre ;
 *  - l'état « déverrouillé » n'existe qu'en mémoire : il meurt avec le processus,
 *    et retombe après un passage en arrière-plan prolongé.
 */
@Singleton
class AppLockManager @Inject constructor(
    private val store: PassphraseStore,
    private val hasher: PassphraseHasher,
    private val clock: Clock
) {

    enum class Status { LOADING, DISABLED, LOCKED, UNLOCKED }

    data class State(
        val status: Status = Status.LOADING,
        val failedAttempts: Int = 0,
        val lockedUntil: Long = 0L
    ) {
        val isConfigured: Boolean
            get() = status == Status.LOCKED || status == Status.UNLOCKED
        val isLocked: Boolean
            get() = status == Status.LOCKED
    }

    sealed interface Result {
        /** Le code est bon (ou l'opération est faite). */
        data object Success : Result

        /** Aucun code n'a été défini : la protection n'est pas verrouillable. */
        data object NotConfigured : Result

        /** Un code existe déjà : il faut passer par [changePassphrase]. */
        data object AlreadyConfigured : Result

        /** Code faux. [attemptsLeft] avant la prochaine temporisation. */
        data class Wrong(val attemptsLeft: Int, val nextDelayMs: Long) : Result

        /** Trop d'échecs : il faut attendre [until] (horodatage epoch ms). */
        data class RateLimited(val until: Long) : Result

        /** Le nouveau code ne respecte pas la politique de composition. */
        data class Rejected(val violations: List<PassphrasePolicy.Violation>) : Result

        /**
         * Le stockage a refusé l'écriture (disque plein, DataStore corrompu…).
         * On ne prétend jamais qu'un code est enregistré s'il ne l'est pas.
         */
        data object StorageError : Result
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    private var loaded = false

    private var backgroundedAt: Long = 0L

    /** Charge l'état une seule fois par processus. */
    suspend fun ensureLoaded() {
        if (loaded) return
        val stored = runCatching { store.read() }.getOrNull()
        val lockout = runCatching { store.readLockout() }.getOrDefault(LockoutState())
        _state.value = State(
            status = if (stored == null) Status.DISABLED else Status.LOCKED,
            failedAttempts = lockout.failedAttempts,
            lockedUntil = lockout.lockedUntil
        )
        loaded = true
    }

    /** Verrouille immédiatement (sans effet si aucun code n'est défini). */
    fun lock() {
        _state.update { if (it.isConfigured) it.copy(status = Status.LOCKED) else it }
    }

    /**
     * À appeler quand la dernière activité de l'app passe en arrière-plan.
     * Un passage très court (ouvrir les Paramètres, revenir) ne reverrouille pas.
     */
    fun onAppBackgrounded() {
        backgroundedAt = clock.millis()
    }

    fun onAppForegrounded() {
        val left = backgroundedAt
        if (left == 0L) return
        backgroundedAt = 0L
        if (clock.millis() - left >= GRACE_MS) lock()
    }

    /**
     * Crée le code. Refusé si un code existe déjà : dans ce cas il faut passer
     * par [changePassphrase], qui exige l'ancien.
     *
     * Le code est créé **verrouillé** : la première action qui affaiblit la
     * protection redemandera immédiatement le code. Sans ça, on pourrait tout
     * couper dans les secondes qui suivent la création, sans rien retaper.
     */
    suspend fun setPassphrase(new: String): Result {
        ensureLoaded()
        if (_state.value.isConfigured) return Result.AlreadyConfigured

        val violations = PassphrasePolicy.violations(new)
        if (violations.isNotEmpty()) return Result.Rejected(violations)

        return if (writePassphrase(new, unlocked = false)) Result.Success else Result.StorageError
    }

    /** Change le code : l'ancien est obligatoire, avec les mêmes temporisations. */
    suspend fun changePassphrase(current: String, new: String): Result {
        ensureLoaded()
        if (!_state.value.isConfigured) return Result.NotConfigured

        val violations = PassphrasePolicy.violations(new)
        if (violations.isNotEmpty()) return Result.Rejected(violations)

        return when (val check = verify(current)) {
            is Result.Success ->
                // L'utilisateur vient de prouver qu'il connaît le code : on ne le
                // punit pas en le reverrouillant dans la seconde.
                if (writePassphrase(new, unlocked = true)) Result.Success else Result.StorageError
            else -> check
        }
    }

    /**
     * Supprime le code — **volontairement absent de l'interface**.
     *
     * Le garder ici sert à un éventuel flux de réinitialisation explicite ; s'il
     * était exposé dans l'onglet Sécurité, toute la protection redeviendrait
     * désactivable en un dialogue, ce qui annulerait son intérêt.
     */
    suspend fun removePassphrase(current: String): Result {
        ensureLoaded()
        if (!_state.value.isConfigured) return Result.NotConfigured

        return when (val check = verify(current)) {
            is Result.Success -> {
                val cleared = runCatching { store.clear() }.isSuccess
                // Relecture : ne jamais afficher « aucun code » s'il est toujours là.
                val stillStored = runCatching { store.read() }.getOrNull() != null
                if (!cleared || stillStored) return Result.StorageError

                runCatching { store.writeLockout(LockoutState()) }
                _state.value = State(status = Status.DISABLED)
                Result.Success
            }
            else -> check
        }
    }

    /**
     * Vérifie le code et marque l'app comme déverrouillée pour la session.
     *
     * Persiste l'échec AVANT de rendre la main : tuer l'app ne remet pas le
     * compteur à zéro.
     */
    suspend fun unlock(code: String): Result {
        ensureLoaded()
        if (!_state.value.isConfigured) return Result.NotConfigured

        return when (val check = verify(code)) {
            is Result.Success -> {
                _state.update { it.copy(status = Status.UNLOCKED) }
                Result.Success
            }
            else -> check
        }
    }

    /** Millisecondes restantes de temporisation (0 si aucune). */
    fun remainingLockoutMs(): Long {
        val until = _state.value.lockedUntil
        return (until - clock.millis()).coerceAtLeast(0L)
    }

    private suspend fun writePassphrase(new: String, unlocked: Boolean): Boolean {
        val normalized = PassphrasePolicy.normalize(new)
        val salt = hasher.newSalt()
        val hash = hasher.derive(normalized, salt, hasher.iterations)
        val written = runCatching {
            store.write(
                StoredPassphrase(
                    salt = hasher.encode(salt),
                    hash = hasher.encode(hash),
                    iterations = hasher.iterations
                )
            )
        }.isSuccess
        if (!written) return false

        runCatching { store.writeLockout(LockoutState()) }
        _state.value = State(status = if (unlocked) Status.UNLOCKED else Status.LOCKED)
        return true
    }

    /** Vérification pure, sans toucher au statut déverrouillé. */
    private suspend fun verify(code: String): Result {
        val now = clock.millis()

        val lockout = _state.value
        if (lockout.lockedUntil > now) return Result.RateLimited(lockout.lockedUntil)

        val stored = runCatching { store.read() }.getOrNull()
            ?: return Result.NotConfigured

        if (PassphrasePolicy.normalize(code).isEmpty()) {
            return Result.Wrong(
                attemptsLeft = LockoutPolicy.attemptsLeft(lockout.failedAttempts),
                nextDelayMs = 0L
            )
        }

        val matches = withContext(Dispatchers.Default) {
            runCatching { hasher.matches(code, stored) }.getOrDefault(false)
        }

        if (matches) {
            runCatching { store.writeLockout(LockoutState()) }
            _state.update { it.copy(failedAttempts = 0, lockedUntil = 0L) }
            return Result.Success
        }

        val failed = lockout.failedAttempts + 1
        val delay = LockoutPolicy.delayMs(failed)
        val until = if (delay > 0L) now + delay else 0L
        runCatching { store.writeLockout(LockoutState(failed, until)) }
        _state.update { it.copy(failedAttempts = failed, lockedUntil = until) }

        return if (until > now) {
            Result.RateLimited(until)
        } else {
            Result.Wrong(
                attemptsLeft = LockoutPolicy.attemptsLeft(failed),
                nextDelayMs = LockoutPolicy.delayMs(failed + 1)
            )
        }
    }

    private companion object {
        /** Au-delà de 60 s hors de l'app, il faut retaper le code. */
        const val GRACE_MS = 60_000L
    }
}
