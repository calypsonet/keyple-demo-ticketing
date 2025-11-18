/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include <memory>
#include <vector>
#include <string>

// NOTE: Inclure les headers Keyple C++ appropriés
// #include <keyple/core/service/Plugin.h>
// #include <keyple/core/service/CardReader.h>
// #include <keyple/plugin/pcsc/PcscPlugin.h>

namespace data::repository {

/**
 * @brief Manages PC/SC card readers
 *
 * Équivalent de ReaderRepository.kt avec PC/SC au lieu des plugins Android
 */
class ReaderRepository
{
public:
    ReaderRepository();
    ~ReaderRepository();

    /**
     * @brief Register PC/SC plugin
     *
     * Remplace les plugins Android (Bluebird, Coppernic, etc.)
     */
    void registerPlugin();

    /**
     * @brief Initialize contactless card reader
     *
     * Recherche et configure un lecteur contactless PC/SC
     * (ex: ACS ACR122U, Identiv SCR3500)
     *
     * @return Shared pointer to initialized reader
     * @throws std::runtime_error if no reader found
     */
    // std::shared_ptr<CardReader> initCardReader();

    /**
     * @brief Initialize SAM readers
     *
     * Recherche et configure les lecteurs SAM (contact)
     *
     * @return Vector of SAM readers
     */
    // std::vector<std::shared_ptr<CardReader>> initSamReaders();

    /**
     * @brief Get the contactless card reader
     */
    // std::shared_ptr<CardReader> getCardReader() const;

    /**
     * @brief Get the selected SAM reader
     */
    // std::shared_ptr<CardReader> getSamReader() const;

    /**
     * @brief Check if storage cards are supported
     *
     * @return true if MIFARE/ST25 protocols available
     */
    bool isStorageCardSupported() const;

    /**
     * @brief Play success audio feedback
     *
     * Équivalent de displayResultSuccess() sur Android
     *
     * @return true if audio played successfully
     */
    bool displayResultSuccess() const;

    /**
     * @brief Play failure audio feedback
     *
     * @return true if audio played successfully
     */
    bool displayResultFailed() const;

    /**
     * @brief Clear and release all readers
     */
    void clear();

private:
    // Keyple plugin and readers (uncomment when Keyple C++ is available)
    // std::shared_ptr<Plugin> m_pcscPlugin;
    // std::shared_ptr<CardReader> m_cardReader;
    // std::shared_ptr<CardReader> m_samReader;
    // std::vector<std::shared_ptr<CardReader>> m_samReaders;

    bool m_initialized = false;

    /**
     * @brief Find PC/SC reader by name pattern
     *
     * @param namePattern Pattern to match (e.g., "ACR122", "Omnikey")
     * @return Reader if found, nullptr otherwise
     */
    // std::shared_ptr<CardReader> findReaderByName(const std::string& namePattern);

    /**
     * @brief Activate card protocols on reader
     *
     * @param reader Reader to configure
     */
    // void activateProtocols(std::shared_ptr<CardReader> reader);
};

} // namespace data::repository
