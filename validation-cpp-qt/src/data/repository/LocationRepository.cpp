/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#include "LocationRepository.h"

namespace data::repository {

std::vector<domain::model::Location> LocationRepository::getLocations() const
{
    return initializeLocations();
}

std::vector<domain::model::Location> LocationRepository::initializeLocations() const
{
    // Same 12 European locations as Android app
    return {
        domain::model::Location(1, "Bruxelles"),
        domain::model::Location(2, "Konstanz"),
        domain::model::Location(3, "Lisboa"),
        domain::model::Location(4, "Milan"),
        domain::model::Location(5, "Munich"),
        domain::model::Location(6, "Paris"),
        domain::model::Location(7, "Riga"),
        domain::model::Location(8, "Roma"),
        domain::model::Location(9, "Strasbourg"),
        domain::model::Location(10, "Torino"),
        domain::model::Location(11, "Venice"),
        domain::model::Location(12, "Barcelona")
    };
}

} // namespace data::repository
