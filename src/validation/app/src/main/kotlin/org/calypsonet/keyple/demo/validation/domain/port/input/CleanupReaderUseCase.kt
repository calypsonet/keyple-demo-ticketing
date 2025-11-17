package org.calypsonet.keyple.demo.validation.domain.port.input

/**
 * Use case for cleaning up reader resources.
 */
interface CleanupReaderUseCase {
    /**
     * Clean up and release all reader resources.
     * Should be called when the application is being destroyed.
     */
    suspend fun cleanup()
}
