package com.instantpause.app.domain.pause

import org.junit.Assert.assertEquals
import org.junit.Test

class PauseMessagePolicyTest {

    @Test
    fun `une saisie vide reste vide et laisse l affichage choisir la langue`() {
        assertEquals("", PauseMessagePolicy.normalize(""))
        assertEquals("", PauseMessagePolicy.normalize("   "))
        assertEquals("", PauseMessagePolicy.normalize("\n\t "))
    }

    @Test
    fun `les espaces superflus sont reduits`() {
        assertEquals(
            "Cet appel peut attendre demain.",
            PauseMessagePolicy.normalize("  Cet   appel peut attendre demain.  ")
        )
    }

    @Test
    fun `la longueur est bornee sans casser la phrase`() {
        val trop = "a".repeat(PauseMessagePolicy.MAX_LENGTH + 50)
        assertEquals(PauseMessagePolicy.MAX_LENGTH, PauseMessagePolicy.normalize(trop).length)
    }

    @Test
    fun `une phrase ordinaire est conservee telle quelle`() {
        val phrase = "Je préfère te parler demain, à tête reposée."
        assertEquals(phrase, PauseMessagePolicy.normalize(phrase))
    }
}
