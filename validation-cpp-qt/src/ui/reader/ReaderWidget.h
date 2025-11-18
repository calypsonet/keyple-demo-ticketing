/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include <QWidget>
#include <QMovie>
#include <memory>
#include "domain/model/CardReaderResponse.h"

namespace Ui {
class ReaderWidget;
}

namespace ui {

/**
 * @brief Card reading and NFC detection widget
 *
 * Équivalent de ReaderActivity.kt
 */
class ReaderWidget : public QWidget
{
    Q_OBJECT

public:
    explicit ReaderWidget(QWidget *parent = nullptr);
    ~ReaderWidget();

protected:
    void showEvent(QShowEvent *event) override;
    void hideEvent(QHideEvent *event) override;
    void closeEvent(QCloseEvent *event) override;

private slots:
    /**
     * @brief Called when card is detected
     */
    void onCardDetected();

    /**
     * @brief Called when validation is complete
     */
    void onValidationComplete(const domain::model::CardReaderResponse& response);

private:
    Ui::ReaderWidget *ui;
    QMovie *m_animation = nullptr;

    // TODO: Add TicketingService when implemented
    // domain::service::TicketingService* m_ticketingService = nullptr;

    /**
     * @brief Initialize readers and ticketing service
     */
    void initializeReaders();

    /**
     * @brief Start NFC detection
     */
    void startDetection();

    /**
     * @brief Stop NFC detection
     */
    void stopDetection();

    /**
     * @brief Show validation result dialog
     */
    void showResultDialog(const domain::model::CardReaderResponse& response);
};

} // namespace ui
