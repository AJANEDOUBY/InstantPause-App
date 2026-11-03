package com.instantpause.app.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PassphrasePolicyTest {

    @Test
    fun `les espaces de bord sont ignores`() {
        assertEquals("abc123", PassphrasePolicy.normalize("  abc123  "))
        assertTrue(PassphrasePolicy.isValid("  abc123  "))
    }

    @Test
    fun `trop court est refuse`() {
        val violations = PassphrasePolicy.violations("ab12")
        assertTrue(PassphrasePolicy.Violation.TOO_SHORT in violations)
    }

    @Test
    fun `il faut au moins une lettre`() {
        val violations = PassphrasePolicy.violations("123456")
        assertTrue(PassphrasePolicy.Violation.MISSING_LETTER in violations)
        assertFalse(PassphrasePolicy.Violation.MISSING_DIGIT in violations)
    }

    @Test
    fun `il faut au moins un chiffre`() {
        val violations = PassphrasePolicy.violations("abcdef")
        assertTrue(PassphrasePolicy.Violation.MISSING_DIGIT in violations)
        assertFalse(PassphrasePolicy.Violation.MISSING_LETTER in violations)
    }

    @Test
    fun `un code alphanumerique valide passe`() {
        assertTrue(PassphrasePolicy.isValid("Cooldown2026"))
        assertTrue(PassphrasePolicy.violations("Cooldown2026").isEmpty())
    }

    @Test
    fun `trop long est refuse`() {
        val code = "a1".repeat(PassphrasePolicy.MAX_LENGTH)
        assertTrue(PassphrasePolicy.Violation.TOO_LONG in PassphrasePolicy.violations(code))
    }

    @Test
    fun `les lettres accentuees comptent comme des lettres`() {
        assertTrue(PassphrasePolicy.isValid("ete2026é"))
    }
}
