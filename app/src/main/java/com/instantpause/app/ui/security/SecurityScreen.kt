package com.instantpause.app.ui.security

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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instantpause.app.R
import com.instantpause.app.domain.pause.PauseMessagePolicy
import com.instantpause.app.domain.security.AppLockManager
import com.instantpause.app.system.CoolDownDeviceAdminReceiver

private const val GREEN = 0xFF10B981
private const val AMBER = 0xFFF59E0B

@Composable
fun SecurityScreen(
    vm: SecurityViewModel = hiltViewModel()
) {
    val lockState by vm.state.collectAsStateWithLifecycle()
    val pauseMessage by vm.pauseMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val resources = LocalResources.current
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
            text = stringResource(R.string.security_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.security_subtitle),
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
        PauseMessageCard(
            current = pauseMessage,
            onSave = { vm.savePauseMessage(it) }
        )

        Spacer(Modifier.height(12.dp))
        UninstallCard(
            adminActive = adminActive,
            onActivate = {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        resources.getString(R.string.security_admin_explanation)
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
            text = stringResource(R.string.version_label, version.orEmpty()),
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
            title = stringResource(R.string.action_remove_protection),
            message = stringResource(R.string.dialog_remove_protection_message),
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

/**
 * Phrase affichée sur l'écran de pause, au moment où l'appel est intercepté.
 *
 * Elle est saisie ici, à froid, puis relue pendant l'impulsion : c'est la voix
 * de l'utilisateur lui-même, pas celle du développeur.
 */
@Composable
private fun PauseMessageCard(
    current: String,
    onSave: (String) -> Unit
) {
    var draft by remember(current) { mutableStateOf(current) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pause_message_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.pause_message_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(PauseMessagePolicy.MAX_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.pause_message_label)) },
                placeholder = { Text(stringResource(R.string.pause_message_default)) },
                minLines = 2,
                supportingText = {
                    Text("${draft.length} / ${PauseMessagePolicy.MAX_LENGTH}")
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pause_message_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(draft) },
                    enabled = draft != current
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

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
                        text = if (enabled) {
                            stringResource(R.string.lock_configured)
                        } else {
                            stringResource(R.string.lock_none)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            !enabled -> stringResource(R.string.lock_none_help)
                            state.isLocked -> stringResource(R.string.lock_locked)
                            else -> stringResource(R.string.lock_unlocked)
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
                    Text(stringResource(R.string.dialog_set_code))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.lock_set_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedButton(onClick = onChange, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.dialog_change_code))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.lock_change_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!state.isLocked) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onLockNow, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_lock_now))
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
                        text = stringResource(R.string.uninstall_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (adminActive) {
                            stringResource(R.string.uninstall_protected)
                        } else {
                            stringResource(R.string.uninstall_unprotected)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (adminActive) {
                OutlinedButton(onClick = onDeactivate, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_remove_protection))
                }
            } else {
                Button(onClick = onActivate, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_protect_uninstall))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.uninstall_help),
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
                text = stringResource(R.string.how_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Bullet(stringResource(R.string.how_bullet_1))
            Bullet(stringResource(R.string.how_bullet_2))
            Bullet(stringResource(R.string.how_bullet_3))
            Bullet(stringResource(R.string.how_bullet_4))
            Bullet(stringResource(R.string.how_bullet_5))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.how_footer),
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
