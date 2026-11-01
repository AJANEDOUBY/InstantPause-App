package com.cooldown.app.ui.main

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooldown.app.data.datastore.ProtectionPrefs
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
    private val prefs: ProtectionPrefs
) : ViewModel() {

    private val roleManager: RoleManager? =
        context.getSystemService(RoleManager::class.java)

    private val _roleHeld = MutableStateFlow(false)
    val roleHeld: StateFlow<Boolean> = _roleHeld.asStateFlow()

    private val _helpBannerVisible = MutableStateFlow(false)
    val helpBannerVisible: StateFlow<Boolean> = _helpBannerVisible.asStateFlow()

    /** `null` tant que la valeur n'est pas chargée : l'onboarding ne doit pas clignoter. */
    private val _uninstallGuardDone = MutableStateFlow<Boolean?>(null)
    val uninstallGuardDone: StateFlow<Boolean?> = _uninstallGuardDone.asStateFlow()

    /** `null` tant que la valeur n'est pas chargée : l'onboarding ne doit pas clignoter. */
    private val _fullScreenIntentDone = MutableStateFlow<Boolean?>(null)
    val fullScreenIntentDone: StateFlow<Boolean?> = _fullScreenIntentDone.asStateFlow()

    init {
        // Charge l'état initial du bandeau d'aide (visible si pas encore fermé)
        viewModelScope.launch {
            _helpBannerVisible.value = !prefs.isHelpBannerDismissed()
        }
        viewModelScope.launch {
            _uninstallGuardDone.value = prefs.isUninstallGuardDone()
        }
        viewModelScope.launch {
            _fullScreenIntentDone.value = prefs.isFullScreenIntentDone()
        }
    }

    /**
     * Mémorise que l'étape « protéger la désinstallation » est franchie.
     *
     * Appelée dès que l'administrateur est actif : l'étape ne réapparaît donc plus,
     * même si l'utilisateur retire ensuite la protection avec son code depuis
     * l'onglet Sécurité.
     */
    fun markUninstallGuardDone() {
        if (_uninstallGuardDone.value == true) return
        viewModelScope.launch {
            prefs.setUninstallGuardDone(true)
            _uninstallGuardDone.value = true
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

    fun refreshRole() {
        _roleHeld.value = roleManager
            ?.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION) == true
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