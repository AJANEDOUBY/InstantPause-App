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
 *
 * Les durées (temporisation après échecs, délai d'arrière-plan) sont mesurées
 * avec [MonotonicClock] : avancer ou reculer l'heure du téléphone ne les raccourcit
 * pas. L'horloge murale ([Clock]) ne sert qu'à l'affichage (`lockedUntil`).
 */
@Singleton
class AppLockManager @Inject constructor(
    private val store: PassphraseStore,
    private val hasher: PassphraseHasher,
    private val clock: Clock,
    private val monotonic: MonotonicClock
) {

    /** Pour les tests : le temps monotone suit alors l'horloge fournie. */
    constructor(store: PassphraseStore, hasher: PassphraseHasher, clock: Clock) :
        this(store, hasher, clock, MonotonicClock { clock.millis() })

    enum class Status { LOADING, DISABLED, LOCKED, UNLOCKED }

    data class State(
        val status: Status = Status.LOADING,
        val failedAttempts: Int = 0,
        /** Échéance d'affichage (epoch ms). La vérité est mesurée en temps monotone. */
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

        /** Trop d'échecs : il faut attendre [until] (horodatage epoch ms, pour l'affichage). */
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

    /** Instant monotone de la dernière temporisation, et sa durée (0 = aucune). */
    @Volatile
    private var lockedAtElapsed: Long = 0L

    @Volatile
    private var lockDelayMs: Long = 0L

    private var backgroundedAtElapsed: Long = NOT_BACKGROUNDED

    /** Charge l'état une seule fois par processus. */
    suspend fun ensureLoaded() {
        if (loaded) return
        val stored = runCatching { store.read() }.getOrNull()
        val lockout = runCatching { store.readLockout() }.getOrDefault(LockoutState())
        lockedAtElapsed = lockout.lockedAtElapsed
        lockDelayMs = lockout.lockDelayMs
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
        backgroundedAtElapsed = monotonic.elapsedMillis()
    }

    fun onAppForegrounded() {
        val left = backgroundedAtElapsed
        if (left == NOT_BACKGROUNDED) return
        backgroundedAtElapsed = NOT_BACKGROUNDED
        // Un temps négatif (redémarrage entre-temps) est traité comme « long » : on verrouille.
        val away = monotonic.elapsedMillis() - left
        if (away < 0L || away >= GRACE_MS) lock()
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
                resetLockoutMemory()
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

    /** Millisecondes restantes de temporisation (0 si aucune), en temps monotone. */
    fun remainingLockoutMs(): Long = remainingMs()

    private fun resetLockoutMemory() {
        lockedAtElapsed = 0L
        lockDelayMs = 0L
    }

    /**
     * Reste de la temporisation.
     *
     * - Cas normal : `délai - temps monotone écoulé` depuis l'échec. Changer l'heure
     *   du téléphone n'y change rien.
     * - Redémarrage détecté (le temps monotone est repassé sous l'instant de
     *   l'échec) : on ne peut plus savoir ce qui s'est écoulé. Par prudence on
     *   compte depuis le démarrage, ce qui ne dépasse jamais le délai d'origine
     *   mais ne permet pas de le contourner en redémarrant.
     * - Ancien format (sans délai enregistré) : repli sur l'horloge murale, plafonné
     *   à [MAX_LOCKOUT_MS] pour qu'une date aberrante ne bloque pas indéfiniment.
     */
    private fun remainingMs(): Long {
        val delay = lockDelayMs
        if (delay > 0L) {
            val nowElapsed = monotonic.elapsedMillis()
            val since = if (nowElapsed >= lockedAtElapsed) nowElapsed - lockedAtElapsed else nowElapsed
            return (delay - since).coerceIn(0L, MAX_LOCKOUT_MS)
        }
        val legacyUntil = _state.value.lockedUntil
        if (legacyUntil <= 0L) return 0L
        return (legacyUntil - clock.millis()).coerceIn(0L, MAX_LOCKOUT_MS)
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
        resetLockoutMemory()
        _state.value = State(status = if (unlocked) Status.UNLOCKED else Status.LOCKED)
        return true
    }

    /** Vérification pure, sans toucher au statut déverrouillé. */
    private suspend fun verify(code: String): Result {
        val wallNow = clock.millis()

        val lockout = _state.value
        val remaining = remainingMs()
        if (remaining > 0L) {
            // Échéance recalculée pour l'affichage : elle reste cohérente même si
            // l'heure du téléphone a été modifiée.
            val until = wallNow + remaining
            _state.update { it.copy(lockedUntil = until) }
            return Result.RateLimited(until)
        }

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
            resetLockoutMemory()
            _state.update { it.copy(failedAttempts = 0, lockedUntil = 0L) }
            return Result.Success
        }

        val failed = lockout.failedAttempts + 1
        val delay = LockoutPolicy.delayMs(failed)
        val atElapsed = if (delay > 0L) monotonic.elapsedMillis() else 0L
        val until = if (delay > 0L) wallNow + delay else 0L
        lockedAtElapsed = atElapsed
        lockDelayMs = delay
        runCatching {
            store.writeLockout(
                LockoutState(
                    failedAttempts = failed,
                    lockedUntil = until,
                    lockedAtElapsed = atElapsed,
                    lockDelayMs = delay
                )
            )
        }
        _state.update { it.copy(failedAttempts = failed, lockedUntil = until) }

        return if (delay > 0L) {
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

        /** Plafond de [LockoutPolicy] (15 min), utilisé pour borner les repli. */
        const val MAX_LOCKOUT_MS = 15 * 60_000L

        const val NOT_BACKGROUNDED = Long.MIN_VALUE
    }
}
