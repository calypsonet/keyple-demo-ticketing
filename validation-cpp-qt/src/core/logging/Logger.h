/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include <spdlog/spdlog.h>
#include <spdlog/sinks/stdout_color_sinks.h>
#include <spdlog/sinks/rotating_file_sink.h>
#include <memory>
#include <string>

namespace core {

/**
 * @brief Wrapper around spdlog for centralized logging
 *
 * Équivalent de Timber sur Android
 */
class Logger
{
public:
    /**
     * @brief Initialize logging system
     *
     * Doit être appelé au démarrage de l'application (dans main)
     */
    static void init();

    /**
     * @brief Log debug message
     */
    template<typename... Args>
    static void debug(const std::string& fmt, Args&&... args) {
        getInstance()->debug(fmt, std::forward<Args>(args)...);
    }

    /**
     * @brief Log info message
     */
    template<typename... Args>
    static void info(const std::string& fmt, Args&&... args) {
        getInstance()->info(fmt, std::forward<Args>(args)...);
    }

    /**
     * @brief Log warning message
     */
    template<typename... Args>
    static void warn(const std::string& fmt, Args&&... args) {
        getInstance()->warn(fmt, std::forward<Args>(args)...);
    }

    /**
     * @brief Log error message
     */
    template<typename... Args>
    static void error(const std::string& fmt, Args&&... args) {
        getInstance()->error(fmt, std::forward<Args>(args)...);
    }

    /**
     * @brief Log critical error message
     */
    template<typename... Args>
    static void critical(const std::string& fmt, Args&&... args) {
        getInstance()->critical(fmt, std::forward<Args>(args)...);
    }

    /**
     * @brief Set log level
     */
    static void setLevel(spdlog::level::level_enum level);

private:
    static std::shared_ptr<spdlog::logger> getInstance();
    static std::shared_ptr<spdlog::logger> s_logger;
};

} // namespace core
