package org.calypsonet.keyple.demo.validation.domain.port.input

/**
 * Use case for starting NFC card detection.
 */
interface StartCardDetectionUseCase {
    /**
     * Start listening for card detection events.
     * Prepares the card selection scenario and schedules it for automatic execution.
     */
    suspend fun start()
}
