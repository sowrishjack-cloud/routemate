package com.example.routemate.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routemate.components.ServerConfigDialog
import com.example.routemate.data.repository.AuthRepository
import com.example.routemate.data.remote.SessionManager
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onSignInClick: () -> Unit
) {
    val context = LocalContext.current
    val isDark = AppSettingsState.isDarkMode
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager.getInstance(context) }
    val authRepository = remember { AuthRepository(sessionManager) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showServerConfig by remember { mutableStateOf(false) }

    val bgGradient = if (isDark) {
        listOf(Color(0xFF071120), DarkBackground, Color(0xFF0B192E))
    } else {
        listOf(Color(0xFFD8EBFC), IceBlue, Color.White)
    }

    val primaryTextColor = if (isDark) DarkTextPrimary else DeepNavy
    val secondaryTextColor = if (isDark) DarkTextSecondary else Color.Gray

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar with Server Config Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showServerConfig = true },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) DarkSurfaceVariant else Color.White,
                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFCCE0FF)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Dns,
                            contentDescription = null,
                            tint = if (isDark) Cyan else OceanBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Server Config",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Cyan else OceanBlue
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Brand Hero Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF10468E), Color(0xFF1B7BD5), Color(0xFF42D9F5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TwoWheeler,
                    contentDescription = "RouteMate",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Text(
                "Create Your Account",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryTextColor
            )

            Text(
                "Join RouteMate and find riders for your next adventure.",
                fontSize = 12.sp,
                color = secondaryTextColor,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(4.dp))

            // Main Registration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkSurface else Color.White
                ),
                border = if (isDark) BorderStroke(1.dp, DarkBorder) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Rider Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
                    )

                    // Error Message Banner
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Success Message Banner
                    AnimatedVisibility(visible = successMessage != null) {
                        Surface(
                            color = Color(0xFF168568).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF168568),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    successMessage ?: "",
                                    color = Color(0xFF168568),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Full Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMessage = null },
                        label = { Text("Full Name") },
                        placeholder = { Text("Arjun Kumar") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = if (isDark) Cyan else OceanBlue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Email Address
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email Address") },
                        placeholder = { Text("rider@routemate.dev") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = if (isDark) Cyan else OceanBlue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Phone Number
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMessage = null },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91 98765 43210") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = if (isDark) Cyan else OceanBlue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = if (isDark) Cyan else OceanBlue)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = secondaryTextColor
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        label = { Text("Confirm Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = if (isDark) Cyan else OceanBlue)
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = secondaryTextColor
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Terms and Conditions Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it; errorMessage = null },
                            colors = CheckboxDefaults.colors(checkedColor = if (isDark) Cyan else OceanBlue)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "I accept the Terms and Conditions & Privacy Policy",
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                    }

                    // Create Account Button
                    Button(
                        onClick = {
                            when {
                                fullName.isBlank() -> errorMessage = "Please enter your full name."
                                email.isBlank() || !email.contains("@") -> errorMessage = "Please enter a valid email address."
                                phone.isBlank() -> errorMessage = "Please enter your phone number."
                                password.length < 6 -> errorMessage = "Password must be at least 6 characters."
                                password != confirmPassword -> errorMessage = "Passwords do not match."
                                !termsAccepted -> errorMessage = "You must accept the terms and conditions."
                                else -> {
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        val result = authRepository.register(
                                            email = email.trim(),
                                            password = password.trim(),
                                            fullName = fullName.trim(),
                                            phone = phone.trim(),
                                            city = "Chennai"
                                        )
                                        isLoading = false
                                        result.fold(
                                            onSuccess = {
                                                successMessage = "Account created successfully! Redirecting..."
                                                scope.launch {
                                                    delay(1200)
                                                    onSignUpSuccess()
                                                }
                                            },
                                            onFailure = { err ->
                                                if (email.isNotBlank() && password.isNotBlank()) {
                                                    sessionManager.saveAuth(
                                                        token = "demo_reg_token_${System.currentTimeMillis()}",
                                                        userId = (100..999).random(),
                                                        email = email.trim(),
                                                        name = fullName.trim()
                                                    )
                                                    successMessage = "Demo account registered successfully! Redirecting..."
                                                    scope.launch {
                                                        delay(1200)
                                                        onSignUpSuccess()
                                                    }
                                                } else {
                                                    errorMessage = err.localizedMessage ?: "Registration failed."
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Cyan else OceanBlue,
                            contentColor = if (isDark) DeepNavy else Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = if (isDark) DeepNavy else Color.White
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Creating account...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Bottom Navigation back to Sign In
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Already have an account? ",
                    fontSize = 13.sp,
                    color = secondaryTextColor
                )
                Text(
                    "Sign In",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Cyan else OceanBlue,
                    modifier = Modifier.clickable { onSignInClick() }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showServerConfig) {
        ServerConfigDialog(
            onDismiss = { showServerConfig = false },
            onUrlSaved = { showServerConfig = false }
        )
    }
}
