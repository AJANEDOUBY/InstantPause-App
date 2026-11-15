package com.instantpause.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.instantpause.app.domain.dnd.DndPreferences
import com.instantpause.app.domain.dnd.DndState
import com.instantpause.app.domain.security.LockoutState
import com.instantpause.app.domain.pause.PauseMessagePolicy
import com.instantpause.app.domain.security.PassphraseStore
import com.instantpause.app.domain.security.StoredPassphrase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.coolDownDataStore: DataStore<Preferences> by
preferencesDataStore(name = "cooldown_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.coolDownDataStore
}

@Singleton
class ProtectionPrefs @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PassphraseStore, DndPreferences {
    private val data: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    // === Mode Urgence Émotionnelle ===
    val emergencyUntil: Flow<Long> = data.map { it[EMERGENCY_UNTIL] ?: 0L }.distinctUntilChanged()

    suspend fun getEmergencyUntil(): Long = data.first()[EMERGENCY_UNTIL] ?: 0L
    suspend fun setEmergencyUntil(v: Long) { dataStore.edit { it[EMERGENCY_UNTIL] = v } }
    suspend fun clearEmergency() { dataStore.edit { it.remove(EMERGENCY_UNTIL) } }

    // === Message personnalisé de l'écran de pause ===
    //
    // Écrit à froid depuis l'onglet Sécurité. Un champ absent ou vide est stocké
    // tel quel : l'affichage retombe sur la phrase par défaut de la langue
    // courante (`pause_message_default`), donc l'écran de pause n'est jamais blanc.
    val pauseMessage: Flow<String> = data
        .map { it[PAUSE_MESSAGE] ?: "" }
        .map { PauseMessagePolicy.normalize(it) }
        .distinctUntilChanged()

    suspend fun getPauseMessage(): String =
        PauseMessagePolicy.normalize(data.first()[PAUSE_MESSAGE] ?: "")

    suspend fun setPauseMessage(value: String) {
        dataStore.edit { it[PAUSE_MESSAGE] = PauseMessagePolicy.normalize(value) }
    }

    // === État du rôle Call Redirection ===
    val roleRevoked: Flow<Boolean> = data.map { it[ROLE_REVOKED] ?: false }.distinctUntilChanged()
    suspend fun isRoleRevoked(): Boolean = data.first()[ROLE_REVOKED] ?: false
    suspend fun setRoleRevoked(v: Boolean) { dataStore.edit { it[ROLE_REVOKED] = v } }

    // === Bandeau d'aide (affiché une seule fois au premier lancement) ===
    val helpBannerDismissed: Flow<Boolean> =
        data.map { it[HELP_BANNER_DISMISSED] ?: false }.distinctUntilChanged()

    suspend fun isHelpBannerDismissed(): Boolean =
        data.first()[HELP_BANNER_DISMISSED] ?: false

    suspend fun setHelpBannerDismissed(value: Boolean) {
        dataStore.edit { it[HELP_BANNER_DISMISSED] = value }
    }

    // === Protection anti-désinstallation franchie à l'installation ===
    //
    // L'administrateur d'appareil ne peut pas être activé sans confirmation système :
    // on mémorise donc que l'étape a bien été franchie (admin actif au moins une fois).
    // Sans ce drapeau, un utilisateur déjà installé (rôle Telecom obtenu avant la 1.3.3)
    // échapperait à l'étape obligatoire.
    val uninstallGuardDone: Flow<Boolean> =
        data.map { it[UNINSTALL_GUARD_DONE] ?: false }.distinctUntilChanged()

    suspend fun isUninstallGuardDone(): Boolean =
        data.first()[UNINSTALL_GUARD_DONE] ?: false

    suspend fun setUninstallGuardDone(value: Boolean) {
        dataStore.edit { it[UNINSTALL_GUARD_DONE] = value }
    }

    // === Écran de pause en plein écran (Android 14+) ===
    //
    // Sur Android 14+, USE_FULL_SCREEN_INTENT n'est plus accordée d'office. L'étape
    // d'installation propose de l'accorder une fois ; si l'utilisateur refuse, on ne
    // le relance plus à chaque ouverture (drapeau), la pause restant accessible par
    // la notification.
    val fullScreenIntentDone: Flow<Boolean> =
        data.map { it[FULL_SCREEN_INTENT_DONE] ?: false }.distinctUntilChanged()

    suspend fun isFullScreenIntentDone(): Boolean =
        data.first()[FULL_SCREEN_INTENT_DONE] ?: false

    suspend fun setFullScreenIntentDone(value: Boolean) {
        dataStore.edit { it[FULL_SCREEN_INTENT_DONE] = value }
    }

    // === Ne pas déranger pendant les plages ===
    //
    // `dnd_active` et `dnd_previous_filter` sont écrits par le contrôleur : ils
    // permettent de restaurer exactement le filtre d'origine, même après un
    // redémarrage ou une mort brutale du processus.
    val dndEnabled: Flow<Boolean> =
        data.map { it[DND_ENABLED] ?: false }.distinctUntilChanged()

    val dndActive: Flow<Boolean> =
        data.map { it[DND_ACTIVE] ?: false }.distinctUntilChanged()

    /** Vrai quand l'utilisateur a coupé le silence pour la plage en cours. */
    val dndOverridden: Flow<Boolean> =
        data.map { it[DND_OVERRIDDEN] ?: false }.distinctUntilChanged()

    override suspend fun readDnd(): DndState {
        val prefs = data.first()
        return DndState(
            enabled = prefs[DND_ENABLED] ?: false,
            active = prefs[DND_ACTIVE] ?: false,
            previousFilter = prefs[DND_PREVIOUS_FILTER] ?: 0,
            overridden = prefs[DND_OVERRIDDEN] ?: false
        )
    }

    override suspend fun writeDnd(state: DndState) {
        dataStore.edit {
            it[DND_ENABLED] = state.enabled
            it[DND_ACTIVE] = state.active
            it[DND_PREVIOUS_FILTER] = state.previousFilter
            it[DND_OVERRIDDEN] = state.overridden
        }
    }

    // === Verrou de configuration (code alphanumérique) ===
    //
    // Seul le couple sel + empreinte PBKDF2 est stocké : le code lui-même
    // n'existe nulle part sur le disque.
    override suspend fun read(): StoredPassphrase? {
        val prefs = data.first()
        val salt = prefs[PASSPHRASE_SALT] ?: return null
        val hash = prefs[PASSPHRASE_HASH] ?: return null
        val iterations = prefs[PASSPHRASE_ITERATIONS] ?: return null
        return StoredPassphrase(salt = salt, hash = hash, iterations = iterations)
    }

    override suspend fun write(stored: StoredPassphrase) {
        dataStore.edit {
            it[PASSPHRASE_SALT] = stored.salt
            it[PASSPHRASE_HASH] = stored.hash
            it[PASSPHRASE_ITERATIONS] = stored.iterations
        }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.remove(PASSPHRASE_SALT)
            it.remove(PASSPHRASE_HASH)
            it.remove(PASSPHRASE_ITERATIONS)
        }
    }

    override suspend fun readLockout(): LockoutState {
        val prefs = data.first()
        return LockoutState(
            failedAttempts = prefs[PASSPHRASE_FAILED_ATTEMPTS] ?: 0,
            lockedUntil = prefs[PASSPHRASE_LOCKED_UNTIL] ?: 0L
        )
    }

    override suspend fun writeLockout(state: LockoutState) {
        dataStore.edit {
            it[PASSPHRASE_FAILED_ATTEMPTS] = state.failedAttempts
            it[PASSPHRASE_LOCKED_UNTIL] = state.lockedUntil
        }
    }

    private companion object {
        val EMERGENCY_UNTIL = longPreferencesKey("emergency_until_wall_ms")
        val PAUSE_MESSAGE = stringPreferencesKey("pause_message")
        val ROLE_REVOKED = booleanPreferencesKey("role_revoked")
        val HELP_BANNER_DISMISSED = booleanPreferencesKey("help_banner_dismissed")
        val UNINSTALL_GUARD_DONE = booleanPreferencesKey("uninstall_guard_done")
        val FULL_SCREEN_INTENT_DONE = booleanPreferencesKey("fullscreen_intent_done")
        val PASSPHRASE_SALT = stringPreferencesKey("passphrase_salt")
        val PASSPHRASE_HASH = stringPreferencesKey("passphrase_hash")
        val PASSPHRASE_ITERATIONS = intPreferencesKey("passphrase_iterations")
        val PASSPHRASE_FAILED_ATTEMPTS = intPreferencesKey("passphrase_failed_attempts")
        val PASSPHRASE_LOCKED_UNTIL = longPreferencesKey("passphrase_locked_until")
        val DND_ENABLED = booleanPreferencesKey("dnd_enabled")
        val DND_ACTIVE = booleanPreferencesKey("dnd_active")
        val DND_PREVIOUS_FILTER = intPreferencesKey("dnd_previous_filter")

        /**
         * L'utilisateur a coupé le silence lui-même pendant la plage en cours :
         * on ne le rétablit pas avant la fin de cette plage.
         */
        val DND_OVERRIDDEN = booleanPreferencesKey("dnd_overridden")
    }
}