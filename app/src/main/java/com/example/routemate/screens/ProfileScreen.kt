package com.example.routemate.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.routemate.R
import com.example.routemate.components.ServerConfigDialog
import com.example.routemate.data.remote.ApiConstants
import com.example.routemate.components.InfoCard
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.DarkBackground
import com.example.routemate.ui.theme.DarkBorder
import com.example.routemate.ui.theme.DarkSurface
import com.example.routemate.ui.theme.DarkSurfaceVariant
import com.example.routemate.ui.theme.DarkTextPrimary
import com.example.routemate.ui.theme.DarkTextSecondary
import com.example.routemate.viewmodel.ProfileViewModel

private val DeepNavy: Color @Composable get() = if (AppSettingsState.isDarkMode) DarkTextPrimary else Color(0xFF101D40)
private val OceanBlue: Color @Composable get() = if (AppSettingsState.isDarkMode) Cyan else Color(0xFF1764C0)
private val Cyan = Color(0xFF42D9F5)
private val IceBlue: Color @Composable get() = if (AppSettingsState.isDarkMode) DarkSurfaceVariant else Color(0xFFEDF5FF)
private val AccentGreen = Color(0xFF168568)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showServerConfig by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoSelected(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (AppSettingsState.isDarkMode) DarkBackground else IceBlue)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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
                    color = Color.Gray
                )
            }

            Spacer(Modifier.width(12.dp))

            Surface(
                color = IceBlue,
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
            trackColor = IceBlue
        )

        // Photo Header & Avatar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (AppSettingsState.isDarkMode) DarkSurface else Color.White),
        border = if (AppSettingsState.isDarkMode) androidx.compose.foundation.BorderStroke(1.dp, DarkBorder) else null,
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
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(4.dp, Cyan, CircleShape)
                            .background(IceBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.photoUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(state.photoUri),
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
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        containerColor = DeepNavy,
                        contentColor = Color.White,
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

                Text(
                    state.fullName.ifBlank { "Solo Rider" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )

                Text(
                    "${state.motorcycleBrandModel} • ${state.cityState}",
                    fontSize = 13.sp,
                    color = OceanBlue,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isEditing) {
                        Button(
                            onClick = { viewModel.saveProfile() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.cancelEditing() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = DeepNavy, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startEditing() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.togglePublicPreview(true) },
                            modifier = Modifier.weight(1f)
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
            Text("Riding Experience Level", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Beginner", "Intermediate", "Experienced").forEach { level ->
                    FilterChip(
                        selected = state.ridingExperience == level,
                        enabled = state.isEditing,
                        onClick = { viewModel.updateRidingExperience(level) },
                        label = { Text(level) }
                    )
                }
            }

            ProfileField("Years of Riding Experience", state.yearsExperience, state.isEditing, keyboardType = KeyboardType.Number) { viewModel.updateYearsExperience(it) }
            ProfileField("Motorcycle Brand & Model", state.motorcycleBrandModel, state.isEditing) { viewModel.updateMotorcycleBrandModel(it) }
            ProfileField("Engine Capacity (CC)", state.motorcycleCc, state.isEditing, keyboardType = KeyboardType.Number) { viewModel.updateMotorcycleCc(it) }

            Text("Preferred Riding Styles", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Scenic", "Adventure", "Touring", "Weekend rides").forEach { style ->
                    FilterChip(
                        selected = state.ridingStyles.contains(style),
                        enabled = state.isEditing,
                        onClick = { viewModel.toggleRidingStyle(style) },
                        label = { Text(style) }
                    )
                }
            }

            Text("Preferred Destinations", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Ooty", "Coorg", "Munnar", "Goa", "Pondicherry", "Araku Valley", "Yelagiri").forEach { dest ->
                    FilterChip(
                        selected = state.preferredDestinations.contains(dest),
                        enabled = state.isEditing,
                        onClick = { viewModel.toggleDestination(dest) },
                        label = { Text(dest) }
                    )
                }
            }

            SwitchRow("Open to Joining New Teams", state.isOpenToNewTeams, state.isEditing) { viewModel.updateOpenToNewTeams(it) }
            SwitchRow("Willing to Mentor Beginners", state.isWillingToMentor, state.isEditing) { viewModel.updateWillingToMentor(it) }
        }

        // Section 3: Trip Preferences & Budget Card
        SectionCard(title = "Trip Preferences & Budget") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
            ProfileField("Emergency Contact Name (Private)", state.emergencyContactName, state.isEditing) { viewModel.updateEmergencyContactName(it) }
            ProfileField("Emergency Contact Phone (Private)", state.emergencyContactPhone, state.isEditing, keyboardType = KeyboardType.Phone) { viewModel.updateEmergencyContactPhone(it) }
            ProfileField("Preferred Language", state.preferredLanguage, state.isEditing) { viewModel.updatePreferredLanguage(it) }
            SwitchRow("Share Live Location During Rides", state.shareLiveLocation, state.isEditing) { viewModel.updateShareLiveLocation(it) }
            CheckboxRow("I Agree to Group Riding & Safety Rules", state.agreeSafetyGuidelines, state.isEditing) { viewModel.updateAgreeSafetyGuidelines(it) }
        }

        // Section 5: Backend Server Connection
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
                        color = Color.Gray
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

        InfoCard(
            "Ride safely",
            "Wear protective gear, agree on meeting points, and share your trip plan with someone you trust."
        )
    }

    if (showServerConfig) {
        ServerConfigDialog(
            onDismiss = { showServerConfig = false },
            onUrlSaved = { showServerConfig = false }
        )
    }

    // Public Profile Preview Dialog
    if (state.isPreviewingPublic) {
        AlertDialog(
            onDismissRequest = { viewModel.togglePublicPreview(false) },
            confirmButton = {
                TextButton(onClick = { viewModel.togglePublicPreview(false) }) {
                    Text("Close Preview", color = OceanBlue, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text("Public Rider Profile Preview", fontWeight = FontWeight.Bold, color = DeepNavy)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This is how other riders see your profile:", fontSize = 12.sp, color = Color.Gray)
                    HorizontalDivider()
                    Text("• Name: ${state.fullName}", fontWeight = FontWeight.Bold)
                    Text("• Bike: ${state.motorcycleBrandModel} (${state.motorcycleCc} CC)")
                    Text("• Location: ${state.cityState}")
                    Text("• Experience: ${state.ridingExperience} (${state.yearsExperience} yrs)")
                    Text("• Preferred Styles: ${state.ridingStyles.joinToString(", ")}")
                    Text("• Destinations: ${state.preferredDestinations.joinToString(", ")}")
                    Text("• Budget Range: ₹${state.minBudget} - ₹${state.maxBudget}")
                    Text("• Bio: ${state.bio}")
                    Text("• Mentor Status: ${if (state.isWillingToMentor) "Available to mentor beginners ✓" else "Not mentoring"}")
                    Text("(Private emergency contact details are excluded from public view)", fontSize = 11.sp, color = Color.Gray)
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
        border = if (AppSettingsState.isDarkMode) androidx.compose.foundation.BorderStroke(1.dp, DarkBorder) else null,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy
            )
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    isEditing: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    if (isEditing) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(
                value.ifBlank { "Not set" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DeepNavy
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
        Text(label, fontSize = 14.sp, color = DeepNavy, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Cyan)
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
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = DeepNavy)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, color = DeepNavy)
    }
}
