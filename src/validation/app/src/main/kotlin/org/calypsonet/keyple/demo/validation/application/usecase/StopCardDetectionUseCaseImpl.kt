package org.calypsonet.keyple.demo.validation.application.usecase

import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.adapter.secondary.reader.CardSelectionService
import org.calypsonet.keyple.demo.validation.domain.port.input.StopCardDetectionUseCase
import timber.log.Timber

/**
 * Implementation of the stop card detection use case.
 * Stops listening for card detection events.
 */
class StopCardDetectionUseCaseImpl
@Inject
constructor(
    private val cardSelectionService: CardSelectionService
) : StopCardDetectionUseCase {

    override suspend fun stop() {
        try {
            cardSelectionService.stopCardDetection()
            Timber.d("Card detection stopped")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop card detection")
            // Don't throw here as this is often called during cleanup
        }
    }
}
