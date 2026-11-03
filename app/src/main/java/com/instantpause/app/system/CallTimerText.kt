package com.instantpause.app.system

import java.util.Locale

/**
 * Mise en forme du minuteur, isolée du service pour rester testable sans Android.
 *
 * Un chronomètre nu (« 0:58 / 3:00 ») ne dit pas ce qu'il mesure : la pastille affiche
 * donc toujours le préfixe [PREFIX] au-dessus du décompte, et la notification le répète
 * en clair.
 */
internal object CallTimerText {

    /** Étiquette affichée au-dessus du décompte, dans la pastille. */
    const val PREFIX = "Temps restant"

    /** Ex. « 2:59 / 3:00 ». */
    fun countdown(leftSeconds: Int, limitSeconds: Int): String =
        "${format(leftSeconds)} / ${format(limitSeconds)}"

    /** Ex. « Temps restant : 2:59 / 3:00 », pour la notification. */
    fun notificationLine(leftSeconds: Int, limitSeconds: Int): String =
        "$PREFIX : ${countdown(leftSeconds, limitSeconds)}"

    /** Ex. 179 -> « 2:59 ». Les secondes sont bornées à zéro côté appelant. */
    fun format(totalSeconds: Int): String =
        String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
