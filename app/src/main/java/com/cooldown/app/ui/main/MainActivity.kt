package com.cooldown.app.ui.main

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooldown.app.domain.security.AppLockManager
import com.cooldown.app.system.CoolDownWatchdogService
import com.cooldown.app.system.WatchdogAlarmReceiver
import com.cooldown.app.ui.logs.LogsScreen
import com.cooldown.app.ui.schedule.ScheduleScreen
import com.cooldown.app.ui.security.SecurityScreen
import com.cooldown.app.ui.security.SecurityViewModel
import com.cooldown.app.ui.security.SetupScreen
import com.cooldown.app.ui.theme.CoolDownTheme
import com.cooldown.app.ui.whitelist.WhitelistScreen
import com.cooldown.app.ui.whitelist.WhitelistViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var hasOverlayPermission by mutableStateOf(false)
    private var batteryOptimized by mutableStateOf(false)
    private var canHangUp by mutableStateOf(false)

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshRole()
    }

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    /** Raccrocher un appel autorisé à la fin des 3 minutes (TelecomManager.endCall). */
    private val hangupPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> canHangUp = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoolDownWatchdogService.startIfNeeded(this)
        WatchdogAlarmReceiver.startAlarm(this)
        batteryOptimized = isBatteryOptimized()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Raccrochage à 3:00 : permission dangereuse, demandée une fois au démarrage.
        // Refusée, le minuteur redevient un simple rappel (bouton dans le bandeau).
        if (checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            hangupPermissionLauncher.launch(Manifest.permission.ANSWER_PHONE_CALLS)
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
                val securityViewModel: SecurityViewModel = hiltViewModel()
                val lockState by securityViewModel.state.collectAsStateWithLifecycle()
                val whitelistViewModel: WhitelistViewModel = hiltViewModel()
                val trustedContacts by whitelistViewModel.entries.collectAsStateWithLifecycle()

                when {
                    // Chargement du verrou : ne pas afficher l'onboarding par erreur.
                    lockState.status == AppLockManager.Status.LOADING -> LoadingScreen()

                    // Aucun code : l'app ne va pas plus loin. Le verrou n'est pas une option.
                    lockState.status == AppLockManager.Status.DISABLED ->
                        SetupScreen(vm = securityViewModel)

                    !roleHeld -> RoleActivationScreen(
                        onActivateClick = {
                            viewModel.createRequestRoleIntent()?.let { intent ->
                                roleRequestLauncher.launch(intent)
                            }
                        }
                    )

                    else -> MainScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        batteryOptimized = batteryOptimized,
                        canHangUp = canHangUp,
                        hasTrustedContacts = trustedContacts.isNotEmpty(),
                        onRequestOverlayPermission = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        },
                        onRequestHangupPermission = {
                            hangupPermissionLauncher.launch(Manifest.permission.ANSWER_PHONE_CALLS)
                        },
                        onRequestBatteryExemption = { requestBatteryExemption() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshRole()
        hasOverlayPermission = Settings.canDrawOverlays(this)
        batteryOptimized = isBatteryOptimized()
        canHangUp = checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) ==
                PackageManager.PERMISSION_GRANTED
    }

    /** Vrai si l'app reste soumise à l'optimisation batterie du système. */
    private fun isBatteryOptimized(): Boolean {
        val pm = getSystemService(PowerManager::class.java) ?: return false
        return !pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestBatteryExemption() {
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName")
        )
        runCatching { startActivity(direct) }.onFailure {
            // Certains constructeurs désactivent cette action : on ouvre la liste générale.
            runCatching {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MainScreen(
    hasOverlayPermission: Boolean,
    batteryOptimized: Boolean,
    canHangUp: Boolean,
    hasTrustedContacts: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestHangupPermission: () -> Unit,
    onRequestBatteryExemption: () -> Unit
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
                    icon = { Icon(Icons.Default.Contacts, contentDescription = "Contacts") },
                    label = { Text("Contacts") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Historique") },
                    label = { Text("Historique") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Sécurité") },
                    label = { Text("Sécurité") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Les bandeaux poussent le contenu vers le bas : aucun recouvrement.
            if (!hasOverlayPermission) {
                HelpBanner(onRequestPermission = onRequestOverlayPermission)
            }
            if (!canHangUp && hasTrustedContacts) {
                HangupBanner(onRequestPermission = onRequestHangupPermission)
            }
            if (batteryOptimized) {
                BatteryBanner(onRequestExemption = onRequestBatteryExemption)
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> ScheduleScreen()
                    1 -> WhitelistScreen()
                    2 -> LogsScreen()
                    3 -> SecurityScreen()
                }
            }
        }
    }
}