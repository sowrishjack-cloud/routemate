package com.example.routemate.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.routemate.R
import com.example.routemate.components.InfoCard
import com.example.routemate.components.ServerConfigDialog
import com.example.routemate.data.remote.ApiConstants
import com.example.routemate.data.remote.RetrofitClient
import com.example.routemate.data.remote.SessionManager
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.DarkBackground
import com.example.routemate.ui.theme.DarkBorder
import com.example.routemate.ui.theme.DarkSurface
import com.example.routemate.ui.theme.DarkSurfaceVariant
import com.example.routemate.ui.theme.DarkTextPrimary
import com.example.routemate.ui.theme.DarkTextSecondary
import com.example.routemate.viewmodel.EmergencyViewModel
import com.example.routemate.viewmodel.ProfileViewModel
import com.example.routemate.viewmodel.ReviewsViewModel
import com.example.routemate.viewmodel.VerificationViewModel

private val DeepNavy: Color @Composable get() = if (AppSettingsState.isDarkMode) DarkTextPrimary else Color(0xFF101D40)
private val OceanBlue: Color @Composable get() = if (AppSettingsState.isDarkMode) Cyan else Color(0xFF1764C0)
private val Cyan = Color(0xFF42D9F5)
private val IceBlue: Color @Composable get() = if (AppSettingsState.isDarkMode) DarkSurfaceVariant else Color(0xFFEDF5FF)
private val AccentGreen = Color(0xFF168568)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    verificationViewModel: VerificationViewModel = viewModel(),
    reviewsViewModel: ReviewsViewModel = viewModel(),
    emergencyViewModel: EmergencyViewModel = viewModel(),
    onOpenVerification: () -> Unit = {},
    onOpenReviews: (userId: Int, userName: String) -> Unit = { _, _ -> },
    onOpenEmergencySos: () -> Unit = {},
    onOpenEmergencyHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val state by viewModel.uiState.collectAsState()
    val verificationState by verificationViewModel.uiState.collectAsState()
    val reviewsState by reviewsViewModel.uiState.collectAsState()
    val emergencyState by emergencyViewModel.uiState.collectAsState()

    val currentUserId = remember { sessionManager.getUserId() }
    val currentUserName = remember { sessionManager.getUserName() ?: "Arjun Kumar" }

    val isDark = AppSettingsState.isDarkMode
    var showServerConfig by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<com.example.routemate.data.remote.EmergencyContactDto?>(null) }

    LaunchedEffect(Unit) {
        verificationViewModel.loadStatus(showLoader = false)
        emergencyViewModel.loadContacts()
        if (currentUserId > 0) {
            reviewsViewModel.loadRiderReviews(currentUserId, showLoader = false)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoSelected(context, uri)
        }
    }

    // Single unified vertical scrolling container with ample bottom clearance for bottom nav
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBackground else IceBlue)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Title & Profile Completeness Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "My Rider Profile",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepNavy
                )
                Text(
                    "Manage your rider specs, bike info & trip budget",
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextSecondary else Color.Gray
                )
            }

            Spacer(Modifier.width(12.dp))

            Surface(
                color = if (isDark) DarkSurfaceVariant else IceBlue,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Profile ${state.completionPercentage}%",
                    color = OceanBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        LinearProgressIndicator(
            progress = { state.completionPercentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Cyan,
            trackColor = if (isDark) DarkSurfaceVariant else IceBlue
        )

        // Photo Header & Avatar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
            border = if (isDark) BorderStroke(1.dp, DarkBorder) else null,
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd
                ) {
                    val resolvedAvatarUrl = remember(state.avatarUrl) {
                        RetrofitClient.resolveMediaUrl(state.avatarUrl)
                    }

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(4.dp, Cyan, CircleShape)
                            .background(if (isDark) DarkSurfaceVariant else IceBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.photoUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(state.photoUri),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (!resolvedAvatarUrl.isNullOrBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(resolvedAvatarUrl),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_avatar_1),
                                contentDescription = "Default Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        if (state.isUploadingPhoto) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Cyan,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        containerColor = if (isDark) Cyan else DeepNavy,
                        contentColor = if (isDark) DeepNavy else Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Edit Photo",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        state.fullName.ifBlank { "Solo Rider" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                    if (verificationState.status.isVerified || verificationState.status.verificationStatus.equals("approved", ignoreCase = true)) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Rider",
                            tint = if (isDark) Cyan else OceanBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    "${state.motorcycleBrandModel} • ${state.cityState}",
                    fontSize = 13.sp,
                    color = OceanBlue,
                    fontWeight = FontWeight.Medium
                )

                // Rider Ratings summary pill
                Surface(
                    onClick = { onOpenReviews(currentUserId, currentUserName) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD),
                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFD6E4F5))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (reviewsState.summary.totalReviews > 0)
                                String.format(java.util.Locale.US, "%.1f", reviewsState.summary.averageRating)
                            else
                                "New Rider",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        Text(
                            text = "(${reviewsState.summary.totalReviews} reviews)",
                            fontSize = 11.sp,
                            color = OceanBlue,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = OceanBlue,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isEditing) {
                        Button(
                            onClick = { viewModel.saveProfile() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Cyan else DeepNavy),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save", fontWeight = FontWeight.Bold, color = if (isDark) DeepNavy else Color.White)
                        }

                        OutlinedButton(
                            onClick = { viewModel.cancelEditing() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = DeepNavy, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startEditing() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Cyan else DeepNavy),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isDark) DeepNavy else Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold, color = if (isDark) DeepNavy else Color.White)
                        }

                        OutlinedButton(
                            onClick = { viewModel.togglePublicPreview(true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Preview", color = DeepNavy, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Banners
        if (state.successMessage != null) {
            Surface(
                color = AccentGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        state.successMessage!!,
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (state.validationError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        state.validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Rider Verification Card
        SectionCard(title = "Rider Verification") {
            val verStatus = verificationState.status.verificationStatus.lowercase()
            val isVerified = verificationState.status.isVerified || verStatus == "approved"

            when {
                isVerified -> {
                    Surface(
                        color = AccentGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Verified",
                                tint = AccentGreen,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Verified Rider Status: Approved ✓",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AccentGreen
                                )
                                Text(
                                    "Your driving licence and rider credentials have been verified by RouteMate administrators.",
                                    fontSize = 12.sp,
                                    color = if (isDark) DarkTextSecondary else Color.Gray,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenVerification,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AccentGreen)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("View Verification Details", color = AccentGreen, fontWeight = FontWeight.Bold)
                    }
                }
                verStatus == "pending" -> {
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Pending",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Verification Under Review",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFD97706)
                                )
                                Text(
                                    "Your documents have been submitted and are being reviewed by administrators. Average approval time is 2-4 hours.",
                                    fontSize = 12.sp,
                                    color = if (isDark) DarkTextSecondary else Color.Gray,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onOpenVerification,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Check Verification Status", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                verStatus == "rejected" -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Rejected",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Verification Rejected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    verificationState.status.rejectionReason?.let { "Reason: $it" }
                                        ?: "Document details could not be validated. Please resubmit clear photos of your driving licence.",
                                    fontSize = 12.sp,
                                    color = if (isDark) DarkTextSecondary else Color.Gray,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onOpenVerification,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Re-submit Verification Documents", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Surface(
                        color = (if (isDark) Cyan else OceanBlue).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, (if (isDark) Cyan else OceanBlue).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Verify Badge",
                                tint = if (isDark) Cyan else OceanBlue,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Get Verified on RouteMate",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DeepNavy
                                )
                                Text(
                                    "Earn the Verified Rider badge to build trust in group rides, join exclusive tours, and mentor beginner riders.",
                                    fontSize = 12.sp,
                                    color = if (isDark) DarkTextSecondary else Color.Gray,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onOpenVerification,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Cyan else OceanBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = if (isDark) DeepNavy else Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Verify Profile & Licence", color = if (isDark) DeepNavy else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Rider Ratings & Reviews Card
        SectionCard(title = "Ratings & Reviews") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (reviewsState.summary.totalReviews > 0)
                                String.format(java.util.Locale.US, "%.1f", reviewsState.summary.averageRating)
                            else
                                "5.0",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepNavy
                        )
                        Column {
                            Row {
                                repeat(5) { index ->
                                    val filled = index < reviewsState.summary.averageRating.toInt()
                                    Icon(
                                        imageVector = if (filled) Icons.Default.Star else Icons.Default.StarOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB800),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                "${reviewsState.summary.totalReviews} verified reviews",
                                fontSize = 11.sp,
                                color = if (isDark) DarkTextSecondary else Color.Gray
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { onOpenReviews(currentUserId, currentUserName) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, OceanBlue)
                ) {
                    Text("View All", color = OceanBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(10.dp))
                }
            }
        }

        // Section 1: Personal Information Card
        SectionCard(title = "Personal Information") {
            ProfileField("Full Name", state.fullName, state.isEditing) { viewModel.updateFullName(it) }
            ProfileField("Email Address", state.email, state.isEditing, keyboardType = KeyboardType.Email) { viewModel.updateEmail(it) }
            ProfileField("Phone Number (Optional)", state.phoneNumber, state.isEditing, keyboardType = KeyboardType.Phone) { viewModel.updatePhoneNumber(it) }
            ProfileField("Age", state.age, state.isEditing, keyboardType = KeyboardType.Number) { viewModel.updateAge(it) }
            ProfileField("City & State", state.cityState, state.isEditing) { viewModel.updateCityState(it) }
            ProfileField("Bio / Riding Pitch", state.bio, state.isEditing, singleLine = false) { viewModel.updateBio(it) }
        }

        // Section 2: Riding Information Card
        SectionCard(title = "Riding Information") {
            Text("Riding Experience Level", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DeepNavy)
            
            // Experience level: 3 equal width buttons with consistent height and no wrapping
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Beginner", "Intermediate", "Experienced").forEach { level ->
                    val isSelected = state.ridingExperience.equals(level, ignoreCase = true)
                    Surface(
                        onClick = { if (state.isEditing) viewModel.updateRidingExperience(level) },
                        enabled = state.isEditing,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) (if (isDark) Cyan.copy(alpha = 0.2f) else OceanBlue.copy(alpha = 0.15f)) else (if (isDark) DarkSurfaceVariant else Color(0xFFF5F8FC)),
                        border = BorderStroke(1.5.dp, if (isSelected) (if (isDark) Cyan else OceanBlue) else (if (isDark) DarkBorder else Color(0xFFD6E2F2))),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = level,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) (if (isDark) Cyan else OceanBlue) else DeepNavy,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            ProfileField("Years of Riding Experience", state.yearsExperience, state.isEditing, keyboardType = KeyboardType.Number) { viewModel.updateYearsExperience(it) }
            ProfileField("Motorcycle Brand & Model", state.motorcycleBrandModel, state.isEditing) { viewModel.updateMotorcycleBrandModel(it) }
            ProfileField("Engine Capacity (CC)", state.motorcycleCc, state.isEditing, keyboardType = KeyboardType.Number) { viewModel.updateMotorcycleCc(it) }

            // Preferred Riding Styles with responsive FlowRow wrapping and distinct chip styles
            Text("Preferred Riding Styles", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DeepNavy)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Scenic", "Adventure", "Touring", "Weekend rides").forEach { style ->
                    val isSelected = state.ridingStyles.any { it.equals(style, ignoreCase = true) }
                    Surface(
                        onClick = { if (state.isEditing) viewModel.toggleRidingStyle(style) },
                        enabled = state.isEditing,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) (if (isDark) Cyan.copy(alpha = 0.2f) else OceanBlue.copy(alpha = 0.12f)) else (if (isDark) DarkSurfaceVariant else Color(0xFFF5F8FC)),
                        border = BorderStroke(1.dp, if (isSelected) (if (isDark) Cyan else OceanBlue) else (if (isDark) DarkBorder else Color(0xFFD6E2F2))),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isDark) Cyan else OceanBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = style,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) (if (isDark) Cyan else OceanBlue) else (if (isDark) DarkTextSecondary else DeepNavy)
                            )
                        }
                    }
                }
            }

            // Preferred Destinations with responsive FlowRow wrapping
            Text("Preferred Destinations", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DeepNavy)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Ooty", "Coorg", "Munnar", "Goa", "Pondicherry", "Araku Valley", "Yelagiri").forEach { dest ->
                    val isSelected = state.preferredDestinations.any { it.equals(dest, ignoreCase = true) }
                    Surface(
                        onClick = { if (state.isEditing) viewModel.toggleDestination(dest) },
                        enabled = state.isEditing,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) (if (isDark) Cyan.copy(alpha = 0.2f) else OceanBlue.copy(alpha = 0.12f)) else (if (isDark) DarkSurfaceVariant else Color(0xFFF5F8FC)),
                        border = BorderStroke(1.dp, if (isSelected) (if (isDark) Cyan else OceanBlue) else (if (isDark) DarkBorder else Color(0xFFD6E2F2))),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isDark) Cyan else OceanBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = dest,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) (if (isDark) Cyan else OceanBlue) else (if (isDark) DarkTextSecondary else DeepNavy)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = if (isDark) DarkBorder else Color(0xFFEEF3FA))

            SwitchRow("Open to Joining New Teams", state.isOpenToNewTeams, state.isEditing) { viewModel.updateOpenToNewTeams(it) }
            SwitchRow("Willing to Mentor Beginners", state.isWillingToMentor, state.isEditing) { viewModel.updateWillingToMentor(it) }
        }

        // Section 3: Trip Preferences & Budget Card
        SectionCard(title = "Trip Preferences & Budget") {
            // Responsive budget row with clear currency labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ProfileField("Min Budget (₹)", state.minBudget, state.isEditing, keyboardType = KeyboardType.Number) { viewModel.updateMinBudget(it) }
                }
                Box(modifier = Modifier.weight(1f)) {
                    ProfileField("Max Budget (₹)", state.maxBudget, state.isEditing, keyboardType = KeyboardType.Number) { viewModel.updateMaxBudget(it) }
                }
            }

            ProfileField("Preferred Trip Duration", state.preferredDuration, state.isEditing) { viewModel.updatePreferredDuration(it) }
            ProfileField("Preferred Departure Time", state.preferredDepartureTime, state.isEditing) { viewModel.updatePreferredDepartureTime(it) }
            ProfileField("Travel Pace (e.g. Relaxed, Moderate)", state.travelPace, state.isEditing) { viewModel.updateTravelPace(it) }
            ProfileField("Accommodation (e.g. Budget, Standard)", state.accommodationPreference, state.isEditing) { viewModel.updateAccommodationPreference(it) }
            ProfileField("Starting City / Meeting Point", state.startingCity, state.isEditing) { viewModel.updateStartingCity(it) }
        }

        // Section 4: Safety & Preferences Card
        SectionCard(title = "Safety & Preferences") {
            CheckboxRow("Helmet & Protective Gear Confirmed", state.helmetGearConfirmed, state.isEditing) { viewModel.updateHelmetGearConfirmed(it) }
            ProfileField("Preferred Language", state.preferredLanguage, state.isEditing) { viewModel.updatePreferredLanguage(it) }
            
            HorizontalDivider(color = if (isDark) DarkBorder else Color(0xFFEEF3FA))

            SwitchRow("Share Live Location During Rides", state.shareLiveLocation, state.isEditing) { viewModel.updateShareLiveLocation(it) }
            CheckboxRow("I Agree to Group Riding & Safety Rules", state.agreeSafetyGuidelines, state.isEditing) { viewModel.updateAgreeSafetyGuidelines(it) }

            HorizontalDivider(color = if (isDark) DarkBorder else Color(0xFFEEF3FA))

            // Emergency Contacts Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Emergency Contacts", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepNavy)
                    Text(
                        "Notified with GPS location when SOS is triggered",
                        fontSize = 11.sp,
                        color = if (isDark) DarkTextSecondary else Color.Gray
                    )
                }
                TextButton(
                    onClick = { showAddContactDialog = true }
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", color = OceanBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (emergencyState.contacts.isEmpty()) {
                Surface(
                    color = if (isDark) DarkSurfaceVariant else Color(0xFFF7F9FC),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No emergency contacts added yet. Add a family member or friend to ensure they receive alerts during SOS situations.",
                        fontSize = 12.sp,
                        color = if (isDark) DarkTextSecondary else Color.Gray,
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 16.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    emergencyState.contacts.forEach { contact ->
                        Surface(
                            color = if (isDark) DarkSurfaceVariant else Color(0xFFF7FAFE),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2EDF9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(contact.contactName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeepNavy)
                                        if (contact.isPrimary) {
                                            Surface(
                                                color = AccentGreen,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "PRIMARY ★",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            color = (if (isDark) Cyan else OceanBlue).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                contact.relationship,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Cyan else OceanBlue,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(contact.phone, fontSize = 12.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingContact = contact
                                            showAddContactDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit contact",
                                            tint = OceanBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { emergencyViewModel.deleteContact(contact.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete contact",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = if (isDark) DarkBorder else Color(0xFFEEF3FA))

            // SOS Trigger and History Actions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenEmergencySos,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Sos, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("TRIGGER EMERGENCY SOS", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = onOpenEmergencyHistory,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFCFD9E8))
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View SOS Alert History", color = DeepNavy, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }

        // Section 5: App Theme & Display Preference (Synced to MySQL users.theme_preference)
        SectionCard(title = "Theme & Display Preferences") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Dark Mode Theme",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DeepNavy
                    )
                    Text(
                        if (isDark) "Dark theme enabled (synced to MySQL)" else "Light theme active (synced to MySQL)",
                        fontSize = 12.sp,
                        color = if (isDark) DarkTextSecondary else Color.Gray
                    )
                }

                Switch(
                    checked = isDark,
                    onCheckedChange = { checked ->
                        AppSettingsState.isDarkMode = checked
                        viewModel.updateThemePreference(if (checked) "dark" else "light")
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Cyan
                    )
                )
            }
        }

        // Section 6: Backend Server Connection
        SectionCard(title = "Backend Server Connection") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "REST API Target URL",
                        fontSize = 12.sp,
                        color = if (isDark) DarkTextSecondary else Color.Gray
                    )
                    Text(
                        ApiConstants.BASE_URL,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepNavy
                    )
                }

                Button(
                    onClick = { showServerConfig = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Change", fontSize = 12.sp)
                }
            }
        }

        // Section 6: Account Session & Logout
        OutlinedButton(
            onClick = { showLogoutConfirm = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Log Out of RouteMate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        InfoCard(
            "Ride safely",
            "Wear protective gear, agree on meeting points, and share your trip plan with someone you trust."
        )

        // Essential bottom clearance so the final content is never covered by the floating bottom navigation bar
        Spacer(Modifier.height(110.dp))
    }

    if (showServerConfig) {
        ServerConfigDialog(
            onDismiss = { showServerConfig = false },
            onUrlSaved = { showServerConfig = false }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = if (isDark) DarkSurface else Color.White,
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    "Log Out of RouteMate?",
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
            },
            text = {
                Text(
                    "You will be signed out from your rider account. Your local session and preferences will be cleared.",
                    fontSize = 13.sp,
                    color = if (isDark) DarkTextSecondary else Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        sessionManager.clearSession()
                        showLogoutConfirm = false
                        AppSettingsState.activeTestNotification = "👋 Logged out from Rider Profile."
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm Log Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add / Edit Emergency Contact Dialog
    if (showAddContactDialog) {
        val editing = editingContact
        var newName by remember(editing) { mutableStateOf(editing?.contactName ?: "") }
        var newPhone by remember(editing) { mutableStateOf(editing?.phone ?: "") }
        var selectedRel by remember(editing) { mutableStateOf(editing?.relationship ?: "Family") }
        var isPrimaryContact by remember(editing) { mutableStateOf(editing?.isPrimary ?: false) }
        val relations = listOf("Parent", "Family", "Friend", "Spouse", "Partner", "Doctor", "Other")

        AlertDialog(
            onDismissRequest = {
                showAddContactDialog = false
                editingContact = null
            },
            title = {
                Text(
                    if (editing != null) "Edit Emergency Contact" else "Add Emergency Contact",
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Contact Name (e.g. Mom, Ramesh)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number (+91 ...)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text("Relationship", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepNavy)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        relations.forEach { rel ->
                            val isSelected = selectedRel == rel
                            Surface(
                                onClick = { selectedRel = rel },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) OceanBlue else (if (isDark) DarkSurfaceVariant else Color(0xFFF0F4FA)),
                                border = BorderStroke(1.dp, if (isSelected) OceanBlue else Color.Transparent)
                            ) {
                                Text(
                                    rel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else DeepNavy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Primary Contact Toggle Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isPrimaryContact = !isPrimaryContact }.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isPrimaryContact,
                            onCheckedChange = { isPrimaryContact = it },
                            colors = CheckboxDefaults.colors(checkedColor = AccentGreen)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Mark as Primary Emergency Contact",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DeepNavy
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newPhone.isNotBlank()) {
                            if (editing != null) {
                                emergencyViewModel.updateContact(
                                    id = editing.id,
                                    name = newName.trim(),
                                    phone = newPhone.trim(),
                                    relationship = selectedRel,
                                    isPrimary = isPrimaryContact
                                ) {
                                    showAddContactDialog = false
                                    editingContact = null
                                }
                            } else {
                                emergencyViewModel.addContact(
                                    name = newName.trim(),
                                    phone = newPhone.trim(),
                                    relationship = selectedRel,
                                    isPrimary = isPrimaryContact
                                ) {
                                    showAddContactDialog = false
                                }
                            }
                            showAddContactDialog = false
                            editingContact = null
                        }
                    },
                    enabled = newName.isNotBlank() && newPhone.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Contact", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddContactDialog = false
                    editingContact = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (AppSettingsState.isDarkMode) DarkSurface else Color.White),
        border = if (AppSettingsState.isDarkMode) BorderStroke(1.dp, DarkBorder) else null,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy
            )
            HorizontalDivider(color = if (AppSettingsState.isDarkMode) DarkBorder else Color(0xFFEEF3FA))
            content()
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String?,
    isEditing: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    val displayVal = value ?: ""
    if (isEditing) {
        OutlinedTextField(
            value = displayVal,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp)
        )
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 12.sp, color = if (AppSettingsState.isDarkMode) DarkTextSecondary else Color.Gray)
            Spacer(Modifier.height(2.dp))
            Text(
                displayVal.ifBlank { "Not set" },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepNavy,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = DeepNavy,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Cyan
            )
        )
    }
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = if (AppSettingsState.isDarkMode) Cyan else DeepNavy,
                checkmarkColor = if (AppSettingsState.isDarkMode) DeepNavy else Color.White
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = DeepNavy,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
