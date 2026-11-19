<?php
declare(strict_types=1);

namespace RouteMate\Helpers;

use Exception;

class JWT {
    private static function base64UrlEncode(string $data): string {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    private static function base64UrlDecode(string $data): string {
        return base64_decode(strtr($data, '-_', '+/'));
    }

    public static function generate(array $payload): string {
        $secret = getenv('JWT_SECRET') ?: 'routemate_super_secret_jwt_key_development_2026_xyz';
        $exp = (int)(getenv('JWT_EXPIRATION') ?: 604800);
        $issuer = getenv('JWT_ISSUER') ?: 'RouteMateAuth';

        $header = [
            'typ' => 'JWT',
            'alg' => 'HS256'
        ];

        $payload['iss'] = $issuer;
        $payload['iat'] = time();
        $payload['exp'] = time() + $exp;

        $encodedHeader = self::base64UrlEncode((string)json_encode($header));
        $encodedPayload = self::base64UrlEncode((string)json_encode($payload));

        $signature = hash_hmac('sha256', "{$encodedHeader}.{$encodedPayload}", $secret, true);
        $encodedSignature = self::base64UrlEncode($signature);

        return "{$encodedHeader}.{$encodedPayload}.{$encodedSignature}";
    }

    public static function verify(string $token): ?array {
        $secret = getenv('JWT_SECRET') ?: 'routemate_super_secret_jwt_key_development_2026_xyz';
        $parts = explode('.', $token);

        if (count($parts) !== 3) {
            return null;
        }

        [$headerB64, $payloadB64, $signatureB64] = $parts;

        $expectedSig = hash_hmac('sha256', "{$headerB64}.{$payloadB64}", $secret, true);
        $expectedSigB64 = self::base64UrlEncode($expectedSig);

        if (!hash_equals($expectedSigB64, $signatureB64)) {
            return null;
        }

        $payload = json_decode(self::base64UrlDecode($payloadB64), true);

        if (!$payload || !isset($payload['exp']) || $payload['exp'] < time()) {
            return null;
        }

        return $payload;
    }
}

