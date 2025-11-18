/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include <QObject>
#include "domain/model/CardReaderResponse.h"
#include "domain/model/Location.h"
#include <vector>

namespace domain::service {

/**
 * @brief Central orchestrator for ticketing operations
 *
 * Équivalent de TicketingService.kt
 */
class TicketingService : public QObject
{
    Q_OBJECT

public:
    explicit TicketingService(QObject *parent = nullptr);
    ~TicketingService();

    /**
     * @brief Initialize readers and Keyple services
     */
    void init();

    /**
     * @brief Start NFC card detection
     */
    void startNfcDetection();

    /**
     * @brief Stop NFC card detection
     */
    void stopNfcDetection();

    /**
     * @brief Execute validation procedure on detected card
     *
     * @param locations Available locations
     * @return Validation result
     */
    model::CardReaderResponse executeValidationProcedure(
        const std::vector<model::Location>& locations
    );

    /**
     * @brief Check if readers are initialized
     */
    bool readersInitialized() const { return m_readersInitialized; }

    /**
     * @brief Cleanup resources
     */
    void onDestroy();

signals:
    /**
     * @brief Emitted when a card is detected
     */
    void cardDetected(const QString& cardType);

    /**
     * @brief Emitted when validation is complete
     */
    void validationComplete(const model::CardReaderResponse& response);

    /**
     * @brief Emitted on error
     */
    void error(const QString& message);

private:
    bool m_readersInitialized = false;

    // TODO: Add Keyple C++ members
    // std::shared_ptr<ReaderRepository> m_readerRepository;
    // std::shared_ptr<CardSelectionManager> m_selectionManager;
    // std::shared_ptr<SmartCard> m_smartCard;
};

} // namespace domain::service
