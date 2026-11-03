package com.example.routemate.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.routemate.data.remote.RiderReviewDto
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.ReviewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderReviewsScreen(
    riderId: Int,
    riderName: String,
    onBack: () -> Unit,
    viewModel: ReviewsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode
    var showAddReviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(riderId) {
        viewModel.loadRiderReviews(riderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Ratings & Reviews",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )
                        Text(
                            riderName,
                            fontSize = 12.sp,
                            color = if (isDark) DarkTextSecondary else Color.Gray
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
                    IconButton(onClick = { viewModel.loadRiderReviews(riderId, showLoader = false) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = if (isDark) Cyan else OceanBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDark) DarkSurface else Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddReviewDialog = true },
                containerColor = if (isDark) Cyan else OceanBlue,
                contentColor = if (isDark) DeepNavy else Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.RateReview, contentDescription = "Add Review")
            }
        },
        containerColor = if (isDark) DarkBackground else IceBlue
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Success / Error Banner
            if (uiState.successMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
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

            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = String.format("%.1f", uiState.summary.averageRating),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) DarkTextPrimary else DeepNavy
                            )
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(30.dp))
                        }
                        Text(
                            text = "Based on ${uiState.summary.totalReviews} verified reviews",
                            fontSize = 12.sp,
                            color = if (isDark) DarkTextSecondary else Color.Gray
                        )
                    }

                    // Star count chips
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        (5 downTo 3).forEach { stars ->
                            val count = uiState.summary.breakdown["${stars}_star"] ?: 0
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${stars}★", fontSize = 11.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
                                LinearProgressIndicator(
                                    progress = { if (uiState.summary.totalReviews > 0) count.toFloat() / uiState.summary.totalReviews else 0f },
                                    modifier = Modifier.width(60.dp).height(5.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (isDark) Cyan else OceanBlue,
                                    trackColor = if (isDark) DarkBorder else IceBlue
                                )
                                Text("$count", fontSize = 10.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
                            }
                        }
                    }
                }
            }

            // Reviews List
            if (uiState.isLoading && uiState.reviews.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Cyan)
                }
            } else if (uiState.reviews.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.StarOutline, contentDescription = null, tint = if (isDark) DarkTextSecondary else Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No reviews yet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDark) DarkTextPrimary else DeepNavy)
                    Spacer(Modifier.height(4.dp))
                    Text("Complete a group ride with $riderName to leave the first review!", fontSize = 12.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.reviews, key = { it.id }) { review ->
                        ReviewCard(
                            review = review,
                            isDark = isDark,
                            onReport = { viewModel.reportReview(riderId, review.id, "Inappropriate review") }
                        )
                    }
                }
            }
        }
    }

    if (showAddReviewDialog) {
        AddReviewDialog(
            riderName = riderName,
            isDark = isDark,
            isSubmitting = uiState.isSubmitting,
            onDismiss = { showAddReviewDialog = false },
            onSubmit = { rating, text ->
                viewModel.submitReview(
                    reviewedUserId = riderId,
                    tripId = null,
                    rating = rating,
                    reviewText = text,
                    onSuccess = { showAddReviewDialog = false }
                )
            }
        )
    }
}

@Composable
fun ReviewCard(
    review: RiderReviewDto,
    isDark: Boolean,
    onReport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(if (isDark) Cyan.copy(alpha = 0.2f) else OceanBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(review.reviewerName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = if (isDark) Cyan else OceanBlue)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(review.reviewerName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isDark) DarkTextPrimary else DeepNavy)
                            if (review.reviewerIsVerified) {
                                Icon(Icons.Default.Verified, contentDescription = "Verified Rider", tint = if (isDark) Cyan else OceanBlue, modifier = Modifier.size(15.dp))
                            }
                        }
                        Text(review.tripReference, fontSize = 11.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
                    }
                }

                // Stars
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(review.rating.toInt()) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(15.dp))
                    }
                }
            }

            if (review.reviewText.isNotBlank()) {
                Text(review.reviewText, fontSize = 13.sp, color = if (isDark) DarkTextPrimary else DeepNavy, lineHeight = 18.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(review.createdAt.take(10), fontSize = 10.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
                TextButton(onClick = onReport, contentPadding = PaddingValues(0.dp)) {
                    Text("Report", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AddReviewDialog(
    riderName: String,
    isDark: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (rating: Double, reviewText: String) -> Unit
) {
    var selectedRating by remember { mutableStateOf(5.0) }
    var reviewText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate & Review $riderName", fontWeight = FontWeight.Bold, color = if (isDark) DarkTextPrimary else DeepNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Rating", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) DarkTextSecondary else DeepNavy)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { selectedRating = star.toDouble() }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (star <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star Stars",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("Write your review (optional)") },
                    placeholder = { Text("How was riding together? Punctuality, safety...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedRating, reviewText) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Cyan else OceanBlue)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit Review", color = if (isDark) DeepNavy else Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

