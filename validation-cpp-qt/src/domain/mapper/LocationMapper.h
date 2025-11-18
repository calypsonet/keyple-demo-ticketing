/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include "domain/model/Location.h"
#include <vector>

namespace domain::mapper {

/**
 * @brief Resolves location ID from event to Location object
 *
 * Équivalent de LocationMapper.kt
 */
class LocationMapper
{
public:
    /**
     * @brief Find location by ID from event
     *
     * @param locations Available locations list
     * @param locationId Location ID from event
     * @return Location object if found
     */
    static model::Location map(
        const std::vector<model::Location>& locations,
        int locationId
    );

private:
    LocationMapper() = delete;  // Static class
};

} // namespace domain::mapper
