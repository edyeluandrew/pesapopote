package com.example.pesapopote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import android.util.Patterns
import com.example.pesapopote.util.UserSessionManager

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = UserSessionManager(context)

    val primaryBlue = Color(0xFF1E88E5)
    val accentGold = Color(0xFFFFC107)
    val backgroundLight = Color(0xFFFFFFFF)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun isValidEmail(input: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(input).matches()

    fun validateInputs(): Boolean {
        var valid = true

        if (email.isBlank()) {
            emailError = "Email cannot be empty"
            valid = false
        } else if (!isValidEmail(email)) {
            emailError = "Invalid email format"
            valid = false
        } else {
            emailError = null
        }

        if (password.isBlank()) {
            passwordError = "Password cannot be empty"
            valid = false
        } else {
            passwordError = null
        }

        return valid
    }

    fun verifyLogin(): Boolean {
        val savedEmail = sessionManager.getUserEmail()
        val savedPassword = sessionManager.getUserPassword()

        println("Debug - Saved email: $savedEmail")
        println("Debug - Saved password exists: ${savedPassword != null}")
        println("Debug - Input email: ${email.trim()}")

        if (savedEmail == null) {
            generalError = "No registered user found. Please register first."
            return false
        }

        if (savedPassword == null) {
            generalError = "User registration incomplete. Please register again."
            return false
        }

        if (email.trim() != savedEmail) {
            generalError = "Email does not match any registered user."
            return false
        }

        if (password != savedPassword) {
            generalError = "Incorrect password."
            return false
        }

        generalError = ""
        return true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundLight)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome Back",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = primaryBlue
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
                generalError = ""
            },
            label = { Text("Email Address") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
            isError = emailError != null,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryBlue,
                unfocusedBorderColor = Color.Gray,
                errorBorderColor = Color.Red
            )
        )
        if (emailError != null) {
            Text(
                text = emailError ?: "",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Start).padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null
                generalError = ""
            },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (passwordVisible) "🙈" else "👁️"
                Text(icon, modifier = Modifier.clickable { passwordVisible = !passwordVisible })
            },
            isError = passwordError != null,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryBlue,
                unfocusedBorderColor = Color.Gray,
                errorBorderColor = Color.Red
            )
        )
        if (passwordError != null) {
            Text(
                text = passwordError ?: "",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Start).padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (generalError.isNotEmpty()) {
            Text(
                text = generalError,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Button(
            onClick = {
                if (validateInputs()) {
                    isLoading = true
                    if (verifyLogin()) {
                        sessionManager.saveUserSession(email.trim(), sessionManager.getFullName() ?: "")
                        onLoginSuccess()
                    }
                    isLoading = false
                }
            },
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentGold)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = primaryBlue,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "Login",
                    fontWeight = FontWeight.Bold,
                    color = primaryBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onNavigateToRegister,
            enabled = !isLoading
        ) {
            Text(
                "Don't have an account? Register",
                color = primaryBlue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = {
                sessionManager.clearSession()
                generalError = "Session cleared. Please register again."
            }
        ) {
            Text(
                "Clear Session (Debug)",
                color = Color.Red,
                fontSize = 12.sp
            )
        }
    }
}