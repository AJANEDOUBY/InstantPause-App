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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneLocked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Étape d'installation : autoriser l'écran de pause à s'afficher en plein écran.
 *
 * Depuis Android 14, `USE_FULL_SCREEN_INTENT` n'est plus accordée d'office aux apps
 * qui ne sont ni une alarme ni l'application téléphone par défaut. Sans elle, la
 * notification de blocage reste une notification : l'appel est bien bloqué, mais
 * l'écran de pause n'apparaît pas par-dessus le verrouillage.
 *
 * Contrairement au raccrochage, cette étape n'est **pas bloquante** : refusée, l'app
 * fonctionne toujours et la pause reste accessible en touchant la notification. Mais
 * elle n'est proposée qu'une fois, pour ne pas ouvrir les réglages à chaque lancement.
 *
 * Le réglage Android s'ouvre automatiquement à l'arrivée ; le bouton permet de le
 * rouvrir si besoin.
 */
@Composable
fun FullScreenIntentScreen(
    onAllow: () -> Unit,
    onSkip: () -> Unit
) {
    // Une seule ouverture automatique, même après une rotation d'écran.
    var autoOpened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoOpened) {
            autoOpened = true
            onAllow()
        }
    }

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
                imageVector = Icons.Default.Fullscreen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = "La pause par-dessus le verrouillage",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Android 14+ n'accorde plus d'office l'affichage en plein écran : " +
                        "l'écran de pause risque de n'apparaître que comme une notification. " +
                        "L'appel est bloqué dans tous les cas — seule la façon de vous " +
                        "montrer la pause change.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            InfoLine(
                icon = { Icon(Icons.Default.PhoneLocked, null, tint = Color(0xFF10B981)) },
                text = "Le blocage de l'appel ne dépend pas de cette autorisation."
            )
            InfoLine(
                icon = {
                    Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFF10B981))
                },
                text = "Sans elle, touchez la notification pour ouvrir la pause."
            )

            Spacer(Modifier.height(24.dp))

            Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                Text("Autoriser l'affichage")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuer sans")
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Cette étape ne reviendra plus. Vous pourrez changer d'avis dans " +
                        "les réglages Android de l'app.",
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
