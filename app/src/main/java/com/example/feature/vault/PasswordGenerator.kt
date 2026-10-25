package com.example.feature.vault

import java.security.SecureRandom
import kotlin.math.log2
import kotlin.math.pow

object PasswordGenerator {
    private val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private val NUMBERS = "0123456789"
    private val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    private val AMBIGUOUS = "0O1lI|"

    private val random = SecureRandom()

    fun generate(
        length: Int = 16,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true,
        excludeAmbiguous: Boolean = false
    ): String {
        var pool = ""
        if (includeUpper) pool += UPPERCASE
        if (includeLower) pool += LOWERCASE
        if (includeNumbers) pool += NUMBERS
        if (includeSymbols) pool += SYMBOLS

        if (excludeAmbiguous) {
            pool = pool.filter { it !in AMBIGUOUS }
        }

        if (pool.isEmpty()) pool = LOWERCASE + NUMBERS

        val chars = CharArray(length)
        for (i in 0 until length) {
            chars[i] = pool[random.nextInt(pool.length)]
        }
        return String(chars)
    }

    data class StrengthResult(
        val entropyBits: Int,
        val label: String,
        val scorePercent: Float, // 0f to 1f
        val colorHex: Long
    )

    fun calculateStrength(password: String): StrengthResult {
        if (password.isEmpty()) {
            return StrengthResult(0, "Empty", 0f, 0xFF94A3B8)
        }

        var poolSize = 0
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32

        if (poolSize == 0) poolSize = 10
        val entropy = (password.length * log2(poolSize.toDouble())).toInt()

        return when {
            entropy < 30 || password.length < 8 -> StrengthResult(entropy, "Very Weak", 0.2f, 0xFFEF4444)
            entropy < 50 || password.length < 12 -> StrengthResult(entropy, "Weak", 0.45f, 0xFFF97316)
            entropy < 70 -> StrengthResult(entropy, "Fair", 0.7f, 0xFFFBBF24)
            entropy < 90 -> StrengthResult(entropy, "Strong", 0.9f, 0xFF10B981)
            else -> StrengthResult(entropy, "Very Strong", 1.0f, 0xFF00E5FF)
        }
    }
}
