package com.instantpause.app.domain.usecase

import com.instantpause.app.domain.phone.PhoneNumberAnalyzer
import com.instantpause.app.domain.protection.ProtectionReason
import com.instantpause.app.domain.protection.ProtectionWindow
import com.instantpause.app.domain.repository.WhitelistRepository
import java.time.ZonedDateTime
import javax.inject.Inject

enum class AllowReason { PROTECTION_INACTIVE, EMERGENCY_NUMBER, WHITELISTED, FAIL_OPEN }

sealed interface CallDecision {
    val normalizedNumber: String?

    data class Allow(
        val reason: AllowReason,
        override val normalizedNumber: String?
    ) : CallDecision

    data class Block(
        val reason: ProtectionReason,
        override val normalizedNumber: String?
    ) : CallDecision
}

class ShouldBlockCallUseCase @Inject constructor(
    private val protection: ProtectionWindow,
    private val whitelist: WhitelistRepository,
    private val normalizer: PhoneNumberAnalyzer
) {
    suspend operator fun invoke(
        rawNumber: String,
        regions: List<String>,
        now: ZonedDateTime = ZonedDateTime.now()
    ): CallDecision {
        val effectiveRegions = regions.ifEmpty { listOf(normalizer.defaultRegion()) }

        // 1. Urgences : toujours autorisées
        if (normalizer.isEmergency(rawNumber, effectiveRegions)) {
            return CallDecision.Allow(AllowReason.EMERGENCY_NUMBER, null)
        }

        val reason = protection.activeReason(now)
        if (reason == ProtectionReason.NONE) {
            return CallDecision.Allow(AllowReason.PROTECTION_INACTIVE, null)
        }

        // 3. Whitelist (comparaison E.164)
        val candidates = normalizer.candidates(rawNumber, effectiveRegions)
        if (whitelist.isWhitelisted(candidates)) {
            return CallDecision.Allow(AllowReason.WHITELISTED, candidates.firstOrNull())
        }

        // 4. Sinon : bloqué
        return CallDecision.Block(reason, candidates.firstOrNull())
    }
}
