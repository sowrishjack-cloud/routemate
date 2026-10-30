package com.example.routemate.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.example.routemate.data.remote.ApiConstants
import com.example.routemate.data.remote.ApiResponse
import com.example.routemate.data.remote.RetrofitClient
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ServerConfigDialog(
    onDismiss: () -> Unit,
    onUrlSaved: () -> Unit
) {
    val context = LocalContext.current
    val isDark = AppSettingsState.isDarkMode
    val scope = rememberCoroutineScope()

    var inputUrl by remember { mutableStateOf(ApiConstants.BASE_URL) }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf<Boolean?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Dns,
                    contentDescription = null,
                    tint = if (isDark) Cyan else OceanBlue
                )
                Text(
                    "Backend Server Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else DeepNavy
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Configure the PHP REST API address for your test environment:",
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextSecondary else Color.Gray
                )

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = {
                        inputUrl = it
                        testStatus = null
                        isSuccess = null
                    },
                    label = { Text("Server Base URL") },
                    placeholder = { Text("http://192.168.x.x:8000/api/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDark) Cyan else OceanBlue,
                        unfocusedBorderColor = if (isDark) DarkBorder else Color(0xFFD0D7DE),
                        focusedTextColor = if (isDark) DarkTextPrimary else DeepNavy,
                        unfocusedTextColor = if (isDark) DarkTextPrimary else DeepNavy
                    )
                )

                Text(
                    "Quick Presets:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) DarkTextPrimary else DeepNavy
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            inputUrl = ApiConstants.LOCALHOST_BASE_URL
                            testStatus = null
                            isSuccess = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🔌 USB Cable / ADB (${ApiConstants.LOCALHOST_BASE_URL})", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            inputUrl = ApiConstants.LAN_BASE_URL
                            testStatus = null
                            isSuccess = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("📡 PC LAN IP (${ApiConstants.LAN_BASE_URL})", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            inputUrl = ApiConstants.EMULATOR_BASE_URL
                            testStatus = null
                            isSuccess = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🖥️ Android Emulator (${ApiConstants.EMULATOR_BASE_URL})", fontSize = 11.sp)
                    }
                }

                // Test Status Display
                if (testStatus != null) {
                    Surface(
                        color = if (isSuccess == true) AccentGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (isSuccess == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isSuccess == true) AccentGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                testStatus ?: "",
                                fontSize = 11.sp,
                                color = if (isSuccess == true) AccentGreen else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            isTesting = true
                            testStatus = "Testing connection..."
                            isSuccess = null
                            scope.launch {
                                try {
                                    ApiConstants.setBaseUrl(context, inputUrl)
                                    val api = RetrofitClient.getApiService()
                                    val res: ApiResponse<Map<String, Any>> = withContext(Dispatchers.IO) {
                                        api.getHealth()
                                    }
                                    if (res.isSuccess) {
                                        testStatus = "Connected! ${res.message ?: "Server running"}"
                                        isSuccess = true
                                    } else {
                                        testStatus = "Server returned: ${res.message}"
                                        isSuccess = false
                                    }
                                } catch (e: Exception) {
                                    testStatus = "Cannot reach server: ${e.localizedMessage ?: "Timeout"}"
                                    isSuccess = false
                                } finally {
                                    isTesting = false
                                }
                            }
                        },
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Test Connection")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    ApiConstants.setBaseUrl(context, inputUrl)
                    onUrlSaved()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Cyan else OceanBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save & Apply", color = if (isDark) DeepNavy else Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = if (isDark) DarkSurface else Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}
