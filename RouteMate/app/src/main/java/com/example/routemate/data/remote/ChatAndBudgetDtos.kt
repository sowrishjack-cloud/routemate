package com.example.routemate.data.remote

import com.google.gson.annotations.SerializedName

// ==========================================
// GROUP CHAT DTOs
// ==========================================

data class TeamMessageDto(
    @SerializedName("id") val id: Int,
    @SerializedName("team_id") val teamId: Int,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("sender_name") val senderName: String,
    @SerializedName("sender_avatar") val senderAvatar: String? = null,
    @SerializedName("message") val message: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_mine") val isMine: Boolean = false
)

data class TeamMessagesResponseDto(
    @SerializedName("team_id") val teamId: Int,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("total_messages") val totalMessages: Int,
    @SerializedName("messages") val messages: List<TeamMessageDto> = emptyList()
)

data class SendMessageRequestDto(
    @SerializedName("message") val message: String
)

// ==========================================
// BUDGET SPLITTER DTOs
// ==========================================

data class ExpenseSplitDto(
    @SerializedName("id") val id: Int,
    @SerializedName("expense_id") val expenseId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_avatar") val userAvatar: String? = null,
    @SerializedName("share_amount") val shareAmount: Double,
    @SerializedName("settlement_status") val settlementStatus: String = "pending"
)

data class TeamExpenseDto(
    @SerializedName("id") val id: Int,
    @SerializedName("team_id") val teamId: Int,
    @SerializedName("paid_by") val paidBy: Int,
    @SerializedName("paid_by_name") val paidByName: String,
    @SerializedName("paid_by_avatar") val paidByAvatar: String? = null,
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("expense_date") val expenseDate: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_mine") val isMine: Boolean = false,
    @SerializedName("splits") val splits: List<ExpenseSplitDto> = emptyList()
)

data class TeamExpensesResponseDto(
    @SerializedName("team_id") val teamId: Int,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("total_expenses_count") val totalExpensesCount: Int,
    @SerializedName("expenses") val expenses: List<TeamExpenseDto> = emptyList()
)

data class CreateExpenseRequestDto(
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("paid_by") val paidBy: Int? = null,
    @SerializedName("expense_date") val expenseDate: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("split_type") val splitType: String = "equal",
    @SerializedName("member_ids") val memberIds: List<Int>? = null,
    @SerializedName("custom_splits") val customSplits: List<CustomSplitItemDto>? = null
)

data class CustomSplitItemDto(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("share_amount") val shareAmount: Double
)

data class MemberBalanceDto(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("role") val role: String = "member",
    @SerializedName("total_paid") val totalPaid: Double,
    @SerializedName("total_share") val totalShare: Double,
    @SerializedName("net_balance") val netBalance: Double,
    @SerializedName("status") val status: String,
    @SerializedName("is_current_user") val isCurrentUser: Boolean = false
)

data class SettlementDto(
    @SerializedName("from_user_id") val fromUserId: Int,
    @SerializedName("from_name") val fromName: String,
    @SerializedName("to_user_id") val toUserId: Int,
    @SerializedName("to_name") val toName: String,
    @SerializedName("amount") val amount: Double
)

data class BudgetSummaryDto(
    @SerializedName("team_id") val teamId: Int,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("total_budget") val totalBudget: Double,
    @SerializedName("total_expenses") val totalExpenses: Double,
    @SerializedName("remaining_budget") val remainingBudget: Double,
    @SerializedName("member_count") val memberCount: Int,
    @SerializedName("member_balances") val memberBalances: List<MemberBalanceDto> = emptyList(),
    @SerializedName("settlements") val settlements: List<SettlementDto> = emptyList()
)

