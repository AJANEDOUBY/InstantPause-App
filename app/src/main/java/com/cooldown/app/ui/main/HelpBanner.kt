package com.cooldown.app.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpBanner(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    // Un vert un peu plus foncé pour que le texte blanc ressorte bien (solide, pas transparent)
    val solidGreen = Color(0xFF059669)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = solidGreen // ✅ Vert 100% opaque
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color.White // Icône blanche pour ressortir
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Finaliser la protection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Pour afficher l'écran de pause par-dessus vos appels, " +
                            "veuillez autoriser CoolDown à s'afficher par-dessus les autres applications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f) // Blanc légèrement grisé pour le texte secondaire
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, // Bouton blanc
                        contentColor = solidGreen     // Texte vert
                    )
                ) {
                    Text("Autoriser l'affichage", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}