package com.cooldown.app.system

// (déjà dans le même package, pas besoin d'import)
import android.content.Intent
import android.net.Uri
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.util.Log
import com.cooldown.app.domain.model.BlockStatus
import com.cooldown.app.domain.phone.PhoneNumberNormalizer
import com.cooldown.app.domain.repository.BlockedLogRepository
import com.cooldown.app.domain.usecase.AllowReason
import com.cooldown.app.domain.usecase.CallDecision
import com.cooldown.app.domain.usecase.ShouldBlockCallUseCase
import com.cooldown.app.ui.pause.PauseActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Intercepte les appels sortants via l'API Telecom (rôle ROLE_CALL_REDIRECTION).
 *
 * Principes :
 *  - Fail-open : toute erreur ou timeout laisse passer l'appel.
 *  - Une seule réponse à Telecom par appel ([responded]).
 *  - Les urgences sont court-circuitées avant toute I/O.
 */
@AndroidEntryPoint
class CoolDownCallRedirectionService : CallRedirectionService() {

    @Inject lateinit var shouldBlock: ShouldBlockCallUseCase
    @Inject lateinit var logs: BlockedLogRepository
    @Inject lateinit var normalizer: PhoneNumberNormalizer
    @Inject lateinit var notifier: ProtectionAlertNotifier

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean
    ) {
        var responded = false

        try {
            // sip:, voicemail: et autres : ce ne sont pas des appels vers un numéro
            if (handle.scheme != PhoneAccount.SCHEME_TEL) {
                placeCallUnmodified()
                responded = true
                return
            }

            val rawNumber = handle.schemeSpecificPart.orEmpty()
            val regions = normalizer.resolveRegions(initialPhoneAccount)

            // Urgences : court-circuit avant toute lecture en base
            if (normalizer.isEmergency(rawNumber, regions)) {
                placeCallUnmodified()
                responded = true
                return
            }

            val decision = blocking(DECISION_TIMEOUT_MS) { shouldBlock(rawNumber, regions) }
                ?: CallDecision.Allow(AllowReason.FAIL_OPEN, null)

            when (decision) {
                is CallDecision.Allow -> {
                    if (decision.reason == AllowReason.WHITELISTED) {
                        blocking(LOG_TIMEOUT_MS) {
                            logs.log(
                                decision.normalizedNumber ?: rawNumber,
                                BlockStatus.ALLOWED_WHITELIST
                            )
                        }
                    }
                    placeCallUnmodified()
                    responded = true
                }

                is CallDecision.Block -> {
                    val number = decision.normalizedNumber ?: rawNumber
                    // Journaliser AVANT cancelCall : le process peut être réduit juste après
                    blocking(LOG_TIMEOUT_MS) { logs.log(number, BlockStatus.BLOCKED) }
                    cancelCall()
                    responded = true

                    // On démarre un Foreground Service ultra-court : lui seul peut ouvrir
// une activité depuis l'arrière-plan sur Android 12+.
                    runCatching { PauseForegroundService.start(this, number) }
                        .onFailure { Log.w(TAG, "Échec démarrage foreground service", it) }

// Notification de secours (utile si le Foreground Service est bloqué).
                    serviceScope.launch {
                        runCatching { notifier.showBlockedCall(number) }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Erreur inattendue, appel laissé passer", t)
            if (!responded) runCatching { placeCallUnmodified() }
        }
    }

    /**
     * Exécute [block] en bloquant le thread appelant, avec un timeout.
     * Le travail s'exécute sur Dispatchers.IO.
     */
    private fun <T> blocking(timeoutMs: Long, block: suspend () -> T): T? =
        runCatching {
            runBlocking(Dispatchers.IO) { withTimeoutOrNull(timeoutMs) { block() } }
        }
            .onFailure { Log.e(TAG, "Échec évaluation", it) }
            .getOrNull()

    private companion object {
        const val TAG = "CoolDownRedirection"
        const val DECISION_TIMEOUT_MS = 2_000L
        const val LOG_TIMEOUT_MS = 1_000L
    }
}