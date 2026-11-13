package com.instantpause.app.domain.pause

/**
 * Règles du message affiché sur l'écran de pause.
 *
 * Volontairement sans dépendance Android : la phrase est écrite par l'utilisateur
 * **à froid**, depuis l'onglet Sécurité, puis relue au moment de l'impulsion.
 * Une phrase vide ou composée uniquement d'espaces ne doit jamais effacer l'écran :
 * on retombe alors sur [DEFAULT].
 */
object PauseMessagePolicy {

    /** Message proposé tant que l'utilisateur n'a rien personnalisé. */
    const val DEFAULT = "Cet appel peut attendre demain."

    /** Deux lignes suffisent ; au-delà, l'écran de pause devient illisible. */
    const val MAX_LENGTH = 120

    /**
     * Nettoie une saisie : espaces multiples réduits, bords rognés, longueur bornée.
     * Une saisie vide renvoie [DEFAULT] plutôt qu'une chaîne vide.
     */
    fun normalize(raw: String): String =
        raw.trim()
            .replace(Regex("\\s+"), " ")
            .take(MAX_LENGTH)
            .trim()
            .ifBlank { DEFAULT }
}
