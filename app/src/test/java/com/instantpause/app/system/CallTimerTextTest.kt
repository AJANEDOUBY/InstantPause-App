package com.instantpause.app.system

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * Le décompte affiché doit dire ce qu'il mesure : un « 0:58 / 3:00 » seul ne
 * rappelle pas qu'il s'agit du temps de parole restant.
 */
class CallTimerTextTest {

    @Test
    fun `le decompte est formate en minutes secondes`() {
        assertEquals("3:00 / 3:00", CallTimerText.countdown(180, 180))
        assertEquals("2:59 / 3:00", CallTimerText.countdown(179, 180))
        assertEquals("0:07 / 3:00", CallTimerText.countdown(7, 180))
        assertEquals("0:00 / 3:00", CallTimerText.countdown(0, 180))
    }

    @Test
    fun `la notification annonce le temps restant`() {
        assertEquals(
            "Temps restant : 2:59 / 3:00",
            CallTimerText.notificationLine(179, 180)
        )
    }

    @Test
    fun `deux chiffres sont conserves pour les secondes`() {
        assertEquals("1:05", CallTimerText.format(65))
        assertEquals("0:05", CallTimerText.format(5))
    }

    /**
     * Sur un téléphone en arabe ou en hindi, `String.format` sans locale explicite
     * produirait des chiffres non latins : le décompte deviendrait illisible pour
     * une notification comparée d'un coup d'œil.
     */
    @Test
    fun `les chiffres restent latins quelle que soit la locale`() {
        val initial = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("2:59 / 3:00", CallTimerText.countdown(179, 180))
        } finally {
            Locale.setDefault(initial)
        }
    }
}
