/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include "domain/model/Status.h"
#include <stdexcept>
#include <string>

namespace domain::exception {

/**
 * @brief Custom exception for validation business rule violations
 *
 * Équivalent de ValidationException.kt
 */
class ValidationException : public std::runtime_error
{
public:
    /**
     * @brief Constructor
     *
     * @param message Error description
     * @param status Associated Status enum value
     */
    ValidationException(const std::string& message, model::Status status)
        : std::runtime_error(message)
        , m_status(status)
    {}

    /**
     * @brief Get associated status
     */
    model::Status status() const { return m_status; }

private:
    model::Status m_status;
};

} // namespace domain::exception
