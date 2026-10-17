package com.cooldown.app.ui.main

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooldown.app.system.CoolDownWatchdogService
import com.cooldown.app.ui.schedule.ScheduleScreen
import com.cooldown.app.ui.theme.CoolDownTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshRole()
    }

    /** Demande la permission POST_NOTIFICATIONS (obligatoire depuis Android 13). */
    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* peu importe la réponse, l'utilisateur peut l'activer plus tard */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Démarre le watchdog sur les marques problématiques (itel, Xiaomi, etc.)
        CoolDownWatchdogService.startIfNeeded(this)

        // 2. Demande la permission notifications (Android 13+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 3. Depuis Android 14, la full-screen intent doit être autorisée explicitement.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm != null && !nm.canUseFullScreenIntent()) {
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                            .setData(Uri.parse("package:$packageName"))
                    )
                }
            }
        }

        setContent {
            CoolDownTheme {
                val roleHeld by viewModel.roleHeld.collectAsStateWithLifecycle()

                if (roleHeld) {
                    ScheduleScreen()
                } else {
                    RoleActivationScreen(
                        onActivateClick = {
                            viewModel.createRequestRoleIntent()?.let { intent ->
                                roleRequestLauncher.launch(intent)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshRole()
    }
}