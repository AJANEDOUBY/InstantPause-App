package com.instantpause.app.domain.language

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Langues proposées à l'utilisateur, sous la forme utilisée par Android.
 *
 * Liste volontairement tenue à part du tableau de bord : `LanguageTags` ne
 * dépend ni d'Android ni de `R`, donc elle se teste sur la JVM. Toute nouvelle
 * traduction doit être ajoutée ici **et** dans `res/xml/locales_config.xml`.
 */
object LanguageTags {
    /** Valeur vide = suivre la langue du téléphone. */
    const val SYSTEM = ""

    val supported = listOf("en", "fr", "es")

    /**
     * Réduit une étiquette quelconque (`fr-FR`, `ES`, `de`, `null`) à une langue
     * proposée, ou renvoie [SYSTEM].
     *
     * Une langue non proposée ne doit jamais être imposée : l'utilisateur
     * retomberait sur l'anglais sans comprendre pourquoi.
     */
    fun normalize(tag: String?): String =
        tag?.trim()?.lowercase()?.substringBefore('-')?.substringBefore('_')
            ?.takeIf { it in supported } ?: SYSTEM
}

/**
 * Choix de la langue de l'application, sans passer par les réglages Android.
 *
 * S'appuie sur `LocaleManager` (Android 13+), qui applique la langue à **tous**
 * les composants de l'app : écrans, notifications déjà programmées, services au
 * premier plan. Sur Android 12 et antérieur, il n'existe pas d'équivalent
 * système ; l'application suit alors la langue du téléphone, ce que l'écran
 * explique plutôt que de promettre un bouton sans effet.
 */
@Singleton
class AppLanguageController @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val manager: LocaleManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
        } else {
            null
        }

    /** Faux avant Android 13 : l'écran affiche alors une explication. */
    fun isAvailable(): Boolean = manager != null

    /** Langue choisie, ou [LanguageTags.SYSTEM] si l'app suit le téléphone. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun currentTag(): String {
        val locales = manager?.applicationLocales ?: return LanguageTags.SYSTEM
        if (locales.isEmpty) return LanguageTags.SYSTEM
        return LanguageTags.normalize(locales[0]?.language)
    }

    /** Lit la langue courante sans exiger le niveau d'API (pour l'affichage). */
    fun safeCurrentTag(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) currentTag()
        else LanguageTags.SYSTEM

    /**
     * Langue du téléphone, indépendante du choix fait pour cette application.
     *
     * `Resources.getSystem()` ne convient pas : il porte la configuration du
     * processus, donc la langue choisie pour l'app. `systemLocales` est la seule
     * source fiable, et elle est aussi disponible dès Android 13.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun systemLocaleTag(): String {
        val first = manager?.systemLocales?.get(0) ?: return ""
        return first.language
    }

    /** Version utilisable partout, pour l'affichage. */
    fun safeSystemLocaleTag(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            systemLocaleTag()
        } else {
            // Avant Android 13, il n'existe pas de langue par application :
            // la langue courante est celle du téléphone.
            java.util.Locale.getDefault().language
        }

    /**
     * Applique la langue. Android recrée l'application de lui-même : rien à
     * redémarrer à la main, et le choix survit à un redémarrage du téléphone.
     */
    fun setLanguage(tag: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val api = manager ?: return
        val normalized = LanguageTags.normalize(tag)
        val locales = if (normalized == LanguageTags.SYSTEM) {
            android.os.LocaleList.getEmptyLocaleList()
        } else {
            android.os.LocaleList.forLanguageTags(normalized)
        }
        runCatching { api.applicationLocales = locales }
    }
}
