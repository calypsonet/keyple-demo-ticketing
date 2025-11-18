/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include <QWidget>
#include <QTimer>
#include <memory>

namespace Ui {
class SplashScreen;
}

namespace ui {

/**
 * @brief Splash screen shown at application startup
 *
 * Équivalent de MainActivity.kt (splash)
 * Affiche le logo pendant 2 secondes puis navigue vers Settings
 */
class SplashScreen : public QWidget
{
    Q_OBJECT

public:
    explicit SplashScreen(QWidget *parent = nullptr);
    ~SplashScreen();

private slots:
    /**
     * @brief Called when splash timeout expires
     *
     * Navigates to Settings dialog
     */
    void onTimeout();

private:
    Ui::SplashScreen *ui;
    QTimer *m_timer;

    static constexpr int SPLASH_DURATION_MS = 2000;
};

} // namespace ui
