package com.example.s7opcuaapp.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

object PasswordUtils {
    private const val ITERATIONS = 65536
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    /**
     * Hash password với salt ngẫu nhiên
     * Format: salt:hash (Base64 encoded)
     */
    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val hash = hash(password, salt)
        val saltStr = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashStr = Base64.encodeToString(hash, Base64.NO_WRAP)
        return "$saltStr:$hashStr"
    }

    /**
     * Verify password với hash đã lưu
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split(":")
            if (parts.size != 2) return false

            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val hash = Base64.decode(parts[1], Base64.NO_WRAP)
            val testHash = hash(password, salt)
            MessageDigest.isEqual(hash, testHash)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validate password requirements
     * - Min 8 characters
     * - At least 1 uppercase
     * - At least 1 lowercase
     * - At least 1 number
     * - At least 1 special character
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false

        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
    }

    /**
     * Get password strength
     */
    fun getPasswordStrength(password: String): PasswordStrength {
        var score = 0

        // Length
        when {
            password.length >= 12 -> score += 2
            password.length >= 8 -> score += 1
        }

        // Character types
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        // Common patterns (negative score)
        val commonPatterns = listOf("123", "abc", "password", "admin", "user")
        if (commonPatterns.any { password.contains(it, ignoreCase = true) }) score--

        return when {
            score <= 2 -> PasswordStrength.WEAK
            score <= 4 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    enum class PasswordStrength {
        WEAK, MEDIUM, STRONG
    }
}