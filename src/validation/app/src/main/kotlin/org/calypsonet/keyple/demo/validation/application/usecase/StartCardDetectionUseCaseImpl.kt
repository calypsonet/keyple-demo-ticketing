package org.calypsonet.keyple.demo.validation.application.usecase

import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.adapter.secondary.reader.CardSelectionService
import org.calypsonet.keyple.demo.validation.domain.port.input.StartCardDetectionUseCase
import org.eclipse.keypop.reader.ObservableCardReader
import timber.log.Timber

/**
 * Implementation of the start card detection use case.
 * Prepares card selection scenario and starts listening for card events.
 */
class StartCardDetectionUseCaseImpl
@Inject
constructor(
    private val cardSelectionService: CardSelectionService
) : StartCardDetectionUseCase {

    override suspend fun start() {
        try {
            // Prepare and schedule card selection scenario with ALWAYS mode
            // This will automatically execute the selection when a card is detected
            cardSelectionService.prepareAndScheduleCardSelectionScenario(
                ObservableCardReader.NotificationMode.ALWAYS
            )

            // Start card detection
            cardSelectionService.startCardDetection()

            Timber.d("Card detection started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start card detection")
            throw IllegalStateException("Failed to start card detection: ${e.message}", e)
        }
    }
}
