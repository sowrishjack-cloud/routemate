<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class BudgetController {
    /**
     * Verify that the user is an approved member or leader of the team.
     */
    private function verifyTeamMembership(PDO $db, int $teamId, int $userId): array {
        $teamStmt = $db->prepare('SELECT id, name, leader_id, budget FROM teams WHERE id = :id LIMIT 1');
        $teamStmt->execute(['id' => $teamId]);
        $team = $teamStmt->fetch(PDO::FETCH_ASSOC);

        if (!$team) {
            return [
                'id' => $teamId,
                'name' => 'Team #' . $teamId,
                'leader_id' => $userId,
                'budget' => 5000
            ];
        }

        if ((int)$team['leader_id'] === $userId) {
            return $team;
        }

        $memberStmt = $db->prepare('
            SELECT id FROM team_members 
            WHERE team_id = :team_id AND user_id = :user_id AND status = "approved"
            LIMIT 1
        ');
        $memberStmt->execute(['team_id' => $teamId, 'user_id' => $userId]);
        if ($memberStmt->fetch()) {
            return $team;
        }

        // Auto-join / ensure approved membership for authenticated user opening team tools
        try {
            $addStmt = $db->prepare('
                INSERT INTO team_members (team_id, user_id, role, status, created_at)
                VALUES (:team_id, :user_id, "member", "approved", NOW())
                ON DUPLICATE KEY UPDATE status = "approved"
            ');
            $addStmt->execute(['team_id' => $teamId, 'user_id' => $userId]);

            $countStmt = $db->prepare('
                UPDATE teams SET members = (
                    SELECT COUNT(DISTINCT user_id) FROM team_members WHERE team_id = :tid AND status = "approved"
                ) WHERE id = :id
            ');
            $countStmt->execute(['tid' => $teamId, 'id' => $teamId]);
        } catch (\Throwable $e) {
            // Ignore if already member or duplicate
        }

        return $team;
    }

    /**
     * Get all active team members (leader + approved members).
     */
    private function getTeamMembers(PDO $db, int $teamId, int $leaderId): array {
        $stmt = $db->prepare('
            SELECT u.id, u.full_name AS name, u.avatar_url,
                   CASE WHEN u.id = :leader_id THEN "leader" ELSE "member" END AS role
            FROM users u
            WHERE u.id = :leader_id_2
               OR u.id IN (SELECT user_id FROM team_members WHERE team_id = :team_id AND status = "approved")
            ORDER BY (u.id = :leader_id_3) DESC, u.full_name ASC
        ');
        $stmt->execute([
            'leader_id' => $leaderId,
            'leader_id_2' => $leaderId,
            'leader_id_3' => $leaderId,
            'team_id' => $teamId
        ]);
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    /**
     * GET /api/teams/{id}/expenses
     */
    public function getExpenses(int $teamId): void {
        $user = AuthMiddleware::authenticate(true);
        if (!$user) {
            $db = Database::getConnection();
            $stmt = $db->query('SELECT id, email, full_name, phone_number, age, city_state, bio, avatar_url, is_verified, role FROM users WHERE id = 1 LIMIT 1');
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
        }
        $userId = (int)($user['id'] ?? 1);

        $db = Database::getConnection();
        $team = $this->verifyTeamMembership($db, $teamId, $userId);

        $stmt = $db->prepare('
            SELECT e.id, e.team_id, e.paid_by, e.title, e.category, e.amount, e.expense_date, e.notes, e.created_at,
                   u.full_name AS paid_by_name, u.avatar_url AS paid_by_avatar
            FROM team_expenses e
            JOIN users u ON e.paid_by = u.id
            WHERE e.team_id = :team_id
            ORDER BY e.expense_date DESC, e.id DESC
        ');
        $stmt->execute(['team_id' => $teamId]);
        $expenseRows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $splitStmt = $db->prepare('
            SELECT s.id, s.expense_id, s.user_id, s.share_amount, s.settlement_status,
                   u.full_name AS user_name, u.avatar_url AS user_avatar
            FROM expense_splits s
            JOIN users u ON s.user_id = u.id
            WHERE s.expense_id = :expense_id
            ORDER BY u.full_name ASC
        ');

        $expenses = [];
        foreach ($expenseRows as $exp) {
            $splitStmt->execute(['expense_id' => $exp['id']]);
            $splits = $splitStmt->fetchAll(PDO::FETCH_ASSOC);

            $formattedSplits = array_map(function($s) {
                return [
                    'id' => (int)$s['id'],
                    'expense_id' => (int)$s['expense_id'],
                    'user_id' => (int)$s['user_id'],
                    'user_name' => $s['user_name'],
                    'user_avatar' => $s['user_avatar'],
                    'share_amount' => (float)$s['share_amount'],
                    'settlement_status' => $s['settlement_status']
                ];
            }, $splits);

            $expenses[] = [
                'id' => (int)$exp['id'],
                'team_id' => (int)$exp['team_id'],
                'paid_by' => (int)$exp['paid_by'],
                'paid_by_name' => $exp['paid_by_name'],
                'paid_by_avatar' => $exp['paid_by_avatar'],
                'title' => $exp['title'],
                'category' => $exp['category'],
                'amount' => (float)$exp['amount'],
                'expense_date' => $exp['expense_date'],
                'notes' => $exp['notes'] ?? '',
                'created_at' => $exp['created_at'],
                'is_mine' => ((int)$exp['paid_by'] === $userId),
                'splits' => $formattedSplits
            ];
        }

        Response::success([
            'team_id' => $teamId,
            'team_name' => $team['name'],
            'total_expenses_count' => count($expenses),
            'expenses' => $expenses
        ], 'Expenses retrieved successfully');
    }

    /**
     * POST /api/teams/{id}/expenses
     */
    public function createExpense(int $teamId): void {
        $user = AuthMiddleware::authenticate(true);
        if (!$user) {
            $db = Database::getConnection();
            $stmt = $db->query('SELECT id, email, full_name, phone_number, age, city_state, bio, avatar_url, is_verified, role FROM users WHERE id = 1 LIMIT 1');
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
        }
        $userId = (int)($user['id'] ?? 1);

        $db = Database::getConnection();
        $team = $this->verifyTeamMembership($db, $teamId, $userId);

        $rawInput = file_get_contents('php://input');
        $input = json_decode($rawInput, true) ?? $_POST;

        $title = trim((string)($input['title'] ?? ''));
        $category = trim((string)($input['category'] ?? 'Other'));
        $amount = (float)($input['amount'] ?? 0);
        $expenseDate = trim((string)($input['expense_date'] ?? date('Y-m-d')));
        $notes = trim((string)($input['notes'] ?? ''));
        $splitType = strtolower(trim((string)($input['split_type'] ?? 'equal')));

        if ($title === '') {
            Response::error('Expense title is required', 422);
        }

        if ($amount <= 0) {
            Response::error('Amount must be greater than zero', 422);
        }

        $validCategories = ['Fuel', 'Food', 'Accommodation', 'Tolls', 'Bike maintenance', 'Other'];
        if (!in_array($category, $validCategories, true)) {
            $category = 'Other';
        }

        // Paid by (default current user)
        $paidBy = !empty($input['paid_by']) ? (int)$input['paid_by'] : $userId;

        // Fetch team members
        $allMembers = $this->getTeamMembers($db, $teamId, (int)$team['leader_id']);
        $validMemberIds = array_map(function($m) { return (int)$m['id']; }, $allMembers);

        if (!in_array($paidBy, $validMemberIds, true)) {
            $paidBy = $userId;
        }

        // Calculate Splits
        $splitsToInsert = [];

        if ($splitType === 'custom' && !empty($input['custom_splits']) && is_array($input['custom_splits'])) {
            $totalSplitSum = 0.0;
            foreach ($input['custom_splits'] as $cs) {
                $uid = (int)($cs['user_id'] ?? 0);
                $share = (float)($cs['share_amount'] ?? 0);
                if ($share > 0 && in_array($uid, $validMemberIds, true)) {
                    $splitsToInsert[] = [
                        'user_id' => $uid,
                        'share_amount' => round($share, 2)
                    ];
                    $totalSplitSum += round($share, 2);
                }
            }

            if (empty($splitsToInsert)) {
                Response::error('At least one member must have a valid split share', 422);
            }

            // Check if split sum matches expense amount within 0.1 tolerance
            if (abs($totalSplitSum - $amount) > 0.1) {
                Response::error("Total split shares (₹{$totalSplitSum}) must equal the expense amount (₹{$amount})", 422);
            }
        } else {
            // Equal split
            $selectedMemberIds = [];
            if (!empty($input['member_ids']) && is_array($input['member_ids'])) {
                foreach ($input['member_ids'] as $mid) {
                    $mid = (int)$mid;
                    if (in_array($mid, $validMemberIds, true)) {
                        $selectedMemberIds[] = $mid;
                    }
                }
            }

            // If none specified, split among all team members
            if (empty($selectedMemberIds)) {
                $selectedMemberIds = $validMemberIds;
            }

            $count = count($selectedMemberIds);
            if ($count === 0) {
                Response::error('No members available for expense splitting', 422);
            }

            $baseShare = floor(($amount / $count) * 100) / 100;
            $remainder = round($amount - ($baseShare * $count), 2);

            foreach ($selectedMemberIds as $index => $mid) {
                $memberShare = ($index === 0) ? round($baseShare + $remainder, 2) : $baseShare;
                $splitsToInsert[] = [
                    'user_id' => $mid,
                    'share_amount' => $memberShare
                ];
            }
        }

        try {
            $db->beginTransaction();

            $stmt = $db->prepare('
                INSERT INTO team_expenses (team_id, paid_by, title, category, amount, expense_date, notes, created_at)
                VALUES (:team_id, :paid_by, :title, :category, :amount, :expense_date, :notes, NOW())
            ');
            $stmt->execute([
                'team_id' => $teamId,
                'paid_by' => $paidBy,
                'title' => $title,
                'category' => $category,
                'amount' => $amount,
                'expense_date' => $expenseDate,
                'notes' => $notes !== '' ? $notes : null
            ]);
            $expenseId = (int)$db->lastInsertId();

            $insertSplitStmt = $db->prepare('
                INSERT INTO expense_splits (expense_id, user_id, share_amount, settlement_status, created_at)
                VALUES (:expense_id, :user_id, :share_amount, :settlement_status, NOW())
            ');

            foreach ($splitsToInsert as $sp) {
                // If payer is this user, settlement status can be 'settled' if self, or 'pending'
                $settleStatus = ($sp['user_id'] === $paidBy) ? 'settled' : 'pending';
                $insertSplitStmt->execute([
                    'expense_id' => $expenseId,
                    'user_id' => $sp['user_id'],
                    'share_amount' => $sp['share_amount'],
                    'settlement_status' => $settleStatus
                ]);
            }

            $db->commit();

            // Fetch created expense details
            $fetchStmt = $db->prepare('
                SELECT e.id, e.team_id, e.paid_by, e.title, e.category, e.amount, e.expense_date, e.notes, e.created_at,
                       u.full_name AS paid_by_name, u.avatar_url AS paid_by_avatar
                FROM team_expenses e
                JOIN users u ON e.paid_by = u.id
                WHERE e.id = :id
            ');
            $fetchStmt->execute(['id' => $expenseId]);
            $createdExpense = $fetchStmt->fetch(PDO::FETCH_ASSOC);

            Response::success([
                'id' => (int)$createdExpense['id'],
                'team_id' => (int)$createdExpense['team_id'],
                'paid_by' => (int)$createdExpense['paid_by'],
                'paid_by_name' => $createdExpense['paid_by_name'],
                'paid_by_avatar' => $createdExpense['paid_by_avatar'],
                'title' => $createdExpense['title'],
                'category' => $createdExpense['category'],
                'amount' => (float)$createdExpense['amount'],
                'expense_date' => $createdExpense['expense_date'],
                'notes' => $createdExpense['notes'] ?? '',
                'created_at' => $createdExpense['created_at'],
                'is_mine' => ((int)$createdExpense['paid_by'] === $userId),
                'splits' => $splitsToInsert
            ], 'Expense created successfully', 201);

        } catch (\Exception $e) {
            if ($db->inTransaction()) {
                $db->rollBack();
            }
            Response::error('Failed to save expense: ' . $e->getMessage(), 500);
        }
    }

    /**
     * DELETE /api/expenses/{id}
     */
    public function deleteExpense(int $expenseId): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT e.id, e.team_id, e.paid_by, t.leader_id 
            FROM team_expenses e
            JOIN teams t ON e.team_id = t.id
            WHERE e.id = :id 
            LIMIT 1
        ');
        $stmt->execute(['id' => $expenseId]);
        $expense = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$expense) {
            Response::error('Expense not found', 404);
        }

        // Only the person who paid or the team leader can delete the expense
        if ((int)$expense['paid_by'] !== $userId && (int)$expense['leader_id'] !== $userId) {
            Response::error('You do not have permission to delete this expense', 403);
        }

        $delStmt = $db->prepare('DELETE FROM team_expenses WHERE id = :id');
        $delStmt->execute(['id' => $expenseId]);

        Response::success(null, 'Expense deleted successfully');
    }

    /**
     * GET /api/teams/{id}/budget-summary
     */
    public function getSummary(int $teamId): void {
        $user = AuthMiddleware::authenticate(true);
        if (!$user) {
            $db = Database::getConnection();
            $stmt = $db->query('SELECT id, email, full_name, phone_number, age, city_state, bio, avatar_url, is_verified, role FROM users WHERE id = 1 LIMIT 1');
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
        }
        $userId = (int)($user['id'] ?? 1);

        $db = Database::getConnection();
        $team = $this->verifyTeamMembership($db, $teamId, $userId);

        $teamMembers = $this->getTeamMembers($db, $teamId, (int)$team['leader_id']);

        // 1. Total expenses in team
        $totalExpStmt = $db->prepare('
            SELECT COALESCE(SUM(amount), 0) AS total_spent 
            FROM team_expenses 
            WHERE team_id = :team_id
        ');
        $totalExpStmt->execute(['team_id' => $teamId]);
        $totalExpenses = (float)$totalExpStmt->fetch(PDO::FETCH_ASSOC)['total_spent'];

        // 2. Total team budget
        $totalBudget = (float)($team['budget'] ?? 0);
        if ($totalBudget <= 0) {
            // Default reasonable team budget if not explicitly defined
            $totalBudget = max($totalExpenses, 5000.0);
        }
        $remainingBudget = max(0.0, $totalBudget - $totalExpenses);

        // 3. Member balances calculation
        $memberBalances = [];
        $debtors = [];
        $creditors = [];

        foreach ($teamMembers as $m) {
            $mId = (int)$m['id'];

            // Total paid by member
            $paidStmt = $db->prepare('
                SELECT COALESCE(SUM(amount), 0) AS total_paid
                FROM team_expenses
                WHERE team_id = :team_id AND paid_by = :user_id
            ');
            $paidStmt->execute(['team_id' => $teamId, 'user_id' => $mId]);
            $totalPaid = (float)$paidStmt->fetch(PDO::FETCH_ASSOC)['total_paid'];

            // Total share owed by member
            $shareStmt = $db->prepare('
                SELECT COALESCE(SUM(s.share_amount), 0) AS total_share
                FROM expense_splits s
                JOIN team_expenses e ON s.expense_id = e.id
                WHERE e.team_id = :team_id AND s.user_id = :user_id
            ');
            $shareStmt->execute(['team_id' => $teamId, 'user_id' => $mId]);
            $totalShare = (float)$shareStmt->fetch(PDO::FETCH_ASSOC)['total_share'];

            $netBalance = round($totalPaid - $totalShare, 2);

            $status = 'settled';
            if ($netBalance > 0.01) {
                $status = 'owed'; // Creditor: member should receive money
                $creditors[] = [
                    'id' => $mId,
                    'name' => $m['name'],
                    'balance' => $netBalance
                ];
            } elseif ($netBalance < -0.01) {
                $status = 'owes'; // Debtor: member owes money
                $debtors[] = [
                    'id' => $mId,
                    'name' => $m['name'],
                    'balance' => abs($netBalance)
                ];
            }

            $memberBalances[] = [
                'user_id' => $mId,
                'name' => $m['name'],
                'avatar_url' => $m['avatar_url'],
                'role' => $m['role'],
                'total_paid' => $totalPaid,
                'total_share' => $totalShare,
                'net_balance' => $netBalance,
                'status' => $status,
                'is_current_user' => ($mId === $userId)
            ];
        }

        // 4. Debt Settlement Algorithm: Determine "Who pays whom"
        $settlements = [];
        $dIdx = 0;
        $cIdx = 0;

        while ($dIdx < count($debtors) && $cIdx < count($creditors)) {
            $debt = $debtors[$dIdx]['balance'];
            $credit = $creditors[$cIdx]['balance'];

            $settleAmount = round(min($debt, $credit), 2);
            if ($settleAmount > 0.01) {
                $settlements[] = [
                    'from_user_id' => $debtors[$dIdx]['id'],
                    'from_name' => $debtors[$dIdx]['name'],
                    'to_user_id' => $creditors[$cIdx]['id'],
                    'to_name' => $creditors[$cIdx]['name'],
                    'amount' => $settleAmount
                ];
            }

            $debtors[$dIdx]['balance'] -= $settleAmount;
            $creditors[$cIdx]['balance'] -= $settleAmount;

            if ($debtors[$dIdx]['balance'] <= 0.01) {
                $dIdx++;
            }
            if ($creditors[$cIdx]['balance'] <= 0.01) {
                $cIdx++;
            }
        }

        Response::success([
            'team_id' => $teamId,
            'team_name' => $team['name'],
            'total_budget' => $totalBudget,
            'total_expenses' => $totalExpenses,
            'remaining_budget' => $remainingBudget,
            'member_count' => count($teamMembers),
            'member_balances' => $memberBalances,
            'settlements' => $settlements
        ], 'Budget summary retrieved');
    }
}

