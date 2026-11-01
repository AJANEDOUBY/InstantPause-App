package com.cooldown.app.ui.main

import android.Manifest
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
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
import com.cooldown.app.system.CoolDownDeviceAdminReceiver
import com.cooldown.app.system.CoolDownWatchdogService
import com.cooldown.app.system.WatchdogAlarmReceiver
import com.cooldown.app.ui.logs.LogsScreen
import com.cooldown.app.ui.schedule.ScheduleScreen
import com.cooldown.app.ui.security.HangupPermissionScreen
import com.cooldown.app.ui.security.SecurityScreen
import com.cooldown.app.ui.security.SecurityViewModel
import com.cooldown.app.ui.security.SetupScreen
import com.cooldown.app.ui.security.UninstallGuardScreen
import com.cooldown.app.ui.theme.CoolDownTheme
import com.cooldown.app.ui.whitelist.WhitelistScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var hasOverlayPermission by mutableStateOf(false)
    private var batteryOptimized by mutableStateOf(false)
    private var canHangUp by mutableStateOf(false)
    private var adminActive by mutableStateOf(false)
    private var hangupAsked by mutableStateOf(false)

    private val adminComponent by lazy {
        ComponentName(this, CoolDownDeviceAdminReceiver::class.java)
    }

    /**
     * Administration d'appareil : seule protection contre une désinstallation impulsive.
     *
     * Android impose une confirmation système, donc l'app ne peut pas s'activer seule.
     * En revanche elle peut refuser d'aller plus loin tant que ce n'est pas fait —
     * c'est ce que fait [UninstallGuardScreen] pendant l'onboarding.
     */
    private val adminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshAdmin() }

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
    ) { granted ->
        canHangUp = granted
        hangupAsked = true
    }

    /**
     * Android ne réaffiche plus la demande après deux refus : il faut passer par les
     * réglages. On ne le propose qu'après une première demande, sinon l'écran
     * afficherait « ouvrez les réglages » avant même d'avoir demandé.
     */
    private val hangupNeedsSettings: Boolean
        get() = hangupAsked && !canHangUp &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ANSWER_PHONE_CALLS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoolDownWatchdogService.startIfNeeded(this)
        WatchdogAlarmReceiver.startAlarm(this)
        batteryOptimized = isBatteryOptimized()
        refreshAdmin()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Raccrochage à 3:00 : la permission n'est plus demandée ici, mais dans l'étape
        // d'installation dédiée (HangupPermissionScreen) — sinon le dialogue surgirait
        // par-dessus l'écran de création du code.

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
                val guardAcknowledged by viewModel.uninstallGuardDone.collectAsStateWithLifecycle()

                when (onboardingStage(
                    loading = lockState.status == AppLockManager.Status.LOADING,
                    hasCode = lockState.status != AppLockManager.Status.DISABLED,
                    roleHeld = roleHeld,
                    adminActive = adminActive,
                    guardAcknowledged = guardAcknowledged,
                    canHangUp = canHangUp
                )) {
                    // Chargement du verrou : ne pas afficher l'onboarding par erreur.
                    OnboardingStage.LOADING -> LoadingScreen()

                    // Aucun code : l'app ne va pas plus loin. Le verrou n'est pas une option.
                    OnboardingStage.SETUP_CODE -> SetupScreen(vm = securityViewModel)

                    // Protection anti-désinstallation : obligatoire avant tout le reste.
                    OnboardingStage.PROTECT_UNINSTALL ->
                        UninstallGuardScreen(onActivate = { requestAdmin() })

                    // Raccrochage à 3 minutes : obligatoire, ce n'est pas une option.
                    OnboardingStage.HANGUP_PERMISSION -> HangupPermissionScreen(
                        needsSettings = hangupNeedsSettings,
                        onRequest = {
                            hangupPermissionLauncher.launch(
                                Manifest.permission.ANSWER_PHONE_CALLS
                            )
                        },
                        onOpenSettings = { openAppSettings() }
                    )

                    OnboardingStage.ACTIVATE_ROLE -> RoleActivationScreen(
                        onActivateClick = {
                            viewModel.createRequestRoleIntent()?.let { intent ->
                                roleRequestLauncher.launch(intent)
                            }
                        }
                    )

                    OnboardingStage.MAIN -> MainScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        batteryOptimized = batteryOptimized,
                        onRequestOverlayPermission = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
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
        refreshAdmin()
    }

    private fun refreshAdmin() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        adminActive = dpm?.isAdminActive(adminComponent) == true
        if (adminActive) viewModel.markUninstallGuardDone()
    }

    /**
     * Ouvre l'écran système d'activation de l'administrateur.
     *
     * C'est le seul chemin possible : `ACTION_ADD_DEVICE_ADMIN` demande toujours une
     * confirmation explicite. On l'appelle donc au moment de l'installation, et on ne
     * laisse pas l'onboarding continuer sans elle.
     */
    private fun requestAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Instant Pause empêchera sa désinstallation accidentelle ou impulsive. " +
                        "Aucun autre droit n'est demandé."
            )
        }
        runCatching { adminLauncher.launch(intent) }
            .onFailure { refreshAdmin() }
    }

    /** Vrai si l'app reste soumise à l'optimisation batterie du système. */
    private fun isBatteryOptimized(): Boolean {
        val pm = getSystemService(PowerManager::class.java) ?: return false
        return !pm.isIgnoringBatteryOptimizations(packageName)
    }

    /**
     * Réglages Android de l'app : seul recours quand une permission a été refusée
     * définitivement (Android ne réaffiche plus la demande).
     */
    private fun openAppSettings() {
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }
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
    onRequestOverlayPermission: () -> Unit,
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