package com.cooldown.app.ui.main

/**
 * Étape d'onboarding où se trouve l'utilisateur.
 *
 * Extraite de `MainActivity` pour être testable : l'ordre des étapes est le garde-fou
 * lui-même. Tant que l'administrateur d'appareil n'est pas actif, ou que l'app n'a pas
 * le droit de raccrocher, on ne passe pas au rôle Telecom — donc pas à l'app utilisable.
 */
internal enum class OnboardingStage {
    LOADING,
    SETUP_CODE,
    PROTECT_UNINSTALL,
    HANGUP_PERMISSION,
    ACTIVATE_ROLE,
    MAIN
}

/**
 * @param hasCode un code de protection existe déjà
 * @param roleHeld l'app détient `ROLE_CALL_REDIRECTION`
 * @param adminActive l'administrateur anti-désinstallation est actif
 * @param guardAcknowledged l'étape anti-désinstallation a déjà été franchie un jour
 *   (`null` tant que la préférence n'est pas chargée)
 * @param canHangUp l'app peut couper un appel (`ANSWER_PHONE_CALLS` accordée)
 *
 * Les deux étapes de protection sont **volontairement** placées avant le rôle : elles
 * doivent être franchie à l'installation, pas découvertes plus tard dans un onglet.
 *
 * L'étape anti-désinstallation est aussi imposée aux installations existantes : un
 * utilisateur qui détenait déjà le rôle Telecom mais jamais activé l'administrateur
 * (`guardAcknowledged == false`) la voit une fois.
 *
 * Le raccrochage, lui, est vérifié **en permanence** : sans `ANSWER_PHONE_CALLS`, les
 * 3 minutes ne sont pas garanties, donc l'app ne s'ouvre pas. Retirer la permission
 * depuis les réglages Android ramène ici au lancement suivant — c'est voulu.
 */
internal fun onboardingStage(
    loading: Boolean,
    hasCode: Boolean,
    roleHeld: Boolean,
    adminActive: Boolean,
    guardAcknowledged: Boolean?,
    canHangUp: Boolean
): OnboardingStage = when {
    loading || guardAcknowledged == null -> OnboardingStage.LOADING
    !hasCode -> OnboardingStage.SETUP_CODE
    !adminActive && (!roleHeld || !guardAcknowledged) -> OnboardingStage.PROTECT_UNINSTALL
    !canHangUp -> OnboardingStage.HANGUP_PERMISSION
    !roleHeld -> OnboardingStage.ACTIVATE_ROLE
    else -> OnboardingStage.MAIN
}
