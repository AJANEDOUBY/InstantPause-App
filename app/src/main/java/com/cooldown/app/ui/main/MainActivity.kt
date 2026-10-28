package com.cooldown.app.ui.main

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooldown.app.system.CoolDownWatchdogService
import com.cooldown.app.system.WatchdogAlarmReceiver
import com.cooldown.app.ui.logs.LogsScreen
import com.cooldown.app.ui.schedule.ScheduleScreen
import com.cooldown.app.ui.theme.CoolDownTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var hasOverlayPermission by mutableStateOf(false)

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshRole()
    }

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoolDownWatchdogService.startIfNeeded(this)
        WatchdogAlarmReceiver.startAlarm(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

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

                if (!roleHeld) {
                    RoleActivationScreen(
                        onActivateClick = {
                            viewModel.createRequestRoleIntent()?.let { intent ->
                                roleRequestLauncher.launch(intent)
                            }
                        }
                    )
                } else {
                    MainScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        onRequestOverlayPermission = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshRole()
        hasOverlayPermission = Settings.canDrawOverlays(this)
    }
}

@Composable
private fun MainScreen(
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Plages") },
                    label = { Text("Plages") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Historique") },
                    label = { Text("Historique") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> ScheduleScreen()
                1 -> LogsScreen()
            }

            // Le bandeau n'apparaît QUE si la permission overlay manque
            if (!hasOverlayPermission) {
                HelpBanner(
                    modifier = Modifier.align(Alignment.TopCenter),
                    onRequestPermission = onRequestOverlayPermission
                )
            }
        }
    }
}