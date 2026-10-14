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

import org.calypsonet.keyple.demo.common.constants.CalypsoAids
import org.calypsonet.keyple.demo.common.constants.CalypsoFiles
import org.calypsonet.keyple.demo.common.constants.DefaultKifs
import org.calypsonet.keyple.demo.control.domain.model.CardProtocol
import org.calypsonet.keyple.demo.control.domain.model.SelectionError
import org.calypsonet.keyple.demo.control.domain.model.TerminalType
import org.calypsonet.keyple.demo.control.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.control.domain.spi.Logger
import org.calypsonet.keyple.demo.control.domain.spi.TerminalReaders
import org.calypsonet.keyple.demo.control.domain.spi.UiContext
import org.calypsonet.keyple.demo.control.domain.spi.UserFeedback
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory
import org.eclipse.keypop.calypso.card.WriteAccessLevel
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.transaction.AsymmetricCryptoSecuritySetting
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting
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
import org.eclipse.keypop.storagecard.StorageCardApiFactory
import org.eclipse.keypop.storagecard.card.ProductType
import org.eclipse.keypop.storagecard.card.StorageCard

/**
 * Session of the card reader of the terminal: initialization of the readers, of the SAM and of the
 * user feedback devices, detection and selection of the presented cards.
 *
 * The operations are blocking calls, made by the UI outside of the main thread (the choice of the
 * threads belongs to the UI), one operation at a time: no internal synchronization is provided.
 */
class CardReaderSession(
    private val keypopApiProvider: KeypopApiProvider,
    private val terminalReaders: TerminalReaders,
    private val userFeedback: UserFeedback,
    private val logger: Logger
) {

  /** Result of the selection of the presented card. */
  sealed interface CardSelection {

    /** The card is selected. */
    data class Selected(val card: SmartCard) : CardSelection

    /**
     * The card cannot be selected.
     *
     * @property fileStructure The file structure of the Calypso application, in hexadecimal, when
     *   it is not supported.
     */
    data class NotSelected(val error: SelectionError, val fileStructure: String? = null) :
        CardSelection
  }

  private val readerApiFactory: ReaderApiFactory = keypopApiProvider.getReaderApiFactory()

  private val calypsoCardApiFactory: CalypsoCardApiFactory =
      keypopApiProvider.getCalypsoCardApiFactory()

  private var legacySamApiFactory: LegacySamApiFactory = keypopApiProvider.getLegacySamApiFactory()

  private val storageCardApiFactory: StorageCardApiFactory? =
      keypopApiProvider.getStorageCardApiFactory()

  init {
    if (storageCardApiFactory == null) {
      logger.w("Storage card extension not available: storage cards are not supported")
    }
  }

  private lateinit var legacySam: LegacySam
  private lateinit var cardSelectionManager: CardSelectionManager

  /** Indicates whether the readers have been initialized by [init]. */
  var isInitialized = false
    private set

  /** Indicates whether a SAM is available for the secure sessions. */
  var isSamAvailable: Boolean = false
    private set

  /** Security setting of the Calypso secure sessions, null if no SAM is available. */
  var symmetricCryptoSecuritySetting: SymmetricCryptoSecuritySetting? = null
    private set

  /** Security setting of the Calypso PKI transactions. */
  val asymmetricCryptoSecuritySetting: AsymmetricCryptoSecuritySetting =
      keypopApiProvider.getAsymmetricCryptoSecuritySetting()

  /** The reader of the presented cards. */
  val cardReader: CardReader
    get() = checkNotNull(terminalReaders.getCardReader()) { "Card reader not initialized" }

  private var indexOfKeypleGenericCardSelection = 0
  private var indexOfCdLightGtmlCardSelection = 0
  private var indexOfCalypsoLightCardSelection = 0
  private var indexOfNavigoIdfCardSelection = 0
  private var indexOfMifareCardSelection = 0
  private var indexOfST25CardSelection = 0
  private var indexOfMifareClassic1KCardSelection = 0

  /**
   * Initializes the user feedback, the card reader and the SAM, if available.
   *
   * Steps:
   * - Registers the appropriate reader plugin according to [terminalType].
   * - Initializes the primary card reader and SAM reader(s).
   * - Attaches the optional [observer] to the card reader to receive detection events.
   * - Selects a SAM and prepares secured session capabilities.
   *
   * @param observer Optional reader observer to receive card detection notifications.
   * @param terminalType The type of terminal.
   * @param uiContext Platform-specific context used to register plugins.
   * @throws IllegalStateException if the plugin or the card reader cannot be initialized.
   */
  fun init(observer: CardReaderObserverSpi?, terminalType: TerminalType, uiContext: UiContext) {
    // Register plugin
    try {
      userFeedback.init(terminalType, uiContext)
      terminalReaders.registerPlugin(terminalType, uiContext)
    } catch (e: Exception) {
      logger.e("Error while registering the reader plugins", e)
      throw IllegalStateException(e.message)
    }
    // Init card reader
    val cardReader: CardReader?
    try {
      cardReader = terminalReaders.initCardReader()
    } catch (e: Exception) {
      logger.e("Error while initializing the card reader", e)
      throw IllegalStateException(e.message)
    }
    // Init SAM reader
    var samReaders: List<CardReader>? = null
    try {
      samReaders = terminalReaders.initSamReaders()
    } catch (e: Exception) {
      logger.e("Error while initializing the SAM readers", e)
    }
    if (samReaders.isNullOrEmpty()) {
      logger.w("No SAM reader available")
    }
    // Register a card event observer and init the ticketing session
    cardReader?.let { reader ->
      (reader as ObservableCardReader).addObserver(observer)
      // attempts to select a SAM if any, sets the isSamAvailable flag accordingly
      val samReader = terminalReaders.getSamReader()
      isSamAvailable = samReader != null && selectSam(samReader)
    }
    symmetricCryptoSecuritySetting =
        if (isSamAvailable) createSymmetricCryptoSecuritySetting() else null
    isInitialized = true
  }

  /** Starts the repeated detection of the cards, with the selection scenario of the cards. */
  fun startCardDetection() {
    // Provide the CardReader with the selection operation to be processed when a Card is inserted.
    prepareAndScheduleCardSelectionScenario()
    (terminalReaders.getCardReader() as ObservableCardReader).startCardDetection(
        ObservableCardReader.DetectionMode.REPEATING)
  }

  /** Stops the detection of the cards. */
  fun stopCardDetection() {
    try {
      // notify the reader that se detection has been switched off
      (terminalReaders.getCardReader() as ObservableCardReader).stopCardDetection()
    } catch (e: Exception) {
      logger.e("Error while stopping the card detection", e)
    }
  }

  /** Releases the readers and the user feedback, detaching the given [observer] from the reader. */
  fun release(observer: CardReaderObserverSpi?) {
    isInitialized = false
    terminalReaders.release(observer)
    userFeedback.release()
  }

  /** Ends the processing of the presented card, the reader waiting for its removal. */
  fun endCardProcessing() {
    try {
      logger.d("Ending the card processing")
      (terminalReaders.getCardReader() as ObservableCardReader).finalizeCardProcessing()
    } catch (e: Exception) {
      logger.w("Error while ending the card processing", e)
    }
  }

  private fun prepareAndScheduleCardSelectionScenario() {

    // Get a new card selection manager
    cardSelectionManager = readerApiFactory.createCardSelectionManager()

    // Prepare card selection case #1: Keyple generic
    indexOfKeypleGenericCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CalypsoAids.KEYPLE_GENERIC)
                .filterByCardProtocol(CardProtocol.ISO_14443_4.name),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Prepare card selection case #2: CD LIGHT/GTML
    indexOfCdLightGtmlCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CalypsoAids.CD_LIGHT_GTML)
                .filterByCardProtocol(CardProtocol.ISO_14443_4.name),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Prepare card selection case #3: CALYPSO LIGHT
    indexOfCalypsoLightCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CalypsoAids.CALYPSO_LIGHT)
                .filterByCardProtocol(CardProtocol.ISO_14443_4.name),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Prepare card selection case #4: Navigo IDF
    indexOfNavigoIdfCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CalypsoAids.NORMALIZED_IDF)
                .filterByCardProtocol(CardProtocol.ISO_14443_4.name),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    if (storageCardApiFactory != null && terminalReaders.isStorageCardSupported()) {
      indexOfMifareCardSelection =
          cardSelectionManager.prepareSelection(
              readerApiFactory
                  .createBasicCardSelector()
                  .filterByCardProtocol(CardProtocol.MIFARE_ULTRALIGHT.name),
              storageCardApiFactory.createStorageCardSelectionExtension(
                  ProductType.MIFARE_ULTRALIGHT))
      indexOfST25CardSelection =
          cardSelectionManager.prepareSelection(
              readerApiFactory
                  .createBasicCardSelector()
                  .filterByCardProtocol(CardProtocol.ST25_SRT512.name),
              storageCardApiFactory.createStorageCardSelectionExtension(ProductType.ST25_SRT512))
      indexOfMifareClassic1KCardSelection =
          cardSelectionManager.prepareSelection(
              readerApiFactory
                  .createBasicCardSelector()
                  .filterByCardProtocol(CardProtocol.MIFARE_CLASSIC.name),
              storageCardApiFactory.createStorageCardSelectionExtension(
                  ProductType.MIFARE_CLASSIC_1K))
    }

    // Schedule the execution of the prepared card selection scenario as soon as a card is presented
    cardSelectionManager.scheduleCardSelectionScenario(
        terminalReaders.getCardReader() as ObservableCardReader,
        ObservableCardReader.NotificationMode.ALWAYS)
  }

  /**
   * Analyzes the result of the selection scenario executed when a card has been presented, and
   * checks the selected Calypso application (DF name and file structure).
   */
  fun selectCard(scheduledCardSelectionsResponse: ScheduledCardSelectionsResponse): CardSelection {
    logger.d("Card selection response received [response=$scheduledCardSelectionsResponse]")
    val cardSelectionResult: CardSelectionResult =
        cardSelectionManager.parseScheduledCardSelectionsResponse(scheduledCardSelectionsResponse)
    if (cardSelectionResult.activeSelectionIndex == -1) {
      return CardSelection.NotSelected(SelectionError.CARD_NOT_RECOGNIZED)
    }
    val smartCard = cardSelectionResult.activeSmartCard
    when (smartCard) {
      is CalypsoCard -> { // check is the DF name is the expected one (Req. TL-SEL-AIDMATCH.1)
        if ((cardSelectionResult.activeSelectionIndex == indexOfKeypleGenericCardSelection &&
            !CalypsoAids.matches(CalypsoAids.KEYPLE_GENERIC, smartCard.dfName)) ||
            (cardSelectionResult.activeSelectionIndex == indexOfCdLightGtmlCardSelection &&
                !CalypsoAids.matches(CalypsoAids.CD_LIGHT_GTML, smartCard.dfName)) ||
            (cardSelectionResult.activeSelectionIndex == indexOfCalypsoLightCardSelection &&
                !CalypsoAids.matches(CalypsoAids.CALYPSO_LIGHT, smartCard.dfName)) ||
            (cardSelectionResult.activeSelectionIndex == indexOfNavigoIdfCardSelection &&
                !CalypsoAids.matches(CalypsoAids.NORMALIZED_IDF, smartCard.dfName))) {
          return CardSelection.NotSelected(SelectionError.UNEXPECTED_DF_NAME)
        }
        if (smartCard.applicationSubtype !in CalypsoFiles.ALLOWED_FILE_STRUCTURES) {
          return CardSelection.NotSelected(
              SelectionError.UNSUPPORTED_FILE_STRUCTURE,
              HexUtil.toHex(smartCard.applicationSubtype))
        }
        logger.i("Calypso card selected [dfName=${HexUtil.toHex(smartCard.dfName)}]")
      }
      is StorageCard -> {
        logger.i(
            "Storage card selected [productType=${smartCard.productType.name}, uid=${HexUtil.toHex(smartCard.uid)}]")
      }
    }
    return CardSelection.Selected(smartCard)
  }

  private fun createSymmetricCryptoSecuritySetting(): SymmetricCryptoSecuritySetting {
    return calypsoCardApiFactory
        .createSymmetricCryptoSecuritySetting(
            legacySamApiFactory.createSymmetricCryptoCardTransactionManagerFactory(
                terminalReaders.getSamReader(), legacySam))
        .assignDefaultKif(WriteAccessLevel.PERSONALIZATION, DefaultKifs.PERSONALIZATION)
        .assignDefaultKif(WriteAccessLevel.LOAD, DefaultKifs.LOAD)
        .assignDefaultKif(WriteAccessLevel.DEBIT, DefaultKifs.DEBIT)
        .enableMultipleSession()
  }

  private fun selectSam(samReader: CardReader): Boolean {

    // Create a SAM selection manager.
    val samSelectionManager: CardSelectionManager = readerApiFactory.createCardSelectionManager()

    // Create a SAM selection using the Calypso card extension.
    samSelectionManager.prepareSelection(
        readerApiFactory.createBasicCardSelector(),
        keypopApiProvider.getLegacySamApiFactory().createLegacySamSelectionExtension())
    try {
      // SAM communication: run the selection scenario.
      val samSelectionResult = samSelectionManager.processCardSelectionScenario(samReader)

      // Get the Calypso SAM SmartCard resulting of the selection.
      legacySam = samSelectionResult.activeSmartCard!! as LegacySam
      return true
    } catch (e: Exception) {
      logger.e("Error while selecting the SAM", e)
    }
    return false
  }
}
