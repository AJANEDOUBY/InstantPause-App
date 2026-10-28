package com.cooldown.app.domain.security

import org.junit.Assert.assertEquals
import org.junit.Test

class LockoutPolicyTest {

    @Test
    fun `trois premiers essais sans attente`() {
        assertEquals(0L, LockoutPolicy.delayMs(0))
        assertEquals(0L, LockoutPolicy.delayMs(1))
        assertEquals(0L, LockoutPolicy.delayMs(2))
    }

    @Test
    fun `l attente double a partir du troisieme echec`() {
        assertEquals(30_000L, LockoutPolicy.delayMs(3))
        assertEquals(60_000L, LockoutPolicy.delayMs(4))
        assertEquals(120_000L, LockoutPolicy.delayMs(5))
        assertEquals(240_000L, LockoutPolicy.delayMs(6))
    }

    @Test
    fun `l attente est plafonnee a quinze minutes`() {
        assertEquals(15 * 60_000L, LockoutPolicy.delayMs(8))
        assertEquals(15 * 60_000L, LockoutPolicy.delayMs(50))
    }

    @Test
    fun `le compteur d essais restants ne devient jamais negatif`() {
        assertEquals(3, LockoutPolicy.attemptsLeft(0))
        assertEquals(1, LockoutPolicy.attemptsLeft(2))
        assertEquals(0, LockoutPolicy.attemptsLeft(3))
        assertEquals(0, LockoutPolicy.attemptsLeft(12))
    }
}
