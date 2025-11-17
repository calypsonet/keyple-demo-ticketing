package org.calypsonet.keyple.demo.validation.adapter.secondary.reader

import javax.inject.Inject
import org.calypsonet.keyple.card.storagecard.StorageCardExtensionService
import org.calypsonet.keyple.demo.common.constant.CardConstant
import org.calypsonet.keyple.demo.validation.adapter.secondary.repository.CardRepositoryFacade
import org.calypsonet.keyple.demo.validation.data.ReaderRepository
import org.calypsonet.keyple.demo.validation.data.model.CardProtocolEnum
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.ReaderApiFactory
import org.eclipse.keypop.reader.selection.CardSelectionManager
import org.eclipse.keypop.reader.selection.ScheduledCardSelectionsResponse
import org.eclipse.keypop.reader.selection.spi.SmartCard
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.storagecard.card.ProductType
import org.eclipse.keypop.storagecard.card.StorageCard
import timber.log.Timber

/**
 * Service managing card selection scenarios and observation.
 * This handles the complex card selection logic for multiple card types.
 */
class CardSelectionService
@Inject
constructor(
    private val readerRepository: ReaderRepository,
    private val cardRepositoryFacade: CardRepositoryFacade,
    private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi
) {

    private val readerApiFactory: ReaderApiFactory =
        SmartCardServiceProvider.getService().readerApiFactory
    private val calypsoExtensionService: CalypsoExtensionService =
        CalypsoExtensionService.getInstance()
    private val storageCardExtension = StorageCardExtensionService.getInstance()

    private lateinit var cardSelectionManager: CardSelectionManager
    private var smartCard: SmartCard? = null

    private var indexOfKeypleGenericCardSelection = 0
    private var indexOfCdLightGtmlCardSelection = 0
    private var indexOfCalypsoLightCardSelection = 0
    private var indexOfNavigoIdfCardSelection = 0
    private var indexOfMifareCardSelection = 0
    private var indexOfST25CardSelection = 0

    /**
     * Prepare and schedule the card selection scenario on the reader.
     * This configures selection for all supported card types.
     */
    fun prepareAndScheduleCardSelectionScenario(notificationMode: ObservableCardReader.NotificationMode) {
        val cardReader = readerRepository.getCardReader()
            ?: throw IllegalStateException("Card reader not initialized")

        // Create card selection manager
        cardSelectionManager = readerApiFactory.createCardSelectionManager()

        // Schedule Calypso card selections (various AIDs)
        indexOfKeypleGenericCardSelection = scheduleCalypsoCardSelection(
            CardConstant.AID_KEYPLE_GENERIC, "Keyple Generic"
        )
        indexOfCdLightGtmlCardSelection = scheduleCalypsoCardSelection(
            CardConstant.AID_CD_LIGHT_GTML, "CD Light/GTML"
        )
        indexOfCalypsoLightCardSelection = scheduleCalypsoCardSelection(
            CardConstant.AID_CALYPSO_LIGHT, "Calypso Light"
        )
        indexOfNavigoIdfCardSelection = scheduleCalypsoCardSelection(
            CardConstant.AID_NORMALIZED_IDF, "Navigo IDF"
        )

        // Schedule Storage Card selections (if supported)
        if (readerRepository.isStorageCardSupported()) {
            indexOfMifareCardSelection = scheduleStorageCardSelection(
                ProductType.MIFARE_ULTRALIGHT, "Mifare Ultralight"
            )
            indexOfST25CardSelection = scheduleStorageCardSelection(
                ProductType.ST25_SRT512, "ST25 SRT512"
            )
        }

        // Schedule selection scenario on the reader
        cardSelectionManager.scheduleCardSelectionScenario(
            cardReader as ObservableCardReader,
            notificationMode
        )

        Timber.d("Card selection scenario scheduled")
    }

    private fun scheduleCalypsoCardSelection(aid: ByteArray, label: String): Int {
        // Create ISO card selector with AID filter
        val cardSelector = readerApiFactory
            .createIsoCardSelector()
            .filterByDfName(aid)
            .filterByCardProtocol(CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name)

        // Create Calypso card selection extension
        val calypsoCardSelectionExtension = calypsoExtensionService.calypsoCardApiFactory
            .createCalypsoCardSelectionExtension()

        val index = cardSelectionManager.prepareSelection(cardSelector, calypsoCardSelectionExtension)
        Timber.d("Scheduled Calypso selection #$index: $label")
        return index
    }

    private fun scheduleStorageCardSelection(productType: ProductType, label: String): Int {
        val protocol = when (productType) {
            ProductType.MIFARE_ULTRALIGHT -> CardProtocolEnum.MIFARE_ULTRALIGHT_LOGICAL_PROTOCOL.name
            ProductType.ST25_SRT512 -> CardProtocolEnum.ST25_SRT512_LOGICAL_PROTOCOL.name
            else -> throw IllegalArgumentException("Unsupported storage card type: $productType")
        }

        // Create basic card selector with protocol filter
        val cardSelector = readerApiFactory
            .createBasicCardSelector()
            .filterByCardProtocol(protocol)

        // Create storage card selection extension with product type
        val storageCardSelectionExtension = storageCardExtension
            .createStorageCardSelectionExtension(productType)

        val index = cardSelectionManager.prepareSelection(cardSelector, storageCardSelectionExtension)
        Timber.d("Scheduled Storage Card selection #$index: $label ($productType)")
        return index
    }

    /**
     * Analyze the scheduled card selection response to determine card type.
     * @return The selected SmartCard or null if no card matched
     */
    fun analyzeScheduledCardSelection(scheduledResponse: ScheduledCardSelectionsResponse): SmartCard? {
        val selectionResult = cardSelectionManager.parseScheduledCardSelectionsResponse(scheduledResponse)

        smartCard = when {
            selectionResult.activeSelectionIndex == indexOfKeypleGenericCardSelection ||
            selectionResult.activeSelectionIndex == indexOfCdLightGtmlCardSelection ||
            selectionResult.activeSelectionIndex == indexOfCalypsoLightCardSelection ||
            selectionResult.activeSelectionIndex == indexOfNavigoIdfCardSelection -> {
                val calypsoCard = selectionResult.activeSmartCard as? CalypsoCard
                Timber.i("Calypso card detected: ${calypsoCard?.dfName?.let { org.eclipse.keyple.core.util.HexUtil.toHex(it) }}")
                calypsoCard
            }
            selectionResult.activeSelectionIndex == indexOfMifareCardSelection -> {
                val storageCard = selectionResult.activeSmartCard as? StorageCard
                Timber.i("Mifare Ultralight card detected")
                storageCard
            }
            selectionResult.activeSelectionIndex == indexOfST25CardSelection -> {
                val storageCard = selectionResult.activeSmartCard as? StorageCard
                Timber.i("ST25 SRT512 card detected")
                storageCard
            }
            else -> {
                Timber.w("Unknown card type or no card matched selection")
                null
            }
        }

        // Update the facade with the current card
        smartCard?.let { cardRepositoryFacade.setCurrentCard(it) }

        return smartCard
    }

    /**
     * Get the current selected smart card.
     */
    fun getCurrentCard(): SmartCard? = smartCard

    /**
     * Set up card reader observation.
     */
    fun setupCardObservation(observer: CardReaderObserverSpi) {
        val cardReader = readerRepository.getCardReader() as? ObservableCardReader
            ?: throw IllegalStateException("Card reader is not observable")

        cardReader.setReaderObservationExceptionHandler(readerObservationExceptionHandler)
        cardReader.addObserver(observer)
    }

    /**
     * Start card detection.
     */
    fun startCardDetection() {
        val cardReader = readerRepository.getCardReader() as? ObservableCardReader
            ?: throw IllegalStateException("Card reader is not observable")

        cardReader.startCardDetection(ObservableCardReader.DetectionMode.REPEATING)
        Timber.d("Card detection started")
    }

    /**
     * Stop card detection.
     */
    fun stopCardDetection() {
        val cardReader = readerRepository.getCardReader() as? ObservableCardReader
            ?: return

        try {
            cardReader.stopCardDetection()
            Timber.d("Card detection stopped")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping card detection")
        }
    }
}
