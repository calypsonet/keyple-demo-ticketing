/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the BSD 3-Clause License which is available at
 * https://opensource.org/licenses/BSD-3-Clause.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */
package org.calypsonet.keyple.demo.control.domain

import org.calypsonet.keyple.card.storagecard.StorageCardExtensionService
import org.calypsonet.keyple.demo.common.constants.CardConstants
import org.calypsonet.keyple.demo.control.domain.managers.CalypsoCardControlManager
import org.calypsonet.keyple.demo.control.domain.managers.StorageCardControlManager
import org.calypsonet.keyple.demo.control.di.scope.AppScoped
import org.calypsonet.keyple.demo.control.domain.model.CardProtocolEnum
import org.calypsonet.keyple.demo.control.domain.model.CardReaderResponse
import org.calypsonet.keyple.demo.control.domain.model.Location
import org.calypsonet.keyple.demo.control.domain.model.ReaderType
import org.calypsonet.keyple.demo.control.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.control.domain.spi.Logger
import org.calypsonet.keyple.demo.control.domain.spi.ReaderManager
import org.calypsonet.keyple.demo.control.domain.spi.UiContext
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory
import org.eclipse.keypop.calypso.card.WriteAccessLevel
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.transaction.AsymmetricCryptoSecuritySetting
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting
import org.eclipse.keypop.calypso.card.transaction.spi.AsymmetricCryptoCardTransactionManagerFactory
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.ReaderApiFactory
import org.eclipse.keypop.reader.selection.CardSelectionManager
import org.eclipse.keypop.reader.selection.CardSelectionResult
import org.eclipse.keypop.reader.selection.ScheduledCardSelectionsResponse
import org.eclipse.keypop.reader.selection.spi.SmartCard
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import org.eclipse.keypop.storagecard.card.ProductType
import org.eclipse.keypop.storagecard.card.StorageCard
import java.time.LocalDateTime
import javax.inject.Inject

@AppScoped
class TicketingService @Inject constructor(
    private var keypopApiProvider: KeypopApiProvider,
    private var readerManager: ReaderManager,
    private var logger: Logger
) {

  private val readerApiFactory: ReaderApiFactory =
      keypopApiProvider.getReaderApiFactory()

  //private val calypsoExtensionService: CalypsoExtensionService =
  //    CalypsoExtensionService.getInstance()

  private val calypsoCardApiFactory: CalypsoCardApiFactory =
      keypopApiProvider.getCalypsoCardApiFactory()

  private val asymmetricCryptoSecuritySettings: AsymmetricCryptoSecuritySetting =
      buildAsymmetricCryptoSecuritySetting()
  private var symmetricCryptoSecuritySetting: SymmetricCryptoSecuritySetting? = null

  private var legacySamApiFactory: LegacySamApiFactory = keypopApiProvider.getLegacySamApiFactory()

  /** Get the Storage card extension service */
  private val storageCardExtension = StorageCardExtensionService.getInstance()
  private lateinit var legacySam: LegacySam
  private lateinit var smartCard: SmartCard
  private lateinit var cardSelectionManager: CardSelectionManager

  var readersInitialized = false
    private set

  var isSamAvailable: Boolean = false
    private set

  private var indexOfKeypleGenericCardSelection = 0
  private var indexOfCdLightGtmlCardSelection = 0
  private var indexOfCalypsoLightCardSelection = 0
  private var indexOfNavigoIdfCardSelection = 0
  private var indexOfMifareCardSelection = 0
  private var indexOfST25CardSelection = 0
  private var indexOfMifareClassic1KCardSelection = 0

    /**
     * Initializes the ticketing environment and selects a SAM if available.
     *
     * Steps:
     * - Registers the appropriate reader plugin according to [readerType].
     * - Initializes the primary card reader and SAM reader(s).
     * - Attaches the optional [observer] to the card reader to receive detection events.
     * - Selects a SAM and prepares secured session capabilities.
     *
     * @param observer Optional reader observer to receive card detection notifications.
     * @param readerType The target reader type to initialize (e.g., NFC).
     * @param uiContext Platform-specific context used to register plugins.
     * @throws IllegalStateException if no SAM reader is available or SAM selection fails.
     */
  fun init(observer: CardReaderObserverSpi?, readerType: ReaderType, uiContext: UiContext) {
    // Register plugin
    try {
      readerManager.registerPlugin(readerType, uiContext)
    } catch (e: Exception) {
      logger.e("An error occurred while registering plugin ${e.message}")
      throw IllegalStateException(e.message)
    }
    // Init card reader
    val cardReader: CardReader?
    try {
      cardReader = readerManager.initCardReader()
    } catch (e: Exception) {
      logger.e("An error occurred while init card reader ${e.message}")
      throw IllegalStateException(e.message)
    }
    // Init SAM reader
    var samReaders: List<CardReader>? = null
    try {
      samReaders = readerManager.initSamReaders()
    } catch (e: Exception) {
      logger.e( "An error occurred while init sam reader ${e.message}")
    }
    if (samReaders.isNullOrEmpty()) {
      logger.w("No SAM reader available")
    }
    // Register a card event observer and init the ticketing session
    cardReader?.let { reader ->
      (reader as ObservableCardReader).addObserver(observer)
      // attempts to select a SAM if any, sets the isSamAvailable flag accordingly
      val samReader = readerManager.getSamReader()
      isSamAvailable = samReader != null && selectSam(samReader)
    }
    symmetricCryptoSecuritySetting =
        if (isSamAvailable) buildSymmetricCryptoSecuritySetting() else null
    readersInitialized = true
  }

  fun startNfcDetection() {
    // Provide the CardReader with the selection operation to be processed when a Card is inserted.
    prepareAndScheduleCardSelectionScenario()
    (readerManager.getCardReader() as ObservableCardReader).startCardDetection(
        ObservableCardReader.DetectionMode.REPEATING)
  }

  fun stopNfcDetection() {
    try {
      // notify the reader that se detection has been switched off
      (readerManager.getCardReader() as ObservableCardReader).stopCardDetection()
    } catch (e: Exception) {
      logger.e("An error occurred while stopping nfc detection ${e.message}")
    }
  }

  fun onDestroy(observer: CardReaderObserverSpi?) {
    readersInitialized = false
    readerManager.clear()
    if (observer != null && readerManager.getCardReader() != null) {
      (readerManager.getCardReader() as ObservableCardReader).removeObserver(observer)
    }
    val smartCardService = SmartCardServiceProvider.getService()
    smartCardService.plugins.forEach { smartCardService.unregisterPlugin(it.name) }
  }

  fun displayResultSuccess(): Boolean = readerManager.displayResultSuccess()

  fun displayResultFailed(): Boolean = readerManager.displayResultFailed()

  fun prepareAndScheduleCardSelectionScenario() {

    // Get the Keyple main service
    val smartCardService = SmartCardServiceProvider.getService()

    // Check the Calypso card extension
    smartCardService.checkCardExtension(calypsoExtensionService)

    // Get a new card selection manager
    cardSelectionManager = readerApiFactory.createCardSelectionManager()

    // Prepare card selection case #1: Keyple generic
    indexOfKeypleGenericCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CardConstants.AID_KEYPLE_GENERIC)
                .filterByCardProtocol(CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Prepare card selection case #2: CD LIGHT/GTML
    indexOfCdLightGtmlCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CardConstants.AID_CD_LIGHT_GTML)
                .filterByCardProtocol(CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Prepare card selection case #3: CALYPSO LIGHT
    indexOfCalypsoLightCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CardConstants.AID_CALYPSO_LIGHT)
                .filterByCardProtocol(CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Prepare card selection case #4: Navigo IDF
    indexOfNavigoIdfCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CardConstants.AID_NORMALIZED_IDF)
                .filterByCardProtocol(CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    if (readerManager.isStorageCardSupported()) {
      indexOfMifareCardSelection =
          cardSelectionManager.prepareSelection(
              readerApiFactory
                  .createBasicCardSelector()
                  .filterByCardProtocol(CardProtocolEnum.MIFARE_ULTRALIGHT_LOGICAL_PROTOCOL.name),
              storageCardExtension.storageCardApiFactory.createStorageCardSelectionExtension(
                  ProductType.MIFARE_ULTRALIGHT))
      indexOfST25CardSelection =
          cardSelectionManager.prepareSelection(
              readerApiFactory
                  .createBasicCardSelector()
                  .filterByCardProtocol(CardProtocolEnum.ST25_SRT512_LOGICAL_PROTOCOL.name),
              storageCardExtension.storageCardApiFactory.createStorageCardSelectionExtension(
                  ProductType.ST25_SRT512))
      indexOfMifareClassic1KCardSelection =
          cardSelectionManager.prepareSelection(
              readerApiFactory
                  .createBasicCardSelector()
                  .filterByCardProtocol(CardProtocolEnum.MIFARE_CLASSIC_LOGICAL_PROTOCOL.name),
              storageCardExtension.storageCardApiFactory.createStorageCardSelectionExtension(
                  ProductType.MIFARE_CLASSIC_1K))
    }

    // Schedule the execution of the prepared card selection scenario as soon as a card is presented
    cardSelectionManager.scheduleCardSelectionScenario(
        readerManager.getCardReader() as ObservableCardReader,
        ObservableCardReader.NotificationMode.ALWAYS)
  }

  fun analyseSelectionResult(
      scheduledCardSelectionsResponse: ScheduledCardSelectionsResponse
  ): String? {
    logger.i("selectionResponse = $scheduledCardSelectionsResponse")
    val cardSelectionResult: CardSelectionResult =
        cardSelectionManager.parseScheduledCardSelectionsResponse(scheduledCardSelectionsResponse)
    if (cardSelectionResult.activeSelectionIndex == -1) {
      return "Selection error: card not recognized."
    }
    smartCard = cardSelectionResult.activeSmartCard
    when (smartCard) {
      is CalypsoCard -> { // check is the DF name is the expected one (Req. TL-SEL-AIDMATCH.1)
        if ((cardSelectionResult.activeSelectionIndex == indexOfKeypleGenericCardSelection &&
            !CardConstants.aidMatch(
                CardConstants.AID_KEYPLE_GENERIC, (smartCard as CalypsoCard).dfName)) ||
            (cardSelectionResult.activeSelectionIndex == indexOfCdLightGtmlCardSelection &&
                !CardConstants.aidMatch(
                    CardConstants.AID_CD_LIGHT_GTML, (smartCard as CalypsoCard).dfName)) ||
            (cardSelectionResult.activeSelectionIndex == indexOfCalypsoLightCardSelection &&
                !CardConstants.aidMatch(
                    CardConstants.AID_CALYPSO_LIGHT, (smartCard as CalypsoCard).dfName)) ||
            (cardSelectionResult.activeSelectionIndex == indexOfNavigoIdfCardSelection &&
                !CardConstants.aidMatch(
                    CardConstants.AID_NORMALIZED_IDF, (smartCard as CalypsoCard).dfName))) {
          return "Unexpected DF name"
        }
        if ((smartCard as CalypsoCard).applicationSubtype !in
            CardConstants.ALLOWED_FILE_STRUCTURES) {
          return "Invalid card\nFile structure " +
              HexUtil.toHex((smartCard as CalypsoCard).applicationSubtype) +
              "h not supported"
        }
        logger.i("Card DF Name = ${HexUtil.toHex((smartCard as CalypsoCard).dfName)}")
      }
      is StorageCard -> {
        logger.i(
            "${(smartCard as StorageCard).productType.name} Card UID = ${HexUtil.toHex((smartCard as StorageCard).uid)}")
      }
    }
    return null
  }

  fun executeControlProcedure(locations: List<Location>): CardReaderResponse {
    return when (smartCard) {
      is CalypsoCard -> {
        CalypsoCardControlManager()
            .executeControlProcedure(
                cardReader = readerManager.getCardReader()!!,
                calypsoCard = smartCard as CalypsoCard,
                symmetricCryptoSecuritySetting = symmetricCryptoSecuritySetting,
                asymmetricCryptoSecuritySetting = asymmetricCryptoSecuritySettings,
                locations = locations,
                controlDateTime = LocalDateTime.now(),
                logger = logger)
      }
      is StorageCard -> {
        StorageCardControlManager()
            .executeControlProcedure(
                cardReader = readerManager.getCardReader()!!,
                storageCard = smartCard as StorageCard,
                locations = locations,
                controlDateTime = LocalDateTime.now(),
                logger = logger)
      }
      else -> {
        error("Unsupported card type")
      }
    }
  }

  private fun buildSymmetricCryptoSecuritySetting(): SymmetricCryptoSecuritySetting {
    return calypsoCardApiFactory
        .createSymmetricCryptoSecuritySetting(
            legacySamApiFactory
                .createSymmetricCryptoCardTransactionManagerFactory(
                    readerManager.getSamReader(), legacySam))
        .assignDefaultKif(
            WriteAccessLevel.PERSONALIZATION, CardConstants.DEFAULT_KIF_PERSONALIZATION)
        .assignDefaultKif(WriteAccessLevel.LOAD, CardConstants.DEFAULT_KIF_LOAD)
        .assignDefaultKif(WriteAccessLevel.DEBIT, CardConstants.DEFAULT_KIF_DEBIT)
        .enableMultipleSession()
  }

  private fun buildAsymmetricCryptoSecuritySetting(): AsymmetricCryptoSecuritySetting {
    val pkiExtensionService: PkiExtensionService = PkiExtensionService.getInstance()
    pkiExtensionService.setTestMode()
    val transactionManagerFactory: AsymmetricCryptoCardTransactionManagerFactory? =
        pkiExtensionService.createAsymmetricCryptoCardTransactionManagerFactory()
    val asymmetricCryptoSecuritySetting =
        calypsoCardApiFactory.createAsymmetricCryptoSecuritySetting(transactionManagerFactory)
    asymmetricCryptoSecuritySetting
        .addPcaCertificate(
            pkiExtensionService.createPcaCertificate(
                CardConstants.PCA_PUBLIC_KEY_REFERENCE, CardConstants.PCA_PUBLIC_KEY))
        .addCaCertificate(pkiExtensionService.createCaCertificate(CardConstants.CA_CERTIFICATE))
        .addCaCertificateParser(
            pkiExtensionService.createCaCertificateParser(CertificateType.CALYPSO_LEGACY))
        .addCardCertificateParser(
            pkiExtensionService.createCardCertificateParser(CertificateType.CALYPSO_LEGACY))
    return asymmetricCryptoSecuritySetting
  }

  private fun selectSam(samReader: CardReader): Boolean {

    // Create a SAM selection manager.
    val samSelectionManager: CardSelectionManager = readerApiFactory.createCardSelectionManager()

    // Create a SAM selection using the Calypso card extension.
    samSelectionManager.prepareSelection(
        readerApiFactory
            .createBasicCardSelector()
            .filterByPowerOnData(
                LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null)),
        LegacySamExtensionService.getInstance()
            .legacySamApiFactory
            .createLegacySamSelectionExtension())
    try {
      // SAM communication: run the selection scenario.
      val samSelectionResult = samSelectionManager.processCardSelectionScenario(samReader)

      // Get the Calypso SAM SmartCard resulting of the selection.
      legacySam = samSelectionResult.activeSmartCard!! as LegacySam
      return true
    } catch (e: Exception) {
      logger.e("An exception occurred while selecting the SAM.  ${e.message}")
    }
    return false
  }
}
