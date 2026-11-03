package com.example.routemate.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.VerificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderVerificationScreen(
    onBack: () -> Unit,
    viewModel: VerificationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode

    var fullName by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var selectedDocType by remember { mutableStateOf("Driving Licence") }
    var idDocAttached by remember { mutableStateOf(false) }
    var rcDocAttached by remember { mutableStateOf(false) }
    var selfieAttached by remember { mutableStateOf(false) }

    val status = uiState.status.verificationStatus.lowercase()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Rider Verification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) DarkTextPrimary else DeepNavy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) DarkTextPrimary else DeepNavy
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadStatus(showLoader = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = if (isDark) Cyan else OceanBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) DarkSurface else Color.White
                )
            )
        },
        containerColor = if (isDark) DarkBackground else IceBlue
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status Banner
            when (status) {
                "approved" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AccentGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text("Verified Rider", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccentGreen)
                                Text("Your identity and licence are authenticated. The verified badge appears on your profile and rides.", fontSize = 12.sp, color = if (isDark) DarkTextSecondary else DeepNavy)
                                if (uiState.status.maskedLicense != null) {
                                    Text("Licence: ${uiState.status.maskedLicense}", fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                "pending" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AccentOrange)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AccentOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text("Verification Pending Review", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccentOrange)
                                Text("Your documents have been submitted and are under review by RouteMate administrators.", fontSize = 12.sp, color = if (isDark) DarkTextSecondary else DeepNavy)
                            }
                        }
                    }
                }
                "rejected" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                            Column {
                                Text("Verification Not Approved", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.error)
                                Text(uiState.status.rejectionReason ?: "Document details could not be authenticated.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text("Please re-check your details and resubmit below.", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {
                    // Unverified Intro Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = if (isDark) Cyan else OceanBlue)
                                Text("Get Verified Rider Badge", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDark) DarkTextPrimary else DeepNavy)
                            }
                            Text(
                                "RouteMate verifies riders to foster safe group touring. Verified riders get a blue checkmark badge on their profile, matching cards, and teams.",
                                fontSize = 12.sp,
                                color = if (isDark) DarkTextSecondary else Color.Gray,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Success message
            if (uiState.successMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        uiState.successMessage ?: "",
                        color = AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Submission Form (Shown if not yet approved and not currently pending)
            if (status != "approved" && status != "pending") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Submit Identity Documents",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Legal Name (as on Driving Licence)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Document Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) DarkTextSecondary else DeepNavy)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("Driving Licence", "Motorcycle RC", "Govt ID").forEach { dt ->
                                FilterChip(
                                    selected = selectedDocType == dt,
                                    onClick = { selectedDocType = dt },
                                    label = { Text(dt, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = licenseNumber,
                            onValueChange = { licenseNumber = it },
                            label = { Text("Licence / Registration Number (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Document Attachment Actions
                        DocumentUploadRow(
                            label = "Driving Licence Document",
                            isAttached = idDocAttached,
                            onToggle = { idDocAttached = !idDocAttached },
                            isDark = isDark
                        )

                        DocumentUploadRow(
                            label = "Motorcycle RC (Optional)",
                            isAttached = rcDocAttached,
                            onToggle = { rcDocAttached = !rcDocAttached },
                            isDark = isDark
                        )

                        DocumentUploadRow(
                            label = "Selfie / Identity Photo",
                            isAttached = selfieAttached,
                            onToggle = { selfieAttached = !selfieAttached },
                            isDark = isDark
                        )

                        if (uiState.errorMessage != null) {
                            Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.submitVerification(
                                    fullName = fullName.ifBlank { "Rider" },
                                    documentType = selectedDocType,
                                    licenseNumber = licenseNumber.ifBlank { null },
                                    idDocumentPath = if (idDocAttached) "docs/dl_attached.pdf" else null,
                                    vehicleRcPath = if (rcDocAttached) "docs/rc_attached.pdf" else null,
                                    selfiePath = if (selfieAttached) "docs/selfie_attached.jpg" else null
                                )
                            },
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Cyan else OceanBlue,
                                contentColor = if (isDark) DeepNavy else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Submit for Administrator Verification", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Security and Privacy Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceVariant else IceBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = if (isDark) Cyan else OceanBlue, modifier = Modifier.size(18.dp))
                    Text(
                        "Your verification documents are securely stored and never exposed to other riders. Full licence numbers are masked for your safety.",
                        fontSize = 11.sp,
                        color = if (isDark) DarkTextSecondary else DeepNavy,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentUploadRow(
    label: String,
    isAttached: Boolean,
    onToggle: () -> Unit,
    isDark: Boolean
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isAttached) AccentGreen else (if (isDark) DarkBorder else Color.LightGray))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (isAttached) Icons.Default.CheckCircle else Icons.Default.AttachFile,
                    contentDescription = null,
                    tint = if (isAttached) AccentGreen else (if (isDark) DarkTextSecondary else Color.Gray)
                )
                Text(
                    label,
                    fontSize = 13.sp,
                    color = if (isDark) DarkTextPrimary else DeepNavy,
                    fontWeight = if (isAttached) FontWeight.Bold else FontWeight.Normal
                )
            }
            TextButton(onClick = onToggle) {
                Text(
                    if (isAttached) "Attached" else "Attach File",
                    color = if (isAttached) AccentGreen else (if (isDark) Cyan else OceanBlue),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

