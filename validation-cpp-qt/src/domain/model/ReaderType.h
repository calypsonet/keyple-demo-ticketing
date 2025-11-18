/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

namespace domain::model {

/**
 * @brief Reader hardware type enum
 *
 * Sur desktop, on utilise uniquement PC/SC
 * Les types Android (Bluebird, Coppernic, etc.) sont conservés pour référence
 */
enum class ReaderType {
    PCSC,          ///< PC/SC standard reader (desktop)
    BLUEBIRD,      ///< Bluebird Android device (legacy)
    COPPERNIC,     ///< Coppernic CNA device (legacy)
    FAMOCO,        ///< Famoco Android device (legacy)
    FLOWBIRD       ///< Flowbird device (legacy)
};

/**
 * @brief Convert ReaderType to string
 */
inline const char* readerTypeToString(ReaderType type) {
    switch (type) {
        case ReaderType::PCSC:      return "PC/SC";
        case ReaderType::BLUEBIRD:  return "Bluebird";
        case ReaderType::COPPERNIC: return "Coppernic";
        case ReaderType::FAMOCO:    return "Famoco";
        case ReaderType::FLOWBIRD:  return "Flowbird";
        default:                    return "Unknown";
    }
}

} // namespace domain::model
