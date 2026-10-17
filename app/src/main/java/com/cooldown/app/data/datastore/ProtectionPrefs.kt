package com.cooldown.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
) {
    private val data: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val emergencyUntil: Flow<Long> = data.map { it[EMERGENCY_UNTIL] ?: 0L }.distinctUntilChanged()

    suspend fun getEmergencyUntil(): Long = data.first()[EMERGENCY_UNTIL] ?: 0L
    suspend fun setEmergencyUntil(v: Long) { dataStore.edit { it[EMERGENCY_UNTIL] = v } }
    suspend fun clearEmergency() { dataStore.edit { it.remove(EMERGENCY_UNTIL) } }

    val roleRevoked: Flow<Boolean> = data.map { it[ROLE_REVOKED] ?: false }.distinctUntilChanged()
    suspend fun isRoleRevoked(): Boolean = data.first()[ROLE_REVOKED] ?: false
    suspend fun setRoleRevoked(v: Boolean) { dataStore.edit { it[ROLE_REVOKED] = v } }

    private companion object {
        val EMERGENCY_UNTIL = longPreferencesKey("emergency_until_wall_ms")
        val ROLE_REVOKED = booleanPreferencesKey("role_revoked")
    }
}