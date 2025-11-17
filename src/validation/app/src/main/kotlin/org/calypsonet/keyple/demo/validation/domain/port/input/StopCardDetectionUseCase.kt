package org.calypsonet.keyple.demo.validation.domain.port.input

/**
 * Use case for stopping NFC card detection.
 */
interface StopCardDetectionUseCase {
    /**
     * Stop listening for card detection events.
     */
    suspend fun stop()
}
