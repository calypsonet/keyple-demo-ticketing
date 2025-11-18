/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include "domain/model/CardReaderResponse.h"
#include "domain/model/Location.h"
#include <vector>
// TODO: Include Keyple C++ headers
// #include <keyple/card/calypso/CalypsoCard.h>

namespace data::repository {

/**
 * @brief Handles Calypso card validation workflow
 *
 * Équivalent de CalypsoCardRepository.kt
 */
class CalypsoCardRepository
{
public:
    CalypsoCardRepository() = default;
    ~CalypsoCardRepository() = default;

    /**
     * @brief Execute validation procedure on Calypso card
     *
     * Steps:
     * 1. Open secure session for debit
     * 2. Read environment, event, contracts
     * 3. Validate with ValidationRules
     * 4. Process contract by priority
     * 5. Update counters/events
     * 6. Close secure session
     *
     * @param validationDateTime Current date/time
     * @param validationAmount Amount to debit
     * @param cardReader Card reader instance
     * @param calypsoCard Calypso card instance
     * @param cardSecuritySettings Security settings with SAM
     * @param locations List of available locations
     * @return CardReaderResponse with validation result
     */
    domain::model::CardReaderResponse executeValidationProcedure(
        // const QDateTime& validationDateTime,
        // int validationAmount,
        // std::shared_ptr<CardReader> cardReader,
        // std::shared_ptr<CalypsoCard> calypsoCard,
        // std::shared_ptr<SymmetricCryptoSecuritySetting> cardSecuritySettings,
        const std::vector<domain::model::Location>& locations
    );

private:
    // TODO: Implement helper methods
    // void openSecureSession(...);
    // void readEnvironment(...);
    // void readEvent(...);
    // std::vector<Contract> readContracts(...);
    // void updateEvent(...);
    // void closeSession(...);
};

} // namespace data::repository
