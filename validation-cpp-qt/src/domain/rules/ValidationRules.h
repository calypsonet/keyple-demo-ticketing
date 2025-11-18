/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include <QDateTime>

namespace domain::rules {

/**
 * @brief Centralized business rule validation logic
 *
 * Équivalent de ValidationRules.kt (singleton object)
 * Toutes les méthodes throw ValidationException en cas de violation
 */
class ValidationRules
{
public:
    // Environment validation
    static void validateEnvironmentVersionOrThrow(int version);
    static void validateEnvironmentDateOrThrow(const QDateTime& endDate);

    // Event validation
    static void validateEventVersionOrThrow(int version);

    // Contract validation
    static void validateContractVersionOrThrow(int version);
    static void validateContractDateOrThrow(const QDateTime& validityEndDate);

    // Counter validation
    static void validateTripsAvailableOrThrow(int counter);
    static void validateSufficientStoredValueOrThrow(int counter, int requiredAmount);

    // Anti-passback
    static void validateAntiPassbackOrThrow(const QDateTime& lastEventDateTime);

    // Helper methods
    static int calculateDecrementAmount(int contractType);
    static bool isCounterBasedContract(int contractType);

private:
    ValidationRules() = delete;  // Static class

    static constexpr int CURRENT_VERSION = 1;
    static constexpr int ANTI_PASSBACK_DELAY_MS = 60000;  // 1 minute
};

} // namespace domain::rules
