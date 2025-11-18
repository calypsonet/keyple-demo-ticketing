/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include "domain/model/Validation.h"
#include "domain/model/Location.h"
#include <vector>

namespace domain::mapper {

/**
 * @brief Maps EventStructure to domain Validation model
 *
 * Équivalent de ValidationMapper.kt
 */
class ValidationMapper
{
public:
    /**
     * @brief Map event data to Validation object
     *
     * @param event Event structure from card
     * @param locations Available locations list
     * @return Validation object
     */
    static model::Validation map(
        // const EventStructure& event,
        const std::vector<model::Location>& locations
    );

private:
    ValidationMapper() = delete;  // Static class
};

} // namespace domain::mapper
