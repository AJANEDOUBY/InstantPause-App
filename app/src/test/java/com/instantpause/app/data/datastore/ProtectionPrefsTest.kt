package com.instantpause.app.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.instantpause.app.domain.pause.PauseMessagePolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionPrefsTest {

    private fun prefs(): ProtectionPrefs =
        ProtectionPrefs(FakeDataStore(emptyPreferences()))

    @Test
    fun `le message de pause par defaut n'est jamais vide`() = runTest {
        assertEquals(PauseMessagePolicy.DEFAULT, prefs().getPauseMessage())
    }

    @Test
    fun `un message enregistre est relu normalise`() = runTest {
        val p = prefs()

        p.setPauseMessage("  Je  t'appelle demain.  ")

        assertEquals("Je t'appelle demain.", p.getPauseMessage())
        assertEquals("Je t'appelle demain.", p.pauseMessage.first())
    }

    @Test
    fun `effacer le message restaure la phrase par defaut`() = runTest {
        val p = prefs()
        p.setPauseMessage("Une phrase")

        p.setPauseMessage("")

        assertEquals(PauseMessagePolicy.DEFAULT, p.getPauseMessage())
    }
}

private class FakeDataStore<T>(initial: T) : DataStore<T> {
    private val state = MutableStateFlow(initial)

    override val data: Flow<T> = state

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
