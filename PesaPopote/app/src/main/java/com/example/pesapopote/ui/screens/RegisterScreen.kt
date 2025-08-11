package com.example.pesapopote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pesapopote.util.UserSessionManager
import android.util.Patterns

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val primaryBlue = Color(0xFF1E88E5)
    val accentGold = Color(0xFFFFC107)
    val backgroundLight = Color(0xFFFFFFFF)

    // Country data (flag, dialing code, name)
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
    var emailError by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Email validation helper
    fun isValidEmail(input: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(input).matches()
    }

    // Password validation helpers
    fun hasUpperCase(pw: String) = pw.any { it.isUpperCase() }
    fun hasLowerCase(pw: String) = pw.any { it.isLowerCase() }
    fun hasDigit(pw: String) = pw.any { it.isDigit() }
    fun hasSpecialChar(pw: String) = pw.any { !it.isLetterOrDigit() }
    fun hasMinLength(pw: String) = pw.length >= 8

    val isPasswordValid = hasMinLength(password) &&
            hasUpperCase(password) &&
            hasLowerCase(password) &&
            hasDigit(password) &&
            hasSpecialChar(password)

    val doPasswordsMatch = password == confirmPassword && confirmPassword.isNotEmpty()
    val areFieldsFilled = fullName.isNotBlank() && phone.isNotBlank() && email.isNotBlank()

    val isFormValid = areFieldsFilled && isPasswordValid && doPasswordsMatch && emailError == null

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

        // Email with validation on change
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = if (it.isBlank()) "Email cannot be empty"
                else if (!isValidEmail(it)) "Please enter a valid email"
                else null
            },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            isError = emailError != null,
            singleLine = true
        )

        if (emailError != null) {
            Text(
                text = emailError ?: "",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Password with visibility toggle
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = icon, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Live password checklist
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            PasswordRequirement("At least 8 characters", hasMinLength(password))
            PasswordRequirement("At least one uppercase letter", hasUpperCase(password))
            PasswordRequirement("At least one lowercase letter", hasLowerCase(password))
            PasswordRequirement("At least one digit", hasDigit(password))
            PasswordRequirement("At least one special character", hasSpecialChar(password))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm Password with visibility toggle
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = icon, contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (confirmPassword.isNotEmpty()) {
            PasswordRequirement("Passwords match", doPasswordsMatch)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorText.isNotEmpty()) {
            Text(errorText, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        Button(
            onClick = {
                // Double check before saving session and proceeding
                if (!isFormValid) {
                    errorText = "Please fill all fields correctly."
                    return@Button
                }
                errorText = ""
                val sessionManager = UserSessionManager(context)
                sessionManager.saveUserSession(email = email.trim(), fullName = fullName.trim())
                onRegisterSuccess()
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFormValid) accentGold else Color.LightGray
            )
        ) {
            Text(
                "Sign Up",
                fontWeight = FontWeight.Bold,
                color = if (isFormValid) primaryBlue else Color.DarkGray,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun PasswordRequirement(text: String, met: Boolean) {
    val color = if (met) Color(0xFF4CAF50) else Color.Red
    val symbol = if (met) "✔" else "✘"
    Text(
        text = "$symbol $text",
        color = color,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
    )
}
