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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                error = resultMessage(result)
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
                text = "Bienvenue dans Instant Pause",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Dernière étape avant la protection : choisissez votre code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Il sera demandé pour couper une plage, la supprimer, ou retirer " +
                        "la protection anti-désinstallation. Choisissez-le maintenant, à froid : " +
                        "c'est lui qui tiendra ce soir-là.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            SecretField(
                value = code,
                onValueChange = { code = it; error = null },
                label = "Code alphanumérique"
            )
            Spacer(Modifier.height(12.dp))
            SecretField(
                value = confirm,
                onValueChange = { confirm = it; error = null },
                label = "Confirmer le code",
                imeAction = ImeAction.Done,
                onDone = { submit() }
            )

            if (code.isNotEmpty() && violations.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Il faut ${violationText(violations)}.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (mismatch) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Les deux saisies diffèrent.",
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
                Text("Activer la protection")
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "6 caractères minimum, avec au moins une lettre et un chiffre. " +
                        "Le code n'est jamais stocké en clair et ne peut pas être récupéré : " +
                        "si vous l'oubliez, il faudra effacer les données de l'app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
