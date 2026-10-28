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

    init {
        // Charge l'état initial du bandeau d'aide (visible si pas encore fermé)
        viewModelScope.launch {
            _helpBannerVisible.value = !prefs.isHelpBannerDismissed()
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