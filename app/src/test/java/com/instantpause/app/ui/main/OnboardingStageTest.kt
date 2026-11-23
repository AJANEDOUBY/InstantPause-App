package com.instantpause.app.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * L'ordre des étapes est la protection elle-même : si le raccrochage peut être sauté,
 * un utilisateur arrive dans l'app avec des appels qui durent une heure. Ces tests
 * verrouillent cet ordre.
 */
class OnboardingStageTest {

    private fun stage(
        loading: Boolean = false,
        hasCode: Boolean = true,
        roleHeld: Boolean = false,
        canHangUp: Boolean = true,
        canUseFullScreenIntent: Boolean = true,
        fullScreenAcknowledged: Boolean? = false
    ) = onboardingStage(
        loading,
        hasCode,
        roleHeld,
        canHangUp,
        canUseFullScreenIntent,
        fullScreenAcknowledged
    )

    @Test
    fun `pas de code, pas d'app`() {
        assertEquals(OnboardingStage.SETUP_CODE, stage(hasCode = false))
    }

    @Test
    fun `le raccrochage vient avant le role Telecom`() {
        assertEquals(
            OnboardingStage.HANGUP_PERMISSION,
            stage(roleHeld = false, canHangUp = false)
        )
    }

    @Test
    fun `ecran de chargement tant que la preference plein ecran est inconnue`() {
        assertEquals(OnboardingStage.LOADING, stage(fullScreenAcknowledged = null))
    }

    @Test
    fun `tout est en place, on entre dans l'app`() {
        assertEquals(
            OnboardingStage.MAIN,
            stage(roleHeld = true, canHangUp = true, fullScreenAcknowledged = true)
        )
    }

    // === Raccrochage à 3 minutes : obligatoire, pas une option ===

    @Test
    fun `sans permission de raccrocher, l'app ne s'ouvre pas`() {
        assertEquals(
            OnboardingStage.HANGUP_PERMISSION,
            stage(
                roleHeld = true,
                canHangUp = false
            )
        )
    }

    @Test
    fun `permission de raccrocher retiree apres coup, l'app redemande`() {
        assertEquals(
            OnboardingStage.HANGUP_PERMISSION,
            stage(roleHeld = true, canHangUp = false)
        )
    }

    @Test
    fun `permission accordee, on entre dans l'app`() {
        assertEquals(
            OnboardingStage.ACTIVATE_ROLE,
            stage(roleHeld = false, canHangUp = true)
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
