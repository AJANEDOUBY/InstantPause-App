package com.instantpause.app.ui.main

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantpause.app.data.datastore.ProtectionPrefs
import com.instantpause.app.domain.dnd.DndController
import com.instantpause.app.domain.protection.ProtectionAlarms
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val prefs: ProtectionPrefs,
    private val dnd: DndController,
    private val alarms: ProtectionAlarms
) : ViewModel() {

    private val roleManager: RoleManager? =
        context.getSystemService(RoleManager::class.java)

    private val _roleHeld = MutableStateFlow(false)
    val roleHeld: StateFlow<Boolean> = _roleHeld.asStateFlow()

    private val _helpBannerVisible = MutableStateFlow(false)
    val helpBannerVisible: StateFlow<Boolean> = _helpBannerVisible.asStateFlow()

    /** `null` tant que la valeur n'est pas chargée : l'onboarding ne doit pas clignoter. */
    private val _fullScreenIntentDone = MutableStateFlow<Boolean?>(null)
    val fullScreenIntentDone: StateFlow<Boolean?> = _fullScreenIntentDone.asStateFlow()

    /** `null` tant que la valeur n'est pas chargée, comme pour le plein écran. */
    private val _hangupPromptDone = MutableStateFlow<Boolean?>(null)
    val hangupPromptDone: StateFlow<Boolean?> = _hangupPromptDone.asStateFlow()

    init {
        // Charge l'état initial du bandeau d'aide (visible si pas encore fermé)
        viewModelScope.launch {
            _helpBannerVisible.value = !prefs.isHelpBannerDismissed()
        }
        viewModelScope.launch {
            _fullScreenIntentDone.value = prefs.isFullScreenIntentDone()
        }
        viewModelScope.launch {
            _hangupPromptDone.value = prefs.isHangupPromptDone()
        }
    }

    /**
     * Mémorise que l'étape « pause en plein écran » a été vue.
     *
     * Appelée quand la permission est accordée, ou quand l'utilisateur choisit de
     * continuer sans : dans les deux cas, l'étape ne revient plus au lancement.
     */
    fun markFullScreenIntentDone() {
        if (_fullScreenIntentDone.value == true) return
        viewModelScope.launch {
            prefs.setFullScreenIntentDone(true)
            _fullScreenIntentDone.value = true
        }
    }

    /**
     * Mémorise que l'écran « raccrocher à 3 minutes » a été vu.
     *
     * Appelée quand la permission est accordée, ou quand l'utilisateur choisit de
     * continuer sans : dans les deux cas, l'étape ne revient plus au lancement.
     * Le refus n'enlève pas la protection, il retire seulement la coupure à 3 minutes.
     */
    fun markHangupPromptDone() {
        if (_hangupPromptDone.value == true) return
        viewModelScope.launch {
            prefs.setHangupPromptDone(true)
            _hangupPromptDone.value = true
        }
    }

    fun refreshRole() {
        _roleHeld.value = roleManager
            ?.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION) == true
    }

    /**
     * Recale le filtre « Ne pas déranger » sur l'état réel de la protection.
     *
     * Appelé à chaque reprise de l'activité : c'est le moment où l'utilisateur
     * revient des réglages système (accès accordé ou retiré), et où une plage a
     * pu se terminer pendant que l'app était en arrière-plan.
     */
    fun syncDnd() {
        viewModelScope.launch {
            runCatching { dnd.sync() }
            runCatching { alarms.reschedule() }
        }
    }

    fun isRoleAvailable(): Boolean =
        roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION) == true

    fun createRequestRoleIntent(): Intent? =
        if (isRoleAvailable()) {
            roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)
        } else {
            null
        }

    /** Appelé quand l'utilisateur ferme le bandeau : persiste le flag et cache le bandeau. */
    fun dismissHelpBanner() {
        viewModelScope.launch {
            prefs.setHelpBannerDismissed(true)
            _helpBannerVisible.value = false
        }
    }
}
