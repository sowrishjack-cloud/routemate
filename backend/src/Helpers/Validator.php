<?php
declare(strict_types=1);

namespace RouteMate\Helpers;

class Validator {
    public static function validate(array $data, array $rules): array {
        $errors = [];

        foreach ($rules as $field => $fieldRules) {
            $value = $data[$field] ?? null;
            $ruleList = is_array($fieldRules) ? $fieldRules : explode('|', $fieldRules);

            foreach ($ruleList as $rule) {
                if ($rule === 'required') {
                    if ($value === null || trim((string)$value) === '') {
                        $errors[$field][] = "The {$field} field is required.";
                    }
                } elseif ($rule === 'email') {
                    if ($value !== null && !filter_var($value, FILTER_VALIDATE_EMAIL)) {
                        $errors[$field][] = "The {$field} must be a valid email address.";
                    }
                } elseif (str_starts_with($rule, 'min:')) {
                    $min = (int)substr($rule, 4);
                    if ($value !== null && strlen((string)$value) < $min) {
                        $errors[$field][] = "The {$field} must be at least {$min} characters.";
                    }
                } elseif (str_starts_with($rule, 'max:')) {
                    $max = (int)substr($rule, 4);
                    if ($value !== null && strlen((string)$value) > $max) {
                        $errors[$field][] = "The {$field} must not exceed {$max} characters.";
                    }
                } elseif ($rule === 'numeric') {
                    if ($value !== null && !is_numeric($value)) {
                        $errors[$field][] = "The {$field} must be a number.";
                    }
                }
            }
        }

        return $errors;
    }
}

