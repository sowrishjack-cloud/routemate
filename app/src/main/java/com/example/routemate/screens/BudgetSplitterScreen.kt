package com.example.routemate.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.data.remote.CustomSplitItemDto
import com.example.routemate.data.remote.MemberBalanceDto
import com.example.routemate.data.remote.SettlementDto
import com.example.routemate.data.remote.TeamExpenseDto
import com.example.routemate.models.AppSettingsState
import com.example.routemate.models.Team
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSplitterScreen(
    team: Team,
    onBack: () -> Unit,
    viewModel: BudgetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode
    val teamIdInt = team.id.toIntOrNull() ?: 1

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Expenses, 1 = Balances & Settlements

    LaunchedEffect(team.id) {
        viewModel.loadData(teamIdInt, fallbackBudget = team.budget, teamName = team.name)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Budget Splitter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )
                        Text(
                            text = team.name,
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
                    IconButton(onClick = { viewModel.loadData(teamIdInt, fallbackBudget = team.budget, teamName = team.name, showLoader = false) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (isDark) Cyan else OceanBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) DarkSurface else Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                containerColor = if (isDark) Cyan else OceanBlue,
                contentColor = if (isDark) DeepNavy else Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        },
        containerColor = if (isDark) DarkBackground else IceBlue
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Action Success Toast Banner
            if (uiState.actionSuccess != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = uiState.actionSuccess ?: "",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearActionMessage() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AccentGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Error Message Banner
            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.loadData(teamIdInt, fallbackBudget = team.budget, teamName = team.name, showLoader = true) }) {
                            Text("Retry", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 1. Budget Overview Summary Card
            uiState.summary?.let { summary ->
                BudgetSummaryHeaderCard(summary = summary, isDark = isDark)
            }

            // Tab Selector: Expenses vs Settlements
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = if (isDark) DarkSurface else Color.White,
                contentColor = if (isDark) Cyan else OceanBlue,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Expenses (${uiState.expenses.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Settlements & Balances",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Body Content
            if (uiState.isLoading && uiState.summary == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Cyan)
                }
            } else if (selectedTab == 0) {
                // Tab 0: Expenses List
                ExpensesTabContent(
                    expenses = uiState.expenses,
                    isDark = isDark,
                    onDeleteExpense = { expId -> viewModel.deleteExpense(teamIdInt, expId) }
                )
            } else {
                // Tab 1: Settlements & Balances
                SettlementsTabContent(
                    balances = uiState.summary?.memberBalances ?: emptyList(),
                    settlements = uiState.summary?.settlements ?: emptyList(),
                    isDark = isDark
                )
            }
        }
    }

    // Add Expense Modal Dialog
    if (showAddExpenseDialog) {
        val members = uiState.summary?.memberBalances ?: emptyList()
        AddExpenseDialog(
            members = members,
            isDark = isDark,
            isSaving = uiState.isActionLoading,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { title, category, amount, date, notes, splitType, memberIds, customSplits ->
                viewModel.addExpense(
                    teamId = teamIdInt,
                    title = title,
                    category = category,
                    amount = amount,
                    date = date,
                    notes = notes,
                    splitType = splitType,
                    memberIds = memberIds,
                    customSplits = customSplits,
                    onSuccess = { showAddExpenseDialog = false }
                )
            }
        )
    }
}

// ==========================================
// SUMMARY HEADER CARD
// ==========================================

@Composable
fun BudgetSummaryHeaderCard(
    summary: com.example.routemate.data.remote.BudgetSummaryDto,
    isDark: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Trip Budget", fontSize = 11.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
                    Text("₹${String.format("%,.0f", summary.totalBudget)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDark) DarkTextPrimary else DeepNavy)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Spent", fontSize = 11.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
                    Text("₹${String.format("%,.0f", summary.totalExpenses)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (isDark) Cyan else OceanBlue)
                }
            }

            // Progress Bar showing budget consumption
            val progress = if (summary.totalBudget > 0) {
                (summary.totalExpenses / summary.totalBudget).coerceIn(0.0, 1.0).toFloat()
            } else 0f

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (progress > 0.9f) MaterialTheme.colorScheme.error else (if (isDark) Cyan else OceanBlue),
                    trackColor = if (isDark) DarkBorder else IceBlue
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Remaining: ₹${String.format("%,.0f", summary.remainingBudget)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (summary.remainingBudget > 0) AccentGreen else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "${summary.memberCount} Team Members",
                        fontSize = 11.sp,
                        color = if (isDark) DarkTextSecondary else Color.Gray
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 0: EXPENSES LIST
// ==========================================

@Composable
fun ExpensesTabContent(
    expenses: List<TeamExpenseDto>,
    isDark: Boolean,
    onDeleteExpense: (Int) -> Unit
) {
    if (expenses.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isDark) DarkSurface else Cyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = if (isDark) Cyan else OceanBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("No expenses logged yet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDark) DarkTextPrimary else DeepNavy)
            Spacer(Modifier.height(4.dp))
            Text("Tap + below to add petrol, meals, tolls or stay expenses.", fontSize = 12.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(expenses, key = { it.id }) { expense ->
                ExpenseItemCard(expense = expense, isDark = isDark, onDelete = { onDeleteExpense(expense.id) })
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: TeamExpenseDto,
    isDark: Boolean,
    onDelete: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    val categoryIcon: ImageVector = when (expense.category.lowercase()) {
        "fuel" -> Icons.Default.LocalGasStation
        "food" -> Icons.Default.Restaurant
        "accommodation" -> Icons.Default.Hotel
        "tolls" -> Icons.Default.DirectionsCar
        "bike maintenance" -> Icons.Default.Build
        else -> Icons.Default.Receipt
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) DarkBackground else Cyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = expense.category,
                    tint = if (isDark) Cyan else OceanBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Title & Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) DarkTextPrimary else DeepNavy
                )
                Text(
                    text = "Paid by ${expense.paidByName} • ${expense.expenseDate}",
                    fontSize = 11.sp,
                    color = if (isDark) DarkTextSecondary else Color.Gray
                )
                if (expense.notes?.isNotBlank() == true) {
                    Text(
                        text = expense.notes,
                        fontSize = 11.sp,
                        color = if (isDark) DarkTextSecondary else Color.Gray
                    )
                }
            }

            // Amount & Delete action
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%,.2f", expense.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (isDark) Cyan else OceanBlue
                )
                if (expense.isMine) {
                    IconButton(
                        onClick = { showConfirmDelete = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete '${expense.title}'? This will recalculate team splits.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDelete = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// TAB 1: SETTLEMENTS & BALANCES
// ==========================================

@Composable
fun SettlementsTabContent(
    balances: List<MemberBalanceDto>,
    settlements: List<SettlementDto>,
    isDark: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Section A: Who Pays Whom (Settlement Recommendations)
        item {
            Text(
                "Settlement Plan (Who Pays Whom)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isDark) DarkTextPrimary else DeepNavy,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (settlements.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                        Text(
                            "All expenses are settled! No payments pending.",
                            fontSize = 13.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            items(settlements) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.fromName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isDark) DarkTextPrimary else DeepNavy)
                                Text(" pays ", fontSize = 13.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
                                Text(item.toName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isDark) Cyan else OceanBlue)
                            }
                            Text("to balance team expenses", fontSize = 11.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentOrange.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "₹${String.format("%,.2f", item.amount)}",
                                color = AccentOrange,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section B: Member Balances
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Individual Member Balances",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isDark) DarkTextPrimary else DeepNavy,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(balances, key = { it.userId }) { member ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
                border = if (member.isCurrentUser) BorderStroke(1.5.dp, if (isDark) Cyan else OceanBlue) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = member.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) DarkTextPrimary else DeepNavy
                            )
                            if (member.isCurrentUser) {
                                Surface(
                                    color = if (isDark) Cyan.copy(alpha = 0.2f) else OceanBlue.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("You", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Cyan else OceanBlue, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text(
                            text = "Paid ₹${String.format("%,.0f", member.totalPaid)} • Share ₹${String.format("%,.0f", member.totalShare)}",
                            fontSize = 11.sp,
                            color = if (isDark) DarkTextSecondary else Color.Gray
                        )
                    }

                    // Balance status badge
                    val isCreditor = member.netBalance > 0.01
                    val isDebtor = member.netBalance < -0.01

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isCreditor -> AccentGreen.copy(alpha = 0.15f)
                            isDebtor -> AccentOrange.copy(alpha = 0.15f)
                            else -> if (isDark) DarkBorder else IceBlue
                        }
                    ) {
                        Text(
                            text = when {
                                isCreditor -> "+₹${String.format("%,.2f", member.netBalance)}"
                                isDebtor -> "-₹${String.format("%,.2f", kotlin.math.abs(member.netBalance))}"
                                else -> "Settled"
                            },
                            color = when {
                                isCreditor -> AccentGreen
                                isDebtor -> AccentOrange
                                else -> if (isDark) DarkTextSecondary else DeepNavy
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// ADD EXPENSE DIALOG
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    members: List<MemberBalanceDto>,
    isDark: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        category: String,
        amount: Double,
        date: String,
        notes: String?,
        splitType: String,
        memberIds: List<Int>?,
        customSplits: List<CustomSplitItemDto>?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Fuel") }
    var splitType by remember { mutableStateOf("equal") } // "equal" or "custom"
    var validationError by remember { mutableStateOf<String?>(null) }

    val categories = remember { listOf("Fuel", "Food", "Accommodation", "Tolls", "Bike maintenance", "Other") }
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Equal split selection: all members selected by default
    val selectedMemberIds = remember { mutableStateMapOf<Int, Boolean>() }
    LaunchedEffect(members) {
        members.forEach { m -> selectedMemberIds[m.userId] = true }
    }

    // Custom split share amounts
    val customShares = remember { mutableStateMapOf<Int, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Team Expense", fontWeight = FontWeight.Bold, color = if (isDark) DarkTextPrimary else DeepNavy) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationError != null) {
                    Text(validationError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Expense Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; validationError = null },
                    label = { Text("Expense Title (e.g. Petrol, Hotel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount (₹)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; validationError = null },
                    label = { Text("Amount in ₹ (Rupees)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category chips
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) DarkTextSecondary else DeepNavy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.drop(3).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Split Type Selection: Equal vs Custom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = splitType == "equal", onClick = { splitType = "equal" })
                        Text("Equal Split", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = splitType == "custom", onClick = { splitType = "custom" })
                        Text("Custom Split", fontSize = 12.sp)
                    }
                }

                if (splitType == "custom") {
                    Text("Enter share per member:", fontSize = 11.sp, color = Color.Gray)
                    members.forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(m.name, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = customShares[m.userId] ?: "",
                                onValueChange = { customShares[m.userId] = it },
                                placeholder = { Text("₹0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(90.dp)
                            )
                        }
                    }
                }

                // Notes (Optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (title.isBlank()) {
                        validationError = "Please enter an expense title"
                        return@Button
                    }
                    if (amt == null || amt <= 0) {
                        validationError = "Please enter a valid amount greater than 0"
                        return@Button
                    }

                    if (splitType == "custom") {
                        val customList = members.mapNotNull { m ->
                            val sAmt = customShares[m.userId]?.toDoubleOrNull()
                            if (sAmt != null && sAmt > 0) {
                                CustomSplitItemDto(userId = m.userId, shareAmount = sAmt)
                            } else null
                        }
                        val sumShares = customList.sumOf { it.shareAmount }
                        if (kotlin.math.abs(sumShares - amt) > 1.0) {
                            validationError = "Sum of member shares (₹${sumShares}) must match total amount (₹${amt})"
                            return@Button
                        }
                        onConfirm(title, selectedCategory, amt, todayDate, notes.ifBlank { null }, "custom", null, customList)
                    } else {
                        val chosenIds = selectedMemberIds.filter { it.value }.map { it.key }
                        onConfirm(title, selectedCategory, amt, todayDate, notes.ifBlank { null }, "equal", chosenIds, null)
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Cyan else OceanBlue)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Save Expense", color = if (isDark) DeepNavy else Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
