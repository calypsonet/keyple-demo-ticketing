/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include "Location.h"
#include <QString>
#include <QDateTime>
#include <QMetaType>
#include <optional>

namespace domain::model {

/**
 * @brief Represents a successful validation event
 *
 * Équivalent de Validation.kt
 */
class Validation
{
public:
    Validation() = default;

    Validation(const QString& name,
               const Location& location,
               const QString& destination,
               const QDateTime& dateTime,
               std::optional<int> provider = std::nullopt)
        : m_name(name)
        , m_location(location)
        , m_destination(destination)
        , m_dateTime(dateTime)
        , m_provider(provider)
    {}

    // Getters
    QString name() const { return m_name; }
    Location location() const { return m_location; }
    QString destination() const { return m_destination; }
    QDateTime dateTime() const { return m_dateTime; }
    std::optional<int> provider() const { return m_provider; }

    // Setters
    void setName(const QString& name) { m_name = name; }
    void setLocation(const Location& location) { m_location = location; }
    void setDestination(const QString& destination) { m_destination = destination; }
    void setDateTime(const QDateTime& dateTime) { m_dateTime = dateTime; }
    void setProvider(std::optional<int> provider) { m_provider = provider; }

private:
    QString m_name;
    Location m_location;
    QString m_destination;
    QDateTime m_dateTime;
    std::optional<int> m_provider;
};

} // namespace domain::model

Q_DECLARE_METATYPE(domain::model::Validation)
