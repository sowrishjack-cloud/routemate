<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class EmergencyController {
    // ==========================================
    // EMERGENCY CONTACTS CRUD
    // ==========================================

    /**
     * GET /api/emergency/contacts
     */
    public function getContacts(): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT id, user_id, contact_name, phone, relationship, is_primary, created_at, updated_at
            FROM emergency_contacts
            WHERE user_id = :uid
            ORDER BY is_primary DESC, id ASC
        ');
        $stmt->execute(['uid' => $userId]);
        $contacts = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success([
            'total_contacts' => count($contacts),
            'contacts' => array_map(function($c) {
                return [
                    'id' => (int)$c['id'],
                    'user_id' => (int)$c['user_id'],
                    'contact_name' => $c['contact_name'],
                    'phone' => $c['phone'],
                    'relationship' => $c['relationship'],
                    'is_primary' => (bool)$c['is_primary'],
                    'created_at' => $c['created_at'],
                    'updated_at' => $c['updated_at']
                ];
            }, $contacts)
        ], 'Emergency contacts retrieved');
    }

    /**
     * POST /api/emergency/contacts
     */
    public function addContact(): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $input = json_decode(file_get_contents('php://input'), true) ?? $_POST;

        $name = trim((string)($input['contact_name'] ?? ''));
        $phone = trim((string)($input['phone'] ?? ''));
        $relationship = trim((string)($input['relationship'] ?? 'Family'));
        $isPrimary = !empty($input['is_primary']) ? 1 : 0;

        if ($name === '') {
            Response::error('Emergency contact name is required', 422);
        }

        if ($phone === '' || strlen($phone) < 7) {
            Response::error('A valid phone number is required (with country code recommended)', 422);
        }

        $validRelationships = ['Parent', 'Family', 'Friend', 'Spouse', 'Partner', 'Doctor', 'Sibling', 'Other'];
        if (!in_array($relationship, $validRelationships, true)) {
            $relationship = 'Family';
        }

        $db = Database::getConnection();

        // If this contact is marked primary, unset primary on previous contacts
        if ($isPrimary === 1) {
            $unsetStmt = $db->prepare('UPDATE emergency_contacts SET is_primary = 0 WHERE user_id = :uid');
            $unsetStmt->execute(['uid' => $userId]);
        }

        $stmt = $db->prepare('
            INSERT INTO emergency_contacts (user_id, contact_name, phone, relationship, is_primary, created_at, updated_at)
            VALUES (:uid, :name, :phone, :rel, :prim, NOW(), NOW())
        ');
        $stmt->execute([
            'uid' => $userId,
            'name' => $name,
            'phone' => $phone,
            'rel' => $relationship,
            'prim' => $isPrimary
        ]);

        $id = (int)$db->lastInsertId();

        Response::success([
            'id' => $id,
            'user_id' => $userId,
            'contact_name' => $name,
            'phone' => $phone,
            'relationship' => $relationship,
            'is_primary' => (bool)$isPrimary
        ], 'Emergency contact added successfully', 201);
    }

    /**
     * PUT /api/emergency/contacts/{id}
     */
    public function updateContact(int $contactId): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();
        $checkStmt = $db->prepare('SELECT id, user_id, is_primary FROM emergency_contacts WHERE id = :id LIMIT 1');
        $checkStmt->execute(['id' => $contactId]);
        $contact = $checkStmt->fetch(PDO::FETCH_ASSOC);

        if (!$contact) {
            Response::error('Emergency contact not found', 404);
        }

        if ((int)$contact['user_id'] !== $userId) {
            Response::error('You do not have permission to modify this contact', 403);
        }

        $input = json_decode(file_get_contents('php://input'), true) ?? $_POST;
        $name = trim((string)($input['contact_name'] ?? ''));
        $phone = trim((string)($input['phone'] ?? ''));
        $relationship = trim((string)($input['relationship'] ?? ''));
        $hasIsPrimary = array_key_exists('is_primary', $input);
        $isPrimary = !empty($input['is_primary']) ? 1 : 0;

        if ($hasIsPrimary && $isPrimary === 1) {
            $unsetStmt = $db->prepare('UPDATE emergency_contacts SET is_primary = 0 WHERE user_id = :uid');
            $unsetStmt->execute(['uid' => $userId]);
        }

        $stmt = $db->prepare('
            UPDATE emergency_contacts 
            SET contact_name = COALESCE(NULLIF(:name, ""), contact_name),
                phone = COALESCE(NULLIF(:phone, ""), phone),
                relationship = COALESCE(NULLIF(:rel, ""), relationship),
                is_primary = CASE WHEN :hasPrim = 1 THEN :primVal ELSE is_primary END,
                updated_at = NOW()
            WHERE id = :id
        ');
        $stmt->execute([
            'name' => $name,
            'phone' => $phone,
            'rel' => $relationship,
            'hasPrim' => $hasIsPrimary ? 1 : 0,
            'primVal' => $isPrimary,
            'id' => $contactId
        ]);

        Response::success(null, 'Emergency contact updated successfully');
    }

    /**
     * DELETE /api/emergency/contacts/{id}
     */
    public function deleteContact(int $contactId): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();
        $checkStmt = $db->prepare('SELECT id, user_id FROM emergency_contacts WHERE id = :id LIMIT 1');
        $checkStmt->execute(['id' => $contactId]);
        $contact = $checkStmt->fetch(PDO::FETCH_ASSOC);

        if (!$contact) {
            Response::error('Emergency contact not found', 404);
        }

        if ((int)$contact['user_id'] !== $userId) {
            Response::error('You do not have permission to delete this contact', 403);
        }

        $stmt = $db->prepare('DELETE FROM emergency_contacts WHERE id = :id');
        $stmt->execute(['id' => $contactId]);

        Response::success(null, 'Emergency contact deleted successfully');
    }

    // ==========================================
    // EMERGENCY SOS ALERTS
    // ==========================================

    /**
     * POST /api/emergency/alerts
     * Trigger Emergency SOS alert.
     */
    public function createAlert(): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $input = json_decode(file_get_contents('php://input'), true) ?? $_POST;

        $emergencyType = trim((string)($input['emergency_type'] ?? 'General SOS'));
        $lat = isset($input['latitude']) && is_numeric($input['latitude']) ? (float)$input['latitude'] : null;
        $lng = isset($input['longitude']) && is_numeric($input['longitude']) ? (float)$input['longitude'] : null;
        $address = trim((string)($input['address'] ?? ''));
        $message = trim((string)($input['message'] ?? ''));
        $notifyTeam = (bool)($input['notify_team'] ?? true);

        $db = Database::getConnection();

        // Rate limit: prevent duplicate alert submission within 15 seconds
        $rateStmt = $db->prepare('
            SELECT id, created_at 
            FROM emergency_alerts 
            WHERE user_id = :uid AND created_at > DATE_SUB(NOW(), INTERVAL 15 SECOND)
            LIMIT 1
        ');
        $rateStmt->execute(['uid' => $userId]);
        if ($rateStmt->fetch()) {
            Response::error('An emergency alert was just transmitted. Please wait a few seconds before sending another alert.', 429);
        }

        try {
            $db->beginTransaction();

            $insertAlert = $db->prepare('
                INSERT INTO emergency_alerts 
                    (user_id, emergency_type, latitude, longitude, address, message, alert_status, created_at)
                VALUES 
                    (:uid, :type, :lat, :lng, :address, :message, "active", NOW())
            ');
            $insertAlert->execute([
                'uid' => $userId,
                'type' => $emergencyType,
                'lat' => $lat,
                'lng' => $lng,
                'address' => $address !== '' ? $address : null,
                'message' => $message !== '' ? $message : null
            ]);
            $alertId = (int)$db->lastInsertId();

            // Fetch configured emergency contacts
            $contactStmt = $db->prepare('SELECT id, contact_name, phone, relationship, is_primary FROM emergency_contacts WHERE user_id = :uid ORDER BY is_primary DESC, id ASC');
            $contactStmt->execute(['uid' => $userId]);
            $contacts = $contactStmt->fetchAll(PDO::FETCH_ASSOC);

            $mapUrl = ($lat !== null && $lng !== null) ? "https://maps.google.com/?q={$lat},{$lng}" : null;
            $locationText = $mapUrl ? "Current location: {$mapUrl}" : "Location: Coordinates unavailable";
            $customPart = $message !== '' ? " Note: {$message}." : "";
            $standardAlertMessage = "EMERGENCY ALERT! {$user['full_name']} has activated RouteMate SOS. Emergency type: {$emergencyType}. {$locationText}.{$customPart} Please contact them immediately.";

            // Real SMS Gateway check: Do not claim delivered unless an actual provider is configured
            $smsGatewayConfigured = !empty(getenv('SMS_GATEWAY_URL')) || !empty(getenv('TWILIO_AUTH_TOKEN'));
            $initialDeliveryStatus = $smsGatewayConfigured ? 'queued' : 'failed';

            $recipientStmt = $db->prepare('
                INSERT INTO emergency_alert_recipients 
                    (alert_id, recipient_contact_id, recipient_name, recipient_phone, delivery_status, notified_at)
                VALUES 
                    (:aid, :cid, :cname, :cphone, :status, NOW())
            ');

            $recipients = [];
            foreach ($contacts as $c) {
                $recipientStmt->execute([
                    'aid' => $alertId,
                    'cid' => $c['id'],
                    'cname' => $c['contact_name'],
                    'cphone' => $c['phone'],
                    'status' => $initialDeliveryStatus
                ]);
                $recipients[] = [
                    'contact_id' => (int)$c['id'],
                    'name' => $c['contact_name'],
                    'phone' => $c['phone'],
                    'relationship' => $c['relationship'] ?? 'Family',
                    'is_primary' => (bool)($c['is_primary'] ?? 0),
                    'delivery_status' => $initialDeliveryStatus
                ];
            }

            // Also alert team members if option enabled
            $notifiedTeamCount = 0;
            if ($notifyTeam) {
                $teamStmt = $db->prepare('
                    SELECT DISTINCT tm.user_id 
                    FROM team_members tm
                    WHERE tm.team_id IN (
                        SELECT team_id FROM team_members WHERE user_id = :uid AND status = "approved"
                        UNION
                        SELECT id FROM teams WHERE leader_id = :uid2
                    ) AND tm.user_id != :uid3 AND tm.status = "approved"
                ');
                $teamStmt->execute(['uid' => $userId, 'uid2' => $userId, 'uid3' => $userId]);
                $teamMembers = $teamStmt->fetchAll(PDO::FETCH_COLUMN);

                $notifStmt = $db->prepare('
                    INSERT INTO notifications (user_id, title, message, type)
                    VALUES (:tid, "EMERGENCY SOS ALERT", :msg, "system")
                ');

                $locInfo = ($lat !== null && $lng !== null) ? " Location: https://maps.google.com/?q={$lat},{$lng}" : "";
                foreach ($teamMembers as $tmId) {
                    $notifStmt->execute([
                        'tid' => $tmId,
                        'msg' => "EMERGENCY: Rider {$user['full_name']} triggered an SOS alert ({$emergencyType}).{$locInfo}"
                    ]);
                    $notifiedTeamCount++;
                }
            }

            $db->commit();

            Response::success([
                'alert_id' => $alertId,
                'user_id' => $userId,
                'user_name' => $user['full_name'],
                'emergency_type' => $emergencyType,
                'latitude' => $lat,
                'longitude' => $lng,
                'maps_url' => $mapUrl,
                'address' => $address,
                'message' => $message,
                'standard_message' => $standardAlertMessage,
                'alert_status' => 'active',
                'contacts_alerted' => count($recipients),
                'team_members_notified' => $notifiedTeamCount,
                'sms_provider_configured' => $smsGatewayConfigured,
                'sms_provider_status' => $smsGatewayConfigured 
                    ? 'Gateway active - SMS queued for emergency contacts.' 
                    : 'SMS gateway not configured on server. Direct phone dialer and device SMS are ready.',
                'recipients' => $recipients,
                'created_at' => date('c')
            ], 'Emergency SOS alert recorded in database', 201);

        } catch (\Exception $e) {
            $db->rollBack();
            Response::error('Failed to broadcast emergency alert: ' . $e->getMessage(), 500);
        }
    }

    /**
     * GET /api/emergency/history
     */
    public function getHistory(): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT id, user_id, emergency_type, latitude, longitude, address, message, alert_status, created_at, cancelled_at
            FROM emergency_alerts
            WHERE user_id = :uid
            ORDER BY created_at DESC
        ');
        $stmt->execute(['uid' => $userId]);
        $alerts = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $recipientStmt = $db->prepare('
            SELECT recipient_name, recipient_phone, delivery_status, notified_at
            FROM emergency_alert_recipients
            WHERE alert_id = :aid
        ');

        $formatted = [];
        foreach ($alerts as $a) {
            $recipientStmt->execute(['aid' => $a['id']]);
            $recipients = $recipientStmt->fetchAll(PDO::FETCH_ASSOC);

            $lat = $a['latitude'] !== null ? (float)$a['latitude'] : null;
            $lng = $a['longitude'] !== null ? (float)$a['longitude'] : null;

            $formatted[] = [
                'id' => (int)$a['id'],
                'emergency_type' => $a['emergency_type'],
                'latitude' => $lat,
                'longitude' => $lng,
                'maps_url' => ($lat !== null && $lng !== null) ? "https://maps.google.com/?q={$lat},{$lng}" : null,
                'address' => $a['address'] ?? 'Location coordinates captured',
                'message' => $a['message'] ?? '',
                'alert_status' => $a['alert_status'],
                'recipients_count' => count($recipients),
                'recipients' => $recipients,
                'created_at' => $a['created_at'],
                'cancelled_at' => $a['cancelled_at']
            ];
        }

        Response::success([
            'total_alerts' => count($formatted),
            'history' => $formatted
        ], 'Emergency history retrieved');
    }

    /**
     * GET /api/emergency/alerts/{id}
     */
    public function getAlertDetails(int $alertId): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT ea.*, u.full_name AS user_name, u.phone_number AS user_phone, u.avatar_url
            FROM emergency_alerts ea
            JOIN users u ON ea.user_id = u.id
            WHERE ea.id = :id
            LIMIT 1
        ');
        $stmt->execute(['id' => $alertId]);
        $a = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$a) {
            Response::error('Emergency alert not found', 404);
        }

        $recipientStmt = $db->prepare('
            SELECT recipient_name, recipient_phone, delivery_status, notified_at
            FROM emergency_alert_recipients
            WHERE alert_id = :aid
        ');
        $recipientStmt->execute(['aid' => $alertId]);
        $recipients = $recipientStmt->fetchAll(PDO::FETCH_ASSOC);

        $lat = $a['latitude'] !== null ? (float)$a['latitude'] : null;
        $lng = $a['longitude'] !== null ? (float)$a['longitude'] : null;

        Response::success([
            'id' => (int)$a['id'],
            'user_id' => (int)$a['user_id'],
            'user_name' => $a['user_name'],
            'user_phone' => $a['user_phone'],
            'emergency_type' => $a['emergency_type'],
            'latitude' => $lat,
            'longitude' => $lng,
            'maps_url' => ($lat !== null && $lng !== null) ? "https://maps.google.com/?q={$lat},{$lng}" : null,
            'address' => $a['address'],
            'message' => $a['message'],
            'alert_status' => $a['alert_status'],
            'created_at' => $a['created_at'],
            'cancelled_at' => $a['cancelled_at'],
            'recipients' => $recipients
        ], 'Alert details retrieved');
    }

    /**
     * POST /api/emergency/alerts/{id}/cancel
     */
    public function cancelAlert(int $alertId): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT id, user_id, alert_status FROM emergency_alerts WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $alertId]);
        $alert = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$alert) {
            Response::error('Emergency alert not found', 404);
        }

        if ((int)$alert['user_id'] !== $userId) {
            Response::error('You can only cancel your own emergency alerts', 403);
        }

        if ($alert['alert_status'] === 'cancelled') {
            Response::success(null, 'Alert has already been marked as cancelled');
            return;
        }

        $update = $db->prepare('UPDATE emergency_alerts SET alert_status = "cancelled", cancelled_at = NOW() WHERE id = :id');
        $update->execute(['id' => $alertId]);

        Response::success([
            'alert_id' => $alertId,
            'status' => 'cancelled',
            'cancelled_at' => date('c')
        ], 'Emergency alert successfully cancelled');
    }
}

