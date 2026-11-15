package com.instantpause.app.domain.phone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyNumberMatcherTest {

    @Test
    fun `les numeros d'urgence europeens sont reconnus`() {
        for (number in listOf("112", "15", "17", "18", "19", "177")) {
            assertTrue(number, EmergencyNumberMatcher.isEmergency(number, listOf("FR")))
        }
    }

    @Test
    fun `les separateurs ne trompent pas la detection`() {
        assertTrue(EmergencyNumberMatcher.isEmergency("1 1 2", listOf("FR")))
        assertTrue(EmergencyNumberMatcher.isEmergency("1-1-2", listOf("FR")))
        assertTrue(EmergencyNumberMatcher.isEmergency("(112)", listOf("FR")))
    }

    @Test
    fun `un numero normal ne passe jamais pour une urgence`() {
        assertFalse(EmergencyNumberMatcher.isEmergency("+33612345678", listOf("FR", "MA")))
        assertFalse(EmergencyNumberMatcher.isEmergency("0612345678", listOf("FR")))
    }

    @Test
    fun `une chaine vide n'est pas une urgence`() {
        assertFalse(EmergencyNumberMatcher.isEmergency("", listOf("FR")))
        assertFalse(EmergencyNumberMatcher.isEmergency("   ", listOf("FR")))
    }

    @Test
    fun `la region fournie est prise en compte`() {
        assertTrue(EmergencyNumberMatcher.isEmergency("911", listOf("US")))
        assertTrue(EmergencyNumberMatcher.isEmergency("112", emptyList()))
    }

    @Test
    fun `les regions de secours sont interrogees`() {
        assertTrue(
            EmergencyNumberMatcher.isEmergency(
                raw = "911",
                regions = listOf("FR"),
                fallbackRegions = listOf("US")
            )
        )
    }
}
