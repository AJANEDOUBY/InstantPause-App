package com.instantpause.app.ui.security

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instantpause.app.R
import com.instantpause.app.domain.security.AppLockManager
import com.instantpause.app.domain.security.PassphrasePolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** « 30 s », « 2 min » — jamais « 119 999 ms ». */
internal fun formatWait(ms: Long): String {
    val seconds = ((ms + 999) / 1000).coerceAtLeast(0)
    return if (seconds < 60) "$seconds s" else "${(seconds + 59) / 60} min"
}

internal fun violationText(res: Resources, violations: List<PassphrasePolicy.Violation>): String = buildList {
    if (PassphrasePolicy.Violation.TOO_SHORT in violations) {
        add(res.getString(R.string.violation_too_short, PassphrasePolicy.MIN_LENGTH))
    }
    if (PassphrasePolicy.Violation.MISSING_LETTER in violations) {
        add(res.getString(R.string.violation_missing_letter))
    }
    if (PassphrasePolicy.Violation.MISSING_DIGIT in violations) {
        add(res.getString(R.string.violation_missing_digit))
    }
}.joinToString(res.getString(R.string.list_separator))

internal fun resultMessage(
    result: AppLockManager.Result,
    res: Resources,
    now: Long = System.currentTimeMillis()
): String? =
    when (result) {
        is AppLockManager.Result.Success -> null
        is AppLockManager.Result.NotConfigured -> res.getString(R.string.error_no_code)
        is AppLockManager.Result.AlreadyConfigured -> res.getString(R.string.error_code_exists)
        is AppLockManager.Result.RateLimited ->
            res.getString(R.string.error_too_many_attempts, formatWait(result.until - now))
        is AppLockManager.Result.Wrong ->
            res.getString(R.string.error_wrong_code, result.attemptsLeft)
        is AppLockManager.Result.Rejected -> violationText(res, result.violations)
        is AppLockManager.Result.StorageError -> res.getString(R.string.error_storage)
    }

/** Champ de saisie masqué, avec bouton « œil ». */
@Composable
internal fun SecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation =
            if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) {
                        stringResource(R.string.action_hide)
                    } else {
                        stringResource(R.string.action_show)
                    }
                )
            }
        }
    )
}

/**
 * Demande le code avant une action qui affaiblit la protection.
 *
 * Le compteur de temporisation est lu depuis le verrou : fermer le dialogue et
 * le rouvrir ne remet pas les essais à zéro.
 */
@Composable
fun UnlockDialog(
    vm: SecurityViewModel,
    message: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    title: String? = null
) {
    val lockState by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val res = LocalContext.current.resources

    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var remainingMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(lockState.lockedUntil) {
        while (true) {
            remainingMs = (lockState.lockedUntil - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remainingMs <= 0L) break
            delay(1_000)
        }
    }

    val blocked = remainingMs > 0L

    fun submit() {
        if (busy || blocked) return
        scope.launch {
            busy = true
            error = null
            val result = vm.unlock(code)
            if (result is AppLockManager.Result.Success) {
                onSuccess()
                onDismiss()
            } else {
                error = resultMessage(result, res)
                code = ""
            }
            busy = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text(title ?: stringResource(R.string.dialog_code_required)) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                SecretField(
                    value = code,
                    onValueChange = { code = it; error = null },
                    label = stringResource(R.string.field_code),
                    imeAction = ImeAction.Done,
                    onDone = { submit() }
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (blocked) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.error_retry_in, formatWait(remainingMs)),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = code.isNotBlank() && !busy && !blocked
            ) { Text(stringResource(R.string.action_unlock)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

enum class PassphraseMode { CREATE, CHANGE }

/** Création, changement ou suppression du code. */
@Composable
fun PassphraseDialog(
    vm: SecurityViewModel,
    mode: PassphraseMode,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    val lockState by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val res = LocalContext.current.resources

    var current by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var remainingMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(lockState.lockedUntil) {
        while (true) {
            remainingMs = (lockState.lockedUntil - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remainingMs <= 0L) break
            delay(1_000)
        }
    }

    val blocked = remainingMs > 0L
    val policyErrors = PassphrasePolicy.violations(newCode)
    val mismatch = confirm.isNotEmpty() && confirm != newCode
    val canSubmit = !busy && !blocked &&
        PassphrasePolicy.isValid(newCode) && confirm == newCode &&
        (mode == PassphraseMode.CREATE || current.isNotBlank())

    fun submit() {
        if (!canSubmit) return
        scope.launch {
            busy = true
            error = null
            val result = when (mode) {
                PassphraseMode.CREATE -> vm.setPassphrase(newCode)
                PassphraseMode.CHANGE -> vm.changePassphrase(current, newCode)
            }
            if (result is AppLockManager.Result.Success) {
                onDone()
                onDismiss()
            } else {
                error = resultMessage(result, res)
            }
            busy = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = {
            Text(
                when (mode) {
                    PassphraseMode.CREATE -> stringResource(R.string.dialog_set_code)
                    PassphraseMode.CHANGE -> stringResource(R.string.dialog_change_code)
                }
            )
        },
        text = {
            Column {
                Text(
                    text = when (mode) {
                        PassphraseMode.CREATE -> stringResource(R.string.dialog_set_code_help)
                        PassphraseMode.CHANGE -> stringResource(R.string.dialog_change_code_help)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))

                if (mode != PassphraseMode.CREATE) {
                    SecretField(
                        value = current,
                        onValueChange = { current = it; error = null },
                        label = stringResource(R.string.field_current_code)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                SecretField(
                    value = newCode,
                    onValueChange = { newCode = it; error = null },
                    label = stringResource(R.string.field_new_code)
                )
                Spacer(Modifier.height(12.dp))
                SecretField(
                    value = confirm,
                    onValueChange = { confirm = it; error = null },
                    label = stringResource(R.string.field_confirm_code),
                    imeAction = ImeAction.Done,
                    onDone = { submit() }
                )

                if (newCode.isNotEmpty() && policyErrors.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.error_requirements, violationText(res, policyErrors)),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (mismatch) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.error_mismatch),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (blocked) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.error_retry_in, formatWait(remainingMs)),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = canSubmit
            ) {
                Text(
                    when (mode) {
                        PassphraseMode.CREATE -> stringResource(R.string.action_save)
                        PassphraseMode.CHANGE -> stringResource(R.string.action_change)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
