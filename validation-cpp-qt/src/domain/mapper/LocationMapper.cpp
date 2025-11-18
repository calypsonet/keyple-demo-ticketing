/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#include "LocationMapper.h"
#include <algorithm>

namespace domain::mapper {

model::Location LocationMapper::map(
    const std::vector<model::Location>& locations,
    int locationId)
{
    auto it = std::find_if(locations.begin(), locations.end(),
        [locationId](const model::Location& loc) {
            return loc.id() == locationId;
        });

    if (it != locations.end()) {
        return *it;
    }

    // Return default/unknown location
    return model::Location(0, "Unknown");
}

} // namespace domain::mapper
