package com.instantpause.app.system

import android.net.Uri
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.util.Log
import com.instantpause.app.domain.model.BlockStatus
import com.instantpause.app.domain.phone.PhoneNumberNormalizer
import com.instantpause.app.domain.repository.BlockedLogRepository
import com.instantpause.app.domain.usecase.AllowReason
import com.instantpause.app.domain.usecase.CallDecision
import com.instantpause.app.domain.usecase.ShouldBlockCallUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Intercepte les appels sortants via l'API Telecom (rôle ROLE_CALL_REDIRECTION).
 *
 * Principes :
 *  - Aucune I/O sur le thread du callback Telecom : la décision est prise dans [serviceScope].
 *  - Fail-open : toute erreur, timeout ou destruction laisse passer l'appel.
 *  - Une seule réponse à Telecom par appel (garde [AtomicBoolean] par appel).
 *  - Les urgences sont court-circuitées avant toute I/O.
 */
@AndroidEntryPoint
class CoolDownCallRedirectionService : CallRedirectionService() {

    @Inject lateinit var shouldBlock: ShouldBlockCallUseCase
    @Inject lateinit var logs: BlockedLogRepository
    @Inject lateinit var normalizer: PhoneNumberNormalizer
    @Inject lateinit var notifier: ProtectionAlertNotifier

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Gardes des appels dont la réponse est encore en attente (fail-open à la destruction). */
    private val pendingResponses = ConcurrentHashMap.newKeySet<AtomicBoolean>()

    override fun onDestroy() {
        // Si le service meurt avant d'avoir répondu, on ne laisse pas Telecom
        // dans l'attente : fail-open une dernière fois.
        pendingResponses.forEach { guard ->
            if (guard.compareAndSet(false, true)) {
                runCatching { placeCallUnmodified() }
                    .onFailure { Log.w(TAG, "Réponse de secours impossible à la destruction", it) }
            }
        }
        pendingResponses.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean
    ) {
        val responded = AtomicBoolean(false)
        pendingResponses += responded

        try {
            // sip:, voicemail: et autres : ce ne sont pas des appels vers un numéro
            if (handle.scheme != PhoneAccount.SCHEME_TEL) {
                respondUnmodified(responded)
                return
            }

            val rawNumber = handle.schemeSpecificPart.orEmpty()
            val regions = normalizer.resolveRegions(initialPhoneAccount)

            // Urgences : court-circuit avant toute lecture en base
            if (normalizer.isEmergency(rawNumber, regions)) {
                respondUnmodified(responded)
                return
            }

            // Décision 100 % asynchrone : on rend immédiatement le thread Telecom.
            serviceScope.launch {
                try {
                    evaluateAndRespond(rawNumber, regions, responded)
                } finally {
                    pendingResponses -= responded
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Erreur inattendue, appel laissé passer", t)
            respondUnmodified(responded)
        }
    }

    private suspend fun evaluateAndRespond(
        rawNumber: String,
        regions: List<String>,
        responded: AtomicBoolean
    ) {
        val decision = withTimeoutOrNull(DECISION_TIMEOUT_MS) {
            shouldBlock(rawNumber, regions)
        } ?: CallDecision.Allow(AllowReason.FAIL_OPEN, null)

        when (decision) {
            is CallDecision.Allow -> {
                if (decision.reason == AllowReason.WHITELISTED) {
                    runCatching {
                        logs.log(
                            decision.normalizedNumber ?: rawNumber,
                            BlockStatus.ALLOWED_WHITELIST
                        )
                    }.onFailure { Log.w(TAG, "Échec journalisation whitelist", it) }

                    // Appel autorisé : on lance le compte à rebours (3 minutes).
                    runCatching { CallTimerService.start(this@CoolDownCallRedirectionService) }
                        .onFailure { Log.w(TAG, "Échec démarrage du minuteur", it) }
                }
                respondUnmodified(responded)
            }

            is CallDecision.Block -> {
                val number = decision.normalizedNumber ?: rawNumber
                // Journaliser AVANT cancelCall : le process peut être réduit juste après
                runCatching { logs.log(number, BlockStatus.BLOCKED) }
                    .onFailure { Log.w(TAG, "Échec journalisation blocage", it) }

                decline(number, responded)
            }
        }
    }

    /** Répond une seule fois à Telecom : l'appel passe. */
    private fun respondUnmodified(responded: AtomicBoolean) {
        if (!responded.compareAndSet(false, true)) return
        pendingResponses -= responded
        runCatching { placeCallUnmodified() }
            .onFailure { Log.e(TAG, "placeCallUnmodified a échoué", it) }
    }

    /** Répond une seule fois à Telecom : l'appel est annulé. */
    private fun decline(number: String, responded: AtomicBoolean) {
        if (!responded.compareAndSet(false, true)) return
        pendingResponses -= responded

        runCatching { cancelCall() }
            .onFailure { Log.e(TAG, "cancelCall a échoué", it) }

        // On démarre un Foreground Service ultra-court : lui seul peut ouvrir
        // une activité depuis l'arrière-plan sur Android 12+.
        runCatching { PauseForegroundService.start(this, number) }
            .onFailure { Log.w(TAG, "Échec démarrage foreground service", it) }

        // Ré-armement opportuniste : l'app est déjà réveillée par Telecom, autant
        // en profiter pour relancer le veilleur si le système l'avait tué.
        runCatching { CoolDownWatchdogService.startIfNeeded(this) }
            .onFailure { Log.w(TAG, "Échec du ré-armement du watchdog", it) }

        // Notification de secours : appel synchrone volontaire. Le service de
        // redirection est détruit juste après cancelCall(), donc un launch()
        // sur serviceScope serait annulé avant de s'exécuter.
        runCatching { notifier.showBlockedCall(number) }
            .onFailure { Log.w(TAG, "Échec notification de blocage", it) }
    }

    private companion object {
        const val TAG = "CoolDownRedirection"
        const val DECISION_TIMEOUT_MS = 2_000L
    }
}
