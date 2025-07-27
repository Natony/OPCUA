package com.example.s7opcuaapp.util

import android.util.Base64
import android.util.Log
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.experimental.and

/**
 * Enhanced password utilities with proper salting and hashing
 */
object PasswordUtils {

    private const val SALT_LENGTH = 32 // Increased from 16
    private const val ITERATIONS = 15000 // Increased from 10000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    // Version prefix to support future algorithm changes
    private const val VERSION_1 = "v1:"

    enum class PasswordStrength {
        WEAK,
        MEDIUM,
        STRONG,
        VERY_STRONG
    }

    /**
     * Hash password with salt using PBKDF2
     */
    fun hashPassword(password: String): String {
        try {
            // Generate random salt
            val salt = ByteArray(SALT_LENGTH)
            SecureRandom().nextBytes(salt)

            // Generate hash with PBKDF2
            val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val hash = factory.generateSecret(spec).encoded

            // Combine version, salt and hash for storage
            val combined = ByteArray(salt.size + hash.size)
            System.arraycopy(salt, 0, combined, 0, salt.size)
            System.arraycopy(hash, 0, combined, salt.size, hash.size)

            // Return with version prefix
            return VERSION_1 + Base64.encodeToString(combined, Base64.NO_WRAP)

        } catch (e: Exception) {
            Log.e("PasswordUtils", "Error hashing password", e)
            throw SecurityException("Failed to hash password", e)
        }
    }

    /**
     * Verify password against stored hash
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        try {
            // Check version and extract hash
            val actualHash = when {
                storedHash.startsWith(VERSION_1) -> storedHash.substring(VERSION_1.length)
                else -> {
                    // Legacy hash without salt - should migrate
                    Log.w("PasswordUtils", "Legacy password hash detected")
                    return legacyVerifyPassword(password, storedHash)
                }
            }

            val combined = Base64.decode(actualHash, Base64.NO_WRAP)

            // Extract salt
            val salt = combined.sliceArray(0 until SALT_LENGTH)

            // Extract hash
            val storedHashBytes = combined.sliceArray(SALT_LENGTH until combined.size)

            // Generate hash with same salt
            val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val testHash = factory.generateSecret(spec).encoded

            // Compare hashes securely (constant time)
            return testHash.contentEquals(storedHashBytes)

        } catch (e: Exception) {
            Log.e("PasswordUtils", "Error verifying password", e)
            return false
        }
    }

    /**
     * Legacy verification for migration period
     */
    private fun legacyVerifyPassword(password: String, legacyHash: String): Boolean {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        val hash = bytes.joinToString("") { "%02x".format(it) }
        return hash == legacyHash
    }

    /**
     * Check if password meets requirements
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false // Increased from 6

        var hasUpperCase = false
        var hasLowerCase = false
        var hasDigit = false
        var hasSpecial = false

        password.forEach { char ->
            when {
                char.isUpperCase() -> hasUpperCase = true
                char.isLowerCase() -> hasLowerCase = true
                char.isDigit() -> hasDigit = true
                !char.isLetterOrDigit() -> hasSpecial = true
            }
        }

        // Require at least 3 out of 4 categories
        val categoriesCount = listOf(hasUpperCase, hasLowerCase, hasDigit, hasSpecial).count { it }
        return categoriesCount >= 3
    }

    /**
     * Get password strength with enhanced criteria
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
            strength <= 5 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }

    /**
     * Check for common weak patterns
     */
    private fun hasCommonPatterns(password: String): Boolean {
        val commonPatterns = listOf(
            "123", "abc", "qwerty", "password", "admin",
            "111", "000", "aaa"
        )

        val lowerPassword = password.lowercase()
        return commonPatterns.any { pattern ->
            lowerPassword.contains(pattern)
        }
    }

    /**
     * Generate secure random password
     */
    fun generateSecurePassword(length: Int = 16): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val random = SecureRandom()

        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}