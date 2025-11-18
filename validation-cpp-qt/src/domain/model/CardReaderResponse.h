/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include "Status.h"
#include "Validation.h"
#include <QString>
#include <QDateTime>
#include <QMetaType>
#include <optional>

namespace domain::model {

/**
 * @brief Encapsulates card reading and validation result
 *
 * Équivalent de CardReaderResponse.kt
 */
class CardReaderResponse
{
public:
    CardReaderResponse() = default;

    CardReaderResponse(Status status,
                      const QString& cardType,
                      const QString& contract,
                      std::optional<Validation> validation = std::nullopt,
                      std::optional<int> nbTicketsLeft = std::nullopt,
                      std::optional<QDateTime> eventDateTime = std::nullopt,
                      std::optional<QDateTime> passValidityEndDate = std::nullopt,
                      std::optional<QString> errorMessage = std::nullopt)
        : m_status(status)
        , m_cardType(cardType)
        , m_contract(contract)
        , m_validation(validation)
        , m_nbTicketsLeft(nbTicketsLeft)
        , m_eventDateTime(eventDateTime)
        , m_passValidityEndDate(passValidityEndDate)
        , m_errorMessage(errorMessage)
    {}

    // Getters
    Status status() const { return m_status; }
    QString cardType() const { return m_cardType; }
    QString contract() const { return m_contract; }
    std::optional<Validation> validation() const { return m_validation; }
    std::optional<int> nbTicketsLeft() const { return m_nbTicketsLeft; }
    std::optional<QDateTime> eventDateTime() const { return m_eventDateTime; }
    std::optional<QDateTime> passValidityEndDate() const { return m_passValidityEndDate; }
    std::optional<QString> errorMessage() const { return m_errorMessage; }

    // Setters
    void setStatus(Status status) { m_status = status; }
    void setCardType(const QString& cardType) { m_cardType = cardType; }
    void setContract(const QString& contract) { m_contract = contract; }
    void setValidation(std::optional<Validation> validation) { m_validation = validation; }
    void setNbTicketsLeft(std::optional<int> nb) { m_nbTicketsLeft = nb; }
    void setEventDateTime(std::optional<QDateTime> dt) { m_eventDateTime = dt; }
    void setPassValidityEndDate(std::optional<QDateTime> dt) { m_passValidityEndDate = dt; }
    void setErrorMessage(std::optional<QString> msg) { m_errorMessage = msg; }

private:
    Status m_status = Status::LOADING;
    QString m_cardType;
    QString m_contract;
    std::optional<Validation> m_validation;
    std::optional<int> m_nbTicketsLeft;
    std::optional<QDateTime> m_eventDateTime;
    std::optional<QDateTime> m_passValidityEndDate;
    std::optional<QString> m_errorMessage;
};

} // namespace domain::model

Q_DECLARE_METATYPE(domain::model::CardReaderResponse)
