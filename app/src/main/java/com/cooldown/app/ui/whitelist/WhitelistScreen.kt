package com.cooldown.app.ui.whitelist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import com.cooldown.app.domain.model.WhitelistEntry
import com.cooldown.app.ui.security.SecurityViewModel
import com.cooldown.app.ui.security.UnlockDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    viewModel: WhitelistViewModel = hiltViewModel(),
    securityViewModel: SecurityViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val limitReached = entries.size >= WhitelistViewModel.MAX_CONTACTS

    // Action retenue tant que le code n'est pas saisi.
    var gatedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var gatedReason by remember { mutableStateOf("") }

    /**
     * Autoriser un contact **affaiblit** la protection (ses appels passeront même
     * pendant une plage) ; le retirer la renforce mais supprime une exception
     * voulue. Les deux passent donc par le code, comme couper une plage.
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

    val pickContact = rememberLauncherForActivityResult(contract = PickPhoneNumber()) { uri ->
        if (uri != null) viewModel.addFromUri(context, uri)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            if (!limitReached) {
                FloatingActionButton(
                    onClick = {
                        guard("Ajouter un contact autorisé ? Ses appels passeront même pendant une plage.") {
                            runCatching { pickContact.launch(Unit) }
                                .onFailure { viewModel.reportPickerFailure() }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un contact")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TrustCircleBanner()
            }

            item {
                CounterRow(count = entries.size)
            }

            if (entries.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    ContactCard(
                        entry = entry,
                        onDelete = {
                            guard(
                                "Retirer ${entry.contactName} du cercle de confiance ? " +
                                        "Ses appels seront de nouveau bloqués pendant les plages."
                            ) { viewModel.remove(entry) }
                        }
                    )
                }
            }

            if (limitReached) {
                item {
                    Text(
                        text = "Limite de ${WhitelistViewModel.MAX_CONTACTS} contacts atteinte. " +
                                "Retirez un contact pour en ajouter un autre.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
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
private fun TrustCircleBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Cercle de confiance",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ces contacts ne sont jamais bloqués, même pendant une plage active. " +
                            "${WhitelistViewModel.MAX_CONTACTS} contacts maximum, et un appel " +
                            "autorisé dure 3 minutes : il est coupé automatiquement ensuite.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CounterRow(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Contacts autorisés",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$count / ${WhitelistViewModel.MAX_CONTACTS}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (count >= WhitelistViewModel.MAX_CONTACTS) {
                Color(0xFFF59E0B)
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Aucun contact autorisé",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sans contact autorisé, tous les appels sont bloqués pendant les plages " +
                    "(sauf urgences : 15, 17, 18, 19, 112).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ContactCard(entry: WhitelistEntry, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.contactName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = entry.phoneDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Retirer", tint = Color.Gray)
            }
        }
    }
}
