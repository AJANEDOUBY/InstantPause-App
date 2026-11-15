package com.instantpause.app.domain.language

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Le choix de langue passe par une étiquette Android (`fr-FR`, `ES`, `de`…).
 * Une langue non proposée doit retomber sur « langue du téléphone », jamais sur
 * l'anglais : sinon l'application changerait de langue sans que l'utilisateur
 * l'ait demandé.
 */
class LanguageTagsTest {

    @Test
    fun `une etiquette vide signifie langue du telephone`() {
        assertEquals(LanguageTags.SYSTEM, LanguageTags.normalize(""))
        assertEquals(LanguageTags.SYSTEM, LanguageTags.normalize(null))
        assertEquals(LanguageTags.SYSTEM, LanguageTags.normalize("   "))
    }

    @Test
    fun `les langues proposees sont reconnues avec leur region`() {
        assertEquals("fr", LanguageTags.normalize("fr-FR"))
        assertEquals("es", LanguageTags.normalize("es-419"))
        assertEquals("en", LanguageTags.normalize("en-US"))
    }

    @Test
    fun `la casse et le separateur ne changent pas le resultat`() {
        assertEquals("fr", LanguageTags.normalize("FR"))
        assertEquals("fr", LanguageTags.normalize("fr_FR"))
        assertEquals("es", LanguageTags.normalize("ES"))
    }

    @Test
    fun `une langue non traduite retombe sur la langue du telephone`() {
        assertEquals(LanguageTags.SYSTEM, LanguageTags.normalize("de-DE"))
        assertEquals(LanguageTags.SYSTEM, LanguageTags.normalize("ja"))
        assertEquals(LanguageTags.SYSTEM, LanguageTags.normalize("ar"))
    }

    @Test
    fun `les trois langues livrees sont annoncees`() {
        assertEquals(listOf("en", "fr", "es"), LanguageTags.supported)
    }
}
