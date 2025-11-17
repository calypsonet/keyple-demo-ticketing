package org.calypsonet.keyple.demo.validation.adapter.secondary.reader

import android.app.Activity
import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.data.ReaderRepository
import org.calypsonet.keyple.demo.validation.data.model.ReaderType
import org.calypsonet.keyple.demo.validation.domain.port.output.ReaderManager
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi

/**
 * Adapter implementing ReaderManager port using the existing ReaderRepository.
 * This acts as a bridge to adapt the existing repository to the hexagonal architecture.
 */
class KeypleReaderManager
@Inject
constructor(
    private val readerRepository: ReaderRepository,
    private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi,
    private val activity: Activity
) : ReaderManager {

    override suspend fun registerPlugin(readerType: ReaderType) {
        readerRepository.registerPlugin(activity, readerType)
    }

    override suspend fun initializeCardReader(): CardReader? {
        return readerRepository.initCardReader()
    }

    override suspend fun initializeSamReaders(): List<CardReader> {
        return readerRepository.initSamReaders()
    }

    override fun getCardReader(): CardReader? {
        return readerRepository.getCardReader()
    }

    override fun getSamReader(): CardReader? {
        return readerRepository.getSamReader()
    }

    override fun isStorageCardSupported(): Boolean {
        return readerRepository.isStorageCardSupported()
    }

    override suspend fun setCardObserver(observer: CardReaderObserverSpi) {
        val cardReader = getCardReader() as? ObservableCardReader
        cardReader?.setReaderObservationExceptionHandler(readerObservationExceptionHandler)
    }

    override suspend fun scheduleCardSelectionScenario(
        notificationMode: ObservableCardReader.NotificationMode
    ) {
        // This will be handled by the TicketingService/UseCase
        // as it requires CardSelectionManager which is complex
    }

    override suspend fun startCardDetection() {
        val cardReader = getCardReader() as? ObservableCardReader
        cardReader?.startCardDetection(ObservableCardReader.DetectionMode.REPEATING)
    }

    override suspend fun stopCardDetection() {
        val cardReader = getCardReader() as? ObservableCardReader
        cardReader?.stopCardDetection()
    }

    override suspend fun cleanup() {
        readerRepository.clear()
    }
}
