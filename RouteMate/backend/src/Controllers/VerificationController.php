<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class VerificationController {
    /**
     * POST /api/verification/submit
     * Submit rider identity & licence verification.
     */
    public function submit(): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $input = json_decode(file_get_contents('php://input'), true) ?? $_POST;

        $fullName = trim((string)($input['full_name'] ?? $user['full_name'] ?? ''));
        $documentType = trim((string)($input['document_type'] ?? 'Driving Licence'));
        $licenseNumber = trim((string)($input['license_number'] ?? ''));
        $idDocumentPath = trim((string)($input['id_document_path'] ?? ''));
        $vehicleRcPath = trim((string)($input['vehicle_rc_path'] ?? ''));
        $selfiePath = trim((string)($input['selfie_path'] ?? ''));

        if ($fullName === '') {
            Response::error('Full legal name is required for verification', 422);
        }

        // Support uploaded files if sent via multipart/form-data
        $uploadDir = dirname(__DIR__, 2) . DIRECTORY_SEPARATOR . 'storage' . DIRECTORY_SEPARATOR . 'verifications';
        if (!is_dir($uploadDir)) {
            @mkdir($uploadDir, 0755, true);
        }

        if (isset($_FILES['id_document']) && $_FILES['id_document']['error'] === UPLOAD_ERR_OK) {
            $ext = strtolower(pathinfo($_FILES['id_document']['name'], PATHINFO_EXTENSION));
            if (!in_array($ext, ['jpg', 'jpeg', 'png', 'pdf'], true)) {
                Response::error('Invalid document format. Only JPG, PNG, and PDF are accepted.', 422);
            }
            $filename = 'id_' . $userId . '_' . time() . '.' . $ext;
            $dest = $uploadDir . DIRECTORY_SEPARATOR . $filename;
            if (move_uploaded_file($_FILES['id_document']['tmp_name'], $dest)) {
                $idDocumentPath = 'storage/verifications/' . $filename;
            }
        }

        if (isset($_FILES['vehicle_rc']) && $_FILES['vehicle_rc']['error'] === UPLOAD_ERR_OK) {
            $ext = strtolower(pathinfo($_FILES['vehicle_rc']['name'], PATHINFO_EXTENSION));
            $filename = 'rc_' . $userId . '_' . time() . '.' . $ext;
            $dest = $uploadDir . DIRECTORY_SEPARATOR . $filename;
            if (move_uploaded_file($_FILES['vehicle_rc']['tmp_name'], $dest)) {
                $vehicleRcPath = 'storage/verifications/' . $filename;
            }
        }

        if (isset($_FILES['selfie']) && $_FILES['selfie']['error'] === UPLOAD_ERR_OK) {
            $ext = strtolower(pathinfo($_FILES['selfie']['name'], PATHINFO_EXTENSION));
            $filename = 'selfie_' . $userId . '_' . time() . '.' . $ext;
            $dest = $uploadDir . DIRECTORY_SEPARATOR . $filename;
            if (move_uploaded_file($_FILES['selfie']['tmp_name'], $dest)) {
                $selfiePath = 'storage/verifications/' . $filename;
            }
        }

        if ($idDocumentPath === '') {
            $idDocumentPath = 'docs/dl_' . $userId . '.pdf'; // Reference fallback
        }

        $db = Database::getConnection();

        // Check for existing pending request
        $checkStmt = $db->prepare('SELECT id, verification_status FROM rider_verifications WHERE user_id = :uid ORDER BY id DESC LIMIT 1');
        $checkStmt->execute(['uid' => $userId]);
        $existing = $checkStmt->fetch(PDO::FETCH_ASSOC);

        if ($existing && $existing['verification_status'] === 'approved') {
            Response::error('You are already an approved verified rider', 400);
        }

        $insertStmt = $db->prepare('
            INSERT INTO rider_verifications 
                (user_id, full_name, document_type, license_number, id_document_path, vehicle_rc_path, selfie_path, verification_status, submitted_at)
            VALUES 
                (:user_id, :full_name, :document_type, :license_number, :id_doc, :rc_doc, :selfie, "pending", NOW())
        ');
        $insertStmt->execute([
            'user_id' => $userId,
            'full_name' => $fullName,
            'document_type' => $documentType,
            'license_number' => $licenseNumber !== '' ? $licenseNumber : null,
            'id_doc' => $idDocumentPath,
            'rc_doc' => $vehicleRcPath !== '' ? $vehicleRcPath : null,
            'selfie' => $selfiePath !== '' ? $selfiePath : null
        ]);

        $subId = (int)$db->lastInsertId();

        Response::success([
            'verification_id' => $subId,
            'user_id' => $userId,
            'full_name' => $fullName,
            'document_type' => $documentType,
            'verification_status' => 'pending',
            'submitted_at' => date('c'),
            'message' => 'Verification documents submitted successfully. An administrator will review your application.'
        ], 'Verification submitted successfully', 201);
    }

    /**
     * GET /api/verification/status
     */
    public function getStatus(): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT id, user_id, full_name, document_type, license_number, verification_status, rejection_reason, submitted_at, reviewed_at
            FROM rider_verifications
            WHERE user_id = :uid
            ORDER BY id DESC
            LIMIT 1
        ');
        $stmt->execute(['uid' => $userId]);
        $record = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$record) {
            Response::success([
                'is_verified' => (bool)($user['is_verified'] ?? false),
                'verification_status' => 'unverified',
                'rejection_reason' => null,
                'submitted_at' => null,
                'reviewed_at' => null
            ], 'Verification status');
            return;
        }

        // Mask license number for security (e.g. DL-****1234)
        $maskedLicense = null;
        if (!empty($record['license_number'])) {
            $len = strlen($record['license_number']);
            $maskedLicense = $len > 4 ? 'DL-****' . substr($record['license_number'], -4) : 'DL-****';
        }

        Response::success([
            'verification_id' => (int)$record['id'],
            'is_verified' => $record['verification_status'] === 'approved',
            'verification_status' => $record['verification_status'],
            'document_type' => $record['document_type'],
            'masked_license' => $maskedLicense,
            'rejection_reason' => $record['rejection_reason'],
            'submitted_at' => $record['submitted_at'],
            'reviewed_at' => $record['reviewed_at']
        ], 'Verification status retrieved');
    }

    /**
     * GET /api/admin/verifications
     */
    public function adminList(): void {
        $admin = AuthMiddleware::authenticate(false);
        if (($admin['role'] ?? '') !== 'admin') {
            Response::error('Access restricted to administrators only', 403);
        }

        $db = Database::getConnection();
        $stmt = $db->query('
            SELECT rv.id, rv.user_id, rv.full_name, rv.document_type, rv.verification_status, rv.submitted_at,
                   u.email, u.avatar_url, u.phone_number
            FROM rider_verifications rv
            JOIN users u ON rv.user_id = u.id
            WHERE rv.verification_status = "pending"
            ORDER BY rv.submitted_at ASC
        ');
        $list = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success([
            'total_pending' => count($list),
            'submissions' => $list
        ], 'Pending verifications retrieved');
    }

    /**
     * POST /api/admin/verifications/{id}/approve
     */
    public function adminApprove(int $id): void {
        $admin = AuthMiddleware::authenticate(false);
        if (($admin['role'] ?? '') !== 'admin') {
            Response::error('Access restricted to administrators only', 403);
        }

        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT id, user_id, verification_status FROM rider_verifications WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$row) {
            Response::error('Verification submission not found', 404);
        }

        // Prevent admin from approving their own submission
        if ((int)$row['user_id'] === (int)$admin['id']) {
            Response::error('Administrators cannot approve their own verification application', 403);
        }

        try {
            $db->beginTransaction();

            $updateRv = $db->prepare('
                UPDATE rider_verifications 
                SET verification_status = "approved", reviewed_by = :admin_id, reviewed_at = NOW(), rejection_reason = NULL
                WHERE id = :id
            ');
            $updateRv->execute(['admin_id' => $admin['id'], 'id' => $id]);

            // Set is_verified = 1 on users table
            $updateUser = $db->prepare('UPDATE users SET is_verified = 1 WHERE id = :uid');
            $updateUser->execute(['uid' => $row['user_id']]);

            // Insert notification
            $notif = $db->prepare('
                INSERT INTO notifications (user_id, title, message, type)
                VALUES (:uid, "Profile Verified!", "Congratulations! Your rider profile has been verified. You now display the verified rider badge.", "system")
            ');
            $notif->execute(['uid' => $row['user_id']]);

            $db->commit();

            Response::success([
                'verification_id' => $id,
                'user_id' => (int)$row['user_id'],
                'status' => 'approved'
            ], 'Verification approved successfully');

        } catch (\Exception $e) {
            $db->rollBack();
            Response::error('Failed to approve verification: ' . $e->getMessage(), 500);
        }
    }

    /**
     * POST /api/admin/verifications/{id}/reject
     */
    public function adminReject(int $id): void {
        $admin = AuthMiddleware::authenticate(false);
        if (($admin['role'] ?? '') !== 'admin') {
            Response::error('Access restricted to administrators only', 403);
        }

        $input = json_decode(file_get_contents('php://input'), true) ?? $_POST;
        $reason = trim((string)($input['rejection_reason'] ?? 'Document details could not be authenticated.'));

        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT id, user_id FROM rider_verifications WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$row) {
            Response::error('Verification submission not found', 404);
        }

        // Prevent admin self-review
        if ((int)$row['user_id'] === (int)$admin['id']) {
            Response::error('Administrators cannot reject their own verification application', 403);
        }

        try {
            $db->beginTransaction();

            $updateRv = $db->prepare('
                UPDATE rider_verifications 
                SET verification_status = "rejected", rejection_reason = :reason, reviewed_by = :admin_id, reviewed_at = NOW()
                WHERE id = :id
            ');
            $updateRv->execute(['reason' => $reason, 'admin_id' => $admin['id'], 'id' => $id]);

            $updateUser = $db->prepare('UPDATE users SET is_verified = 0 WHERE id = :uid');
            $updateUser->execute(['uid' => $row['user_id']]);

            $notif = $db->prepare('
                INSERT INTO notifications (user_id, title, message, type)
                VALUES (:uid, "Verification Update", :msg, "system")
            ');
            $notif->execute([
                'uid' => $row['user_id'],
                'msg' => "Your verification was not approved: {$reason}. You may resubmit with clearer documents."
            ]);

            $db->commit();

            Response::success([
                'verification_id' => $id,
                'user_id' => (int)$row['user_id'],
                'status' => 'rejected',
                'rejection_reason' => $reason
            ], 'Verification rejected');

        } catch (\Exception $e) {
            $db->rollBack();
            Response::error('Failed to reject verification: ' . $e->getMessage(), 500);
        }
    }
}

