package org.calypsonet.keyple.demo.validation.domain.port.output

import org.calypsonet.keyple.demo.validation.data.model.ReaderType
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi

/**
 * Port for managing card reader hardware.
 * This abstraction allows the domain to interact with various reader types without
 * knowing their specific implementation details.
 */
interface ReaderManager {
    /**
     * Register the plugin for the specified reader type.
     *
     * @param readerType The type of reader hardware
     */
    suspend fun registerPlugin(readerType: ReaderType)

    /**
     * Initialize and configure the card reader.
     *
     * @return The initialized card reader
     */
    suspend fun initializeCardReader(): CardReader?

    /**
     * Initialize and configure SAM (Secure Access Module) readers.
     *
     * @return List of initialized SAM readers
     */
    suspend fun initializeSamReaders(): List<CardReader>

    /**
     * Get the current card reader instance.
     *
     * @return The card reader or null if not initialized
     */
    fun getCardReader(): CardReader?

    /**
     * Get the SAM reader instance.
     *
     * @return The SAM reader or null if not initialized
     */
    fun getSamReader(): CardReader?

    /**
     * Check if storage cards (Mifare Ultralight, ST25) are supported by this reader.
     *
     * @return true if storage cards are supported
     */
    fun isStorageCardSupported(): Boolean

    /**
     * Set the observer for card detection events.
     *
     * @param observer The observer to receive card events
     */
    suspend fun setCardObserver(observer: CardReaderObserverSpi)

    /**
     * Schedule a card selection scenario on the observable reader.
     *
     * @param notificationMode The notification mode for card detection
     */
    suspend fun scheduleCardSelectionScenario(notificationMode: ObservableCardReader.NotificationMode)

    /**
     * Start observing card events on the reader.
     */
    suspend fun startCardDetection()

    /**
     * Stop observing card events on the reader.
     */
    suspend fun stopCardDetection()

    /**
     * Clean up and release all reader resources.
     */
    suspend fun cleanup()
}
