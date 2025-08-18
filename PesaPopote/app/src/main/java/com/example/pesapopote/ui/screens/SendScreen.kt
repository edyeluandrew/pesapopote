package com.example.pesapopote.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pesapopote.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.net.URL
import com.example.pesapopote.ui.theme.backgroundLight
import androidx.compose.foundation.background


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    onRechargeClick: () -> Unit = {},
    onSendClick: suspend (String, String) -> Unit = { _, _ -> }
) {
    var walletAddress by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    var xlmToUsd by remember { mutableStateOf<Double?>(null) }
    var estimatedFee by remember { mutableStateOf<Double?>(null) }
    var isLoadingData by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoadingData = true
            try {
                val xlmJob = async { fetchXlmToUsd() }
                val feeJob = async { fetchStellarBaseFee() }

                xlmToUsd = xlmJob.await()
                estimatedFee = feeJob.await()
            } catch (e: Exception) {
                message = "Failed to fetch data. Check internet."
            } finally {
                isLoadingData = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .background(backgroundLight),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Send Money", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = walletAddress,
            onValueChange = {
                walletAddress = it
                message = ""
            },
            label = { Text("Recipient Wallet Address", color = textDark) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = textDark),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryBlue,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = {
                amount = it
                message = ""
            },
            label = { Text("Amount (XLM)", color = textDark) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(color = textDark),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryBlue,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (isLoadingData) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = primaryBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading rates...", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    val amountVal = amount.toDoubleOrNull()

                    // Exchange Rate Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Exchange Rate:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(
                            if (xlmToUsd != null) "1 XLM = ${"%.4f".format(xlmToUsd)} USD" else "Unavailable",
                            color = if (xlmToUsd != null) Color.DarkGray else Color.Red,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Network Fee Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Network Fee:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(
                            if (estimatedFee != null) {
                                val feeInUSD = if (xlmToUsd != null) xlmToUsd!! * estimatedFee!! else 0.0
                                "${"%.5f".format(estimatedFee)} XLM (~${"%.4f".format(feeInUSD)} USD)"
                            } else "Unavailable",
                            color = if (estimatedFee != null) Color.DarkGray else Color.Red,
                            fontSize = 14.sp
                        )
                    }

                    if (amountVal != null && amountVal > 0 && xlmToUsd != null && estimatedFee != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))

                        val usdValue = xlmToUsd!! * amountVal
                        val feeInXLM = estimatedFee!!
                        val feeInUSD = xlmToUsd!! * feeInXLM
                        val totalXLM = amountVal + feeInXLM
                        val totalUSD = usdValue + feeInUSD
                        val recipientGetsXLM = amountVal
                        val recipientGetsUSD = usdValue

                        // Amount breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Send Amount:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(
                                "${"%.2f".format(amountVal)} XLM (~${"%.2f".format(usdValue)} USD)",
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Cost:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "${"%.5f".format(totalXLM)} XLM (~${"%.2f".format(totalUSD)} USD)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recipient Gets:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "${"%.2f".format(recipientGetsXLM)} XLM (~${"%.2f".format(recipientGetsUSD)} USD)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryBlue
                            )
                        }
                    } else if (amountVal != null && amountVal <= 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Enter a valid amount greater than 0", color = Color.Red, fontSize = 14.sp)
                    } else if (amount.isNotBlank() && amountVal == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Invalid amount format", color = Color.Red, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.contains("Success", ignoreCase = true))
                        Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
                )
            ) {
                Text(
                    text = message,
                    color = if (message.contains("Success", ignoreCase = true))
                        Color(0xFF2E7D32) else Color(0xFFD32F2F),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    if (walletAddress.isBlank() || amount.isBlank()) {
                        message = "All fields are required."
                        return@launch
                    }

                    val amountVal = amount.toDoubleOrNull()
                    if (amountVal == null || amountVal <= 0) {
                        message = "Enter a valid amount greater than 0."
                        return@launch
                    }

                    if (estimatedFee == null) {
                        message = "Unable to calculate fees. Please try again."
                        return@launch
                    }

                    isSending = true
                    message = ""
                    try {
                        onSendClick(walletAddress, amount)
                        message = "Transaction sent successfully!"
                        walletAddress = ""
                        amount = ""
                    } catch (e: Exception) {
                        message = "Transaction failed: ${e.message ?: "Unknown error"}"
                    } finally {
                        isSending = false
                    }
                }
            },
            enabled = !isSending && !isLoadingData && xlmToUsd != null && estimatedFee != null,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isSending) "Sending..." else "Send Money", color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        var showCountrySelection by remember { mutableStateOf(false) }
        var selectedCountry by remember { mutableStateOf<Country?>(null) }
        var showWithdrawOptions by remember { mutableStateOf(false) }
        var showMobileMoneyList by remember { mutableStateOf(false) }
        var showBankList by remember { mutableStateOf(false) }
        var selectedWithdrawMethod by remember { mutableStateOf<String?>(null) }
        var showMethodDetails by remember { mutableStateOf(false) }

        OutlinedButton(
            onClick = { showCountrySelection = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentGold)
        ) {
            Text("Withdraw Funds", color = accentGold)
        }

        if (showCountrySelection) {
            AlertDialog(
                onDismissRequest = { showCountrySelection = false },
                title = {
                    Text("Select Your Country", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            "Choose your country to see available withdrawal options:",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        LazyColumn {
                            items(getAvailableCountries()) { country ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            selectedCountry = country
                                            showCountrySelection = false
                                            showWithdrawOptions = true
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            country.flag,
                                            fontSize = 24.sp,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                        Text(
                                            country.name,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCountrySelection = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showWithdrawOptions && selectedCountry != null) {
            AlertDialog(
                onDismissRequest = {
                    showWithdrawOptions = false
                    selectedCountry = null
                },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedCountry!!.flag, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                            Text("${selectedCountry!!.name} - Withdrawal Method", fontWeight = FontWeight.Bold)
                        }
                        Text("Choose your preferred withdrawal method", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Normal)
                    }
                },
                text = {
                    Column {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showWithdrawOptions = false
                                    showMobileMoneyList = true
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📱", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                                Column {
                                    Text("Mobile Money", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text("Available mobile money services in ${selectedCountry!!.name}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showWithdrawOptions = false
                                    showBankList = true
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🏦", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                                Column {
                                    Text("Bank Transfer", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text("Local banks in ${selectedCountry!!.name}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showWithdrawOptions = false
                        selectedCountry = null
                    }) {
                        Text("Back")
                    }
                }
            )
        }

        if (showMobileMoneyList && selectedCountry != null) {
            AlertDialog(
                onDismissRequest = {
                    showMobileMoneyList = false
                },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedCountry!!.flag, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                            Text("${selectedCountry!!.name} - Mobile Money", fontWeight = FontWeight.Bold)
                        }
                        Text("Select your mobile money provider", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Normal)
                    }
                },
                text = {
                    val countryProviders = getMobileMoneyProvidersByCountry(selectedCountry!!.code)

                    if (countryProviders.isEmpty()) {
                        Text("No mobile money providers available for ${selectedCountry!!.name} yet.", color = Color.Gray)
                    } else {
                        LazyColumn {
                            items(countryProviders) { provider ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            selectedWithdrawMethod = provider.name
                                            showMobileMoneyList = false
                                            showMethodDetails = true
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            selectedCountry!!.flag,
                                            fontSize = 20.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(provider.name, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showMobileMoneyList = false
                        showWithdrawOptions = true
                    }) {
                        Text("Back")
                    }
                }
            )
        }

        if (showBankList && selectedCountry != null) {
            AlertDialog(
                onDismissRequest = {
                    showBankList = false
                },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedCountry!!.flag, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                            Text("${selectedCountry!!.name} - Banks", fontWeight = FontWeight.Bold)
                        }
                        Text("Select your bank", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Normal)
                    }
                },
                text = {
                    val countryBanks = getBankProvidersByCountry(selectedCountry!!.code)

                    if (countryBanks.isEmpty()) {
                        Text("No banks available for ${selectedCountry!!.name} yet.", color = Color.Gray)
                    } else {
                        LazyColumn {
                            items(countryBanks) { bank ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            selectedWithdrawMethod = bank.name
                                            showBankList = false
                                            showMethodDetails = true
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            selectedCountry!!.flag,
                                            fontSize = 20.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(bank.name, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showBankList = false
                        showWithdrawOptions = true
                    }) {
                        Text("Back")
                    }
                }
            )
        }

        if (showMethodDetails && selectedWithdrawMethod != null) {
            WithdrawDetailsDialog(
                method = selectedWithdrawMethod!!,
                onDismiss = {
                    showMethodDetails = false
                    selectedWithdrawMethod = null
                    selectedCountry = null
                },
                onConfirm = { withdrawAmount, accountDetails ->
                    showMethodDetails = false
                    selectedWithdrawMethod = null
                    selectedCountry = null
                    message = "Withdrawal request submitted successfully!"
                }
            )
        }
    }
}

suspend fun fetchXlmToUsd(): Double? = withContext(Dispatchers.IO) {
    return@withContext try {
        val response = URL("https://api.coinbase.com/v2/exchange-rates?currency=XLM").readText()
        val json = Json.parseToJsonElement(response).jsonObject
        val ratesObject = json["data"]?.jsonObject?.get("rates")?.jsonObject
        val usdRate = ratesObject?.get("USD")?.jsonPrimitive?.content
        usdRate?.toDoubleOrNull()
    } catch (e: Exception) {
        println("fetchXlmToUsd error: ${e.message}")
        try {
            val fallbackResponse = URL("https://api.coingecko.com/api/v3/simple/price?ids=stellar&vs_currencies=usd").readText()
            val fallbackJson = Json.parseToJsonElement(fallbackResponse).jsonObject
            fallbackJson["stellar"]?.jsonObject?.get("usd")?.jsonPrimitive?.double
        } catch (fallbackError: Exception) {
            println("Fallback API also failed: ${fallbackError.message}")
            null
        }
    }
}

suspend fun fetchStellarBaseFee(): Double? = withContext(Dispatchers.IO) {
    return@withContext try {
        val response = URL("https://horizon-testnet.stellar.org/fee_stats").readText()
        val json = Json.parseToJsonElement(response).jsonObject
        val feeStroops = json["last_ledger_base_fee"]?.jsonPrimitive?.content
            ?: json["mode_accepted_fee"]?.jsonPrimitive?.content
            ?: json["min_accepted_fee"]?.jsonPrimitive?.content

        feeStroops?.toDoubleOrNull()?.div(10_000_000.0)
    } catch (e: Exception) {
        println("fetchStellarBaseFee error: ${e.message}")
        0.00001
    }
}

data class Country(
    val code: String,
    val name: String,
    val flag: String
)

data class WithdrawProvider(
    val name: String,
    val flag: String,
    val countries: String,
    val countryCode: String,
    val type: String,
    val phoneValidation: PhoneValidation? = null,
    val accountValidation: AccountValidation? = null
)

data class PhoneValidation(
    val minLength: Int,
    val maxLength: Int,
    val prefixes: List<String>,
    val countryCode: String
)

data class AccountValidation(
    val minLength: Int,
    val maxLength: Int,
    val pattern: String? = null,
    val description: String
)

fun getAvailableCountries(): List<Country> {
    return listOf(
        Country("UG", "Uganda", "🇺🇬"),
        Country("KE", "Kenya", "🇰🇪"),
        Country("TZ", "Tanzania", "🇹🇿"),
        Country("RW", "Rwanda", "🇷🇼"),
        Country("GH", "Ghana", "🇬🇭"),
        Country("NG", "Nigeria", "🇳🇬"),
        Country("ZA", "South Africa", "🇿🇦")
    )
}

fun getMobileMoneyProvidersByCountry(countryCode: String): List<WithdrawProvider> {
    return getMobileMoneyProviders().filter { it.countryCode == countryCode }
}

fun getBankProvidersByCountry(countryCode: String): List<WithdrawProvider> {
    return getBankProviders().filter { it.countryCode == countryCode }
}

fun getMobileMoneyProviders(): List<WithdrawProvider> {
    return listOf(
        WithdrawProvider(
            "M-Pesa", "🇰🇪", "Kenya", "KE", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("07", "01"), "+254")
        ),
        WithdrawProvider(
            "M-Pesa", "🇹🇿", "Tanzania", "TZ", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("06", "07"), "+255")
        ),
        WithdrawProvider(
            "MTN Mobile Money", "🇺🇬", "Uganda", "UG", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("077", "078"), "+256")
        ),
        WithdrawProvider(
            "MTN Mobile Money", "🇷🇼", "Rwanda", "RW", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("078", "079"), "+250")
        ),
        WithdrawProvider(
            "MTN Mobile Money", "🇬🇭", "Ghana", "GH", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("024", "054", "055"), "+233")
        ),
        WithdrawProvider(
            "Airtel Money", "🇺🇬", "Uganda", "UG", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("070", "075"), "+256")
        ),
        WithdrawProvider(
            "Airtel Money", "🇰🇪", "Kenya", "KE", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("073", "078"), "+254")
        ),
        WithdrawProvider(
            "Airtel Money", "🇹🇿", "Tanzania", "TZ", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("068", "069"), "+255")
        ),
        WithdrawProvider(
            "Airtel Money", "🇷🇼", "Rwanda", "RW", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("073", "072"), "+250")
        ),
        WithdrawProvider(
            "Tigo Pesa", "🇹🇿", "Tanzania", "TZ", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("071", "065"), "+255")
        ),
        WithdrawProvider(
            "Vodacom M-Pesa", "🇹🇿", "Tanzania", "TZ", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("074", "076"), "+255")
        ),
        WithdrawProvider(
            "Opay", "🇳🇬", "Nigeria", "NG", "mobile_money",
            phoneValidation = PhoneValidation(11, 11, listOf("070"), "+234")
        ),
        WithdrawProvider(
            "PalmPay", "🇳🇬", "Nigeria", "NG", "mobile_money",
            phoneValidation = PhoneValidation(11, 11, listOf("070", "080", "081", "090", "091"), "+234")
        ),
        WithdrawProvider(
            "Vodafone Cash", "🇬🇭", "Ghana", "GH", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("020", "050"), "+233")
        ),
        WithdrawProvider(
            "AirtelTigo Money", "🇬🇭", "Ghana", "GH", "mobile_money",
            phoneValidation = PhoneValidation(10, 10, listOf("027", "057", "026", "056"), "+233")
        )
    )
}

fun getBankProviders(): List<WithdrawProvider> {
    return listOf(
        WithdrawProvider(
            "Stanbic Bank", "🇺🇬", "Uganda", "UG", "bank",
            accountValidation = AccountValidation(9, 13, null, "9-13 digit account number")
        ),
        WithdrawProvider(
            "Centenary Bank", "🇺🇬", "Uganda", "UG", "bank",
            accountValidation = AccountValidation(10, 13, null, "10-13 digit account number")
        ),
        WithdrawProvider(
            "DFCU Bank", "🇺🇬", "Uganda", "UG", "bank",
            accountValidation = AccountValidation(10, 12, null, "10-12 digit account number")
        ),
        WithdrawProvider(
            "Equity Bank", "🇺🇬", "Uganda", "UG", "bank",
            accountValidation = AccountValidation(10, 13, null, "10-13 digit account number")
        ),

        WithdrawProvider(
            "Equity Bank", "🇰🇪", "Kenya", "KE", "bank",
            accountValidation = AccountValidation(10, 13, null, "10-13 digit account number")
        ),
        WithdrawProvider(
            "KCB Bank", "🇰🇪", "Kenya", "KE", "bank",
            accountValidation = AccountValidation(12, 12, null, "12 digit account number")
        ),
        WithdrawProvider(
            "Safaricom Bank", "🇰🇪", "Kenya", "KE", "bank",
            accountValidation = AccountValidation(10, 12, null, "10-12 digit account number")
        ),
        WithdrawProvider(
            "NCBA Bank", "🇰🇪", "Kenya", "KE", "bank",
            accountValidation = AccountValidation(9, 12, null, "9-12 digit account number")
        ),

        WithdrawProvider(
            "CRDB Bank", "🇹🇿", "Tanzania", "TZ", "bank",
            accountValidation = AccountValidation(13, 16, null, "13-16 digit account number")
        ),
        WithdrawProvider(
            "NMB Bank", "🇹🇿", "Tanzania", "TZ", "bank",
            accountValidation = AccountValidation(13, 16, null, "13-16 digit account number")
        ),
        WithdrawProvider(
            "Stanbic Bank", "🇹🇿", "Tanzania", "TZ", "bank",
            accountValidation = AccountValidation(13, 13, null, "13 digit account number")
        ),

        WithdrawProvider(
            "Bank of Kigali", "🇷🇼", "Rwanda", "RW", "bank",
            accountValidation = AccountValidation(10, 16, null, "10-16 digit account number")
        ),
        WithdrawProvider(
            "Equity Bank", "🇷🇼", "Rwanda", "RW", "bank",
            accountValidation = AccountValidation(10, 13, null, "10-13 digit account number")
        ),
        WithdrawProvider(
            "I&M Bank", "🇷🇼", "Rwanda", "RW", "bank",
            accountValidation = AccountValidation(10, 12, null, "10-12 digit account number")
        ),

        WithdrawProvider(
            "GTBank", "🇳🇬", "Nigeria", "NG", "bank",
            accountValidation = AccountValidation(10, 10, null, "10 digit NUBAN account number")
        ),
        WithdrawProvider(
            "First Bank Nigeria", "🇳🇬", "Nigeria", "NG", "bank",
            accountValidation = AccountValidation(10, 10, null, "10 digit NUBAN account number")
        ),
        WithdrawProvider(
            "Access Bank", "🇳🇬", "Nigeria", "NG", "bank",
            accountValidation = AccountValidation(10, 10, null, "10 digit NUBAN account number")
        ),
        WithdrawProvider(
            "Zenith Bank", "🇳🇬", "Nigeria", "NG", "bank",
            accountValidation = AccountValidation(10, 10, null, "10 digit NUBAN account number")
        ),
        WithdrawProvider(
            "UBA", "🇳🇬", "Nigeria", "NG", "bank",
            accountValidation = AccountValidation(10, 10, null, "10 digit NUBAN account number")
        ),

        WithdrawProvider(
            "GCB Bank", "🇬🇭", "Ghana", "GH", "bank",
            accountValidation = AccountValidation(13, 13, null, "13 digit account number")
        ),
        WithdrawProvider(
            "Ecobank Ghana", "🇬🇭", "Ghana", "GH", "bank",
            accountValidation = AccountValidation(10, 13, null, "10-13 digit account number")
        ),
        WithdrawProvider(
            "Standard Chartered Ghana", "🇬🇭", "Ghana", "GH", "bank",
            accountValidation = AccountValidation(12, 14, null, "12-14 digit account number")
        ),

        WithdrawProvider(
            "Standard Bank", "🇿🇦", "South Africa", "ZA", "bank",
            accountValidation = AccountValidation(9, 11, null, "9-11 digit account number")
        ),
        WithdrawProvider(
            "FNB", "🇿🇦", "South Africa", "ZA", "bank",
            accountValidation = AccountValidation(10, 10, null, "10 digit account number")
        ),
        WithdrawProvider(
            "ABSA", "🇿🇦", "South Africa", "ZA", "bank",
            accountValidation = AccountValidation(9, 10, null, "9-10 digit account number")
        ),
        WithdrawProvider(
            "Nedbank", "🇿🇦", "South Africa", "ZA", "bank",
            accountValidation = AccountValidation(10, 11, null, "10-11 digit account number")
        ),
        WithdrawProvider(
            "Capitec Bank", "🇿🇦", "South Africa", "ZA", "bank",
            accountValidation = AccountValidation(10, 10, null, "10 digit account number")
        )
    )
}

fun validatePhoneNumber(phoneNumber: String, validation: PhoneValidation): String? {
    val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")

    if (cleanPhone.length < validation.minLength || cleanPhone.length > validation.maxLength) {
        return "Phone number must be ${validation.minLength}-${validation.maxLength} digits"
    }

    val hasValidPrefix = validation.prefixes.any { prefix ->
        cleanPhone.startsWith(prefix)
    }

    if (!hasValidPrefix) {
        return "Phone number must start with: ${validation.prefixes.joinToString(", ")}"
    }

    return null
}

fun validateAccountNumber(accountNumber: String, validation: AccountValidation): String? {
    val cleanAccount = accountNumber.replace(Regex("[^0-9]"), "")

    if (cleanAccount != accountNumber.replace(Regex("\\s"), "")) {
        return "Account number should contain only digits and spaces"
    }
    if (cleanAccount.length < validation.minLength || cleanAccount.length > validation.maxLength) {
        return validation.description
    }

    validation.pattern?.let { pattern ->
        if (!cleanAccount.matches(Regex(pattern))) {
            return validation.description
        }
    }

    return null
}

fun formatPhoneNumber(phoneNumber: String, countryCode: String): String {
    val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
    return if (cleanPhone.startsWith("0")) {
        "$countryCode${cleanPhone.substring(1)}"
    } else {
        "$countryCode$cleanPhone"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawDetailsDialog(
    method: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var withdrawAmount by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val provider = (getMobileMoneyProviders() + getBankProviders()).find { it.name == method }
    val isMobileMoney = provider?.type == "mobile_money"

    LaunchedEffect(accountNumber) {
        if (accountNumber.isNotBlank()) {
            validationError = if (isMobileMoney) {
                provider?.phoneValidation?.let { validation ->
                    validatePhoneNumber(accountNumber, validation)
                }
            } else {
                provider?.accountValidation?.let { validation ->
                    validateAccountNumber(accountNumber, validation)
                }
            }
        } else {
            validationError = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(provider?.flag ?: "💰", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                    Text("Withdraw to $method", fontWeight = FontWeight.Bold)
                }
                Text(
                    "Enter withdrawal details",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = withdrawAmount,
                    onValueChange = { withdrawAmount = it },
                    label = { Text("Amount (XLM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Minimum withdrawal: 1 XLM", fontSize = 12.sp, color = Color.Gray)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = {
                        Text(if (isMobileMoney) "Phone Number" else "Account Number")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isMobileMoney) KeyboardType.Phone else KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        if (isMobileMoney) {
                            provider?.phoneValidation?.let { validation ->
                                Text("e.g., ${validation.prefixes.first()}xxxxxxx")
                            }
                        } else {
                            provider?.accountValidation?.let { validation ->
                                Text("${validation.minLength}-${validation.maxLength} digits")
                            }
                        }
                    },
                    supportingText = {
                        if (isMobileMoney) {
                            provider?.phoneValidation?.let { validation ->
                                Text(
                                    "Format: ${validation.prefixes.joinToString(" or ")}xxxxxxx (${validation.minLength} digits)",
                                    fontSize = 12.sp,
                                    color = if (validationError != null) Color.Red else Color.Gray
                                )
                            }
                        } else {
                            provider?.accountValidation?.let { validation ->
                                Text(
                                    validation.description,
                                    fontSize = 12.sp,
                                    color = if (validationError != null) Color.Red else Color.Gray
                                )
                            }
                        }
                    },
                    isError = validationError != null
                )

                if (validationError != null) {
                    Text(
                        validationError!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                if (!isMobileMoney) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text("Account Name") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Full name as on account") },
                        supportingText = {
                            Text("Enter name exactly as it appears on your account", fontSize = 12.sp, color = Color.Gray)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            "ℹ️ ${if (isMobileMoney) "Mobile Money" else "Bank Transfer"} Info",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE65100)
                        )

                        val infoText = if (isMobileMoney) {
                            "• Processing time: 5-30 minutes\n• Ensure your mobile money account is active\n• You'll receive SMS confirmation\n• Transaction fees may apply"
                        } else {
                            "• Processing time: 1-24 hours\n• Verify account details carefully\n• Bank charges may apply\n• Available on banking days only"
                        }

                        Text(
                            infoText,
                            fontSize = 12.sp,
                            color = Color(0xFFBF360C)
                        )
                    }
                }
            }
        },
        confirmButton = {
            val amountValid = withdrawAmount.toDoubleOrNull()?.let { it >= 1.0 } == true
            val accountValid = accountNumber.isNotBlank() && validationError == null
            val nameValid = isMobileMoney || accountName.isNotBlank()

            Button(
                onClick = {
                    if (amountValid && accountValid && nameValid) {
                        val formattedDetails = if (isMobileMoney) {
                            provider?.phoneValidation?.let { validation ->
                                val formattedPhone = formatPhoneNumber(accountNumber, validation.countryCode)
                                "Phone: $formattedPhone"
                            } ?: "Phone: $accountNumber"
                        } else {
                            "Account: $accountNumber, Name: $accountName"
                        }
                        onConfirm(withdrawAmount, formattedDetails)
                    }
                },
                enabled = amountValid && accountValid && nameValid
            ) {
                Text("Confirm Withdrawal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}