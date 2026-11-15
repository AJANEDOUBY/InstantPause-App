package com.instantpause.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.instantpause.app.R
import com.instantpause.app.domain.security.AppLockManager
import com.instantpause.app.domain.security.PassphrasePolicy
import kotlinx.coroutines.launch

/**
 * Premier écran de l'app, avant même le rôle Telecom.
 *
 * Le code n'est pas une option : l'app ne s'ouvre pas tant qu'il n'existe pas.
 * C'était le trou de la 1.1.0 — on pouvait activer la protection puis la couper
 * sans jamais définir de code, parce que le verrou était facultatif.
 */
@Composable
fun SetupScreen(vm: SecurityViewModel) {
    var code by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val res = LocalContext.current.resources

    val violations = PassphrasePolicy.violations(code)
    val mismatch = confirm.isNotEmpty() && confirm != code
    val canSubmit = !busy && PassphrasePolicy.isValid(code) && confirm == code

    fun submit() {
        if (!canSubmit) return
        scope.launch {
            busy = true
            error = null
            val result = vm.setPassphrase(code)
            if (result is AppLockManager.Result.Success) {
                // L'état passe à LOCKED : MainActivity bascule sur l'écran suivant.
                code = ""
                confirm = ""
            } else {
                error = resultMessage(result, res)
            }
            busy = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.setup_explanation),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            SecretField(
                value = code,
                onValueChange = { code = it; error = null },
                label = stringResource(R.string.field_code)
            )
            Spacer(Modifier.height(12.dp))
            SecretField(
                value = confirm,
                onValueChange = { confirm = it; error = null },
                label = stringResource(R.string.field_confirm_code),
                imeAction = ImeAction.Done,
                onDone = { submit() }
            )

            if (code.isNotEmpty() && violations.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.error_requirements, violationText(res, violations)),
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

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { submit() },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.role_activate))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.setup_rules),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
