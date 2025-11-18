/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#include "Messages.h"

namespace core::constants {

// Error messages
const QString Messages::ERROR_NO_VALID_TITLE_DETECTED = "No valid title detected";
const QString Messages::ERROR_READING_CARD = "Error reading card";
const QString Messages::ERROR_WRITING_CARD = "Error writing card";

// Environment exceptions
const QString Messages::EXCEPTION_ENVIRONMENT_WRONG_VERSION = "Environment: wrong version";
const QString Messages::EXCEPTION_ENVIRONMENT_END_DATE_EXPIRED = "Environment: end date has expired";
const QString Messages::EXCEPTION_ENVIRONMENT_WRONG_STRUCTURE = "Environment: wrong structure";

// Event exceptions
const QString Messages::EXCEPTION_EVENT_WRONG_VERSION = "Event: wrong version";
const QString Messages::EXCEPTION_EVENT_WRONG_STRUCTURE = "Event: wrong structure";

// Contract exceptions
const QString Messages::EXCEPTION_CONTRACT_WRONG_VERSION = "Contract: wrong version";
const QString Messages::EXCEPTION_CONTRACT_DATE_EXPIRED = "Contract: validity date has expired";
const QString Messages::EXCEPTION_CONTRACT_UNAVAILABLE = "Contract: unavailable";

// Card state exceptions
const QString Messages::EXCEPTION_CARD_ALREADY_TAPPED = "Card already tapped (anti-passback)";
const QString Messages::EXCEPTION_NO_TRIP_AVAILABLE = "No trip available";
const QString Messages::EXCEPTION_INSUFFICIENT_STORED_VALUE = "Insufficient stored value";

// Log messages
const QString Messages::LOG_VALIDATION_SUCCESS = "Validation successful";
const QString Messages::LOG_VALIDATION_FAILURE = "Validation failed";
const QString Messages::LOG_CARD_DETECTED = "Card detected";
const QString Messages::LOG_READER_INITIALIZED = "Reader initialized";

// Card type prefixes
const QString Messages::CARD_TYPE_CALYPSO = "CALYPSO";
const QString Messages::CARD_TYPE_STORAGE = "STORAGE";
const QString Messages::CARD_TYPE_UNKNOWN = "UNKNOWN";

// Empty values
const QString Messages::EMPTY_CONTRACT = "-";
const QString Messages::EMPTY_LOCATION = "Unknown location";

} // namespace core::constants
