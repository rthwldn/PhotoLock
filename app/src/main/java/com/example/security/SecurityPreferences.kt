package com.example.security

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Manages security settings, PIN hash, biometrics state, and auto-lock parameters.
 */
class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("vault_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_INITIALIZED = "is_initialized"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTO_LOCK_SECONDS = "auto_lock_seconds"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER_HASH = "security_answer_hash"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_TIMESTAMP = "lockout_timestamp"
        private const val KEY_DELETE_AFTER_IMPORT = "delete_after_import"
        private const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"
    }

    var isInitialized: Boolean
        get() = prefs.getBoolean(KEY_IS_INITIALIZED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_INITIALIZED, value).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var autoLockSeconds: Int
        get() = prefs.getInt(KEY_AUTO_LOCK_SECONDS, 60) // Default: 60s
        set(value) = prefs.edit().putInt(KEY_AUTO_LOCK_SECONDS, value).apply()

    var deleteAfterImport: Boolean
        get() = prefs.getBoolean(KEY_DELETE_AFTER_IMPORT, true)
        set(value) = prefs.edit().putBoolean(KEY_DELETE_AFTER_IMPORT, value).apply()

    var securityQuestion: String
        get() = prefs.getString(KEY_SECURITY_QUESTION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SECURITY_QUESTION, value).apply()

    var lastBackupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_TIMESTAMP, value).apply()

    fun setupPin(pin: String, cryptoManager: CryptoManager, question: String = "", answer: String = "") {
        val salt = UUID.randomUUID().toString()
        val hash = cryptoManager.hashPin(pin, salt)

        val editor = prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .putBoolean(KEY_IS_INITIALIZED, true)

        if (question.isNotBlank() && answer.isNotBlank()) {
            val answerHash = cryptoManager.hashPin(answer.trim().lowercase(), salt)
            editor.putString(KEY_SECURITY_QUESTION, question)
            editor.putString(KEY_SECURITY_ANSWER_HASH, answerHash)
        }

        editor.apply()
    }

    fun verifyPin(pin: String, cryptoManager: CryptoManager): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val computedHash = cryptoManager.hashPin(pin, salt)
        val valid = (storedHash == computedHash)

        if (valid) {
            resetFailedAttempts()
        } else {
            incrementFailedAttempts()
        }
        return valid
    }

    fun verifyRecoveryAnswer(answer: String, cryptoManager: CryptoManager): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedAnswerHash = prefs.getString(KEY_SECURITY_ANSWER_HASH, null) ?: return false
        val computedHash = cryptoManager.hashPin(answer.trim().lowercase(), salt)
        return storedAnswerHash == computedHash
    }

    fun getFailedAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    private fun incrementFailedAttempts() {
        val count = getFailedAttempts() + 1
        val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, count)
        if (count >= 5) {
            // 30 seconds lockout
            editor.putLong(KEY_LOCKOUT_TIMESTAMP, System.currentTimeMillis() + 30_000)
        }
        editor.apply()
    }

    fun resetFailedAttempts() {
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCKOUT_TIMESTAMP, 0L).apply()
    }

    fun getLockoutRemainingSeconds(): Long {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        return if (lockoutUntil > now) {
            (lockoutUntil - now) / 1000
        } else {
            0
        }
    }
}
