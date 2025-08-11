package com.example.pesapopote.ui.screens

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import com.example.pesapopote.ui.theme.*
import com.example.pesapopote.util.UserSessionManager
import com.example.pesapopote.util.WalletBalanceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Server

enum class WalletState {
    CHECKING, NO_WALLET, WALLET_EXISTS, CREATING_WALLET, IMPORTING_WALLET, SHOW_NEW_KEYS, ERROR, SETUP_PIN
}

data class WalletCreationResult(val success: Boolean, val error: String?, val publicKey: String?, val secretKey: String?)
data class WalletImportResult(val success: Boolean, val error: String?, val publicKey: String?)

// Biometric/PIN authentication states
data class AuthenticationState(
    val isAuthenticated: Boolean = false,
    val showPinDialog: Boolean = false,
    val showPinSetupDialog: Boolean = false,
    val pinInput: String = "",
    val confirmPinInput: String = "",
    val pinError: String? = null,
    val attemptCount: Int = 0,
    val isSettingUpPin: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(navController: NavHostController, context: Context = LocalContext.current) {
    val walletManager = remember { UserSessionManager(context) }
    val balanceManager = remember { WalletBalanceManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var walletState by remember { mutableStateOf(WalletState.CHECKING) }
    var publicKey by remember { mutableStateOf<String?>(null) }
    var secretKey by remember { mutableStateOf<String?>(null) }
    var balance by remember { mutableStateOf("Loading...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var importSecretKey by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    // Authentication state for viewing secret key
    var authState by remember { mutableStateOf(AuthenticationState()) }
    var showSecretKey by remember { mutableStateOf(false) }

    // Show back button only when not in main wallet view states
    val showBackButton = walletState in listOf(
        WalletState.CREATING_WALLET,
        WalletState.IMPORTING_WALLET,
        WalletState.SHOW_NEW_KEYS,
        WalletState.SETUP_PIN
    )

    // Helper function to load balance using the shared balance manager
    suspend fun loadWalletBalance() {
        val balanceResult = balanceManager.getBalance()
        balance = balanceResult.balance ?: "Error"
        errorMessage = balanceResult.error
    }

    // On first composition and when returning to this screen, reload wallet data from prefs
    LaunchedEffect(Unit) {
        walletState = WalletState.CHECKING
        withContext(Dispatchers.IO) {
            try {
                if (walletManager.hasWallet()) {
                    publicKey = walletManager.getPublicKey()
                    secretKey = walletManager.getSecretKey()

                    // Check if PIN is set up, if not, prompt for setup
                    if (!walletManager.isPinSet()) {
                        walletState = WalletState.SETUP_PIN
                        authState = authState.copy(showPinSetupDialog = true, isSettingUpPin = true)
                    } else {
                        walletState = WalletState.WALLET_EXISTS
                        loadWalletBalance()
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
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar with conditional back button
            if (showBackButton) {
                TopAppBar(
                    title = { Text("MyStellar Wallet") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                // Reset states and go back to main wallet view
                                walletState = if (walletManager.hasWallet()) WalletState.WALLET_EXISTS else WalletState.NO_WALLET
                                importSecretKey = ""
                                importError = null
                                errorMessage = null
                                authState = AuthenticationState()
                            }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundLight,
                        titleContentColor = primaryBlue
                    )
                )
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!showBackButton) {
                    Text(
                        text = "MyStellar Wallet",
                        style = MaterialTheme.typography.titleLarge,
                        color = primaryBlue
                    )
                }

                when (walletState) {
                    WalletState.CHECKING -> LoadingContent("Checking wallet...")

                    WalletState.NO_WALLET -> NoWalletContent(
                        onCreateWallet = {
                            walletState = WalletState.CREATING_WALLET
                            coroutineScope.launch {
                                val creationResult = createNewWallet(walletManager)
                                if (creationResult.success) {
                                    publicKey = creationResult.publicKey
                                    secretKey = creationResult.secretKey

                                    // Check if PIN is set up
                                    if (!walletManager.isPinSet()) {
                                        walletState = WalletState.SETUP_PIN
                                        authState = authState.copy(showPinSetupDialog = true, isSettingUpPin = true)
                                    } else {
                                        walletState = WalletState.SHOW_NEW_KEYS
                                    }
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

                    WalletState.CREATING_WALLET -> LoadingContent("Creating wallet and funding with testnet...")

                    WalletState.SETUP_PIN -> SetupPinContent()

                    WalletState.IMPORTING_WALLET -> ImportWalletContent(
                        secretKey = importSecretKey,
                        onSecretKeyChange = { importSecretKey = it },
                        isImporting = isImporting,
                        importError = importError,
                        onImport = {
                            if (importSecretKey.isBlank()) {
                                importError = "Enter secret key"
                            } else {
                                isImporting = true
                                coroutineScope.launch {
                                    val importResult = importWallet(importSecretKey, walletManager)
                                    isImporting = false
                                    if (importResult.success) {
                                        publicKey = importResult.publicKey
                                        secretKey = importSecretKey

                                        // Check if PIN is set up
                                        if (!walletManager.isPinSet()) {
                                            walletState = WalletState.SETUP_PIN
                                            authState = authState.copy(showPinSetupDialog = true, isSettingUpPin = true)
                                        } else {
                                            walletState = WalletState.WALLET_EXISTS
                                            loadWalletBalance()
                                        }
                                    } else {
                                        importError = importResult.error
                                    }
                                }
                            }
                        },
                        onCancel = {
                            walletState = WalletState.NO_WALLET
                            importSecretKey = ""
                            importError = null
                        }
                    )

                    WalletState.SHOW_NEW_KEYS -> ShowNewWalletContent(
                        publicKey = publicKey,
                        secretKey = secretKey,
                        onCopyPublicKey = {
                            publicKey?.let {
                                clipboardManager.setText(AnnotatedString(it))
                            }
                        },
                        onCopySecretKey = {
                            secretKey?.let {
                                clipboardManager.setText(AnnotatedString(it))
                            }
                        },
                        onContinue = {
                            coroutineScope.launch {
                                walletState = WalletState.WALLET_EXISTS
                                loadWalletBalance()
                            }
                        }
                    )

                    WalletState.WALLET_EXISTS -> WalletInfoContent(
                        publicKey = publicKey,
                        secretKey = secretKey,
                        balance = balance,
                        showSecretKey = showSecretKey,
                        onCopyPublicKey = {
                            publicKey?.let {
                                clipboardManager.setText(AnnotatedString(it))
                            }
                        },
                        onCopySecretKey = {
                            secretKey?.let {
                                clipboardManager.setText(AnnotatedString(it))
                            }
                        },
                        onToggleSecretKeyVisibility = {
                            if (showSecretKey) {
                                showSecretKey = false
                                authState = AuthenticationState()
                            } else {
                                // Check for biometric availability
                                if (isBiometricAvailable(context)) {
                                    authenticateWithBiometric(
                                        context = context,
                                        onSuccess = {
                                            showSecretKey = true
                                            authState = AuthenticationState(isAuthenticated = true)
                                        },
                                        onError = {
                                            // Fallback to PIN
                                            authState = authState.copy(showPinDialog = true)
                                        }
                                    )
                                } else {
                                    // Use PIN authentication
                                    authState = authState.copy(showPinDialog = true)
                                }
                            }
                        },
                        onRefreshBalance = {
                            coroutineScope.launch {
                                balance = "Loading..."
                                loadWalletBalance()
                            }
                        },
                        onDeleteWallet = {
                            walletManager.clearWallet()
                            balanceManager.clearCache()
                            publicKey = null
                            secretKey = null
                            balance = "Loading..."
                            showSecretKey = false
                            authState = AuthenticationState()
                            walletState = WalletState.NO_WALLET
                        }
                    )

                    WalletState.ERROR -> ErrorContent(
                        errorMessage = errorMessage,
                        onRetry = {
                            walletState = WalletState.CHECKING
                            errorMessage = null
                        }
                    )
                }
            }
        }

        // PIN Setup Dialog
        if (authState.showPinSetupDialog) {
            PinSetupDialog(
                authState = authState,
                onPinChange = { pin ->
                    authState = authState.copy(pinInput = pin, pinError = null)
                },
                onConfirmPinChange = { confirmPin ->
                    authState = authState.copy(confirmPinInput = confirmPin, pinError = null)
                },
                onSetupPin = {
                    if (authState.pinInput.length < 4) {
                        authState = authState.copy(pinError = "PIN must be at least 4 digits")
                    } else if (authState.pinInput != authState.confirmPinInput) {
                        authState = authState.copy(pinError = "PINs do not match")
                    } else {
                        walletManager.savePin(authState.pinInput)
                        authState = AuthenticationState(isAuthenticated = true)
                        walletState = if (publicKey != null && secretKey != null) {
                            WalletState.SHOW_NEW_KEYS
                        } else {
                            WalletState.WALLET_EXISTS
                        }

                        if (walletState == WalletState.WALLET_EXISTS) {
                            coroutineScope.launch {
                                loadWalletBalance()
                            }
                        }
                    }
                },
                onCancel = {
                    authState = AuthenticationState()
                    walletState = WalletState.NO_WALLET
                }
            )
        }

        // PIN Authentication Dialog
        if (authState.showPinDialog) {
            PinAuthenticationDialog(
                authState = authState,
                onPinChange = { pin ->
                    authState = authState.copy(pinInput = pin, pinError = null)
                },
                onAuthenticate = {
                    if (walletManager.verifyPin(authState.pinInput)) {
                        showSecretKey = true
                        authState = AuthenticationState(isAuthenticated = true)
                        walletManager.resetPinAttempts()
                    } else {
                        val newAttemptCount = walletManager.incrementPinAttempts()
                        walletManager.setLastPinAttemptTime()

                        if (newAttemptCount >= 3) {
                            authState = AuthenticationState(
                                pinError = "Too many failed attempts. Try again later.",
                                attemptCount = newAttemptCount
                            )
                        } else {
                            authState = authState.copy(
                                pinError = "Incorrect PIN. ${3 - newAttemptCount} attempts remaining.",
                                pinInput = "",
                                attemptCount = newAttemptCount
                            )
                        }
                    }
                },
                onCancel = {
                    authState = AuthenticationState()
                }
            )
        }
    }
}

@Composable
fun LoadingContent(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = primaryBlue)
            Spacer(Modifier.height(8.dp))
            Text(message, color = textDark, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun NoWalletContent(onCreateWallet: () -> Unit, onImportWallet: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome to MyStellar Wallet", style = MaterialTheme.typography.headlineSmall, color = textDark)
        Text("Create or import a wallet to get started.", color = textDark, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCreateWallet,
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Create New Wallet", color = backgroundLight)
        }

        OutlinedButton(
            onClick = onImportWallet,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Import Existing Wallet", color = primaryBlue)
        }
    }
}

@Composable
fun SetupPinContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Setup Security PIN",
            style = MaterialTheme.typography.headlineSmall,
            color = primaryBlue
        )

        Text(
            text = "Set up a PIN to secure your wallet's secret key",
            color = textDark,
            textAlign = TextAlign.Center
        )

        Text(
            text = "You'll use this PIN along with biometric authentication to access sensitive information.",
            color = textDark,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ImportWalletContent(
    secretKey: String,
    onSecretKeyChange: (String) -> Unit,
    isImporting: Boolean,
    importError: String?,
    onImport: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Import Wallet",
            style = MaterialTheme.typography.headlineSmall,
            color = textDark
        )

        Text(
            text = "Enter your secret key to import an existing wallet",
            color = textDark,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = secretKey,
            onValueChange = onSecretKeyChange,
            label = { Text("Secret Key") },
            placeholder = { Text("S...") },
            visualTransformation = PasswordVisualTransformation(),
            isError = importError != null,
            enabled = !isImporting,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        importError?.let {
            Text(it, color = logoutRed, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = backgroundLight)
                } else {
                    Text("Import", color = backgroundLight)
                }
            }
        }
    }
}

@Composable
fun ShowNewWalletContent(
    publicKey: String?,
    secretKey: String?,
    onCopyPublicKey: () -> Unit,
    onCopySecretKey: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Wallet Created Successfully!",
            style = MaterialTheme.typography.headlineSmall,
            color = primaryBlue
        )

        Text(
            text = "Your wallet has been funded with testnet XLM. Save your keys securely!",
            color = textDark,
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundLight)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Public Key", style = MaterialTheme.typography.labelMedium, color = textDark)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = publicKey?.take(20) + "..." ?: "Unavailable",
                        modifier = Modifier.weight(1f),
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = onCopyPublicKey) {
                        Text("Copy", color = primaryBlue)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Secret Key", style = MaterialTheme.typography.labelMedium, color = logoutRed)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "S" + "*".repeat(15) + "...",
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onCopySecretKey) {
                        Text("Copy", color = logoutRed)
                    }
                }
            }
        }

        Text(
            text = "⚠️ Keep your secret key safe! It's needed to recover your wallet.",
            color = logoutRed,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )

        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue to Wallet", color = backgroundLight)
        }
    }
}

@Composable
fun WalletInfoContent(
    publicKey: String?,
    secretKey: String?,
    balance: String,
    showSecretKey: Boolean,
    onCopyPublicKey: () -> Unit,
    onCopySecretKey: () -> Unit,
    onToggleSecretKeyVisibility: () -> Unit,
    onRefreshBalance: () -> Unit,
    onDeleteWallet: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Your Wallet",
            style = MaterialTheme.typography.headlineSmall,
            color = textDark
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = primaryBlue.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Balance", style = MaterialTheme.typography.labelMedium, color = textDark)
                Text(
                    text = "$balance XLM",
                    style = MaterialTheme.typography.headlineMedium,
                    color = primaryOrange
                )
                TextButton(onClick = onRefreshBalance) {
                    Text("Refresh", color = primaryBlue)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundLight)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Public Key Section
                Text("Public Address", style = MaterialTheme.typography.labelMedium, color = textDark)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = publicKey?.take(20) + "..." ?: "Unavailable",
                        modifier = Modifier.weight(1f),
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = onCopyPublicKey) {
                        Text("Copy", color = primaryBlue)
                    }
                }

                Divider()

                // Secret Key Section
                Text("Secret Key", style = MaterialTheme.typography.labelMedium, color = logoutRed)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (showSecretKey) {
                            secretKey?.take(20) + "..." ?: "Unavailable"
                        } else {
                            "S" + "*".repeat(15) + "..."
                        },
                        modifier = Modifier.weight(1f),
                        overflow = TextOverflow.Ellipsis
                    )
                    Row {
                        IconButton(onClick = onToggleSecretKeyVisibility) {
                            Icon(
                                imageVector = if (showSecretKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showSecretKey) "Hide" else "Show",
                                tint = logoutRed
                            )
                        }
                        if (showSecretKey) {
                            TextButton(onClick = onCopySecretKey) {
                                Text("Copy", color = logoutRed)
                            }
                        }
                    }
                }

                if (showSecretKey) {
                    Text(
                        text = "⚠️ Keep your secret key safe and private!",
                        color = logoutRed,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onDeleteWallet,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = logoutRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete Wallet")
        }
    }
}

@Composable
fun ErrorContent(errorMessage: String?, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            color = logoutRed
        )
        Text(
            text = errorMessage ?: "Unknown error occurred",
            color = textDark,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
        ) {
            Text("Retry", color = backgroundLight)
        }
    }
}

@Composable
fun PinSetupDialog(
    authState: AuthenticationState,
    onPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onSetupPin: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text("Setup Security PIN", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Create a PIN to secure your wallet's secret key access")

                OutlinedTextField(
                    value = authState.pinInput,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) onPinChange(it) },
                    label = { Text("Enter PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("6 digits") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = authState.confirmPinInput,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) onConfirmPinChange(it) },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("Re-enter PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                authState.pinError?.let { error ->
                    Text(
                        text = error,
                        color = logoutRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSetupPin,
                enabled = authState.pinInput.length >= 4 && authState.confirmPinInput.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                Text("Setup PIN", color = backgroundLight)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = textDark)
            }
        }
    )
}

@Composable
fun PinAuthenticationDialog(
    authState: AuthenticationState,
    onPinChange: (String) -> Unit,
    onAuthenticate: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text("Enter PIN", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter your PIN to view the secret key")

                OutlinedTextField(
                    value = authState.pinInput,
                    onValueChange = { if (it.length <= 6) onPinChange(it) },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = authState.pinError != null,
                    enabled = authState.attemptCount < 3,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                authState.pinError?.let { error ->
                    Text(
                        text = error,
                        color = logoutRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAuthenticate,
                enabled = authState.pinInput.isNotBlank() && authState.attemptCount < 3,
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                Text("Authenticate", color = backgroundLight)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = textDark)
            }
        }
    )
}

// Helper functions for biometric authentication
fun isBiometricAvailable(context: Context): Boolean {
    val biometricManager = BiometricManager.from(context)
    return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
        BiometricManager.BIOMETRIC_SUCCESS -> true
        else -> false
    }
}

fun authenticateWithBiometric(
    context: Context,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (context !is FragmentActivity) {
        onError("Biometric authentication requires FragmentActivity")
        return
    }

    val biometricPrompt = BiometricPrompt(context as FragmentActivity, ContextCompat.getMainExecutor(context),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Authentication failed")
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Authenticate to View Secret Key")
        .setSubtitle("Use your biometric credential to access your wallet's secret key")
        .setNegativeButtonText("Use PIN instead")
        .build()

    biometricPrompt.authenticate(promptInfo)
}

// --- Logic helpers ---
suspend fun createNewWallet(walletManager: UserSessionManager): WalletCreationResult = withContext(Dispatchers.IO) {
    try {
        val keyPair = KeyPair.random()
        val newPublic = keyPair.accountId
        val newSecret = String(keyPair.secretSeed)

        // Fund the account with Friendbot (testnet)
        val request = Request.Builder().url("https://friendbot.stellar.org/?addr=$newPublic").build()
        val response = OkHttpClient().newCall(request).execute()

        if (!response.isSuccessful) {
            return@withContext WalletCreationResult(false, "Failed to fund account with testnet", null, null)
        }

        walletManager.saveWalletKeys(newPublic, newSecret)
        WalletCreationResult(true, null, newPublic, newSecret)
    } catch (e: Exception) {
        WalletCreationResult(false, e.localizedMessage ?: "Failed to create wallet", null, null)
    }
}

suspend fun importWallet(secretKey: String, walletManager: UserSessionManager): WalletImportResult = withContext(Dispatchers.IO) {
    try {
        val keyPair = KeyPair.fromSecretSeed(secretKey)
        val publicKey = keyPair.accountId

        // Verify the account exists on the network
        Server("https://horizon-testnet.stellar.org").accounts().account(publicKey)

        walletManager.saveWalletKeys(publicKey, secretKey)
        WalletImportResult(true, null, publicKey)
    } catch (e: Exception) {
        WalletImportResult(false, "Invalid secret key or account not found on testnet", null)
    }
}