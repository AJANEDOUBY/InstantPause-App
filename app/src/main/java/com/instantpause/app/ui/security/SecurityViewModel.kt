package com.instantpause.app.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantpause.app.data.datastore.ProtectionPrefs
import com.instantpause.app.domain.dnd.DndController
import com.instantpause.app.domain.dnd.DndGateway
import com.instantpause.app.domain.language.AppLanguageController
import com.instantpause.app.domain.pause.PauseMessagePolicy
import com.instantpause.app.domain.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val lock: AppLockManager,
    private val prefs: ProtectionPrefs,
    private val dnd: DndController,
    private val dndGateway: DndGateway,
    private val language: AppLanguageController
) : ViewModel() {

    val state: StateFlow<AppLockManager.State> = lock.state

    val pauseMessage: StateFlow<String> = prefs.pauseMessage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    /** Option « Silence pendant la pause » (désactivée par défaut). */
    val dndEnabled: StateFlow<Boolean> = prefs.dndEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    /** Vrai quand l'application a effectivement posé le filtre. */
    val dndActive: StateFlow<Boolean> = prefs.dndActive
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    /** Vrai quand l'utilisateur a coupé le silence pour la plage en cours. */
    val dndSuspended: StateFlow<Boolean> = prefs.dndOverridden
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    private val _dndAccessGranted = MutableStateFlow(dndGateway.isAccessGranted())
    val dndAccessGranted: StateFlow<Boolean> = _dndAccessGranted.asStateFlow()

    /** Langue choisie ("" = langue du téléphone). */
    private val _languageTag = MutableStateFlow(language.safeCurrentTag())
    val languageTag: StateFlow<String> = _languageTag.asStateFlow()

    /** Faux avant Android 13 : la carte explique au lieu de proposer un choix. */
    val languageAvailable: Boolean = language.isAvailable()

    /** Langue du téléphone (étiquette `fr`, `es`…), pour l'option « système ». */
    val deviceLanguage: String = language.safeSystemLocaleTag()

    /** Applique la langue choisie ; le système recrée l'application. */
    fun setLanguage(tag: String) {
        _languageTag.value = tag
        language.setLanguage(tag)
    }

    init {
        viewModelScope.launch { lock.ensureLoaded() }
        refreshDndAccess()
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
     * Enregistre la phrase de l'écran de pause.
     *
     * Le nettoyage est fait par [PauseMessagePolicy] : jamais plus de
     * [PauseMessagePolicy.MAX_LENGTH] caractères. Une valeur vide est conservée
     * et l'affichage retombe sur la phrase par défaut de la langue courante.
     */
    fun savePauseMessage(raw: String) {
        viewModelScope.launch { prefs.setPauseMessage(raw) }
    }

    /**
     * Active ou coupe le silence des applications VoIP pendant les plages.
     *
     * Aucun code n'est demandé : rendre le téléphone silencieux est une action
     * qui renforce la protection, et couper l'option rend simplement le son.
     */
    fun setDndEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dnd.setEnabled(enabled)
            refreshDndAccess()
        }
    }

    /** Relit l'accord système, à chaque retour dans l'écran Sécurité. */
    fun refreshDndAccess() {
        _dndAccessGranted.value = dndGateway.isAccessGranted()
        viewModelScope.launch { runCatching { dnd.sync() } }
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
