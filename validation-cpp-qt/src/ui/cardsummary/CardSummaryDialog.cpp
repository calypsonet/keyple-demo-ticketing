/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#include "CardSummaryDialog.h"
#include "ui_CardSummaryDialog.h"
#include "core/logging/Logger.h"

namespace ui {

CardSummaryDialog::CardSummaryDialog(const domain::model::CardReaderResponse& response,
                                     QWidget *parent)
    : QDialog(parent)
    , ui(new Ui::CardSummaryDialog)
    , m_response(response)
{
    ui->setupUi(this);

    setWindowTitle("Validation Result");
    setModal(true);

    applyStatusStyling();
    displayResult();
    playAudioFeedback();

    core::Logger::info("CardSummaryDialog shown with status: {}",
                      statusToString(m_response.status()));
}

CardSummaryDialog::~CardSummaryDialog()
{
    delete ui;
}

void CardSummaryDialog::applyStatusStyling()
{
    QString bgColor;
    QString iconPath;

    switch (m_response.status()) {
        case domain::model::Status::SUCCESS:
            bgColor = "#4CAF50";  // Green
            iconPath = ":/images/success.png";
            ui->statusLabel->setText("VALIDATION SUCCESSFUL");
            break;

        case domain::model::Status::INVALID_CARD:
            bgColor = "#FF9800";  // Orange
            iconPath = ":/images/warning.png";
            ui->statusLabel->setText("INVALID CARD");
            break;

        case domain::model::Status::EMPTY_CARD:
        case domain::model::Status::ERROR:
            bgColor = "#F44336";  // Red
            iconPath = ":/images/error.png";
            ui->statusLabel->setText("ERROR");
            break;

        default:
            bgColor = "#9E9E9E";  // Gray
            iconPath = ":/images/info.png";
            ui->statusLabel->setText("PROCESSING");
            break;
    }

    // Apply background color
    setStyleSheet(QString("QDialog { background-color: %1; }").arg(bgColor));

    // Set icon (if exists)
    // ui->iconLabel->setPixmap(QPixmap(iconPath));
}

void CardSummaryDialog::displayResult()
{
    // Card type
    ui->cardTypeLabel->setText(m_response.cardType());

    // Contract
    ui->contractLabel->setText(m_response.contract());

    // Tickets left or pass validity
    if (m_response.nbTicketsLeft().has_value()) {
        int ticketsLeft = m_response.nbTicketsLeft().value();
        ui->ticketsLabel->setText(QString("%1 trip(s) remaining").arg(ticketsLeft));
        ui->ticketsLabel->setVisible(true);
    } else if (m_response.passValidityEndDate().has_value()) {
        QString validUntil = m_response.passValidityEndDate().value()
                               .toString("yyyy-MM-dd");
        ui->ticketsLabel->setText(QString("Valid until: %1").arg(validUntil));
        ui->ticketsLabel->setVisible(true);
    } else {
        ui->ticketsLabel->setVisible(false);
    }

    // Error message (if any)
    if (m_response.errorMessage().has_value()) {
        ui->errorMessageLabel->setText(m_response.errorMessage().value());
        ui->errorMessageLabel->setVisible(true);
    } else {
        ui->errorMessageLabel->setVisible(false);
    }

    // Validation details
    if (m_response.validation().has_value()) {
        const auto& validation = m_response.validation().value();
        ui->locationLabel->setText(validation.location().name());
        ui->dateTimeLabel->setText(validation.dateTime().toString("yyyy-MM-dd hh:mm:ss"));
    }
}

void CardSummaryDialog::playAudioFeedback()
{
    // TODO: Play sound with Qt Multimedia
    /*
    if (m_response.status() == domain::model::Status::SUCCESS) {
        QSound::play(":/sounds/success.wav");
    } else {
        QSound::play(":/sounds/error.wav");
    }
    */

    core::Logger::info("Audio feedback played (stub)");
}

} // namespace ui
