package com.example.routemate.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.data.remote.SessionManager
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.EmergencyViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencySosScreen(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: EmergencyViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val currentUserName = remember { sessionManager.getUserName() ?: "Rider" }
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode

    var selectedType by remember { mutableStateOf("Bike Breakdown") }
    var customMessage by remember { mutableStateOf("") }
    var notifyTeamMembers by remember { mutableStateOf(true) }

    // Screen mode: whether SOS has been activated and shows Emergency Action Screen
    var isSosActivated by remember { mutableStateOf(false) }

    // Location state
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            fetchLastKnownLocation(context) { loc -> currentLocation = loc }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fetchLastKnownLocation(context) { loc -> currentLocation = loc }
        }
    }

    // Load fresh contacts on entry
    LaunchedEffect(Unit) {
        viewModel.loadContacts()
    }

    // Auto-switch to active mode if backend has active alert
    LaunchedEffect(uiState.activeAlert) {
        if (uiState.activeAlert != null) {
            isSosActivated = true
        }
    }

    // Pulsing animation for the SOS button and active alerts
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Press and hold timer state (strictly 1.5 seconds)
    var isPressing by remember { mutableStateOf(false) }
    var pressProgress by remember { mutableFloatStateOf(0f) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Cooldown / debounce to prevent accidental repeated activations
    var lastActivatedTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPressing) {
        if (isPressing) {
            val startTime = System.currentTimeMillis()
            val holdDuration = 1500L
            while (isPressing && pressProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                pressProgress = (elapsed.toFloat() / holdDuration).coerceIn(0f, 1f)
                if (pressProgress >= 1f) {
                    val now = System.currentTimeMillis()
                    if (now - lastActivatedTime > 3000L) {
                        lastActivatedTime = now
                        // 1.5s Hold Complete -> Activate SOS & transmit to backend
                        isSosActivated = true
                        viewModel.triggerEmergencyAlert(
                            type = selectedType,
                            latitude = currentLocation?.latitude,
                            longitude = currentLocation?.longitude,
                            address = if (currentLocation != null) "GPS Lat: ${currentLocation?.latitude}, Lng: ${currentLocation?.longitude}" else null,
                            message = customMessage.ifBlank { null },
                            notifyTeam = notifyTeamMembers
                        )
                    }
                    isPressing = false
                    pressProgress = 0f
                }
                delay(25)
            }
        } else {
            pressProgress = 0f
        }
    }

    // Google Maps Link
    val mapLink = currentLocation?.let { "https://maps.google.com/?q=${it.latitude},${it.longitude}" }
    val standardAlertMessage = remember(currentUserName, selectedType, mapLink, customMessage) {
        val locText = if (mapLink != null) "Current location: $mapLink." else "Location: Coordinates unavailable."
        val extra = if (customMessage.isNotBlank()) " Note: $customMessage." else ""
        "EMERGENCY ALERT! $currentUserName has activated RouteMate SOS. Emergency type: $selectedType. $locText$extra Please contact them immediately."
    }

    // Helper to open dialer safely
    val openDialer: (String) -> Unit = { phoneNumber ->
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phoneNumber.trim()}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open phone dialer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper to open SMS app safely
    val openSmsApp: (String) -> Unit = { phoneNumber ->
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${phoneNumber.trim()}")
                putExtra("sms_body", standardAlertMessage)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open SMS app: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper to share location via Android Share Sheet
    val shareLocationSheet: () -> Unit = {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "RouteMate Emergency SOS Alert")
                putExtra(Intent.EXTRA_TEXT, standardAlertMessage)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "Share Emergency SOS Location"))
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                        Text(
                            if (isSosActivated) "EMERGENCY SOS ACTIVATED" else "Emergency SOS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (isSosActivated) Color(0xFFDC2626) else (if (isDark) DarkTextPrimary else DeepNavy)
                        )
                    }
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
                    TextButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = null, tint = if (isDark) Cyan else OceanBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("History", fontSize = 12.sp, color = if (isDark) Cyan else OceanBlue, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDark) DarkSurface else Color.White)
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ========================================================
            // SECTION 1: EMERGENCY ACTION SCREEN (WHEN ACTIVATED)
            // ========================================================
            if (isSosActivated) {
                // Header Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Column {
                                Text("EMERGENCY SOS ACTIVATED", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.White)
                                Text("Emergency Type: $selectedType", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))

                        Text(
                            "Select an action below. Calls open in your phone dialer for your direct confirmation.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 16.sp
                        )

                        // Notification delivery status notice
                        Surface(
                            color = Color.Black.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Server Notification Status: Event recorded in RouteMate MySQL. Direct phone calls and SMS via your device are ready.",
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(10.dp),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // OPTION A: CALL MY EMERGENCY CONTACTS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2EAF4)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                            Text("A. Call My Emergency Contacts", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isDark) DarkTextPrimary else DeepNavy)
                        }

                        if (uiState.contacts.isEmpty()) {
                            Text(
                                "No emergency contacts found. Please add parents or friends in Profile → Safety & Preferences.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )
                        } else {
                            uiState.contacts.forEach { contact ->
                                Surface(
                                    color = if (isDark) DarkSurfaceVariant else Color(0xFFF7FAFE),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, if (contact.isPrimary) AccentGreen else (if (isDark) DarkBorder else Color(0xFFE0EBF7))),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(contact.contactName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DeepNavy)
                                                Surface(color = OceanBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                                    Text(contact.relationship, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = OceanBlue, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                            if (contact.isPrimary) {
                                                Spacer(Modifier.height(3.dp))
                                                Surface(color = AccentGreen, shape = RoundedCornerShape(6.dp)) {
                                                    Text("PRIMARY ★", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(contact.phone, fontSize = 13.sp, color = if (isDark) DarkTextSecondary else Color.Gray, fontWeight = FontWeight.Medium)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            // Call Button -> ACTION_DIAL
                                            Button(
                                                onClick = { openDialer(contact.phone) },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // SMS Button -> ACTION_SENDTO
                                            IconButton(
                                                onClick = { openSmsApp(contact.phone) },
                                                modifier = Modifier.background(OceanBlue.copy(alpha = 0.12f), RoundedCornerShape(10.dp)).size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Sms, contentDescription = "SMS", tint = OceanBlue, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // OPTION B: CALL AMBULANCE (108)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                            Text("B. Call Ambulance (108)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isDark) DarkTextPrimary else DeepNavy)
                        }
                        Text(
                            "Indian national emergency ambulance response helpline. Operates 24/7 across states. Dialing will open in your phone app for your confirmation.",
                            fontSize = 12.sp,
                            color = if (isDark) DarkTextSecondary else Color.Gray,
                            lineHeight = 16.sp
                        )
                        Button(
                            onClick = { openDialer("108") },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("CALL AMBULANCE 108", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }

                // OPTION C: CALL POLICE / EMERGENCY SERVICES (112)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(20.dp))
                            Text("C. Call Police / Emergency Services (112)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isDark) DarkTextPrimary else DeepNavy)
                        }
                        Text(
                            "India's Unified Emergency Response Support System (ERSS) for Police, Fire, and Rescue. Dialing requires your final button press in the dialer.",
                            fontSize = 12.sp,
                            color = if (isDark) DarkTextSecondary else Color.Gray,
                            lineHeight = 16.sp
                        )
                        Button(
                            onClick = { openDialer("112") },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("CALL EMERGENCY SERVICES 112", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }

                // OPTION D: SHARE LIVE LOCATION & SOS MESSAGE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ShareLocation, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(20.dp))
                            Text("D. Share Live Location & SOS Message", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isDark) DarkTextPrimary else DeepNavy)
                        }

                        if (currentLocation != null) {
                            Surface(
                                color = OceanBlue.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("GPS: ${String.format("%.5f", currentLocation?.latitude)}, ${String.format("%.5f", currentLocation?.longitude)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepNavy)
                                    Text(mapLink ?: "", fontSize = 11.sp, color = OceanBlue)
                                }
                            }
                        } else {
                            Text("GPS coordinates currently unavailable. Alert will be shared without coordinates.", fontSize = 12.sp, color = Color.Gray)
                        }

                        Button(
                            onClick = shareLocationSheet,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share SOS Message & Location Link", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Reset / Cancel Alert
                OutlinedButton(
                    onClick = {
                        uiState.activeAlert?.let { viewModel.cancelAlert(it.id) }
                        isSosActivated = false
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Deactivate / Cancel Emergency Alert", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

            } else {
                // ========================================================
                // SECTION 2: SOS ACTIVATION SCREEN (BEFORE ACTIVATION)
                // ========================================================

                // Success / Error Banners
                if (uiState.errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                    }
                }

                // Large SOS Button with 1.5s Hold Interaction
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(if (isPressing) 1f else pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Red Glow
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626).copy(alpha = 0.2f))
                    )

                    // Hold Progress Ring (Visual hold-progress indicator)
                    if (pressProgress > 0f) {
                        CircularProgressIndicator(
                            progress = { pressProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFFDC2626),
                            strokeWidth = 7.dp
                        )
                    }

                    // Inner Main SOS Button
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPressing = true
                                        tryAwaitRelease()
                                        // Cancel activation if user releases before 1.5s
                                        isPressing = false
                                    },
                                    onTap = {
                                        // Single tap requires confirmation dialog
                                        showConfirmDialog = true
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SOS", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                            Text(
                                if (isPressing) "HOLD..." else "HOLD 1.5s",
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    "Press and hold for 1.5s to trigger emergency activation.",
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextSecondary else Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                // Quick Call Ambulance & Police Shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { openDialer("108") },
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("108 Ambulance", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { openDialer("112") },
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("112 Police", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Emergency Type Selector Chips
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Emergency Type", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isDark) DarkTextPrimary else DeepNavy)

                        val types = listOf("Bike Breakdown", "Accident", "Medical Emergency", "Severe Weather", "General SOS")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            types.take(3).forEach { t ->
                                FilterChip(
                                    selected = selectedType == t,
                                    onClick = { selectedType = t },
                                    label = { Text(t, fontSize = 11.sp) }
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            types.drop(3).forEach { t ->
                                FilterChip(
                                    selected = selectedType == t,
                                    onClick = { selectedType = t },
                                    label = { Text(t, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customMessage,
                            onValueChange = { customMessage = it },
                            label = { Text("Optional message (e.g. rear tyre blown out)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = notifyTeamMembers, onCheckedChange = { notifyTeamMembers = it })
                            Text("Also broadcast alert to active riding team", fontSize = 12.sp, color = if (isDark) DarkTextSecondary else DeepNavy)
                        }
                    }
                }

                // Location Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = if (hasLocationPermission && currentLocation != null) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                                contentDescription = null,
                                tint = if (hasLocationPermission && currentLocation != null) AccentGreen else Color(0xFFDC2626)
                            )
                            Column {
                                Text(
                                    if (hasLocationPermission) "GPS Location Active" else "Location Permission Required",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) DarkTextPrimary else DeepNavy
                                )
                                Text(
                                    if (currentLocation != null) "Lat: ${String.format("%.4f", currentLocation?.latitude)}, Lng: ${String.format("%.4f", currentLocation?.longitude)}"
                                    else if (!hasLocationPermission) "Tap Grant to include exact coordinates"
                                    else "Acquiring GPS fix...",
                                    fontSize = 11.sp,
                                    color = if (isDark) DarkTextSecondary else Color.Gray
                                )
                            }
                        }

                        if (!hasLocationPermission) {
                            TextButton(
                                onClick = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                            ) {
                                Text("Grant", color = if (isDark) Cyan else OceanBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Emergency Contacts Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Saved Contacts (${uiState.contacts.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDark) DarkTextPrimary else DeepNavy)
                            Text("Configured in Profile", fontSize = 10.sp, color = Color.Gray)
                        }

                        if (uiState.contacts.isEmpty()) {
                            Text("No emergency contacts configured yet. Go to Profile → Safety & Preferences to add contacts.", fontSize = 11.sp, color = Color.Gray)
                        } else {
                            uiState.contacts.forEach { c ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("${c.contactName} (${c.relationship})", fontSize = 12.sp, color = if (isDark) DarkTextPrimary else DeepNavy)
                                        if (c.isPrimary) {
                                            Text("★", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(c.phone, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Cyan else OceanBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Tap Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Emergency SOS", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = {
                Text("Are you sure you want to activate RouteMate SOS ($selectedType)? This will log the emergency event and open immediate emergency calling options.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isSosActivated = true
                        viewModel.triggerEmergencyAlert(
                            type = selectedType,
                            latitude = currentLocation?.latitude,
                            longitude = currentLocation?.longitude,
                            address = if (currentLocation != null) "GPS Lat: ${currentLocation?.latitude}, Lng: ${currentLocation?.longitude}" else null,
                            message = customMessage.ifBlank { null },
                            notifyTeam = notifyTeamMembers
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("YES, ACTIVATE SOS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// Helper to get location via LocationManager (requires no external google-play-services dependency)
@Suppress("MissingPermission")
private fun fetchLastKnownLocation(context: Context, onLocation: (Location?) -> Unit) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm != null) {
            val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val loc = gpsLoc ?: netLoc
            onLocation(loc)
        } else {
            onLocation(null)
        }
    } catch (e: Exception) {
        onLocation(null)
    }
}
