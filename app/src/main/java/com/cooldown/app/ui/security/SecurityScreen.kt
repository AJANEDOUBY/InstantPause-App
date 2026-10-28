package com.cooldown.app.ui.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooldown.app.domain.security.AppLockManager
import com.cooldown.app.system.CoolDownDeviceAdminReceiver

private const val GREEN = 0xFF10B981
private const val AMBER = 0xFFF59E0B

@Composable
fun SecurityScreen(
    vm: SecurityViewModel = hiltViewModel()
) {
    val lockState by vm.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val component = remember { ComponentName(context, CoolDownDeviceAdminReceiver::class.java) }
    val dpm = remember { context.getSystemService(DevicePolicyManager::class.java) }
    var adminActive by remember { mutableStateOf(dpm?.isAdminActive(component) == true) }

    var dialog by remember { mutableStateOf<Dialog?>(null) }

    val adminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        adminActive = dpm?.isAdminActive(component) == true
    }

    fun deactivateAdmin() {
        runCatching { dpm?.removeActiveAdmin(component) }
        adminActive = dpm?.isAdminActive(component) == true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Sécurité",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Empêcher la protection d'être coupée sur un coup de tête.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))
        LockCard(
            state = lockState,
            onSet = { dialog = Dialog.SET },
            onChange = { dialog = Dialog.CHANGE },
            onLockNow = { vm.lockNow() }
        )

        Spacer(Modifier.height(12.dp))
        UninstallCard(
            adminActive = adminActive,
            onActivate = {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Instant Pause empêchera sa désinstallation accidentelle ou impulsive."
                    )
                }
                runCatching { adminLauncher.launch(intent) }
            },
            onDeactivate = {
                if (lockState.isLocked) dialog = Dialog.UNINSTALL_GUARD else deactivateAdmin()
            }
        )

        Spacer(Modifier.height(12.dp))
        HowItWorksCard()

        Spacer(Modifier.height(20.dp))
        val version = remember {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull()
        }
        Text(
            text = "Instant Pause $version",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
    }

    when (dialog) {
        Dialog.SET -> PassphraseDialog(
            vm = vm,
            mode = PassphraseMode.CREATE,
            onDismiss = { dialog = null },
            onDone = { dialog = null }
        )

        Dialog.CHANGE -> PassphraseDialog(
            vm = vm,
            mode = PassphraseMode.CHANGE,
            onDismiss = { dialog = null },
            onDone = { dialog = null }
        )

        Dialog.UNINSTALL_GUARD -> UnlockDialog(
            vm = vm,
            title = "Retirer la protection",
            message = "Sans administrateur, Instant Pause pourra être désinstallé " +
                    "sans aucune résistance. Saisissez votre code pour confirmer.",
            onDismiss = { dialog = null },
            onSuccess = {
                deactivateAdmin()
                dialog = null
            }
        )

        null -> Unit
    }
}

private enum class Dialog { SET, CHANGE, UNINSTALL_GUARD }

@Composable
private fun LockCard(
    state: AppLockManager.State,
    onSet: () -> Unit,
    onChange: () -> Unit,
    onLockNow: () -> Unit
) {
    val enabled = state.isConfigured
    val accent = if (enabled) Color(GREEN) else Color(AMBER)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (enabled) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (enabled) "Code de protection activé" else "Aucun code défini",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            !enabled -> "Un code est requis avant d'utiliser l'app."
                            state.isLocked -> "Verrouillé — le code sera demandé."
                            else -> "Déverrouillé pour cette session."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = accent
                )
            }

            Spacer(Modifier.height(12.dp))

            if (!enabled) {
                Button(onClick = onSet, modifier = Modifier.fillMaxWidth()) {
                    Text("Définir un code")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "À faire maintenant, à froid : c'est ce code qui vous " +
                            "empêchera de tout couper ce soir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedButton(onClick = onChange, modifier = Modifier.fillMaxWidth()) {
                    Text("Changer le code")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Le code est défini à l'installation et ne peut pas être " +
                            "supprimé depuis l'app. Seul un changement (avec l'ancien code) est possible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!state.isLocked) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onLockNow, modifier = Modifier.fillMaxWidth()) {
                        Text("Verrouiller maintenant")
                    }
                }
            }
        }
    }
}

@Composable
private fun UninstallCard(
    adminActive: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = if (adminActive) Color(GREEN) else Color(AMBER),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Désinstallation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (adminActive) {
                            "Protégée — désactivation de l'administrateur requise."
                        } else {
                            "Non protégée — l'app peut être désinstallée en 3 taps."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (adminActive) {
                OutlinedButton(onClick = onDeactivate, modifier = Modifier.fillMaxWidth()) {
                    Text("Retirer la protection")
                }
            } else {
                Button(onClick = onActivate, modifier = Modifier.fillMaxWidth()) {
                    Text("Protéger la désinstallation")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Honnêtement : aucune application ne peut empêcher une " +
                        "désinstallation. Android demandera seulement de désactiver cet " +
                        "administrateur d'abord — deux taps de plus, mais deux taps qui " +
                        "laissent le temps de réfléchir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ce que le code protège (et ne protège pas)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Bullet("Le code est créé à la première ouverture de l'app : impossible de l'ignorer ou de le supprimer ensuite.")
            Bullet("Demandé pour : couper une plage, la supprimer (y compris une plage Zen en cours), changer le code, retirer la protection anti-désinstallation.")
            Bullet("Jamais demandé pour : bloquer un appel, ajouter une plage, activer le Zen, consulter l'historique.")
            Bullet("Le déverrouillage ne dure que le temps de la session : 60 s en arrière-plan et il faut retaper le code.")
            Bullet("Trois essais, puis 30 s, 1 min, 2 min… jusqu'à 15 min d'attente entre deux tentatives.")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "Le code n'est jamais stocké en clair : seule une empreinte PBKDF2 " +
                        "(120 000 itérations) est conservée sur l'appareil.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text("•  ", style = MaterialTheme.typography.bodySmall)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
