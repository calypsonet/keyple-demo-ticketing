/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#include "SettingsDialog.h"
#include "ui_SettingsDialog.h"
#include "ui/reader/ReaderWidget.h"
#include "domain/model/AppSettings.h"
#include "core/logging/Logger.h"

namespace ui {

SettingsDialog::SettingsDialog(QWidget *parent)
    : QDialog(parent)
    , ui(new Ui::SettingsDialog)
{
    ui->setupUi(this);

    setWindowTitle("Settings");
    setModal(true);

    // Load locations into combo box
    loadLocations();

    // Connect signals
    connect(ui->locationComboBox, QOverload<int>::of(&QComboBox::currentIndexChanged),
            this, &SettingsDialog::onLocationChanged);
    connect(ui->batteryPoweredCheckBox, &QCheckBox::toggled,
            this, &SettingsDialog::onBatteryPoweredChanged);
    connect(ui->startButton, &QPushButton::clicked,
            this, &SettingsDialog::onStartClicked);

    core::Logger::info("Settings dialog opened");
}

SettingsDialog::~SettingsDialog()
{
    delete ui;
}

void SettingsDialog::loadLocations()
{
    m_locations = m_locationRepository.getLocations();

    ui->locationComboBox->clear();
    for (const auto& location : m_locations) {
        ui->locationComboBox->addItem(location.name(), location.id());
    }

    // Select Paris by default (id = 6)
    int parisIndex = ui->locationComboBox->findData(6);
    if (parisIndex != -1) {
        ui->locationComboBox->setCurrentIndex(parisIndex);
    }

    core::Logger::info("Loaded {} locations", m_locations.size());
}

void SettingsDialog::onLocationChanged(int index)
{
    if (index < 0 || index >= static_cast<int>(m_locations.size())) {
        return;
    }

    const auto& location = m_locations[index];
    core::Logger::info("Location changed to: {}", location.name().toStdString());
}

void SettingsDialog::onBatteryPoweredChanged(bool checked)
{
    core::Logger::info("Battery powered: {}", checked);
}

void SettingsDialog::onStartClicked()
{
    // Save settings
    saveSettings();

    // Create and show reader widget
    auto readerWidget = new ReaderWidget();
    readerWidget->show();

    // Close settings dialog
    accept();
}

void SettingsDialog::saveSettings()
{
    int currentIndex = ui->locationComboBox->currentIndex();
    if (currentIndex >= 0 && currentIndex < static_cast<int>(m_locations.size())) {
        auto& settings = domain::model::AppSettings::instance();
        settings.setLocation(m_locations[currentIndex]);
        settings.setBatteryPowered(ui->batteryPoweredCheckBox->isChecked());

        core::Logger::info("Settings saved - Location: {}, Battery: {}",
            settings.location().name().toStdString(),
            settings.batteryPowered());
    }
}

} // namespace ui
