package com.example.core.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tori_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_BIOMETRICS_ENABLED = "key_biometrics_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT_MIN = "key_auto_lock_timeout_min"
        private const val KEY_SETUP_COMPLETED = "key_setup_completed"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_CUSTOM_GEMINI_API_KEY = "key_custom_gemini_api_key"
        private const val KEY_FLOATING_SIDEBAR_ENABLED = "key_floating_sidebar_enabled"
        private const val KEY_AUTODETECT_PASSWORDS = "key_autodetect_passwords"
        private const val KEY_AUTODETECT_EMAILS = "key_autodetect_emails"
        private const val KEY_AUTODETECT_APIS = "key_autodetect_apis"
        private const val KEY_AUTODETECT_PROMPTS = "key_autodetect_prompts"
        private const val KEY_AUTODETECT_SITES = "key_autodetect_sites"
        private const val KEY_AUTODETECT_OTHERS = "key_autodetect_others"
    }

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Rony") ?: "Rony"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()

    var isBiometricsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, value).apply()

    var autoLockTimeoutMin: Int
        get() = prefs.getInt(KEY_AUTO_LOCK_TIMEOUT_MIN, 1)
        set(value) = prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT_MIN, value).apply()

    var customGeminiApiKey: String
        get() = prefs.getString(KEY_CUSTOM_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_GEMINI_API_KEY, value).apply()

    var isFloatingSidebarEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_SIDEBAR_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_SIDEBAR_ENABLED, value).apply()

    var autoDetectPasswords: Boolean
        get() = prefs.getBoolean(KEY_AUTODETECT_PASSWORDS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTODETECT_PASSWORDS, value).apply()

    var autoDetectEmails: Boolean
        get() = prefs.getBoolean(KEY_AUTODETECT_EMAILS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTODETECT_EMAILS, value).apply()

    var autoDetectApis: Boolean
        get() = prefs.getBoolean(KEY_AUTODETECT_APIS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTODETECT_APIS, value).apply()

    var autoDetectPrompts: Boolean
        get() = prefs.getBoolean(KEY_AUTODETECT_PROMPTS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTODETECT_PROMPTS, value).apply()

    var autoDetectSites: Boolean
        get() = prefs.getBoolean(KEY_AUTODETECT_SITES, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTODETECT_SITES, value).apply()

    var autoDetectOthers: Boolean
        get() = prefs.getBoolean(KEY_AUTODETECT_OTHERS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTODETECT_OTHERS, value).apply()

    fun hasPin(): Boolean = !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()

    fun setPin(pin: String) {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, saltB64)
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_SETUP_COMPLETED, true)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val hash = hashPin(pin, salt)
        return hash == savedHash
    }

    fun resetPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .putBoolean(KEY_SETUP_COMPLETED, false)
            .apply()
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}
