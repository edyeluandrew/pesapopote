package com.example.pesapopote.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.stellar.sdk.Server

data class BalanceResult(val balance: String?, val error: String?)

class WalletBalanceManager(private val context: Context) {
    private val sessionManager = UserSessionManager(context)


    suspend fun fetchAndCacheBalance(): BalanceResult = withContext(Dispatchers.IO) {
        val publicKey = sessionManager.getPublicKey()
        if (publicKey == null) {
            return@withContext BalanceResult(null, "No wallet found")
        }

        try {
            val account = Server("https://horizon-testnet.stellar.org").accounts().account(publicKey)
            val balance = account.balances.find { it.assetType == "native" }?.balance ?: "0.0"

            sessionManager.saveWalletBalance(balance)

            BalanceResult(balance, null)
        } catch (e: Exception) {
            BalanceResult(null, e.localizedMessage ?: "Failed to fetch balance")
        }
    }

    suspend fun getBalance(forceRefresh: Boolean = false): BalanceResult {
        if (!forceRefresh && !sessionManager.isWalletBalanceStale()) {
            val cachedBalance = sessionManager.getWalletBalance()
            if (cachedBalance != null) {
                return BalanceResult(cachedBalance, null)
            }
        }

        return fetchAndCacheBalance()
    }

    fun getCachedBalance(): String? {
        return if (sessionManager.isWalletBalanceStale()) {
            null
        } else {
            sessionManager.getWalletBalance()
        }
    }

    fun clearCache() {
        sessionManager.clearWalletBalance()
    }


    fun hasWallet(): Boolean {
        return sessionManager.hasWallet()
    }

    fun getPublicKey(): String? {
        return sessionManager.getPublicKey()
    }
}