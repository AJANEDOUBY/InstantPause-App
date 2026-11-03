package com.instantpause.app.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Bandeaux d'aide volontairement **compacts** : une icône, deux lignes et un bouton
 * sur la même rangée. En version pleine hauteur, deux bandeaux mangeaient plus de
 * 500 px sur une tablette en paysage et poussaient la liste des plages hors écran.
 */
@Composable
private fun CompactBanner(
    title: String,
    message: String,
    actionLabel: String,
    containerColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = containerColor
                )
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Bandeau affiché tant que l'app n'a pas le droit de s'afficher par-dessus les autres. */
@Composable
fun HelpBanner(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    CompactBanner(
        title = "Affichage par-dessus les appels",
        message = "Requis pour montrer l'écran de pause pendant un appel.",
        actionLabel = "Autoriser",
        containerColor = Color(0xFF059669),
        icon = Icons.Default.Security,
        onClick = onRequestPermission,
        modifier = modifier
    )
}

/**
 * Bandeau affiché quand l'app reste soumise à l'optimisation batterie.
 *
 * Sans exemption, les constructeurs (itel, Xiaomi, Samsung…) tuent le veilleur
 * en arrière-plan : la protection continue de fonctionner (Telecom réveille
 * l'app à chaque appel) mais elle n'est plus garantie après un redémarrage.
 */
@Composable
fun BatteryBanner(
    modifier: Modifier = Modifier,
    onRequestExemption: () -> Unit
) {
    CompactBanner(
        title = "Protection après redémarrage",
        message = "Autorisez le démarrage auto et l'énergie sans restriction.",
        actionLabel = "Autoriser",
        containerColor = Color(0xFFB45309),
        icon = Icons.Default.BatteryAlert,
        onClick = onRequestExemption,
        modifier = modifier
    )
}
