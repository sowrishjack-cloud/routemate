<?php
declare(strict_types=1);

namespace RouteMate\Services;

class BudgetCalculationEngine {
    public const DEFAULT_FUEL_PRICE_PER_LITRE = 102.50; // INR average for petrol in India
    public const DEFAULT_BIKE_MILEAGE_KMPL = 35.0;     // Typical 350-450cc touring motorcycle
    public const DEFAULT_DAILY_FOOD_PER_RIDER = 600.0;  // Realistic breakfast, lunch, tea, dinner
    public const DEFAULT_DAILY_STAY_PER_ROOM = 1200.0;  // Standard clean rider lodge / homestay

    /**
     * Compute mathematically grounded trip budget breakdown
     */
    public function calculate(
        float $distanceKm,
        int $travelDays = 1,
        int $numberOfRiders = 1,
        ?float $bikeMileage = null,
        ?float $fuelPrice = null,
        ?float $userBudgetLimit = null,
        ?float $customStayCost = null,
        ?float $customFoodCost = null,
        ?float $customTollCost = null
    ): array {
        $distance = max(1.0, $distanceKm);
        $days = max(1, $travelDays);
        $riders = max(1, $numberOfRiders);
        
        $mileage = ($bikeMileage !== null && $bikeMileage > 5.0) ? $bikeMileage : self::DEFAULT_BIKE_MILEAGE_KMPL;
        $pricePerLitre = ($fuelPrice !== null && $fuelPrice > 50.0) ? $fuelPrice : self::DEFAULT_FUEL_PRICE_PER_LITRE;

        // 1. Fuel Calculation
        $fuelLiters = round($distance / $mileage, 2);
        $fuelCost = round($fuelLiters * $pricePerLitre, 2);

        // 2. Food Calculation
        $dailyFood = $customFoodCost ?? self::DEFAULT_DAILY_FOOD_PER_RIDER;
        $foodCost = round($dailyFood * $days * $riders, 2);

        // 3. Accommodation Calculation (days - 1 nights)
        $nights = max(0, $days - 1);
        $roomsNeeded = (int)ceil($riders / 2.0); // Assume double occupancy sharing
        $dailyStay = $customStayCost ?? self::DEFAULT_DAILY_STAY_PER_ROOM;
        $stayCost = round($dailyStay * $nights * $roomsNeeded, 2);

        // 4. Tolls and Emergency Reserve
        // Two-wheelers in India are exempt on most National Highways (NHAI), but state expressways (e.g. Bangalore-Mysore, Mumbai-Pune, Yamuna) or ghat passes may levy entry/green tax
        $tolls = $customTollCost ?? 150.0;
        $emergencyBuffer = round($fuelCost * 0.10 + 300.0, 2); // 10% fuel cushion + puncture/lube buffer

        // 5. Total and Per-Rider Cost
        $totalEstimatedCost = round($fuelCost + $foodCost + $stayCost + $tolls + $emergencyBuffer, 2);
        $perRiderCost = round($totalEstimatedCost / $riders, 2);

        // 6. Compare with User Budget Limit
        $status = 'within_budget';
        $budgetVariance = 0.0;
        $budgetAdvice = 'Estimated trip cost is well within your budget limit.';

        if ($userBudgetLimit !== null && $userBudgetLimit > 0) {
            $budgetVariance = round($userBudgetLimit - $totalEstimatedCost, 2);
            if ($budgetVariance < 0) {
                $status = 'exceeds_budget';
                $deficit = abs($budgetVariance);
                $budgetAdvice = "Estimated cost exceeds your budget limit of ₹" . number_format($userBudgetLimit, 0) . 
                    " by ₹" . number_format($deficit, 0) . 
                    ". Consider adding a riding partner to split room costs, reducing stay to budget homestays, or shortening trip by 1 day.";
            } else {
                $budgetAdvice = "Estimated cost of ₹" . number_format($totalEstimatedCost, 0) . 
                    " is within your budget of ₹" . number_format($userBudgetLimit, 0) . 
                    " (Estimated surplus: ₹" . number_format($budgetVariance, 0) . ").";
            }
        }

        return [
            'distance_km' => $distance,
            'travel_days' => $days,
            'number_of_riders' => $riders,
            'bike_mileage_kmpl' => $mileage,
            'fuel_price_per_litre' => $pricePerLitre,
            'fuel_liters_required' => $fuelLiters,
            'fuel_cost' => $fuelCost,
            'food_cost' => $foodCost,
            'accommodation_cost' => $stayCost,
            'tolls_and_entry_tax' => $tolls,
            'emergency_reserve' => $emergencyBuffer,
            'total_estimated_cost' => $totalEstimatedCost,
            'per_rider_cost' => $perRiderCost,
            'user_budget_limit' => $userBudgetLimit,
            'budget_variance' => $budgetVariance,
            'budget_status' => $status,
            'budget_advice' => $budgetAdvice,
            'formula' => [
                'fuel' => "Fuel Required ({$fuelLiters} L) = Distance ({$distance} km) / Mileage ({$mileage} km/L)",
                'fuel_cost' => "Fuel Cost (₹{$fuelCost}) = Fuel Required ({$fuelLiters} L) × Price (₹{$pricePerLitre}/L)",
                'total' => "Total = Fuel (₹{$fuelCost}) + Food (₹{$foodCost}) + Stay (₹{$stayCost}) + Tolls (₹{$tolls}) + Reserve (₹{$emergencyBuffer})",
                'per_rider' => "Per Rider (₹{$perRiderCost}) = Total (₹{$totalEstimatedCost}) / {$riders} rider(s)"
            ],
            'disclaimer' => 'All figures are mathematical calculations based on entered inputs and verified baseline rates. Fuel consumption varies with throttle, elevation, wind, and luggage.'
        ];
    }
}

