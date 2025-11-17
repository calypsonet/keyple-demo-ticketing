package org.calypsonet.keyple.demo.validation.application.usecase

import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.domain.port.input.CleanupReaderUseCase
import org.calypsonet.keyple.demo.validation.domain.port.output.ReaderManager
import timber.log.Timber

/**
 * Implementation of the cleanup reader use case.
 * Releases all reader resources.
 */
class CleanupReaderUseCaseImpl
@Inject
constructor(
    private val readerManager: ReaderManager
) : CleanupReaderUseCase {

    override suspend fun cleanup() {
        try {
            readerManager.cleanup()
            Timber.d("Reader resources cleaned up")
        } catch (e: Exception) {
            Timber.e(e, "Error during reader cleanup")
            // Don't throw as this is typically called during app shutdown
        }
    }
}
