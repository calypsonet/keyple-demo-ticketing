/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

namespace domain::model {

/**
 * @brief Validation status enum
 *
 * Équivalent de Status.kt dans l'app Android
 */
enum class Status {
    LOADING,          ///< Operation in progress
    SUCCESS,          ///< Validation successful
    INVALID_CARD,     ///< Card failed validation
    EMPTY_CARD,       ///< Card lacks required data/contracts
    ERROR             ///< Generic error state
};

/**
 * @brief Convert Status to string representation
 */
inline const char* statusToString(Status status) {
    switch (status) {
        case Status::LOADING:      return "LOADING";
        case Status::SUCCESS:      return "SUCCESS";
        case Status::INVALID_CARD: return "INVALID_CARD";
        case Status::EMPTY_CARD:   return "EMPTY_CARD";
        case Status::ERROR:        return "ERROR";
        default:                   return "UNKNOWN";
    }
}

} // namespace domain::model
