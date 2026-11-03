package com.instantpause.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Étape d'installation : autoriser le raccrochage automatique à 3 minutes.
 *
 * Ce n'est **pas** une option. `TelecomManager.endCall()`, la seule API qui permette à
 * une app tierce de couper une ligne, exige `ANSWER_PHONE_CALLS`. Sans elle, la limite
 * de 3 minutes n'est qu'un rappel visuel : on peut parler une heure à un contact
 * autorisé. L'app refuse donc d'aller plus loin tant que la permission manque.
 *
 * Le dialogue système est lancé dès l'affichage (une seule fois). S'il ne peut plus
 * s'afficher (refus définitif), l'écran renvoie vers les réglages Android.
 */
@Composable
fun HangupPermissionScreen(
    needsSettings: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    LaunchedEffect(Unit) { onRequest() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Couper les appels à 3 minutes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Un contact autorisé peut toujours vous joindre — mais pas pendant " +
                        "une heure. Android n'autorise une application à raccrocher qu'avec " +
                        "cette permission : sans elle, vous pourriez parler indéfiniment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            InfoLine(
                icon = { Icon(Icons.Default.Timer, null, tint = Color(0xFF10B981)) },
                text = "Le décompte part du décroché : 3 minutes de parole, pas 3 minutes " +
                        "incluant la sonnerie."
            )
            InfoLine(
                icon = { Icon(Icons.Default.CallEnd, null, tint = Color(0xFF10B981)) },
                text = "À 0:00, la ligne est coupée. Vous repartez de zéro pour le prochain " +
                        "appel, autant de fois que nécessaire."
            )

            Spacer(Modifier.height(24.dp))

            if (needsSettings) {
                Text(
                    text = "L'autorisation a été refusée définitivement : Android ne " +
                            "réaffichera plus la demande. Ouvrez les réglages de l'app pour " +
                            "l'accorder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Ouvrir les réglages")
                }
            } else {
                Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                    Text("Autoriser")
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Réglages de l'app")
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "L'app reste bloquée sur cet écran tant que l'autorisation n'est pas " +
                        "accordée.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoLine(
    icon: @Composable () -> Unit,
    text: String
) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        icon()
        Spacer(Modifier.size(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}
