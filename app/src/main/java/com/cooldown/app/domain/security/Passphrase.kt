package com.cooldown.app.domain.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Règles de composition du code de déverrouillage.
 *
 * Le code doit être alphanumérique : c'est ce qui le rend pénible à retaper
 * quand on est énervé ou ivre, tout en restant mémorisable à jeun.
 */
object PassphrasePolicy {

    const val MIN_LENGTH = 6
    const val MAX_LENGTH = 64

    enum class Violation { TOO_SHORT, TOO_LONG, MISSING_LETTER, MISSING_DIGIT }

    /** Normalise la saisie : les espaces de bord sont une source d'erreur classique. */
    fun normalize(value: String): String = value.trim()

    fun violations(value: String): List<Violation> {
        val v = normalize(value)
        return buildList {
            if (v.length < MIN_LENGTH) add(Violation.TOO_SHORT)
            if (v.length > MAX_LENGTH) add(Violation.TOO_LONG)
            if (v.none { it.isLetter() }) add(Violation.MISSING_LETTER)
            if (v.none { it.isDigit() }) add(Violation.MISSING_DIGIT)
        }
    }

    fun isValid(value: String): Boolean = violations(value).isEmpty()
}

/** Empreinte persistée : jamais de code en clair, jamais de sel réutilisé. */
data class StoredPassphrase(
    val salt: String,
    val hash: String,
    val iterations: Int
)

/**
 * Dérivation PBKDF2-HMAC-SHA256.
 *
 * 120 000 itérations ≈ 100 ms sur un téléphone d'entrée de gamme : imperceptible
 * pour une saisie légitime, mais qui rend une attaque par dictionnaire locale
 * coûteuse. Le sel empêche de comparer deux installations entre elles.
 */
class PassphraseHasher(
    val iterations: Int = DEFAULT_ITERATIONS
) {

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun derive(passphrase: String, salt: ByteArray, iterations: Int = this.iterations): ByteArray {
        val spec = PBEKeySpec(
            PassphrasePolicy.normalize(passphrase).toCharArray(),
            salt,
            iterations,
            KEY_BITS
        )
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    /** Comparaison à temps constant. */
    fun matches(passphrase: String, stored: StoredPassphrase): Boolean {
        val expected = runCatching { decode(stored.hash) }.getOrNull() ?: return false
        val salt = runCatching { decode(stored.salt) }.getOrNull() ?: return false
        val candidate = derive(passphrase, salt, stored.iterations)
        return MessageDigest.isEqual(expected, candidate)
    }

    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
        const val DEFAULT_ITERATIONS = 120_000
    }
}
