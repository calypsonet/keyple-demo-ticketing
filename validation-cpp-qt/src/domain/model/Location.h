/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include <QString>
#include <QMetaType>

namespace domain::model {

/**
 * @brief Represents a validation location/station
 *
 * Équivalent de Location.kt dans l'app Android
 */
class Location
{
public:
    /**
     * @brief Default constructor
     */
    Location() = default;

    /**
     * @brief Constructor with parameters
     *
     * @param id Numeric identifier
     * @param name Location name (e.g., "Paris", "Brussels")
     */
    Location(int id, const QString& name)
        : m_id(id)
        , m_name(name)
    {}

    // Getters
    int id() const { return m_id; }
    QString name() const { return m_name; }

    // Setters
    void setId(int id) { m_id = id; }
    void setName(const QString& name) { m_name = name; }

    // Comparison operators
    bool operator==(const Location& other) const {
        return m_id == other.m_id;
    }

    bool operator!=(const Location& other) const {
        return !(*this == other);
    }

private:
    int m_id = 0;
    QString m_name;
};

} // namespace domain::model

// Register for Qt meta type system (needed for signals/slots)
Q_DECLARE_METATYPE(domain::model::Location)
