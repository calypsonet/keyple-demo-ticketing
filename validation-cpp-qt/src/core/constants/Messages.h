/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include <QString>

namespace core::constants {

/**
 * @brief Centralized message constants
 *
 * Équivalent de Messages.kt dans l'app Android
 */
class Messages
{
public:
    // Error messages
    static const QString ERROR_NO_VALID_TITLE_DETECTED;
    static const QString ERROR_READING_CARD;
    static const QString ERROR_WRITING_CARD;

    // Environment exceptions
    static const QString EXCEPTION_ENVIRONMENT_WRONG_VERSION;
    static const QString EXCEPTION_ENVIRONMENT_END_DATE_EXPIRED;
    static const QString EXCEPTION_ENVIRONMENT_WRONG_STRUCTURE;

    // Event exceptions
    static const QString EXCEPTION_EVENT_WRONG_VERSION;
    static const QString EXCEPTION_EVENT_WRONG_STRUCTURE;

    // Contract exceptions
    static const QString EXCEPTION_CONTRACT_WRONG_VERSION;
    static const QString EXCEPTION_CONTRACT_DATE_EXPIRED;
    static const QString EXCEPTION_CONTRACT_UNAVAILABLE;

    // Card state exceptions
    static const QString EXCEPTION_CARD_ALREADY_TAPPED;
    static const QString EXCEPTION_NO_TRIP_AVAILABLE;
    static const QString EXCEPTION_INSUFFICIENT_STORED_VALUE;

    // Log messages
    static const QString LOG_VALIDATION_SUCCESS;
    static const QString LOG_VALIDATION_FAILURE;
    static const QString LOG_CARD_DETECTED;
    static const QString LOG_READER_INITIALIZED;

    // Card type prefixes
    static const QString CARD_TYPE_CALYPSO;
    static const QString CARD_TYPE_STORAGE;
    static const QString CARD_TYPE_UNKNOWN;

    // Empty values
    static const QString EMPTY_CONTRACT;
    static const QString EMPTY_LOCATION;

private:
    Messages() = delete; // Static class, no instantiation
};

} // namespace core::constants
