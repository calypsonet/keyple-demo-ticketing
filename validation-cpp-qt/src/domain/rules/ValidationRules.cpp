/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#include "ValidationRules.h"
#include "domain/exception/ValidationException.h"
#include "core/constants/Messages.h"
#include "domain/model/Status.h"

namespace domain::rules {

void ValidationRules::validateEnvironmentVersionOrThrow(int version)
{
    if (version != CURRENT_VERSION) {
        throw exception::ValidationException(
            core::constants::Messages::EXCEPTION_ENVIRONMENT_WRONG_VERSION.toStdString(),
            model::Status::INVALID_CARD
        );
    }
}

void ValidationRules::validateEnvironmentDateOrThrow(const QDateTime& endDate)
{
    if (endDate < QDateTime::currentDateTime()) {
        throw exception::ValidationException(
            core::constants::Messages::EXCEPTION_ENVIRONMENT_END_DATE_EXPIRED.toStdString(),
            model::Status::INVALID_CARD
        );
    }
}

void ValidationRules::validateEventVersionOrThrow(int version)
{
    if (version != CURRENT_VERSION) {
        throw exception::ValidationException(
            core::constants::Messages::EXCEPTION_EVENT_WRONG_VERSION.toStdString(),
            model::Status::INVALID_CARD
        );
    }
}

void ValidationRules::validateContractVersionOrThrow(int version)
{
    if (version != CURRENT_VERSION) {
        throw exception::ValidationException(
            core::constants::Messages::EXCEPTION_CONTRACT_WRONG_VERSION.toStdString(),
            model::Status::INVALID_CARD
        );
    }
}

void ValidationRules::validateContractDateOrThrow(const QDateTime& validityEndDate)
{
    if (validityEndDate < QDateTime::currentDateTime()) {
        throw exception::ValidationException(
            core::constants::Messages::EXCEPTION_CONTRACT_DATE_EXPIRED.toStdString(),
            model::Status::EMPTY_CARD
        );
    }
}

void ValidationRules::validateTripsAvailableOrThrow(int counter)
{
    if (counter <= 0) {
        throw exception::ValidationException(
            core::constants::Messages::EXCEPTION_NO_TRIP_AVAILABLE.toStdString(),
            model::Status::EMPTY_CARD
        );
    }
}

void ValidationRules::validateSufficientStoredValueOrThrow(int counter, int requiredAmount)
{
    if (counter < requiredAmount) {
        throw exception::ValidationException(
            core::constants::Messages::EXCEPTION_INSUFFICIENT_STORED_VALUE.toStdString(),
            model::Status::EMPTY_CARD
        );
    }
}

void ValidationRules::validateAntiPassbackOrThrow(const QDateTime& lastEventDateTime)
{
    auto timeDiff = lastEventDateTime.msecsTo(QDateTime::currentDateTime());
    if (timeDiff < ANTI_PASSBACK_DELAY_MS) {
        throw exception::ValidationException(
            core::constants::Messages::EXCEPTION_CARD_ALREADY_TAPPED.toStdString(),
            model::Status::INVALID_CARD
        );
    }
}

int ValidationRules::calculateDecrementAmount(int contractType)
{
    // TODO: Implement based on contract type
    // For now, return 1
    return 1;
}

bool ValidationRules::isCounterBasedContract(int contractType)
{
    // TODO: Check if contract uses counters (MULTI_TRIP, STORED_VALUE)
    // vs season pass
    return true;
}

} // namespace domain::rules
