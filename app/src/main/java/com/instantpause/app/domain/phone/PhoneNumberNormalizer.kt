package com.instantpause.app.domain.phone

import android.content.Context
import android.telecom.PhoneAccountHandle
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.ShortNumberInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneNumberNormalizer @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PhoneNumberAnalyzer {
    private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()
    private val shortNumberInfo: ShortNumberInfo = ShortNumberInfo.getInstance()

    override fun resolveRegions(handle: PhoneAccountHandle?): List<String> {
        val baseTm = context.getSystemService(TelephonyManager::class.java)
        val simTm = handle?.let { runCatching { baseTm?.createForPhoneAccountHandle(it) }.getOrNull() }
        val source = simTm ?: baseTm
        return listOfNotNull(source?.networkCountryIso, source?.simCountryIso)
            .map { it.trim().uppercase(Locale.ROOT) }
            .filter { it.length == 2 && it in phoneUtil.supportedRegions }
            .distinct()
            .ifEmpty { listOf(defaultRegion()) }
    }

    /**
     * Région de secours quand Telecom ne fournit pas de compte : SIM, réseau,
     * locale système, puis [DEFAULT_REGION].
     */
    override fun defaultRegion(): String {
        val tm = context.getSystemService(TelephonyManager::class.java)
        return sequenceOf(tm?.simCountryIso, tm?.networkCountryIso, Locale.getDefault().country)
            .filterNotNull()
            .map { it.trim().uppercase(Locale.ROOT) }
            .firstOrNull { it.length == 2 && it in phoneUtil.supportedRegions }
            ?: DEFAULT_REGION
    }

    override fun toE164(raw: String, region: String): String? {
        val cleaned = clean(raw) ?: return null
        return try {
            val parsed = phoneUtil.parse(cleaned, region)
            if (phoneUtil.isValidNumber(parsed)) phoneUtil.format(parsed, PhoneNumberFormat.E164) else null
        } catch (e: NumberParseException) {
            null
        }
    }

    override fun candidates(raw: String, regions: List<String>): List<String> {
        val cleaned = clean(raw) ?: return emptyList()
        val primary = regions.mapNotNull { toE164(cleaned, it) }.distinct()
        if (primary.isNotEmpty()) return primary
        return SUPPORTED_REGIONS.mapNotNull { toE164(cleaned, it) }.distinct()
    }

    override fun isEmergency(raw: String, regions: List<String>): Boolean {
        val cleaned = clean(raw) ?: return false
        if (cleaned in HARDCODED_EMERGENCY_NUMBERS) return true
        return (regions + SUPPORTED_REGIONS).distinct()
            .any { shortNumberInfo.isEmergencyNumber(cleaned, it) }
    }

    private fun clean(raw: String): String? =
        PhoneNumberUtils.stripSeparators(raw)?.takeIf { it.isNotEmpty() }

    companion object {
        const val DEFAULT_REGION = "MA"
        val SUPPORTED_REGIONS: List<String> = listOf("MA", "FR")
        private val HARDCODED_EMERGENCY_NUMBERS = setOf("112", "15", "17", "18", "19", "177")
    }
}