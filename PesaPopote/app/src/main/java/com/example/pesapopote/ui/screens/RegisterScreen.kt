package com.example.pesapopote.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pesapopote.util.UserSessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val primaryBlue = Color(0xFF1E88E5)
    val accentGold = Color(0xFFFFC107)
    val backgroundLight = Color(0xFFFFFFFF)

    val countries = listOf(
        Triple("🇺🇬", "+256", "Uganda"),
        Triple("🇰🇪", "+254", "Kenya"),
        Triple("🇹🇿", "+255", "Tanzania"),
        Triple("🇷🇼", "+250", "Rwanda"),
        Triple("🇳🇬", "+234", "Nigeria"),
        Triple("🇿🇦", "+27", "South Africa"),
        Triple("🇬🇭", "+233", "Ghana")
    )

    var selectedCountry by remember { mutableStateOf(countries[0]) }
    var countryExpanded by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(selectedCountry.second) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundLight)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Country Dropdown
        ExposedDropdownMenuBox(
            expanded = countryExpanded,
            onExpandedChange = { countryExpanded = !countryExpanded }
        ) {
            OutlinedTextField(
                value = "${selectedCountry.first} ${selectedCountry.third} (${selectedCountry.second})",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Country") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            DropdownMenu(
                expanded = countryExpanded,
                onDismissRequest = { countryExpanded = false }
            ) {
                countries.forEach { country ->
                    DropdownMenuItem(
                        text = { Text("${country.first} ${country.third} (${country.second})") },
                        onClick = {
                            selectedCountry = country
                            phone = country.second
                            countryExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorText.isNotEmpty()) {
            Text(errorText, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        Button(
            onClick = {
                fun isStrongPassword(pw: String): Boolean {
                    val hasUpper = pw.any { it.isUpperCase() }
                    val hasLower = pw.any { it.isLowerCase() }
                    val hasDigit = pw.any { it.isDigit() }
                    val hasSpecial = pw.any { !it.isLetterOrDigit() }
                    return pw.length >= 8 && hasUpper && hasLower && hasDigit && hasSpecial
                }

                when {
                    fullName.isBlank() || phone.isBlank() || email.isBlank() ||
                            password.isBlank() || confirmPassword.isBlank() -> {
                        errorText = "Please fill in all fields."
                    }
                    password != confirmPassword -> {
                        errorText = "Passwords do not match."
                    }
                    !isStrongPassword(password) -> {
                        errorText = "Password must be 8+ characters, with uppercase, lowercase, digit, and symbol."
                    }
                    else -> {
                        errorText = ""

                        // Store session and proceed
                        val sessionManager = UserSessionManager(context)
                        sessionManager.saveUserSession(email = email, fullName = fullName)
                        onRegisterSuccess()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentGold)
        ) {
            Text("Sign Up", fontWeight = FontWeight.Bold, color = primaryBlue, fontSize = 18.sp)
        }
    }
}
