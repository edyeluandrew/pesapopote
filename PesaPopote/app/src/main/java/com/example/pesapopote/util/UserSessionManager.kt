package com.example.pesapopote.util

import android.content.Context
import android.content.SharedPreferences

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

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Get saved user email
    fun getEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    // Get saved user full name
    fun getFullName(): String? {
        return prefs.getString(KEY_FULL_NAME, null)
    }

    // Clear user session on logout
    fun clearSession() {
        prefs.edit().clear().apply()
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
            putBoolean(KEY_WALLET_EXISTS, false)
            apply()
        }
    }

    companion object {
        private const val PREF_NAME = "PesaPopotePrefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"

        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_2FA_ENABLED = "two_factor_enabled"

        private const val KEY_FIRST_LAUNCH = "is_first_launch"

        // Wallet keys
        private const val KEY_PUBLIC_KEY = "wallet_public_key"
        private const val KEY_SECRET_KEY = "wallet_secret_key"
        private const val KEY_WALLET_EXISTS = "wallet_exists"

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