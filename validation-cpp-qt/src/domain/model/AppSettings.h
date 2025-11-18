/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include "Location.h"
#include "ReaderType.h"

namespace domain::model {

/**
 * @brief Global application settings singleton
 *
 * Équivalent de AppSettings.kt (object singleton)
 */
class AppSettings
{
public:
    /**
     * @brief Get singleton instance
     */
    static AppSettings& instance() {
        static AppSettings instance;
        return instance;
    }

    // Delete copy/move constructors
    AppSettings(const AppSettings&) = delete;
    AppSettings& operator=(const AppSettings&) = delete;
    AppSettings(AppSettings&&) = delete;
    AppSettings& operator=(AppSettings&&) = delete;

    // Getters
    ReaderType readerType() const { return m_readerType; }
    Location location() const { return m_location; }
    bool batteryPowered() const { return m_batteryPowered; }

    // Setters
    void setReaderType(ReaderType type) { m_readerType = type; }
    void setLocation(const Location& location) { m_location = location; }
    void setBatteryPowered(bool powered) { m_batteryPowered = powered; }

private:
    AppSettings() = default;
    ~AppSettings() = default;

    ReaderType m_readerType = ReaderType::PCSC;  // Desktop uses PC/SC
    Location m_location;
    bool m_batteryPowered = false;
};

} // namespace domain::model
