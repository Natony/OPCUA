package com.example.s7opcuaapp.util

import java.security.MessageDigest
import kotlin.experimental.and

/**
 * FIXED: Removed all password logging for security
 */
object PasswordUtils {

    enum class PasswordStrength {
        WEAK,
        MEDIUM,
        STRONG
    }

    /**
     * Hash password using SHA-256
     * SECURITY: Never log passwords or hashes
     */
    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verify password against hash
     * SECURITY: Never log verification details
     */
    fun verifyPassword(password: String, passwordHash: String): Boolean {
        val inputHash = hashPassword(password)
        return inputHash == passwordHash
    }

    /**
     * Validate password requirements
     * - Minimum 6 characters
     * - Must contain at least one letter
     * - Must contain at least one digit
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 6) {
            return false
        }

        var hasLetter = false
        var hasDigit = false

        password.forEach { char ->
            when {
                char.isLetter() -> hasLetter = true
                char.isDigit() -> hasDigit = true
            }
        }

        return hasLetter && hasDigit
    }

    /**
     * Get password strength assessment
     */
    fun getPasswordStrength(password: String): PasswordStrength {
        var strength = 0

        if (password.length >= 8) strength++
        if (password.length >= 12) strength++
        if (password.any { it.isUpperCase() }) strength++
        if (password.any { it.isLowerCase() }) strength++
        if (password.any { it.isDigit() }) strength++
        if (password.any { !it.isLetterOrDigit() }) strength++

        return when {
            strength <= 2 -> PasswordStrength.WEAK
            strength <= 4 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    /**
     * Get password validation errors
     * Returns list of error messages, empty if valid
     */
    fun getPasswordValidationErrors(password: String): List<String> {
        val errors = mutableListOf<String>()

        if (password.length < 6) {
            errors.add("Password must be at least 6 characters")
        }

        if (!password.any { it.isLetter() }) {
            errors.add("Password must contain at least one letter")
        }

        if (!password.any { it.isDigit() }) {
            errors.add("Password must contain at least one digit")
        }

        return errors
    }

    /**
     * Generate password strength tips
     */
    fun getPasswordStrengthTips(strength: PasswordStrength): String {
        return when (strength) {
            PasswordStrength.WEAK ->
                "Try adding uppercase letters, numbers, and special characters"
            PasswordStrength.MEDIUM ->
                "Good password! Consider making it longer for better security"
            PasswordStrength.STRONG ->
                "Excellent password strength!"
        }
    }
}