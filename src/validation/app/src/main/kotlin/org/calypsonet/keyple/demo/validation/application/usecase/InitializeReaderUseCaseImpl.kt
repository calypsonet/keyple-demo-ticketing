package org.calypsonet.keyple.demo.validation.application.usecase

import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.data.model.ReaderType
import org.calypsonet.keyple.demo.validation.domain.model.Location
import org.calypsonet.keyple.demo.validation.domain.port.input.InitializeReaderUseCase
import org.calypsonet.keyple.demo.validation.domain.port.output.LocationProvider
import org.calypsonet.keyple.demo.validation.domain.port.output.ReaderManager
import timber.log.Timber

/**
 * Implementation of the initialize reader use case.
 * Orchestrates the initialization of card reader hardware and SAM readers.
 */
class InitializeReaderUseCaseImpl
@Inject
constructor(
    private val readerManager: ReaderManager,
    private val locationProvider: LocationProvider
) : InitializeReaderUseCase {

    override suspend fun initialize(readerType: ReaderType, locationId: Int, locationName: String) {
        try {
            // Register the plugin for the specified reader type
            readerManager.registerPlugin(readerType)

            // Initialize card reader
            val cardReader = readerManager.initializeCardReader()
                ?: throw IllegalStateException("Failed to initialize card reader")

            // Initialize SAM readers
            val samReaders = readerManager.initializeSamReaders()
            if (samReaders.isEmpty()) {
                throw IllegalStateException("No SAM readers found")
            }

            // Update location
            locationProvider.updateLocation(Location(locationId, locationName))

            Timber.d("Reader initialized successfully: $readerType")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize reader")
            throw IllegalStateException("Reader initialization failed: ${e.message}", e)
        }
    }
}
