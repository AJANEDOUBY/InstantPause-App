package com.instantpause.app.domain.security

/**
 * Temps écoulé depuis le démarrage du téléphone, insensible au changement
 * d'heure. En production : `SystemClock.elapsedRealtime()` (voir `SecurityModule`).
 *
 * Gardé hors d'Android pour que le verrou reste testable sans appareil.
 */
fun interface MonotonicClock {
    fun elapsedMillis(): Long
}
