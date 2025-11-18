/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include "domain/model/Location.h"
#include <vector>

namespace data::repository {

/**
 * @brief Provides location data
 *
 * Équivalent de LocationRepository.kt avec données hardcodées
 */
class LocationRepository
{
public:
    LocationRepository() = default;
    ~LocationRepository() = default;

    /**
     * @brief Get all available locations
     *
     * Returns hardcoded list of European cities
     *
     * @return Vector of Location objects
     */
    std::vector<domain::model::Location> getLocations() const;

private:
    /**
     * @brief Initialize default locations
     */
    std::vector<domain::model::Location> initializeLocations() const;
};

} // namespace data::repository
