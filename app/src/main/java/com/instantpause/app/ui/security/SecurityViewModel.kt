package com.instantpause.app.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantpause.app.domain.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val lock: AppLockManager
) : ViewModel() {

    val state: StateFlow<AppLockManager.State> = lock.state

    init {
        viewModelScope.launch { lock.ensureLoaded() }
    }

    suspend fun unlock(code: String): AppLockManager.Result = lock.unlock(code)

    suspend fun setPassphrase(new: String): AppLockManager.Result = lock.setPassphrase(new)

    suspend fun changePassphrase(current: String, new: String): AppLockManager.Result =
        lock.changePassphrase(current, new)

    suspend fun removePassphrase(current: String): AppLockManager.Result =
        lock.removePassphrase(current)

    /** Reverrouille tout de suite (bouton « Verrouiller maintenant »). */
    fun lockNow() {
        lock.lock()
    }

    /**
     * Vrai s'il faut demander le code avant une action qui affaiblit la protection.
     *
     * Passe par [AppLockManager.ensureLoaded] : au tout premier affichage, l'état
     * est encore LOADING et répondre « pas verrouillé » ouvrirait une fenêtre de
     * quelques millisecondes où l'on pourrait couper la protection.
     */
    suspend fun requiresCode(): Boolean {
        lock.ensureLoaded()
        return lock.state.value.isLocked
    }
}
