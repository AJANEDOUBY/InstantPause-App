package com.cooldown.app.domain.phone

import android.telecom.PhoneAccountHandle

/**
 * Contrat de normalisation/analyse des numéros utilisé par le domaine.
 * Permet de tester [com.cooldown.app.domain.usecase.ShouldBlockCallUseCase]
 * sans dépendance à Android.
 */
interface PhoneNumberAnalyzer {
    fun resolveRegions(handle: PhoneAccountHandle?): List<String>
    fun toE164(raw: String, region: String): String?
    fun candidates(raw: String, regions: List<String>): List<String>
    fun isEmergency(raw: String, regions: List<String>): Boolean
    fun defaultRegion(): String
}
