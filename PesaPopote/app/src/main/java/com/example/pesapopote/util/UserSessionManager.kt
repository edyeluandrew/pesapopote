package com.example.pesapopote.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Save user session details: email, fullName & logged-in status
    fun saveUserSession(email: String, fullName: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_EMAIL, email)
            putString(KEY_FULL_NAME, fullName)
            apply()
        }
    }

    // Save password (plain text for demo; replace with encryption in production)
    fun saveUserPassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD, password).apply()
    }

    // Get saved password
    fun getUserPassword(): String? {
        return prefs.getString(KEY_PASSWORD, null)
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Get saved user email
    fun getUserEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    // Get saved user full name
    fun getFullName(): String? {
        return prefs.getString(KEY_FULL_NAME, null)
    }

    // Clear user session on logout (also clears password)
    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_EMAIL)
            remove(KEY_FULL_NAME)
            remove(KEY_PASSWORD)
            apply()
        }
    }

    // Biometric login preference
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    // Two-Factor Authentication (2FA) preference
    fun isTwoFactorEnabled(): Boolean {
        return prefs.getBoolean(KEY_2FA_ENABLED, false)
    }

    fun setTwoFactorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_2FA_ENABLED, enabled).apply()
    }

    // Wallet management methods
    fun saveWalletKeys(publicKey: String, secretKey: String) {
        prefs.edit().apply {
            putString(KEY_PUBLIC_KEY, publicKey)
            putString(KEY_SECRET_KEY, secretKey)
            putBoolean(KEY_WALLET_EXISTS, true)
            apply()
        }
    }

    fun getPublicKey(): String? {
        return prefs.getString(KEY_PUBLIC_KEY, null)
    }

    fun getSecretKey(): String? {
        return prefs.getString(KEY_SECRET_KEY, null)
    }

    fun hasWallet(): Boolean {
        return prefs.getBoolean(KEY_WALLET_EXISTS, false) &&
                !prefs.getString(KEY_PUBLIC_KEY, null).isNullOrEmpty() &&
                !prefs.getString(KEY_SECRET_KEY, null).isNullOrEmpty()
    }

    fun clearWallet() {
        prefs.edit().apply {
            remove(KEY_PUBLIC_KEY)
            remove(KEY_SECRET_KEY)
            remove(KEY_STORED_PIN)
            remove(KEY_PIN_HASH)
            remove(KEY_PIN_SALT)
            remove(KEY_WALLET_BALANCE)
            remove(KEY_WALLET_BALANCE_LAST_UPDATED)
            putBoolean(KEY_WALLET_EXISTS, false)
            apply()
        }
    }

    // Wallet Balance Management Methods
    fun saveWalletBalance(balance: String) {
        prefs.edit().apply {
            putString(KEY_WALLET_BALANCE, balance)
            putLong(KEY_WALLET_BALANCE_LAST_UPDATED, System.currentTimeMillis())
            apply()
        }
    }

    fun getWalletBalance(): String? {
        return prefs.getString(KEY_WALLET_BALANCE, null)
    }

    fun getWalletBalanceLastUpdated(): Long {
        return prefs.getLong(KEY_WALLET_BALANCE_LAST_UPDATED, 0)
    }

    fun isWalletBalanceStale(maxAgeMillis: Long = 300_000): Boolean { // 5 minutes default
        val lastUpdated = getWalletBalanceLastUpdated()
        return (System.currentTimeMillis() - lastUpdated) > maxAgeMillis
    }

    fun clearWalletBalance() {
        prefs.edit().apply {
            remove(KEY_WALLET_BALANCE)
            remove(KEY_WALLET_BALANCE_LAST_UPDATED)
            apply()
        }
    }

    // PIN Management Methods

    /**
     * Save PIN securely using hashing with salt
     */
    fun savePin(pin: String) {
        try {
            val salt = generateSalt()
            val hashedPin = hashPin(pin, salt)

            prefs.edit().apply {
                putString(KEY_PIN_HASH, hashedPin)
                putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.DEFAULT))
                putBoolean(KEY_PIN_SET, true)
                apply()
            }
        } catch (e: Exception) {
            // Fallback to simple storage for demo (not recommended for production)
            prefs.edit().apply {
                putString(KEY_STORED_PIN, pin)
                putBoolean(KEY_PIN_SET, true)
                apply()
            }
        }
    }

    /**
     * Get stored PIN (returns null if not set)
     * For demo purposes, also checks for simple stored PIN
     */
    fun getStoredPin(): String? {
        // First check if we have a hashed PIN setup
        if (prefs.contains(KEY_PIN_HASH)) {
            // Return null for hashed PINs as they should be verified, not retrieved
            return null
        }

        // Fallback: return simple stored PIN (for demo/development)
        return prefs.getString(KEY_STORED_PIN, "1234") // Default PIN for demo
    }

    /**
     * Verify if the provided PIN matches the stored PIN
     */
    fun verifyPin(inputPin: String): Boolean {
        return try {
            // Check hashed PIN first
            val storedHash = prefs.getString(KEY_PIN_HASH, null)
            val saltString = prefs.getString(KEY_PIN_SALT, null)

            if (storedHash != null && saltString != null) {
                val salt = Base64.decode(saltString, Base64.DEFAULT)
                val inputHash = hashPin(inputPin, salt)
                storedHash == inputHash
            } else {
                // Fallback to simple PIN comparison
                val storedPin = prefs.getString(KEY_STORED_PIN, "1234")
                inputPin == storedPin
            }
        } catch (e: Exception) {
            // Fallback comparison
            val storedPin = prefs.getString(KEY_STORED_PIN, "1234")
            inputPin == storedPin
        }
    }

    /**
     * Check if PIN is set
     */
    fun isPinSet(): Boolean {
        return prefs.getBoolean(KEY_PIN_SET, false) ||
                prefs.contains(KEY_PIN_HASH) ||
                prefs.contains(KEY_STORED_PIN)
    }

    /**
     * Clear stored PIN
     */
    fun clearPin() {
        prefs.edit().apply {
            remove(KEY_STORED_PIN)
            remove(KEY_PIN_HASH)
            remove(KEY_PIN_SALT)
            putBoolean(KEY_PIN_SET, false)
            apply()
        }
    }

    /**
     * Update/Change PIN
     */
    fun changePin(newPin: String) {
        clearPin()
        savePin(newPin)
    }

    // PIN attempt tracking
    fun getPinAttempts(): Int {
        return prefs.getInt(KEY_PIN_ATTEMPTS, 0)
    }

    fun incrementPinAttempts(): Int {
        val attempts = getPinAttempts() + 1
        prefs.edit().putInt(KEY_PIN_ATTEMPTS, attempts).apply()
        return attempts
    }

    fun resetPinAttempts() {
        prefs.edit().remove(KEY_PIN_ATTEMPTS).apply()
    }

    fun getLastPinAttemptTime(): Long {
        return prefs.getLong(KEY_LAST_PIN_ATTEMPT, 0)
    }

    fun setLastPinAttemptTime() {
        prefs.edit().putLong(KEY_LAST_PIN_ATTEMPT, System.currentTimeMillis()).apply()
    }

    // Helper methods for PIN security
    private fun generateSalt(): ByteArray {
        return java.security.SecureRandom().let { random ->
            ByteArray(16).also { salt ->
                random.nextBytes(salt)
            }
        }
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            digest.update(salt)
            val hashedBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(hashedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            // Fallback: simple hash
            (pin + salt.contentToString()).hashCode().toString()
        }
    }

    companion object {
        private const val PREF_NAME = "PesaPopotePrefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_PASSWORD = "user_password"  // <-- Added password key

        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_2FA_ENABLED = "two_factor_enabled"

        private const val KEY_FIRST_LAUNCH = "is_first_launch"

        // Wallet keys
        private const val KEY_PUBLIC_KEY = "wallet_public_key"
        private const val KEY_SECRET_KEY = "wallet_secret_key"
        private const val KEY_WALLET_EXISTS = "wallet_exists"
        private const val KEY_WALLET_BALANCE = "wallet_balance"
        private const val KEY_WALLET_BALANCE_LAST_UPDATED = "wallet_balance_last_updated"

        // PIN security keys
        private const val KEY_STORED_PIN = "stored_pin" // For demo/fallback
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_SET = "pin_set"
        private const val KEY_PIN_ATTEMPTS = "pin_attempts"
        private const val KEY_LAST_PIN_ATTEMPT = "last_pin_attempt"

        // Checks if this is the app's first launch (default true)
        fun isFirstLaunch(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        }

        // Marks first launch as done (set false)
        fun setFirstLaunchDone(context: Context) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        }
    }
}
