package com.cooldown.app.ui.main

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val roleManager: RoleManager? =
        context.getSystemService(RoleManager::class.java)

    private val _roleHeld = MutableStateFlow(false)
    val roleHeld: StateFlow<Boolean> = _roleHeld.asStateFlow()

    /** À appeler dans onResume() : relit l'état réel du rôle auprès d'Android. */
    fun refreshRole() {
        _roleHeld.value = roleManager
            ?.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION) == true
    }

    /** Le rôle est-il disponible sur cet appareil ? (faux sur tablettes sans téléphone) */
    fun isRoleAvailable(): Boolean =
        roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION) == true

    /** Intent système qui déclenche la pop-up de demande de rôle. */
    fun createRequestRoleIntent(): Intent? =
        if (isRoleAvailable()) {
            roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)
        } else {
            null
        }
}