/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#include "SplashScreen.h"
#include "ui_SplashScreen.h"
#include "ui/settings/SettingsDialog.h"
#include "core/logging/Logger.h"

namespace ui {

SplashScreen::SplashScreen(QWidget *parent)
    : QWidget(parent)
    , ui(new Ui::SplashScreen)
    , m_timer(new QTimer(this))
{
    ui->setupUi(this);

    // Configure window
    setWindowTitle("Keyple Validation");
    setFixedSize(600, 400);

    // Center on screen
    setGeometry(
        QApplication::desktop()->screen()->rect().center().x() - width() / 2,
        QApplication::desktop()->screen()->rect().center().y() - height() / 2,
        width(),
        height()
    );

    // Setup timer for auto-navigation
    m_timer->setSingleShot(true);
    connect(m_timer, &QTimer::timeout, this, &SplashScreen::onTimeout);
    m_timer->start(SPLASH_DURATION_MS);

    core::Logger::info("Splash screen displayed");
}

SplashScreen::~SplashScreen()
{
    delete ui;
}

void SplashScreen::onTimeout()
{
    core::Logger::info("Splash timeout, navigating to settings...");

    // Create and show settings dialog
    auto settingsDialog = new SettingsDialog();
    settingsDialog->show();

    // Close splash screen
    close();
}

} // namespace ui
