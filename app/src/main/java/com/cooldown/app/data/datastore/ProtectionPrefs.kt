package com.cooldown.app.data.datastore

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
import com.cooldown.app.domain.security.LockoutState
import com.cooldown.app.domain.security.PassphraseStore
import com.cooldown.app.domain.security.StoredPassphrase
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
) : PassphraseStore {
    private val data: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    // === Mode Urgence Émotionnelle ===
    val emergencyUntil: Flow<Long> = data.map { it[EMERGENCY_UNTIL] ?: 0L }.distinctUntilChanged()

    suspend fun getEmergencyUntil(): Long = data.first()[EMERGENCY_UNTIL] ?: 0L
    suspend fun setEmergencyUntil(v: Long) { dataStore.edit { it[EMERGENCY_UNTIL] = v } }
    suspend fun clearEmergency() { dataStore.edit { it.remove(EMERGENCY_UNTIL) } }

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
        val ROLE_REVOKED = booleanPreferencesKey("role_revoked")
        val HELP_BANNER_DISMISSED = booleanPreferencesKey("help_banner_dismissed")
        val UNINSTALL_GUARD_DONE = booleanPreferencesKey("uninstall_guard_done")
        val PASSPHRASE_SALT = stringPreferencesKey("passphrase_salt")
        val PASSPHRASE_HASH = stringPreferencesKey("passphrase_hash")
        val PASSPHRASE_ITERATIONS = intPreferencesKey("passphrase_iterations")
        val PASSPHRASE_FAILED_ATTEMPTS = intPreferencesKey("passphrase_failed_attempts")
        val PASSPHRASE_LOCKED_UNTIL = longPreferencesKey("passphrase_locked_until")
    }
}