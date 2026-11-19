package com.example.routemate.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Refresh
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
import com.example.routemate.data.remote.TeamMessageDto
import com.example.routemate.models.AppSettingsState
import com.example.routemate.models.Team
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    team: Team,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var messageInput by remember { mutableStateOf("") }

    val teamIdInt = team.id.toIntOrNull() ?: 1

    LaunchedEffect(team.id) {
        viewModel.initChat(teamIdInt, team.name)
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPolling()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState.teamName.isNotBlank()) uiState.teamName else team.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )
                        Text(
                            text = "${team.members} riders • ${team.destination}",
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
                    IconButton(onClick = { viewModel.loadMessages(teamIdInt, showLoader = false) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Messages",
                            tint = if (isDark) Cyan else OceanBlue
                        )
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
        ) {
            // Error banner if any
            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.loadMessages(teamIdInt, showLoader = true) }) {
                            Text("Retry", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Message List / Empty State / Loading State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading && uiState.messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Cyan)
                    }
                } else if (uiState.messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(if (isDark) DarkSurface else Cyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = if (isDark) Cyan else OceanBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No messages yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Coordinate routes, stops, and gear with your crew in this group chat.",
                            fontSize = 12.sp,
                            color = if (isDark) DarkTextSecondary else Color.Gray,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            ChatMessageBubble(message = message, isDark = isDark)
                        }
                    }
                }
            }

            // Input Bar at the Bottom
            Surface(
                color = if (isDark) DarkSurface else Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Type a message to the team...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 120.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan,
                            unfocusedBorderColor = if (isDark) DarkBorder else Color.LightGray.copy(alpha = 0.5f),
                            focusedContainerColor = if (isDark) DarkBackground else IceBlue.copy(alpha = 0.5f),
                            unfocusedContainerColor = if (isDark) DarkBackground else IceBlue.copy(alpha = 0.3f),
                            focusedTextColor = if (isDark) DarkTextPrimary else DeepNavy,
                            unfocusedTextColor = if (isDark) DarkTextPrimary else DeepNavy
                        ),
                        singleLine = false,
                        maxLines = 4
                    )

                    IconButton(
                        onClick = {
                            val textToSend = messageInput
                            if (textToSend.isNotBlank()) {
                                viewModel.sendMessage(teamIdInt, textToSend)
                                messageInput = ""
                            }
                        },
                        enabled = messageInput.isNotBlank() && !uiState.isSending,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (messageInput.isNotBlank()) (if (isDark) Cyan else OceanBlue)
                                else (if (isDark) DarkBorder else Color.LightGray)
                            )
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (isDark && messageInput.isNotBlank()) DeepNavy else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: TeamMessageDto,
    isDark: Boolean
) {
    val isMine = message.isMine

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (!isMine) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            ) {
                // Sender initials avatar circle
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(OceanBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.senderName.take(1).uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OceanBlue
                    )
                }
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Cyan else DeepNavy
                )
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isMine) {
                        if (isDark) Cyan else OceanBlue
                    } else {
                        if (isDark) DarkSurface else Color.White
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.message,
                color = if (isMine) {
                    if (isDark) DeepNavy else Color.White
                } else {
                    if (isDark) DarkTextPrimary else DeepNavy
                },
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        // Timestamp
        val timeDisplay = formatMessageTime(message.createdAt)
        Text(
            text = timeDisplay,
            fontSize = 10.sp,
            color = if (isDark) DarkTextSecondary else Color.Gray,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatMessageTime(rawDate: String): String {
    return try {
        // rawDate is typically "2026-11-02 10:45:00"
        if (rawDate.contains(" ")) {
            val parts = rawDate.split(" ")
            val timePart = parts[1] // "10:45:00"
            val timeSub = timePart.substring(0, 5) // "10:45"
            timeSub
        } else {
            rawDate
        }
    } catch (e: Exception) {
        rawDate
    }
}

