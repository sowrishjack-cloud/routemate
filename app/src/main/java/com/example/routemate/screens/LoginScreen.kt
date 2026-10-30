package com.example.routemate.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import com.example.routemate.data.remote.ApiConstants
import com.example.routemate.data.remote.SessionManager
import com.example.routemate.data.repository.AuthRepository
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val context = LocalContext.current
    val isDark = AppSettingsState.isDarkMode
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager.getInstance(context) }
    val authRepository = remember { AuthRepository(sessionManager) }

    var email by remember { mutableStateOf("arjun@routemate.dev") }
    var password by remember { mutableStateOf("password123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
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

            Spacer(Modifier.height(10.dp))

            // Brand Hero Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
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
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                "RouteMate",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryTextColor
            )

            Text(
                "Find Your Ride • Share Your Journey",
                fontSize = 13.sp,
                color = secondaryTextColor,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(4.dp))

            // Main Login Card
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
                        "Sign In",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
                    )

                    Text(
                        "Enter your credentials to access your rider profile and teams.",
                        fontSize = 12.sp,
                        color = secondaryTextColor
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

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email Address") },
                        placeholder = { Text("rider@routemate.dev") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = if (isDark) Cyan else OceanBlue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) Cyan else OceanBlue,
                            unfocusedBorderColor = if (isDark) DarkBorder else Color(0xFFD6E2F2),
                            focusedTextColor = primaryTextColor,
                            unfocusedTextColor = primaryTextColor
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = if (isDark) Cyan else OceanBlue)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = secondaryTextColor
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!isLoading) {
                                    performLogin(
                                        email, password, authRepository, scope,
                                        onLoading = { isLoading = it },
                                        onError = { errorMessage = it },
                                        onSuccess = onLoginSuccess
                                    )
                                }
                            }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) Cyan else OceanBlue,
                            unfocusedBorderColor = if (isDark) DarkBorder else Color(0xFFD6E2F2),
                            focusedTextColor = primaryTextColor,
                            unfocusedTextColor = primaryTextColor
                        )
                    )

                    // Sign In Button
                    Button(
                        onClick = {
                            performLogin(
                                email, password, authRepository, scope,
                                onLoading = { isLoading = it },
                                onError = { errorMessage = it },
                                onSuccess = onLoginSuccess
                            )
                        },
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
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
                            Text("Signing in...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Text("Sign In to RouteMate", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Quick Demo Accounts Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD)
                ),
                border = if (isDark) BorderStroke(1.dp, DarkBorder) else null
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "One-Tap Demo Rider Logins",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = primaryTextColor
                        )
                        Surface(
                            color = AccentGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Quick Access",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        "Tap any rider profile below to sign in instantly:",
                        fontSize = 11.sp,
                        color = secondaryTextColor
                    )

                    listOf(
                        DemoRiderAccount(1, "Arjun Kumar", "arjun@routemate.dev", "Chennai • Himalayan 450"),
                        DemoRiderAccount(2, "Meera Rao", "meera@routemate.dev", "Vellore • KTM 390"),
                        DemoRiderAccount(3, "Karthik Raja", "karthik@routemate.dev", "Coimbatore • BMW G310"),
                        DemoRiderAccount(4, "Priya Sharma", "priya@routemate.dev", "Kanchipuram • Ronin 225")
                    ).forEach { account ->
                        Surface(
                            onClick = {
                                email = account.email
                                password = "password123"
                                performLogin(
                                    account.email, "password123", authRepository, scope,
                                    onLoading = { isLoading = it },
                                    onError = { errorMessage = it },
                                    onSuccess = onLoginSuccess
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) DarkSurface else Color.White,
                            border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFDCE6F5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Cyan.copy(alpha = 0.2f) else OceanBlue.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(account.name.first().toString(), fontWeight = FontWeight.Bold, color = if (isDark) Cyan else OceanBlue)
                                    }
                                    Column {
                                        Text(account.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = primaryTextColor)
                                        Text(account.specs, fontSize = 10.sp, color = secondaryTextColor)
                                    }
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = if (isDark) Cyan else OceanBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Continue as Guest Button
            TextButton(
                onClick = onContinueAsGuest,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    "Continue as Guest Rider →",
                    color = if (isDark) Cyan else OceanBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
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

private data class DemoRiderAccount(
    val id: Int,
    val name: String,
    val email: String,
    val specs: String
)

private fun performLogin(
    email: String,
    pass: String,
    repository: AuthRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onLoading: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onSuccess: () -> Unit
) {
    onLoading(true)
    onError(null)
    scope.launch {
        val result = repository.login(email.trim(), pass.trim())
        onLoading(false)
        result.fold(
            onSuccess = {
                AppSettingsState.activeTestNotification = "✨ Welcome back, ${it.fullName}!"
                onSuccess()
            },
            onFailure = { error ->
                onError(error.localizedMessage ?: "Login failed. Check server connection.")
            }
        )
    }
}
