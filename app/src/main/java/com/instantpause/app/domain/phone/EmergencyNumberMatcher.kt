package com.instantpause.app.domain.phone

import com.google.i18n.phonenumbers.ShortNumberInfo

/**
 * Détection des numéros d'urgence, isolée de tout contexte Android pour être testable
 * sur la JVM.
 *
 * C'est la seule règle de l'application dont un échec est irréversible : dans le doute,
 * on autorise. Les numéros courts connus sont donc doublés d'une liste explicite, et
 * [ShortNumberInfo] couvre les variantes locales des régions fournies.
 */
object EmergencyNumberMatcher {

    /** Numéros qui ne doivent jamais dépendre de la région détectée. */
    val HARDCODED: Set<String> = setOf("112", "15", "17", "18", "19", "177")

    private val shortNumberInfo: ShortNumberInfo = ShortNumberInfo.getInstance()

    /** Retire espaces, tirets et parenthèses sans dépendre d'`android.telephony`. */
    fun clean(raw: String): String =
        raw.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }

    fun isEmergency(
        raw: String,
        regions: List<String>,
        fallbackRegions: List<String> = emptyList()
    ): Boolean {
        val cleaned = clean(raw)
        if (cleaned.isEmpty()) return false
        if (cleaned in HARDCODED) return true
        return (regions + fallbackRegions).distinct()
            .filter { it.length == 2 }
            .any { shortNumberInfo.isEmergencyNumber(cleaned, it) }
    }
}
