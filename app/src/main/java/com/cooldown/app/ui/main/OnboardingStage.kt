package com.cooldown.app.ui.main

/**
 * Étape d'onboarding où se trouve l'utilisateur.
 *
 * Extraite de `MainActivity` pour être testable : l'ordre des étapes est le garde-fou
 * lui-même. Tant que l'administrateur d'appareil n'est pas actif, on ne passe pas au
 * rôle Telecom — donc pas à l'app utilisable.
 */
internal enum class OnboardingStage {
    LOADING,
    SETUP_CODE,
    PROTECT_UNINSTALL,
    ACTIVATE_ROLE,
    MAIN
}

/**
 * @param hasCode un code de protection existe déjà
 * @param roleHeld l'app détient `ROLE_CALL_REDIRECTION`
 * @param adminActive l'administrateur anti-désinstallation est actif
 * @param guardAcknowledged l'étape anti-désinstallation a déjà été franchie un jour
 *   (`null` tant que la préférence n'est pas chargée)
 *
 * L'étape anti-désinstallation est **volontairement** placée avant le rôle : elle doit
 * être franchie à l'installation, pas découverte plus tard dans un onglet.
 *
 * Elle est aussi imposée aux installations existantes : un utilisateur qui détenait déjà
 * le rôle Telecom mais jamais activé l'administrateur (`guardAcknowledged == false`) la
 * voit une fois. Une fois franchie, elle ne revient plus : retirer l'administrateur avec
 * son code depuis l'onglet Sécurité reste possible et n'enferme pas l'utilisateur.
 */
internal fun onboardingStage(
    loading: Boolean,
    hasCode: Boolean,
    roleHeld: Boolean,
    adminActive: Boolean,
    guardAcknowledged: Boolean?
): OnboardingStage = when {
    loading || guardAcknowledged == null -> OnboardingStage.LOADING
    !hasCode -> OnboardingStage.SETUP_CODE
    !adminActive && (!roleHeld || !guardAcknowledged) -> OnboardingStage.PROTECT_UNINSTALL
    !roleHeld -> OnboardingStage.ACTIVATE_ROLE
    else -> OnboardingStage.MAIN
}
