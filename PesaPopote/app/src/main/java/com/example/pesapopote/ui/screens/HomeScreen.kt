package com.example.pesapopote.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pesapopote.ui.theme.backgroundLight
import com.example.pesapopote.ui.theme.primaryBlue
import com.example.pesapopote.ui.theme.primaryOrange
import com.example.pesapopote.ui.theme.textDark
import com.example.pesapopote.ui.theme.logoutRed
import com.example.pesapopote.util.UserSessionManager
import com.example.pesapopote.util.WalletBalanceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun HomeScreen(
    userName: String = "Andrew",
    onLogout: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSend: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val balanceManager = remember { WalletBalanceManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var walletBalanceXLM by remember { mutableStateOf(0.0) }
    var isLoadingBalance by remember { mutableStateOf(false) }
    var balanceError by remember { mutableStateOf<String?>(null) }

    // Function to refresh balance
    fun refreshBalance() {
        coroutineScope.launch {
            if (balanceManager.hasWallet()) {
                isLoadingBalance = true
                balanceError = null

                val balanceResult = balanceManager.getBalance(forceRefresh = true)
                isLoadingBalance = false

                if (balanceResult.balance != null) {
                    try {
                        walletBalanceXLM = balanceResult.balance.toDouble()
                        balanceError = null
                    } catch (e: NumberFormatException) {
                        walletBalanceXLM = 0.0
                        balanceError = "Invalid balance format"
                    }
                } else {
                    balanceError = balanceResult.error
                }
            } else {
                isLoadingBalance = false
                walletBalanceXLM = 0.0
                balanceError = "No wallet found"
            }
        }
    }

    // Fetch wallet balance using the shared balance manager
    LaunchedEffect(Unit) {
        if (balanceManager.hasWallet()) {
            isLoadingBalance = true
            balanceError = null

            // Try to get cached balance first for immediate display
            val cachedBalance = balanceManager.getCachedBalance()
            if (cachedBalance != null) {
                try {
                    walletBalanceXLM = cachedBalance.toDouble()
                } catch (e: NumberFormatException) {
                    walletBalanceXLM = 0.0
                }
            }

            // Then fetch fresh balance
            val balanceResult = balanceManager.getBalance()
            isLoadingBalance = false

            if (balanceResult.balance != null) {
                try {
                    walletBalanceXLM = balanceResult.balance.toDouble()
                    balanceError = null
                } catch (e: NumberFormatException) {
                    walletBalanceXLM = 0.0
                    balanceError = "Invalid balance format"
                }
            } else {
                balanceError = balanceResult.error
            }
        } else {
            isLoadingBalance = false
            walletBalanceXLM = 0.0
            balanceError = "No wallet found"
        }
    }

    var selectedIndex by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = backgroundLight,
        bottomBar = {
            NavigationBar(
                containerColor = backgroundLight,
                contentColor = primaryBlue
            ) {
                NavigationBarItem(
                    selected = selectedIndex == 0,
                    onClick = { selectedIndex = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedIndex == 1,
                    onClick = {
                        selectedIndex = 1
                        onNavigateToSend()
                    },
                    icon = { Icon(Icons.Default.Send, contentDescription = "Send") },
                    label = { Text("Send") }
                )
                NavigationBarItem(
                    selected = selectedIndex == 2,
                    onClick = { onNavigateToWallet() },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
                    label = { Text("Wallet") }
                )
                NavigationBarItem(
                    selected = selectedIndex == 3,
                    onClick = {
                        selectedIndex = 3
                        onNavigateToSettings()
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Only show HomeTab content when Home is selected
            if (selectedIndex == 0) {
                HomeTab(
                    userName = userName,
                    walletBalanceXLM = walletBalanceXLM,
                    isLoadingBalance = isLoadingBalance,
                    balanceError = balanceError,
                    hasWallet = balanceManager.hasWallet(),
                    onRefreshBalance = { refreshBalance() }
                )
            }
        }
    }
}

@Composable
fun HomeTab(
    userName: String,
    walletBalanceXLM: Double,
    isLoadingBalance: Boolean,
    balanceError: String?,
    hasWallet: Boolean,
    onRefreshBalance: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var notificationMessage by remember { mutableStateOf<String?>(null) }

    var xlmToUsdRate by remember { mutableStateOf<Double?>(null) }
    var isLoadingRate by remember { mutableStateOf(true) }
    var rateFetchError by remember { mutableStateOf<String?>(null) }

    val recentTransactions = remember {
        listOf(
            Transaction(
                id = "1",
                amountXLM = 20.0,
                to = "GBRPYHIL2C3X...N4A",
                date = Date(System.currentTimeMillis() - 3600_000),
                status = "Success"
            ),
            Transaction(
                id = "2",
                amountXLM = 50.5,
                to = "GCFX6L76...3QY",
                date = Date(System.currentTimeMillis() - 7200_000),
                status = "Pending"
            ),
            Transaction(
                id = "3",
                amountXLM = 10.75,
                to = "GDRX2L5H...9FJ",
                date = Date(System.currentTimeMillis() - 86_400_000),
                status = "Failed"
            )
        )
    }

    LaunchedEffect(Unit) {
        try {
            isLoadingRate = true
            rateFetchError = null
            val rate = fetchXLMtoUSDRate()
            xlmToUsdRate = rate
            isLoadingRate = false
        } catch (e: Exception) {
            rateFetchError = e.message ?: "Error fetching rate"
            isLoadingRate = false
        }
    }

    LaunchedEffect(notificationMessage) {
        notificationMessage?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short,
                actionLabel = "Dismiss"
            )
            if (result == SnackbarResult.ActionPerformed) {
                notificationMessage = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Welcome back, $userName 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textDark
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Wallet Balance Card - matches WalletScreen design
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = primaryBlue.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Wallet Balance",
                            style = MaterialTheme.typography.titleMedium,
                            color = textDark
                        )

                        if (!isLoadingBalance && hasWallet) {
                            TextButton(onClick = onRefreshBalance) {
                                Text("Refresh", color = primaryBlue)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when {
                        !hasWallet -> {
                            Text(
                                text = "No Wallet",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = "Go to Wallet tab to create or import a wallet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        isLoadingBalance -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = primaryBlue
                                )
                                Text(
                                    text = "Loading...",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryBlue
                                )
                            }
                        }
                        balanceError != null -> {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = logoutRed
                            )
                            Text(
                                text = balanceError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = logoutRed
                            )
                        }
                        else -> {
                            Text(
                                text = "%.2f XLM".format(walletBalanceXLM),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryOrange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // USD equivalent - only show when we have a valid balance
                    when {
                        !hasWallet || balanceError != null -> {
                            // Don't show USD equivalent for error states
                        }
                        isLoadingBalance -> {
                            // Don't show USD equivalent while loading balance
                        }
                        isLoadingRate -> {
                            Text(
                                "Loading USD equivalent...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        xlmToUsdRate != null -> {
                            val usdEquivalent = walletBalanceXLM * xlmToUsdRate!!
                            Text(
                                text = "~ $%.2f USD".format(usdEquivalent),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        rateFetchError != null -> {
                            Text(
                                text = "USD rate unavailable",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        notificationMessage = "You have a new transaction notification!"
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Notifications", color = backgroundLight)
                }

                OutlinedButton(
                    onClick = onRefreshBalance,
                    modifier = Modifier.weight(1f),
                    enabled = hasWallet && !isLoadingBalance
                ) {
                    Icon(
                        if (isLoadingBalance) Icons.Default.HourglassTop else Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = if (hasWallet && !isLoadingBalance) primaryBlue else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isLoadingBalance) "Loading..." else "Refresh",
                        color = if (hasWallet && !isLoadingBalance) primaryBlue else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Transactions Section
            Text(
                "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                color = textDark
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (recentTransactions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = backgroundLight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = "No Transactions",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "No recent transactions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentTransactions) { tx ->
                        TransactionItem(tx)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun TransactionItem(tx: Transaction) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundLight),
        border = when (tx.status) {
            "Success" -> null
            "Pending" -> BorderStroke(1.dp, Color(0xFFf0ad4e))
            "Failed" -> BorderStroke(1.dp, logoutRed)
            else -> null
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (tx.status) {
                    "Success" -> Icons.Default.CheckCircle
                    "Pending" -> Icons.Default.HourglassTop
                    "Failed" -> Icons.Default.Error
                    else -> Icons.Default.Info
                },
                contentDescription = "Status Icon",
                tint = when (tx.status) {
                    "Success" -> Color(0xFF4CAF50)
                    "Pending" -> Color(0xFFf0ad4e)
                    "Failed" -> logoutRed
                    else -> Color.Gray
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "To: ${tx.to}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textDark
                )
                Text(
                    "%.2f XLM".format(tx.amountXLM),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = primaryOrange
                )
                Text(
                    dateFormat.format(tx.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    tx.status,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (tx.status) {
                        "Success" -> Color(0xFF4CAF50)
                        "Pending" -> Color(0xFFf0ad4e)
                        "Failed" -> logoutRed
                        else -> Color.Gray
                    }
                )
            }
        }
    }
}

data class Transaction(
    val id: String,
    val amountXLM: Double,
    val to: String,
    val date: Date,
    val status: String
)

private val client = OkHttpClient()

suspend fun fetchXLMtoUSDRate(): Double = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url("https://api.exchangerate.host/latest?base=XLM&symbols=USD")
        .build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("HTTP error: ${response.code}")

        val json = Json.parseToJsonElement(response.body!!.string()).jsonObject
        val rates = json["rates"]?.jsonObject ?: throw Exception("No rates found")
        rates["USD"]?.jsonPrimitive?.doubleOrNull ?: throw Exception("USD rate missing")
    }
}