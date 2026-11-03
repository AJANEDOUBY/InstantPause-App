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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
 * Étape d'installation : protéger la désinstallation, juste après la création du code.
 *
 * Android interdit à une application de s'activer elle-même comme administrateur :
 * `ACTION_ADD_DEVICE_ADMIN` ouvre toujours une confirmation système. On ne peut donc
 * pas l'activer « tout seul » — mais on peut refuser d'aller plus loin sans elle.
 *
 * Le dialogue système est lancé **dès l'affichage** de l'écran (une seule fois) : pour
 * l'utilisateur, l'étape fait partie de l'installation. S'il refuse, l'app reste ici.
 */
@Composable
fun UninstallGuardScreen(onActivate: () -> Unit) {
    // Une seule tentative automatique : sinon un refus relancerait le dialogue en boucle.
    LaunchedEffect(Unit) { onActivate() }

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
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Dernière étape : protégez l'app",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sans cette protection, Instant Pause se désinstalle en trois taps — " +
                        "et vos plages disparaissent avec elle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Android va ouvrir un écran de confirmation. C'est normal : aucune " +
                        "application ne peut devenir administrateur sans votre accord. " +
                        "Appuyez sur « Activer ».",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            InfoLine(
                icon = { Icon(Icons.Default.Shield, null, tint = Color(0xFF10B981)) },
                text = "Ce qu'elle fait : bloquer une désinstallation impulsive. " +
                        "Aucun droit sur vos données, vos appels ou votre écran."
            )
            InfoLine(
                icon = { Icon(Icons.Default.DeleteForever, null, tint = Color(0xFF10B981)) },
                text = "Ce qu'elle empêche : désinstaller l'app sans d'abord la désactiver " +
                        "depuis les réglages Android."
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onActivate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Protéger la désinstallation")
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "L'app reste bloquée sur cet écran tant que la protection n'est pas " +
                        "active. Vous pourrez la retirer plus tard, avec votre code, depuis " +
                        "l'onglet Sécurité.",
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
