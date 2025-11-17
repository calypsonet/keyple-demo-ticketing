package org.calypsonet.keyple.demo.validation.domain.port.input

import org.calypsonet.keyple.demo.validation.data.model.ReaderType

/**
 * Use case for initializing the card reader hardware and registering plugins.
 */
interface InitializeReaderUseCase {
    /**
     * Initialize the card reader with the specified type and location.
     *
     * @param readerType The type of card reader hardware to initialize
     * @param locationId The location identifier for this terminal
     * @param locationName The human-readable location name
     */
    suspend fun initialize(readerType: ReaderType, locationId: Int, locationName: String)
}
