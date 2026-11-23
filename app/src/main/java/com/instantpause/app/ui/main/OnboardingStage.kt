package com.instantpause.app.ui.main

/**
 * Étape d'onboarding où se trouve l'utilisateur.
 *
 * Extraite de `MainActivity` pour être testable : l'ordre des étapes est le garde-fou
 * lui-même. Tant que l'app n'a pas le droit de raccrocher, on ne passe pas au rôle
 * Telecom — donc pas à l'app utilisable.
 */
internal enum class OnboardingStage {
    LOADING,
    SETUP_CODE,
    HANGUP_PERMISSION,
    ACTIVATE_ROLE,
    FULL_SCREEN_INTENT,
    MAIN
}

/**
 * @param hasCode un code de protection existe déjà
 * @param roleHeld l'app détient `ROLE_CALL_REDIRECTION`
 * @param canHangUp l'app peut couper un appel (`ANSWER_PHONE_CALLS` accordée)
 * @param canUseFullScreenIntent l'écran de pause peut s'afficher par-dessus le
 *   verrouillage (toujours vrai avant Android 14)
 * @param fullScreenAcknowledged l'étape « plein écran » a déjà été vue (`null` tant que
 *   la préférence n'est pas chargée)
 *
 * Le raccrochage est vérifié **en permanence** : sans `ANSWER_PHONE_CALLS`, les
 * 3 minutes ne sont pas garanties, donc l'app ne s'ouvre pas. Retirer la permission
 * depuis les réglages Android ramène ici au lancement suivant — c'est voulu.
 *
 * Le plein écran, enfin, est proposé **une seule fois** : refusé puis acquitté, il ne
 * revient plus (la pause reste accessible par la notification). Sans ce drapeau,
 * l'écran de réglages s'ouvrirait à chaque lancement — plus intrusif que le problème.
 */
internal fun onboardingStage(
    loading: Boolean,
    hasCode: Boolean,
    roleHeld: Boolean,
    canHangUp: Boolean,
    canUseFullScreenIntent: Boolean,
    fullScreenAcknowledged: Boolean?
): OnboardingStage = when {
    loading || fullScreenAcknowledged == null ->
        OnboardingStage.LOADING
    !hasCode -> OnboardingStage.SETUP_CODE
    !canHangUp -> OnboardingStage.HANGUP_PERMISSION
    !roleHeld -> OnboardingStage.ACTIVATE_ROLE
    !canUseFullScreenIntent && !fullScreenAcknowledged -> OnboardingStage.FULL_SCREEN_INTENT
    else -> OnboardingStage.MAIN
}
