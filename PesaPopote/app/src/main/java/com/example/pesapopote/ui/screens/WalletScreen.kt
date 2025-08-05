package com.example.pesapopote.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.pesapopote.ui.theme.*
import com.example.pesapopote.util.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Server
import org.stellar.sdk.responses.AccountResponse

enum class WalletState {
    CHECKING,
    NO_WALLET,
    WALLET_EXISTS,
    CREATING_WALLET,
    IMPORTING_WALLET,
    SHOW_NEW_KEYS,
    ERROR
}

data class WalletCreationResult(
    val success: Boolean,
    val error: String?,
    val publicKey: String?,
    val secretKey: String?
)

data class WalletImportResult(
    val success: Boolean,
    val error: String?,
    val publicKey: String?
)

data class BalanceResult(val balance: String?, val error: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavHostController,
    context: Context = LocalContext.current
) {
    val walletManager = remember { UserSessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var walletState by remember { mutableStateOf(WalletState.CHECKING) }
    var publicKey by remember { mutableStateOf<String?>(null) }
    var secretKey by remember { mutableStateOf<String?>(null) }
    var balance by remember { mutableStateOf("Loading...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Import wallet states
    var importSecretKey by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    // On launch, check if wallet exists
    LaunchedEffect(Unit) {
        walletState = WalletState.CHECKING
        withContext(Dispatchers.IO) {
            try {
                if (walletManager.hasWallet()) {
                    publicKey = walletManager.getPublicKey()
                    secretKey = walletManager.getSecretKey()
                    walletState = WalletState.WALLET_EXISTS

                    val balanceResult = fetchBalance(publicKey)
                    balance = balanceResult.balance ?: "Error loading balance"
                    if (balanceResult.error != null) {
                        errorMessage = balanceResult.error
                        walletState = WalletState.ERROR
                    }
                } else {
                    walletState = WalletState.NO_WALLET
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
                walletState = WalletState.ERROR
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundLight)
            .padding(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MyStellar Wallet",
                style = MaterialTheme.typography.titleLarge,
                color = primaryBlue
            )

            when (walletState) {
                WalletState.CHECKING -> {
                    LoadingContent("Checking for existing wallet...")
                }
                WalletState.NO_WALLET -> {
                    NoWalletContent(
                        onCreateWallet = {
                            walletState = WalletState.CREATING_WALLET
                            coroutineScope.launch {
                                val creationResult = createNewWallet(walletManager)
                                if (creationResult.success) {
                                    publicKey = creationResult.publicKey
                                    secretKey = creationResult.secretKey
                                    walletState = WalletState.SHOW_NEW_KEYS
                                } else {
                                    errorMessage = creationResult.error
                                    walletState = WalletState.ERROR
                                }
                            }
                        },
                        onImportWallet = {
                            walletState = WalletState.IMPORTING_WALLET
                        }
                    )
                }
                WalletState.CREATING_WALLET -> {
                    LoadingContent("Creating and funding new wallet...")
                }
                WalletState.SHOW_NEW_KEYS -> {
                    NewWalletKeysDisplay(
                        publicKey = publicKey,
                        secretKey = secretKey,
                        onDone = {
                            walletState = WalletState.WALLET_EXISTS
                            coroutineScope.launch {
                                val balanceResult = fetchBalance(publicKey)
                                balance = balanceResult.balance ?: "Error loading balance"
                                if (balanceResult.error != null) {
                                    errorMessage = balanceResult.error
                                }
                            }
                        }
                    )
                }
                WalletState.IMPORTING_WALLET -> {
                    ImportWalletContent(
                        secretKey = importSecretKey,
                        onSecretKeyChange = { importSecretKey = it },
                        isImporting = isImporting,
                        importError = importError,
                        onImport = {
                            if (importSecretKey.isNotBlank()) {
                                isImporting = true
                                importError = null
                                coroutineScope.launch {
                                    val importResult = importWallet(importSecretKey, walletManager)
                                    isImporting = false
                                    if (importResult.success) {
                                        publicKey = importResult.publicKey
                                        secretKey = importSecretKey
                                        walletState = WalletState.WALLET_EXISTS

                                        val balanceResult = fetchBalance(publicKey)
                                        balance = balanceResult.balance ?: "Error loading balance"
                                        if (balanceResult.error != null) {
                                            errorMessage = balanceResult.error
                                        }
                                    } else {
                                        importError = importResult.error
                                    }
                                }
                            } else {
                                importError = "Please enter a valid secret key"
                            }
                        },
                        onCancel = {
                            walletState = WalletState.NO_WALLET
                            importSecretKey = ""
                            importError = null
                        }
                    )
                }
                WalletState.WALLET_EXISTS -> {
                    WalletInfoContent(
                        publicKey = publicKey,
                        balance = balance,
                        onDeleteWallet = {
                            walletManager.clearWallet()
                            publicKey = null
                            secretKey = null
                            balance = "Loading..."
                            errorMessage = null
                            walletState = WalletState.NO_WALLET
                        }
                    )
                }
                WalletState.ERROR -> {
                    ErrorContent(
                        errorMessage = errorMessage,
                        onRetry = {
                            walletState = WalletState.CHECKING
                            errorMessage = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = primaryBlue)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = textDark
            )
        }
    }
}

@Composable
private fun NoWalletContent(
    onCreateWallet: () -> Unit,
    onImportWallet: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "No wallet found",
            style = MaterialTheme.typography.titleMedium,
            color = textDark,
            textAlign = TextAlign.Center
        )

        Text(
            text = "You need a Stellar wallet to continue. You can create a new wallet or import an existing one.",
            style = MaterialTheme.typography.bodyMedium,
            color = textDark,
            textAlign = TextAlign.Center
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onCreateWallet,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                Text(
                    text = "Create New Wallet",
                    color = backgroundLight
                )
            }

            OutlinedButton(
                onClick = onImportWallet,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryBlue)
            ) {
                Text(text = "Import Existing Wallet")
            }
        }
    }
}

@Composable
private fun NewWalletKeysDisplay(
    publicKey: String?,
    secretKey: String?,
    onDone: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copiedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "New Wallet Created!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Public Key section with copy button
        Text("Public Key:", style = MaterialTheme.typography.labelLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = publicKey ?: "Unavailable",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    publicKey?.let {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it))
                        copiedMessage = "Public key copied to clipboard"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                Text("Copy", color = backgroundLight)
            }
        }

        // Secret Key section with copy button
        Text("Secret Key:", style = MaterialTheme.typography.labelLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = secretKey ?: "Unavailable",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    secretKey?.let {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it))
                        copiedMessage = "Secret key copied to clipboard"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                Text("Copy", color = backgroundLight)
            }
        }

        copiedMessage?.let { message ->
            Text(
                text = message,
                color = primaryOrange,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(onClick = onDone) {
            Text("Done")
        }
    }
}

@Composable
private fun ImportWalletContent(
    secretKey: String,
    onSecretKeyChange: (String) -> Unit,
    isImporting: Boolean,
    importError: String?,
    onImport: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Import Wallet",
            style = MaterialTheme.typography.titleMedium,
            color = textDark
        )

        OutlinedTextField(
            value = secretKey,
            onValueChange = onSecretKeyChange,
            label = { Text("Secret Key") },
            placeholder = { Text("Enter your Stellar secret key") },
            visualTransformation = PasswordVisualTransformation(),
            enabled = !isImporting,
            isError = importError != null,
            modifier = Modifier.fillMaxWidth()
        )

        importError?.let { error ->
            Text(
                text = error,
                color = logoutRed,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isImporting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onImport,
                enabled = !isImporting && secretKey.isNotBlank(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = backgroundLight
                    )
                } else {
                    Text(
                        text = "Import",
                        color = backgroundLight
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletInfoContent(
    publicKey: String?,
    balance: String,
    onDeleteWallet: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(accentGold.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            Text(
                text = "Public Address",
                color = textDark,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = publicKey ?: "Unavailable",
                color = textDark,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Divider(color = primaryBlue.copy(alpha = 0.5f))

            Text(
                text = "Balance",
                color = textDark,
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = "$balance XLM",
                color = primaryOrange,
                style = MaterialTheme.typography.titleMedium
            )
        }

        OutlinedButton(
            onClick = onDeleteWallet,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = logoutRed)
        ) {
            Text("Delete Wallet")
        }
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Error: ${errorMessage ?: "Unknown error occurred"}",
            color = logoutRed,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
        ) {
            Text(
                text = "Retry",
                color = backgroundLight
            )
        }
    }
}

private suspend fun createNewWallet(walletManager: UserSessionManager): WalletCreationResult {
    return withContext(Dispatchers.IO) {
        try {
            val keyPair = KeyPair.random()
            val newPublic = keyPair.accountId
            val newSecret = String(keyPair.secretSeed)

            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://friendbot.stellar.org/?addr=$newPublic")
                .build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext WalletCreationResult(
                    false,
                    "Friendbot funding failed: ${response.message}",
                    null,
                    null
                )
            }

            walletManager.saveWalletKeys(newPublic, newSecret)
            WalletCreationResult(true, null, newPublic, newSecret)

        } catch (e: Exception) {
            WalletCreationResult(false, e.localizedMessage, null, null)
        }
    }
}

private suspend fun importWallet(secretKey: String, walletManager: UserSessionManager): WalletImportResult {
    return withContext(Dispatchers.IO) {
        try {
            val keyPair = KeyPair.fromSecretSeed(secretKey)
            val publicKey = keyPair.accountId

            val server = Server("https://horizon-testnet.stellar.org")
            server.accounts().account(publicKey)

            walletManager.saveWalletKeys(publicKey, secretKey)
            WalletImportResult(true, null, publicKey)

        } catch (e: Exception) {
            WalletImportResult(false, "Invalid secret key or account not found: ${e.localizedMessage}", null)
        }
    }
}

private suspend fun fetchBalance(publicKey: String?): BalanceResult {
    if (publicKey == null) {
        return BalanceResult(null, "Public key is null")
    }

    return withContext(Dispatchers.IO) {
        try {
            val server = Server("https://horizon-testnet.stellar.org")
            val account: AccountResponse = server.accounts().account(publicKey)
            val nativeBalance = account.balances.find { it.assetType == "native" }
            BalanceResult(nativeBalance?.balance ?: "0.0", null)
        } catch (e: Exception) {
            BalanceResult(null, e.localizedMessage)
        }
    }
}
