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
// #include <keyple/card/storagecard/StorageCard.h>

namespace data::repository {

/**
 * @brief Handles Storage Card (MIFARE, ST25) validation
 *
 * Équivalent de StorageCardRepository.kt
 */
class StorageCardRepository
{
public:
    StorageCardRepository() = default;
    ~StorageCardRepository() = default;

    /**
     * @brief Execute validation procedure on storage card
     *
     * Steps:
     * 1. Read environment, event, contract blocks
     * 2. Validate environment version and date
     * 3. Validate event version
     * 4. Validate contract version and date
     * 5. Process contract type (MULTI_TRIP, STORED_VALUE, SEASON_PASS)
     * 6. Decrement counter if needed
     * 7. Write updated event and contract
     * 8. Close transaction
     *
     * @param validationDateTime Current date/time
     * @param validationAmount Amount to debit
     * @param cardReader Card reader instance
     * @param storageCard Storage card instance
     * @param locations List of available locations
     * @return CardReaderResponse with validation result
     */
    domain::model::CardReaderResponse executeValidationProcedure(
        // const QDateTime& validationDateTime,
        // int validationAmount,
        // std::shared_ptr<CardReader> cardReader,
        // std::shared_ptr<StorageCard> storageCard,
        const std::vector<domain::model::Location>& locations
    );

private:
    // TODO: Implement helper methods
    // void readBlocks(...);
    // void validateEnvironment(...);
    // void updateContract(...);
    // void writeBlocks(...);
};

} // namespace data::repository
