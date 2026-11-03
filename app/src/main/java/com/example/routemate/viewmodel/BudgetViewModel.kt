package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.*
import com.example.routemate.data.repository.BudgetRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetUiState(
    val summary: BudgetSummaryDto? = null,
    val expenses: List<TeamExpenseDto> = emptyList(),
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccess: String? = null
)

class BudgetViewModel(
    private val repository: BudgetRepository = BudgetRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState(isLoading = true))
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    fun loadData(
        teamId: Int,
        fallbackBudget: Int = 0,
        teamName: String = "",
        showLoader: Boolean = true
    ) {
        if (showLoader) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }

        viewModelScope.launch {
            val summaryDeferred = async { repository.getBudgetSummary(teamId) }
            val expensesDeferred = async { repository.getExpenses(teamId) }

            val summaryResult = summaryDeferred.await()
            val expensesResult = expensesDeferred.await()

            val fetchedSummary = summaryResult.getOrNull()
            val fetchedExpenses = expensesResult.getOrNull()?.expenses ?: emptyList()

            val resolvedSummary = fetchedSummary ?: if (fallbackBudget > 0 || teamName.isNotBlank()) {
                val budgetVal = if (fallbackBudget > 0) fallbackBudget.toDouble() else 5000.0
                val totalSpent = fetchedExpenses.sumOf { it.amount }
                BudgetSummaryDto(
                    teamId = teamId,
                    teamName = teamName,
                    totalBudget = budgetVal,
                    totalExpenses = totalSpent,
                    remainingBudget = kotlin.math.max(0.0, budgetVal - totalSpent),
                    memberCount = 1,
                    memberBalances = emptyList(),
                    settlements = emptyList()
                )
            } else null

            if (summaryResult.isSuccess || expensesResult.isSuccess || resolvedSummary != null) {
                _uiState.update {
                    it.copy(
                        summary = resolvedSummary ?: it.summary,
                        expenses = if (expensesResult.isSuccess) fetchedExpenses else it.expenses,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } else {
                val error = summaryResult.exceptionOrNull() ?: expensesResult.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error?.localizedMessage ?: "Failed to load budget data from server"
                    )
                }
            }
        }
    }

    fun addExpense(
        teamId: Int,
        title: String,
        category: String,
        amount: Double,
        date: String,
        notes: String?,
        splitType: String = "equal",
        memberIds: List<Int>? = null,
        customSplits: List<CustomSplitItemDto>? = null,
        onSuccess: () -> Unit = {}
    ) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null, actionSuccess = null) }

        viewModelScope.launch {
            val request = CreateExpenseRequestDto(
                title = title,
                category = category,
                amount = amount,
                expenseDate = date,
                notes = notes,
                splitType = splitType,
                memberIds = memberIds,
                customSplits = customSplits
            )

            val result = repository.createExpense(teamId, request)
            result.fold(
                onSuccess = { created ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            actionSuccess = "Expense of ₹${String.format("%.2f", created.amount)} added successfully!"
                        )
                    }
                    loadData(teamId, showLoader = false)
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to add expense"
                        )
                    }
                }
            )
        }
    }

    fun deleteExpense(teamId: Int, expenseId: Int) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null, actionSuccess = null) }

        viewModelScope.launch {
            val result = repository.deleteExpense(expenseId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            actionSuccess = "Expense deleted successfully"
                        )
                    }
                    loadData(teamId, showLoader = false)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to delete expense"
                        )
                    }
                }
            )
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionSuccess = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

