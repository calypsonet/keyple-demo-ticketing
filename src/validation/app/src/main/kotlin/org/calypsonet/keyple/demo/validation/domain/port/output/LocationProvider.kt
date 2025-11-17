package org.calypsonet.keyple.demo.validation.domain.port.output

import org.calypsonet.keyple.demo.validation.domain.model.Location

/**
 * Port for providing terminal location information.
 * This abstraction allows the domain to access location data without
 * knowing how it's stored (preferences, database, etc.).
 */
interface LocationProvider {
    /**
     * Get the current terminal location.
     *
     * @return Location containing ID and name
     */
    suspend fun getCurrentLocation(): Location

    /**
     * Update the terminal location.
     *
     * @param location The new location to set
     */
    suspend fun updateLocation(location: Location)
}
