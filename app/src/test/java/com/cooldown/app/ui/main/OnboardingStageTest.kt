package com.cooldown.app.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * L'ordre des étapes est la protection elle-même : si l'étape anti-désinstallation
 * ou celle du raccrochage peut être sautée, un utilisateur arrive dans l'app avec une
 * désinstallation en trois taps et des appels qui durent une heure. Ces tests
 * verrouillent cet ordre.
 */
class OnboardingStageTest {

    private fun stage(
        loading: Boolean = false,
        hasCode: Boolean = true,
        roleHeld: Boolean = false,
        adminActive: Boolean = false,
        guardAcknowledged: Boolean? = false,
        canHangUp: Boolean = true
    ) = onboardingStage(
        loading, hasCode, roleHeld, adminActive, guardAcknowledged, canHangUp
    )

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

    // === Raccrochage à 3 minutes : obligatoire, pas une option ===

    @Test
    fun `sans permission de raccrocher, l'app ne s'ouvre pas`() {
        assertEquals(
            OnboardingStage.HANGUP_PERMISSION,
            stage(
                roleHeld = true,
                adminActive = true,
                guardAcknowledged = true,
                canHangUp = false
            )
        )
    }

    @Test
    fun `le raccrochage vient avant le role Telecom`() {
        assertEquals(
            OnboardingStage.HANGUP_PERMISSION,
            stage(
                hasCode = true,
                roleHeld = false,
                adminActive = true,
                guardAcknowledged = true,
                canHangUp = false
            )
        )
    }

    @Test
    fun `l'anti-desinstallation reste prioritaire sur le raccrochage`() {
        assertEquals(
            OnboardingStage.PROTECT_UNINSTALL,
            stage(adminActive = false, guardAcknowledged = false, canHangUp = false)
        )
    }

    @Test
    fun `permission de raccrocher retiree apres coup, l'app redemande`() {
        assertEquals(
            OnboardingStage.HANGUP_PERMISSION,
            stage(roleHeld = true, adminActive = true, guardAcknowledged = true, canHangUp = false)
        )
    }

    @Test
    fun `permission accordee, on entre dans l'app`() {
        assertEquals(
            OnboardingStage.MAIN,
            stage(roleHeld = true, adminActive = true, guardAcknowledged = true, canHangUp = true)
        )
    }
}
