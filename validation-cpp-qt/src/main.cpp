/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the BSD 3-Clause License which is available at
 * https://opensource.org/licenses/BSD-3-Clause.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#include <QApplication>
#include <QStyleFactory>
#include <QTranslator>
#include <memory>

#include "core/logging/Logger.h"
#include "ui/splash/SplashScreen.h"

int main(int argc, char *argv[])
{
    // Create Qt Application
    QApplication app(argc, argv);

    // Application metadata
    app.setApplicationName("Keyple Validation");
    app.setApplicationVersion("1.0.0");
    app.setOrganizationName("Calypso Networks Association");
    app.setOrganizationDomain("calypsonet.org");

    // Initialize logging system
    core::Logger::init();
    core::Logger::info("Application starting...");
    core::Logger::info("Qt version: {}", qVersion());

    // Set application style (optional)
    #ifdef Q_OS_WIN
        app.setStyle(QStyleFactory::create("Fusion"));
    #endif

    // Load translations (optional)
    QTranslator translator;
    const QStringList uiLanguages = QLocale::system().uiLanguages();
    for (const QString &locale : uiLanguages) {
        const QString baseName = "validation_" + QLocale(locale).name();
        if (translator.load(":/i18n/" + baseName)) {
            app.installTranslator(&translator);
            break;
        }
    }

    // Show splash screen
    ui::SplashScreen splash;
    splash.show();

    // Process events to show splash immediately
    app.processEvents();

    core::Logger::info("Application initialized successfully");

    // Start event loop
    return app.exec();
}
