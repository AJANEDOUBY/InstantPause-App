package com.instantpause.app.domain.pause

/**
 * Règles du message affiché sur l'écran de pause.
 *
 * Volontairement sans dépendance Android : la phrase est écrite par l'utilisateur
 * **à froid**, depuis l'onglet Sécurité, puis relue au moment de l'impulsion.
 *
 * La phrase par défaut vit dans `strings.xml` (`pause_message_default`) et non
 * ici : elle doit suivre la langue de l'appareil. Une saisie vide est donc
 * conservée telle quelle, et remplacée au moment de l'affichage.
 */
object PauseMessagePolicy {

    /** Deux lignes suffisent ; au-delà, l'écran de pause devient illisible. */
    const val MAX_LENGTH = 120

    /**
     * Nettoie une saisie : espaces multiples réduits, bords rognés, longueur bornée.
     * Une saisie vide renvoie une chaîne vide : l'appelant affiche alors la
     * phrase par défaut de sa langue.
     */
    fun normalize(raw: String): String =
        raw.trim()
            .replace(Regex("\\s+"), " ")
            .take(MAX_LENGTH)
            .trim()
}
