package com.instantpause.app.ui.security

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instantpause.app.R
import com.instantpause.app.domain.language.LanguageTags
import com.instantpause.app.domain.pause.PauseMessagePolicy
import com.instantpause.app.domain.security.AppLockManager
import java.util.Locale

private const val GREEN = 0xFF10B981
private const val AMBER = 0xFFF59E0B

@Composable
fun SecurityScreen(
    vm: SecurityViewModel = hiltViewModel()
) {
    val lockState by vm.state.collectAsStateWithLifecycle()
    val pauseMessage by vm.pauseMessage.collectAsStateWithLifecycle()
    val languageTag by vm.languageTag.collectAsStateWithLifecycle()
    val dndEnabled by vm.dndEnabled.collectAsStateWithLifecycle()
    val dndActive by vm.dndActive.collectAsStateWithLifecycle()
    val dndSuspended by vm.dndSuspended.collectAsStateWithLifecycle()
    val dndAccessGranted by vm.dndAccessGranted.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var dialog by remember { mutableStateOf<Dialog?>(null) }

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
        HangupCard()

        Spacer(Modifier.height(12.dp))
        DndCard(
            enabled = dndEnabled,
            active = dndActive,
            suspended = dndSuspended,
            accessGranted = dndAccessGranted,
            onToggle = { vm.setDndEnabled(it) },
            onAllow = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    )
                }
            },
            onPrioritySettings = {
                val opened = runCatching {
                    context.startActivity(Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS))
                }.isSuccess
                if (!opened) {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        )
                    }
                }
            }
        )



        Spacer(Modifier.height(12.dp))
        LanguageCard(
            currentTag = languageTag,
            deviceLanguage = vm.deviceLanguage,
            available = vm.languageAvailable,
            onSelect = { vm.setLanguage(it) }
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

        null -> Unit
    }
}

private enum class Dialog { SET, CHANGE }

/**
 * Rappel du raccrochage à 3 minutes, accordable après l'installation.
 *
 * L'étape d'onboarding est optionnelle : ce refus n'empêche pas la protection de
 * fonctionner, il prive seulement la coupure automatique de la ligne. La carte
 * disparaît dès que la permission est accordée et revient si elle est retirée.
 */
@Composable
private fun HangupCard() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) ==
            PackageManager.PERMISSION_GRANTED
    }

    if (granted) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = null,
                    tint = Color(AMBER)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.hangup_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.hangup_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    launcher.launch(Manifest.permission.ANSWER_PHONE_CALLS)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_allow))
            }
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_open_settings))
            }
        }
    }
}

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
private fun DndCard(
    enabled: Boolean,
    active: Boolean,
    suspended: Boolean,
    accessGranted: Boolean,
    onToggle: (Boolean) -> Unit,
    onAllow: () -> Unit,
    onPrioritySettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsPaused,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dnd_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.dnd_subtitle),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }

            if (enabled) {
                Spacer(Modifier.height(12.dp))
                if (!accessGranted) {
                    Text(
                        text = if (active) {
                            stringResource(R.string.dnd_access_lost)
                        } else {
                            stringResource(R.string.dnd_access_needed)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(AMBER)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onAllow) {
                        Text(stringResource(R.string.dnd_allow))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.dnd_access_granted),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(GREEN)
                    )
                    if (active) {
                        Text(
                            text = stringResource(R.string.dnd_active_now),
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (suspended) {
                        Text(
                            text = stringResource(R.string.dnd_suspended),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(AMBER)
                        )
                    }
                    TextButton(onClick = onPrioritySettings) {
                        Text(stringResource(R.string.dnd_priority_link))
                    }
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
private fun LanguageCard(
    currentTag: String,
    deviceLanguage: String,
    available: Boolean,
    onSelect: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    // Langue effective de l'app (celle dans laquelle cet écran est écrit).
    val appLocale = LocalConfiguration.current.locales[0]
    val deviceLabel = remember(deviceLanguage, appLocale) {
        // Seulement la langue : « Français » plutôt que « Français (France) »,
        // pour éviter les parenthèses imbriquées dans l'étiquette.
        Locale.forLanguageTag(deviceLanguage)
            .getDisplayLanguage(appLocale)
            .replaceFirstChar { it.titlecase(appLocale) }
    }

    val currentLabel =
        if (currentTag == LanguageTags.SYSTEM) {
            stringResource(R.string.language_option_system, deviceLabel)
        } else {
            nativeName(currentTag)
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.language_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.language_subtitle),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (available) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showDialog = true }) {
                    Text(stringResource(R.string.language_change))
                }
            } else {
                // Avant Android 13 il n'existe pas de langue par application :
                // mieux vaut l'expliquer que d'afficher un bouton sans effet.
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.language_requires_android13),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(AMBER)
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.language_title)) },
            text = {
                Column {
                    LanguageOptionRow(
                        label = stringResource(R.string.language_option_system, deviceLabel),
                        selected = currentTag == LanguageTags.SYSTEM,
                        onClick = {
                            showDialog = false
                            onSelect(LanguageTags.SYSTEM)
                        }
                    )
                    LanguageTags.supported.forEach { tag ->
                        LanguageOptionRow(
                            label = nativeName(tag),
                            selected = currentTag == tag,
                            onClick = {
                                showDialog = false
                                onSelect(tag)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

/** Nom de la langue dans cette langue : « Français » ne se traduit pas. */
@Composable
private fun nativeName(tag: String): String = when (tag) {
    "fr" -> stringResource(R.string.language_name_fr)
    "es" -> stringResource(R.string.language_name_es)
    else -> stringResource(R.string.language_name_en)
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
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
