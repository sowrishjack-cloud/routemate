<?php
declare(strict_types=1);

namespace RouteMate\Services;

class SafetyChecklistEngine {
    /**
     * Generate customized motorcycle safety and prep checklist
     */
    public function generateChecklist(int $days, string $destination, ?array $weather = null): array {
        // 1. Mandatory Gear
        $gear = [
            ['item' => 'Full-face helmet (ECE 22.06 / DOT / ISI certified)', 'required' => true],
            ['item' => 'CE-certified armored riding jacket (Level 2 armor)', 'required' => true],
            ['item' => 'Abrasion-resistant riding pants or knee guards', 'required' => true],
            ['item' => 'Full-gauntlet or reinforced leather riding gloves', 'required' => true],
            ['item' => 'Ankle-high motorcycle riding boots', 'required' => true],
            ['item' => 'Earplugs (reduces highway wind noise fatigue by 40%)', 'required' => false]
        ];

        // 2. Bike Pre-Ride Inspection (T-CLOCS standard)
        $bike = [
            ['item' => 'Tires: Check cold PSI and inspect tread for punctures or cuts', 'required' => true],
            ['item' => 'Controls: Check clutch and throttle free play, brake lever firmness', 'required' => true],
            ['item' => 'Lights: Headlight (high/low beam), tail light, brake light, indicators', 'required' => true],
            ['item' => 'Oil & Fluids: Engine oil level, brake fluid reservoir, coolant level', 'required' => true],
            ['item' => 'Chassis & Chain: Chain slack (25-30 mm), clean and lube with O-ring chain spray', 'required' => true],
            ['item' => 'Stands: Ensure side stand return spring is strong and cut-off switch works', 'required' => true]
        ];

        // 3. Essential Documents
        $documents = [
            ['item' => 'Original Driving License (with motorcycle endorsement)', 'required' => true],
            ['item' => 'Vehicle Registration Certificate (RC smart card or digilocker)', 'required' => true],
            ['item' => 'Valid Comprehensive Motorcycle Insurance copy', 'required' => true],
            ['item' => 'Pollution Under Control (PUC) certificate', 'required' => true]
        ];

        // 4. Emergency & Tools
        $emergency = [
            ['item' => 'Tubeless tire puncture repair kit + portable 12V/battery tire inflator', 'required' => true],
            ['item' => 'Motorcycle OEM toolkit + Allen keys + multi-bit screwdriver', 'required' => true],
            ['item' => 'Compact First-Aid kit (bandages, antiseptic ointment, pain relief, ORS packets)', 'required' => true],
            ['item' => 'RouteMate Emergency SOS contacts pre-configured in app', 'required' => true],
            ['item' => 'Fully charged power bank and phone mount with USB charging cable', 'required' => true]
        ];

        // 5. Destination & Weather Specific
        $weatherItems = [];
        $rainProb = (int)($weather['daily_forecast']['rain_probability'] ?? 0);
        $temp = (float)($weather['temperature'] ?? 25.0);

        if ($rainProb >= 30) {
            $weatherItems[] = ['item' => 'Waterproof jacket & pant rain liners', 'required' => true];
            $weatherItems[] = ['item' => 'Waterproof boot covers & glove covers', 'required' => true];
            $weatherItems[] = ['item' => 'Waterproof rain cover for tail bag / saddlebags', 'required' => true];
            $weatherItems[] = ['item' => 'Microfiber cloth for visor wiping', 'required' => true];
        }

        if ($temp <= 14.0) {
            $weatherItems[] = ['item' => 'Thermal base layer (top & bottom)', 'required' => true];
            $weatherItems[] = ['item' => 'Windproof balaclava / neck gaiter', 'required' => true];
            $weatherItems[] = ['item' => 'Anti-fog pinlock visor insert installed', 'required' => true];
        }

        if ($temp >= 35.0) {
            $weatherItems[] = ['item' => '2-Litre Hydration bladder / water bottles', 'required' => true];
            $weatherItems[] = ['item' => 'Electrolyte replacement drink packets (ORS)', 'required' => true];
            $weatherItems[] = ['item' => 'UV neck wrap / light cooling vest', 'required' => false];
        }

        if (empty($weatherItems)) {
            $weatherItems[] = ['item' => 'Clear visor for evening riding', 'required' => true];
            $weatherItems[] = ['item' => 'Microfiber cloth and helmet spray', 'required' => false];
        }

        return [
            'riding_gear' => $gear,
            'bike_inspection' => $bike,
            'documents' => $documents,
            'emergency_kit' => $emergency,
            'weather_specific' => $weatherItems
        ];
    }
}

