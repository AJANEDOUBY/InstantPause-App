package com.cooldown.app.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooldown.app.R
import com.cooldown.app.domain.model.Schedule
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

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
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter une plage")
            }
        }
    ) { padding ->
        if (schedules.isEmpty()) {
            EmptyState(padding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(schedules, key = { it.id }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onToggle = { viewModel.toggleActive(schedule) },
                        onDelete = { viewModel.delete(schedule) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
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
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.NightsStay,
            contentDescription = null,
            modifier = Modifier.width(64.dp).height(64.dp),
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
            text = "Ajoutez une plage pour activer la protection automatique (ex: 22h → 7h).",
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
        "${schedule.start.format(timeFormatter)} → ${schedule.end.format(timeFormatter)}"
    }
    val daysText = remember(schedule.daysMask) { formatDays(schedule.daysMask) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isActive) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeRange,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
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
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer"
                )
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
    val startState = rememberTimePickerState(
        initialHour = 22,
        initialMinute = 0,
        is24Hour = true
    )
    val endState = rememberTimePickerState(
        initialHour = 7,
        initialMinute = 0,
        is24Hour = true
    )
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
                DayChips(
                    mask = daysMask,
                    onMaskChange = { daysMask = it }
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (crossesMidnight) {
                        "Franchit minuit : la plage commence le jour sélectionné."
                    } else {
                        "Plage dans la même journée."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(startTime, endTime, daysMask) },
                enabled = daysMask != 0 && startTime != endTime
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayChips(
    mask: Int,
    onMaskChange: (Int) -> Unit
) {
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
    if (mask == 0b0011111) return "Lun → Ven"
    if (mask == 0b1100000) return "Week-end"

    val days = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
    return days
        .filterIndexed { i, _ -> (mask and (1 shl i)) != 0 }
        .joinToString(", ")
}