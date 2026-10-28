package com.cooldown.app.domain.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PassphraseHasherTest {

    // Itérations réduites : on teste la logique, pas la résistance au bruteforce.
    private val hasher = PassphraseHasher(iterations = 1_000)

    private fun stored(code: String, salt: ByteArray = hasher.newSalt()): StoredPassphrase =
        StoredPassphrase(
            salt = hasher.encode(salt),
            hash = hasher.encode(hasher.derive(code, salt)),
            iterations = hasher.iterations
        )

    @Test
    fun `la derivation est deterministe pour un meme sel`() {
        val salt = hasher.newSalt()
        assertTrue(
            hasher.derive("abc123", salt).contentEquals(hasher.derive("abc123", salt))
        )
    }

    @Test
    fun `deux sels donnent deux empreintes differentes`() {
        assertFalse(
            hasher.derive("abc123", hasher.newSalt())
                .contentEquals(hasher.derive("abc123", hasher.newSalt()))
        )
    }

    @Test
    fun `le bon code correspond`() {
        assertTrue(hasher.matches("Cooldown2026", stored("Cooldown2026")))
    }

    @Test
    fun `un code faux ne correspond pas`() {
        assertFalse(hasher.matches("Cooldown2027", stored("Cooldown2026")))
        assertFalse(hasher.matches("cooldown2026", stored("Cooldown2026")))
    }

    @Test
    fun `les espaces de bord ne font pas echouer`() {
        assertTrue(hasher.matches("  Cooldown2026  ", stored("Cooldown2026")))
    }

    @Test
    fun `l empreinte ne contient jamais le code en clair`() {
        val entry = stored("Cooldown2026")
        assertNotEquals("Cooldown2026", entry.hash)
        assertFalse(entry.hash.contains("Cooldown2026"))
        assertTrue(entry.hash.length >= 32)
    }

    @Test
    fun `une empreinte corrompue est refusee sans planter`() {
        val broken = StoredPassphrase(salt = "!!!", hash = "!!!", iterations = 1_000)
        assertFalse(hasher.matches("Cooldown2026", broken))
    }
}
