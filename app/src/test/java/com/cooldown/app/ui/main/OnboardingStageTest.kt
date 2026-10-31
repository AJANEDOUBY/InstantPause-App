package com.cooldown.app.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * L'ordre des étapes est la protection elle-même : si l'étape anti-désinstallation
 * peut être sautée, un utilisateur arrive dans l'app avec une désinstallation en
 * trois taps. Ces tests verrouillent cet ordre.
 */
class OnboardingStageTest {

    private fun stage(
        loading: Boolean = false,
        hasCode: Boolean = true,
        roleHeld: Boolean = false,
        adminActive: Boolean = false,
        guardAcknowledged: Boolean? = false
    ) = onboardingStage(loading, hasCode, roleHeld, adminActive, guardAcknowledged)

    @Test
    fun `pas de code, pas d'app`() {
        assertEquals(OnboardingStage.SETUP_CODE, stage(hasCode = false))
    }

    @Test
    fun `la protection anti-desinstallation passe avant le role Telecom`() {
        assertEquals(
            OnboardingStage.PROTECT_UNINSTALL,
            stage(roleHeld = false, adminActive = false)
        )
    }

    @Test
    fun `une installation existante sans administrateur est remise dans le droit chemin`() {
        assertEquals(
            OnboardingStage.PROTECT_UNINSTALL,
            stage(roleHeld = true, adminActive = false, guardAcknowledged = false)
        )
    }

    @Test
    fun `ecran de chargement tant que la preference anti-desinstallation est inconnue`() {
        assertEquals(OnboardingStage.LOADING, stage(guardAcknowledged = null))
    }

    @Test
    fun `etape franchie une fois, l'administrateur peut etre retire sans revenir ici`() {
        val s = stage(roleHeld = true, adminActive = false, guardAcknowledged = true)
        assertEquals(OnboardingStage.MAIN, s)
    }

    @Test
    fun `administrateur actif mais role absent, on demande le role`() {
        assertEquals(
            OnboardingStage.ACTIVATE_ROLE,
            stage(roleHeld = false, adminActive = true, guardAcknowledged = false)
        )
    }

    @Test
    fun `tout est en place, on entre dans l'app`() {
        assertEquals(
            OnboardingStage.MAIN,
            stage(roleHeld = true, adminActive = true, guardAcknowledged = true)
        )
    }
}
