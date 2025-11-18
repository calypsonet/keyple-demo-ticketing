/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#include "Logger.h"
#include <spdlog/sinks/stdout_color_sinks.h>
#include <spdlog/sinks/rotating_file_sink.h>

namespace core {

std::shared_ptr<spdlog::logger> Logger::s_logger = nullptr;

void Logger::init()
{
    if (s_logger) {
        return; // Already initialized
    }

    try {
        // Create sinks
        auto console_sink = std::make_shared<spdlog::sinks::stdout_color_sink_mt>();
        console_sink->set_level(spdlog::level::debug);
        console_sink->set_pattern("[%Y-%m-%d %H:%M:%S.%e] [%^%l%$] [%s:%#] %v");

        // Rotating file sink (5MB max, 3 files)
        auto file_sink = std::make_shared<spdlog::sinks::rotating_file_sink_mt>(
            "validation.log", 1024 * 1024 * 5, 3
        );
        file_sink->set_level(spdlog::level::trace);
        file_sink->set_pattern("[%Y-%m-%d %H:%M:%S.%e] [%l] [%s:%#] %v");

        // Create logger with both sinks
        spdlog::sinks_init_list sink_list = {console_sink, file_sink};
        s_logger = std::make_shared<spdlog::logger>("validation", sink_list);

        // Set default level
        #ifdef NDEBUG
            s_logger->set_level(spdlog::level::info); // Release
        #else
            s_logger->set_level(spdlog::level::debug); // Debug
        #endif

        s_logger->flush_on(spdlog::level::warn);

        // Register as default logger
        spdlog::set_default_logger(s_logger);

        s_logger->info("Logger initialized successfully");
    }
    catch (const spdlog::spdlog_ex& ex) {
        std::cerr << "Logger initialization failed: " << ex.what() << std::endl;
    }
}

std::shared_ptr<spdlog::logger> Logger::getInstance()
{
    if (!s_logger) {
        init();
    }
    return s_logger;
}

void Logger::setLevel(spdlog::level::level_enum level)
{
    getInstance()->set_level(level);
}

} // namespace core
