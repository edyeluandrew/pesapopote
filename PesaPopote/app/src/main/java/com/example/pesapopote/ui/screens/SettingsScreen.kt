package com.example.pesapopote.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pesapopote.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        containerColor = backgroundLight,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", color = textDark) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryBlue)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundLight
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item { SettingsSectionHeader("Account") }
            item { LanguageDropdown() }
            item { CurrencyDropdown() }
            item { SwitchSetting(title = "Enable Biometric Login", icon = Icons.Default.Fingerprint) }

            item { SettingsSectionHeader("Security") }
            item { SwitchSetting(title = "Two-Factor Authentication", icon = Icons.Default.Lock) }
            item { SimpleClickableSetting(title = "Change Password", icon = Icons.Default.Key) }

            item { SettingsSectionHeader("Help & Support") }
            item { SimpleClickableSetting(title = "Contact Support", icon = Icons.Default.Email) }
            item { SimpleClickableSetting(title = "Terms & Conditions", icon = Icons.Default.Description) }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = logoutRed)
                ) {
                    Text("Logout", color = Color.White)
                }
            }
        }
    }
}


@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = primaryBlue,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown() {
    val languages = listOf("English", "Swahili", "Kinyarwanda", "Luganda", "Akan", "Yoruba", "Zulu")
    var expanded by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(languages[0]) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            readOnly = true,
            value = selectedLanguage,
            onValueChange = {},
            label = { Text("Language", color = textDark) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = backgroundLight,
                unfocusedContainerColor = backgroundLight,
                focusedTextColor = textDark,
                unfocusedTextColor = textDark,
                focusedIndicatorColor = primaryBlue,
                unfocusedIndicatorColor = primaryBlue
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language, color = textDark) },
                    onClick = {
                        selectedLanguage = language
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdown() {
    val currencies = listOf("UGX", "KES", "RWF", "TZS", "GHS", "ZAR", "NGN", "USD")
    var expanded by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf(currencies[1]) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            readOnly = true,
            value = selectedCurrency,
            onValueChange = {},
            label = { Text("Currency", color = textDark) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = backgroundLight,
                unfocusedContainerColor = backgroundLight,
                focusedTextColor = textDark,
                unfocusedTextColor = textDark,
                focusedIndicatorColor = primaryBlue,
                unfocusedIndicatorColor = primaryBlue
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency, color = textDark) },
                    onClick = {
                        selectedCurrency = currency
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SwitchSetting(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    var checked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = primaryBlue)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), color = textDark)
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentGold,
                checkedTrackColor = primaryBlue,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.Gray
            )
        )
    }
}

@Composable
fun SimpleClickableSetting(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = primaryBlue)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), color = textDark)
        Icon(Icons.Default.ChevronRight, contentDescription = "Navigate", tint = accentGold)
    }
}
