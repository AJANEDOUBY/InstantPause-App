package com.instantpause.app.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * L'ordre des étapes est la protection elle-même. Le verrou et le rôle Telecom
 * restent obligatoires ; le raccrochage à 3 minutes et le plein écran sont
 * proposés une fois puis acquittés. Ces tests verrouillent cet ordre.
 */
class OnboardingStageTest {

    private fun stage(
        loading: Boolean = false,
        hasCode: Boolean = true,
        roleHeld: Boolean = false,
        canHangUp: Boolean = true,
        hangupAcknowledged: Boolean? = true,
        canUseFullScreenIntent: Boolean = true,
        fullScreenAcknowledged: Boolean? = false
    ) = onboardingStage(
        loading,
        hasCode,
        roleHeld,
        canHangUp,
        hangupAcknowledged,
        canUseFullScreenIntent,
        fullScreenAcknowledged
    )

    @Test
    fun `pas de code, pas d'app`() {
        assertEquals(OnboardingStage.SETUP_CODE, stage(hasCode = false))
    }

    @Test
    fun `tant que les preferences optionnelles sont inconnues, on charge`() {
        assertEquals(OnboardingStage.LOADING, stage(fullScreenAcknowledged = null))
        assertEquals(OnboardingStage.LOADING, stage(hangupAcknowledged = null))
    }

    @Test
    fun `le raccrochage vient avant le role Telecom`() {
        assertEquals(
            OnboardingStage.HANGUP_PERMISSION,
            stage(
                roleHeld = false,
                canHangUp = false,
                hangupAcknowledged = false
            )
        )
    }

    @Test
    fun `tout est en place, on entre dans l'app`() {
        assertEquals(
            OnboardingStage.MAIN,
            stage(roleHeld = true, canHangUp = true, fullScreenAcknowledged = true)
        )
    }

    // === Raccrochage à 3 minutes : proposé, pas imposé ===

    @Test
    fun `sans permission et jamais vu, l'etape est proposee`() {
        assertEquals(
            OnboardingStage.HANGUP_PERMISSION,
            stage(
                roleHeld = true,
                canHangUp = false,
                hangupAcknowledged = false
            )
        )
    }

    @Test
    fun `permission refusee puis acquittee, l'app s'ouvre quand meme`() {
        assertEquals(
            OnboardingStage.ACTIVATE_ROLE,
            stage(
                roleHeld = false,
                canHangUp = false,
                hangupAcknowledged = true
            )
        )
    }

    @Test
    fun `permission accordee, l'etape est inutile`() {
        assertEquals(
            OnboardingStage.ACTIVATE_ROLE,
            stage(
                roleHeld = false,
                canHangUp = true,
                hangupAcknowledged = false
            )
        )
    }

    // === Plein écran : proposé une fois, non bloquant ===

    @Test
    fun `plein ecran refuse et jamais vu, l'etape est proposee`() {
        assertEquals(
            OnboardingStage.FULL_SCREEN_INTENT,
            stage(
                roleHeld = true,
                canHangUp = true,
                canUseFullScreenIntent = false,
                fullScreenAcknowledged = false
            )
        )
    }

    @Test
    fun `plein ecran refuse mais deja vu, on n'insiste plus`() {
        assertEquals(
            OnboardingStage.MAIN,
            stage(
                roleHeld = true,
                canHangUp = true,
                canUseFullScreenIntent = false,
                fullScreenAcknowledged = true
            )
        )
    }

    @Test
    fun `plein ecran accorde, l'etape est inutile`() {
        assertEquals(
            OnboardingStage.MAIN,
            stage(
                roleHeld = true,
                canHangUp = true,
                canUseFullScreenIntent = true,
                fullScreenAcknowledged = false
            )
        )
    }

    @Test
    fun `le plein ecran vient apres le role Telecom`() {
        assertEquals(
            OnboardingStage.ACTIVATE_ROLE,
            stage(
                roleHeld = false,
                canHangUp = true,
                canUseFullScreenIntent = false,
                fullScreenAcknowledged = false
            )
        )
    }
}
