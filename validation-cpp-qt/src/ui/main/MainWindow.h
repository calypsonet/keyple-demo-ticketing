/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include <QMainWindow>

namespace Ui {
class MainWindow;
}

namespace ui {

/**
 * @brief Main application window (optional)
 *
 * Not used in current flow (Splash -> Settings -> Reader)
 * Kept for potential future menu/navigation needs
 */
class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = nullptr);
    ~MainWindow();

private:
    Ui::MainWindow *ui;
};

} // namespace ui
