/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

namespace domain::model {

/**
 * @brief Card protocol types
 *
 * Équivalent de CardProtocolEnum.kt
 */
enum class CardProtocolEnum {
    ISO_7816_LOGICAL_PROTOCOL,           ///< Contact card protocol
    ISO_14443_4_LOGICAL_PROTOCOL,        ///< Contactless ISO 14443-4
    MIFARE_ULTRALIGHT_LOGICAL_PROTOCOL,  ///< MIFARE Ultralight storage cards
    ST25_SRT512_LOGICAL_PROTOCOL         ///< ST25 SRT512 storage cards
};

/**
 * @brief Convert CardProtocolEnum to string name
 */
inline const char* cardProtocolToString(CardProtocolEnum protocol) {
    switch (protocol) {
        case CardProtocolEnum::ISO_7816_LOGICAL_PROTOCOL:
            return "ISO_7816_LOGICAL_PROTOCOL";
        case CardProtocolEnum::ISO_14443_4_LOGICAL_PROTOCOL:
            return "ISO_14443_4_LOGICAL_PROTOCOL";
        case CardProtocolEnum::MIFARE_ULTRALIGHT_LOGICAL_PROTOCOL:
            return "MIFARE_ULTRALIGHT_LOGICAL_PROTOCOL";
        case CardProtocolEnum::ST25_SRT512_LOGICAL_PROTOCOL:
            return "ST25_SRT512_LOGICAL_PROTOCOL";
        default:
            return "UNKNOWN_PROTOCOL";
    }
}

} // namespace domain::model
