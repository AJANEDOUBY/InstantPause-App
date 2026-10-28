package com.cooldown.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooldown.app.R
import com.cooldown.app.domain.model.Schedule
import com.cooldown.app.ui.security.SecurityViewModel
import com.cooldown.app.ui.security.UnlockDialog
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    securityViewModel: SecurityViewModel = hiltViewModel()
) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    // Action retenue tant que le code n'est pas saisi.
    var gatedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var gatedReason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    /**
     * N'exige le code que pour les actions qui **affaiblissent** la protection :
     * couper une plage, la supprimer. Ajouter une plage ou activer le Zen reste
     * toujours possible, même verrouillé.
     */
    fun guard(reason: String, action: () -> Unit) {
        scope.launch {
            if (securityViewModel.requiresCode()) {
                gatedReason = reason
                gatedAction = action
            } else {
                action()
            }
        }
    }

    // Vérifie si au moins une plage est active MAINTENANT
    val isCurrentlyProtected = schedules.any {
        it.isActive && it.isActiveAt(ZonedDateTime.now())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter une plage")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- BANDEAU DE STATUT ---
            StatusBanner(isProtected = isCurrentlyProtected)

            // --- CARTE MODE ZEN (URGENCE) ---
            ZenModeCard(onClick = { viewModel.activateZenMode() })

            // --- LISTE DES PLAGES ---
            if (schedules.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(schedules, key = { it.id }) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
                            onToggle = {
                                if (schedule.isActive) {
                                    guard(
                                        "Désactiver la plage ${schedule.start} – ${schedule.end} ? " +
                                                "Sans elle, plus rien ne bloquera vos appels sur ce créneau."
                                    ) { viewModel.toggleActive(schedule) }
                                } else {
                                    // Réactiver renforce la protection : jamais gated.
                                    viewModel.toggleActive(schedule)
                                }
                            },
                            onDelete = {
                                guard(
                                    "Supprimer définitivement la plage ${schedule.start} – ${schedule.end} ?"
                                ) { viewModel.delete(schedule) }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) } // Espace pour le FAB
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { start, end, daysMask ->
                viewModel.addSchedule(start, end, daysMask)
                showAddDialog = false
            }
        )
    }

    gatedAction?.let { action ->
        UnlockDialog(
            vm = securityViewModel,
            message = gatedReason,
            onDismiss = { gatedAction = null },
            onSuccess = { action() }
        )
    }
}

@Composable
private fun StatusBanner(isProtected: Boolean) {
    val bgColor = if (isProtected) Color(0xFF10B981) else Color(0xFFF59E0B)
    val text = if (isProtected) "Protection Active" else "Protection Inactive"
    val icon = if (isProtected) Icons.Default.CheckCircle else Icons.Default.Warning

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor.copy(alpha = 0.15f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = bgColor)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = bgColor
        )
    }
}

@Composable
private fun ZenModeCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SelfImprovement,
                contentDescription = "Mode Zen",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Besoin d'une pause maintenant ?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Active le mode Zen pendant 2 heures.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.NightsStay,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Aucune plage configurée",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Ajoutez une plage pour activer la protection automatique (ex: 22h – 7h).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val timeRange = remember(schedule.start, schedule.end) {
        "${schedule.start.format(timeFormatter)} – ${schedule.end.format(timeFormatter)}"
    }
    val daysText = remember(schedule.daysMask) { formatDays(schedule.daysMask) }

    // Choisir l'icône selon l'heure
    val isNight = schedule.start.hour >= 20 || schedule.start.hour < 6
    val icon = if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny
    val iconColor = if (schedule.isActive) MaterialTheme.colorScheme.primary else Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeRange,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = schedule.isActive,
                onCheckedChange = { onToggle() }
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalTime, LocalTime, Int) -> Unit
) {
    val startState = rememberTimePickerState(initialHour = 22, initialMinute = 0, is24Hour = true)
    val endState = rememberTimePickerState(initialHour = 7, initialMinute = 0, is24Hour = true)
    var daysMask by remember { mutableIntStateOf(Schedule.ALL_DAYS) }

    val startTime = LocalTime.of(startState.hour, startState.minute)
    val endTime = LocalTime.of(endState.hour, endState.minute)
    val crossesMidnight = endTime < startTime

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle plage") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Début", style = MaterialTheme.typography.labelLarge)
                TimeInput(state = startState)
                Spacer(Modifier.height(16.dp))
                Text("Fin", style = MaterialTheme.typography.labelLarge)
                TimeInput(state = endState)
                Spacer(Modifier.height(16.dp))
                Text("Jours", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                DayChips(mask = daysMask, onMaskChange = { daysMask = it })
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (crossesMidnight) "Franchit minuit : la plage commence le jour sélectionné." else "Plage dans la même journée.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(startTime, endTime, daysMask) },
                enabled = daysMask != 0 && startTime != endTime
            ) { Text("Ajouter") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayChips(mask: Int, onMaskChange: (Int) -> Unit) {
    val days = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEachIndexed { index, label ->
            val bit = 1 shl index
            val selected = (mask and bit) != 0
            FilterChip(
                selected = selected,
                onClick = {
                    val newMask = if (selected) mask and bit.inv() else mask or bit
                    onMaskChange(newMask)
                },
                label = { Text(label) }
            )
        }
    }
}

private fun formatDays(mask: Int): String {
    if (mask == 0) return "Aucun jour"
    if (mask == Schedule.ALL_DAYS) return "Tous les jours"
    if (mask == 0b0011111) return "Lun – Ven"
    if (mask == 0b1100000) return "Week-end"
    val days = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
    return days.filterIndexed { i, _ -> (mask and (1 shl i)) != 0 }.joinToString(", ")
}