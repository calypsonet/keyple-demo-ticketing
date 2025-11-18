/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include <QDialog>
#include <memory>
#include "domain/model/Location.h"
#include "data/repository/LocationRepository.h"

namespace Ui {
class SettingsDialog;
}

namespace ui {

/**
 * @brief Settings dialog for location and power mode configuration
 *
 * Équivalent de SettingsActivity.kt
 */
class SettingsDialog : public QDialog
{
    Q_OBJECT

public:
    explicit SettingsDialog(QWidget *parent = nullptr);
    ~SettingsDialog();

private slots:
    /**
     * @brief Called when location combo box selection changes
     */
    void onLocationChanged(int index);

    /**
     * @brief Called when battery powered checkbox is toggled
     */
    void onBatteryPoweredChanged(bool checked);

    /**
     * @brief Called when Start button is clicked
     */
    void onStartClicked();

private:
    Ui::SettingsDialog *ui;
    data::repository::LocationRepository m_locationRepository;
    std::vector<domain::model::Location> m_locations;

    void loadLocations();
    void saveSettings();
};

} // namespace ui
