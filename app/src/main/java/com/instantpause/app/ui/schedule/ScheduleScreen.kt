package com.instantpause.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instantpause.app.R
import com.instantpause.app.domain.model.Schedule
import com.instantpause.app.ui.security.SecurityViewModel
import com.instantpause.app.ui.security.UnlockDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    securityViewModel: SecurityViewModel = hiltViewModel()
) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val emergencyUntil by viewModel.emergencyUntil.collectAsStateWithLifecycle()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Le mode Zen expire tout seul : on rafraîchit l'état sans attendre une action.
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            nowMs = System.currentTimeMillis()
        }
    }

    // Action retenue tant que le code n'est pas saisi.
    var gatedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var gatedReason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

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

    // Vérifie si au moins une plage est active MAINTENANT, ou si le Zen court encore
    val zenActive = emergencyUntil > nowMs
    val isCurrentlyProtected = zenActive || schedules.any {
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_schedule))
            }
        }
    ) { padding ->
        // Une seule LazyColumn pour tout l'écran : les bandeaux d'aide, le mode Zen
        // et les plages défilent ensemble. Avant, la liste était coincée sous les
        // bandeaux : sur une tablette en paysage, elle se retrouvait avec une
        // hauteur nulle et les plages semblaient ne pas avoir été créées.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatusBanner(
                    isProtected = isCurrentlyProtected,
                    zenActive = zenActive,
                    zenUntil = emergencyUntil
                )
            }

            item {
                ZenModeCard(
                    zenUntil = if (zenActive) emergencyUntil else 0L,
                    onActivate = { durationMs ->
                        val now = System.currentTimeMillis()
                        if (ScheduleViewModel.shortensCurrentZen(now, emergencyUntil, durationMs)) {
                            guard(
                                resources.getString(
                                    R.string.zen_shorten_reason,
                                    formatClock(now + durationMs),
                                    formatClock(emergencyUntil)
                                )
                            ) { viewModel.activateZenMode(durationMs) }
                        } else {
                            viewModel.activateZenMode(durationMs)
                        }
                    },
                    onCancel = {
                        guard(resources.getString(R.string.zen_cancel_reason)) {
                            viewModel.cancelZenMode()
                        }
                    }
                )
            }

            if (schedules.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(schedules, key = { it.id }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onToggle = {
                            if (schedule.isActive) {
                                guard(
                                    resources.getString(
                                        R.string.schedule_disable_reason,
                                        schedule.start.toString(),
                                        schedule.end.toString()
                                    )
                                ) { viewModel.toggleActive(schedule) }
                            } else {
                                // Réactiver renforce la protection : jamais gated.
                                viewModel.toggleActive(schedule)
                            }
                        },
                        onDelete = {
                            guard(
                                resources.getString(
                                    R.string.schedule_delete_reason,
                                    schedule.start.toString(),
                                    schedule.end.toString()
                                )
                            ) { viewModel.delete(schedule) }
                        }
                    )
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
private fun StatusBanner(
    isProtected: Boolean,
    zenActive: Boolean = false,
    zenUntil: Long = 0L
) {
    val bgColor = if (isProtected) Color(0xFF10B981) else Color(0xFFF59E0B)
    val text = when {
        zenActive && zenUntil > 0L ->
            stringResource(R.string.status_zen_until, formatClock(zenUntil))
        zenActive -> stringResource(R.string.status_zen)
        isProtected -> stringResource(R.string.status_active)
        else -> stringResource(R.string.status_inactive)
    }
    val icon = if (isProtected) Icons.Default.CheckCircle else Icons.Default.Warning

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            color = Color.White
        )
    }
}

@Composable
private fun ZenModeCard(
    zenUntil: Long,
    onActivate: (Long) -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = stringResource(R.string.zen_card_desc),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.zen_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (zenUntil > 0L) {
                            stringResource(R.string.zen_running_help, formatClock(zenUntil))
                        } else {
                            stringResource(R.string.zen_help)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScheduleViewModel.ZEN_PRESETS.forEach { (label, durationMs) ->
                    FilterChip(
                        selected = false,
                        onClick = { onActivate(durationMs) },
                        label = { Text(label) }
                    )
                }
            }

            if (zenUntil > 0L) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.action_stop_zen))
                }
            }
        }
    }
}

private fun formatClock(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

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
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_body),
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
    val daysText = formatDays(schedule.daysMask)

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
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = Color.Gray
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
    val startState = rememberTimePickerState(initialHour = 22, initialMinute = 0, is24Hour = true)
    val endState = rememberTimePickerState(initialHour = 7, initialMinute = 0, is24Hour = true)
    var daysMask by remember { mutableIntStateOf(Schedule.ALL_DAYS) }

    val startTime = LocalTime.of(startState.hour, startState.minute)
    val endTime = LocalTime.of(endState.hour, endState.minute)
    val crossesMidnight = endTime < startTime

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_new_schedule)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.label_start), style = MaterialTheme.typography.labelLarge)
                TimeInput(state = startState)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.label_end), style = MaterialTheme.typography.labelLarge)
                TimeInput(state = endState)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.label_days), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                DayChips(mask = daysMask, onMaskChange = { daysMask = it })
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (crossesMidnight) {
                        stringResource(R.string.schedule_crosses_midnight)
                    } else {
                        stringResource(R.string.schedule_same_day)
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
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayChips(mask: Int, onMaskChange: (Int) -> Unit) {
    val days = stringArrayResource(R.array.days_short)
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

@Composable
private fun formatDays(mask: Int): String {
    val days = stringArrayResource(R.array.days_short)
    if (mask == 0) return stringResource(R.string.days_none)
    if (mask == Schedule.ALL_DAYS) return stringResource(R.string.days_every)
    if (mask == 0b0011111) return stringResource(R.string.days_weekdays)
    if (mask == 0b1100000) return stringResource(R.string.days_weekend)
    return days.filterIndexed { i, _ -> (mask and (1 shl i)) != 0 }.joinToString(", ")
}