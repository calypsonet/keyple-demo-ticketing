/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#include "ReaderWidget.h"
#include "ui_ReaderWidget.h"
#include "ui/cardsummary/CardSummaryDialog.h"
#include "core/logging/Logger.h"
#include <QMessageBox>

namespace ui {

ReaderWidget::ReaderWidget(QWidget *parent)
    : QWidget(parent)
    , ui(new Ui::ReaderWidget)
{
    ui->setupUi(this);

    setWindowTitle("Card Reader");
    resize(600, 400);

    // Setup animation
    m_animation = new QMovie(":/animations/card_tap.gif", QByteArray(), this);
    ui->animationLabel->setMovie(m_animation);
    m_animation->start();

    core::Logger::info("ReaderWidget created");
}

ReaderWidget::~ReaderWidget()
{
    delete ui;
}

void ReaderWidget::showEvent(QShowEvent *event)
{
    QWidget::showEvent(event);

    // Initialize readers when widget is shown
    initializeReaders();
    startDetection();

    core::Logger::info("ReaderWidget shown, detection started");
}

void ReaderWidget::hideEvent(QHideEvent *event)
{
    QWidget::hideEvent(event);

    stopDetection();

    core::Logger::info("ReaderWidget hidden, detection stopped");
}

void ReaderWidget::closeEvent(QCloseEvent *event)
{
    stopDetection();

    // TODO: Cleanup TicketingService
    // if (m_ticketingService) {
    //     m_ticketingService->onDestroy();
    // }

    QWidget::closeEvent(event);
}

void ReaderWidget::initializeReaders()
{
    // TODO: Initialize TicketingService with PC/SC
    /*
    try {
        m_ticketingService = new TicketingService(...);
        m_ticketingService->init();
        core::Logger::info("Readers initialized successfully");
    } catch (const std::exception& e) {
        core::Logger::error("Failed to initialize readers: {}", e.what());
        QMessageBox::critical(this, "Error", QString("Failed to initialize readers:\n%1").arg(e.what()));
    }
    */

    core::Logger::warn("Reader initialization not implemented yet");
}

void ReaderWidget::startDetection()
{
    // TODO: Start NFC detection via TicketingService
    /*
    if (m_ticketingService) {
        m_ticketingService->startNfcDetection();
        ui->statusLabel->setText("Present your card...");
    }
    */

    ui->statusLabel->setText("Present your card... (stub mode)");
    core::Logger::info("Card detection started");
}

void ReaderWidget::stopDetection()
{
    // TODO: Stop NFC detection
    /*
    if (m_ticketingService) {
        m_ticketingService->stopNfcDetection();
    }
    */

    core::Logger::info("Card detection stopped");
}

void ReaderWidget::onCardDetected()
{
    core::Logger::info("Card detected!");

    // TODO: Process card via TicketingService
    /*
    QtConcurrent::run([this]() {
        auto response = m_ticketingService->executeValidationProcedure(...);
        emit validationComplete(response);
    });
    */

    // Stub response for testing
    domain::model::CardReaderResponse stubResponse(
        domain::model::Status::SUCCESS,
        "CALYPSO: Test Card",
        "Multi-trip contract",
        std::nullopt,
        5  // 5 tickets left
    );

    onValidationComplete(stubResponse);
}

void ReaderWidget::onValidationComplete(const domain::model::CardReaderResponse& response)
{
    core::Logger::info("Validation complete: {}", statusToString(response.status()));

    showResultDialog(response);
}

void ReaderWidget::showResultDialog(const domain::model::CardReaderResponse& response)
{
    auto dialog = new CardSummaryDialog(response, this);
    dialog->exec();
    dialog->deleteLater();
}

} // namespace ui
