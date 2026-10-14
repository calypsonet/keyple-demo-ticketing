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
package org.calypsonet.keyple.demo.validation.domain

import org.calypsonet.keyple.demo.common.constants.CalypsoAids
import org.calypsonet.keyple.demo.common.constants.CalypsoFiles
import org.calypsonet.keyple.demo.common.constants.DefaultKifs
import org.calypsonet.keyple.demo.validation.domain.model.CardProtocol
import org.calypsonet.keyple.demo.validation.domain.model.SelectionError
import org.calypsonet.keyple.demo.validation.domain.model.TerminalType
import org.calypsonet.keyple.demo.validation.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.validation.domain.spi.Logger
import org.calypsonet.keyple.demo.validation.domain.spi.TerminalReaders
import org.calypsonet.keyple.demo.validation.domain.spi.UiContext
import org.calypsonet.keyple.demo.validation.domain.spi.UserFeedback
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory
import org.eclipse.keypop.calypso.card.WriteAccessLevel
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting
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
import org.eclipse.keypop.storagecard.card.ProductType.MIFARE_CLASSIC_1K
import org.eclipse.keypop.storagecard.card.ProductType.MIFARE_ULTRALIGHT
import org.eclipse.keypop.storagecard.card.ProductType.ST25_SRT512
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

  /** Indicates whether the readers have been initialized by [init]. */
  var isInitialized = false
    private set

  private val readerApiFactory: ReaderApiFactory = keypopApiProvider.getReaderApiFactory()
  private val calypsoCardApiFactory: CalypsoCardApiFactory =
      keypopApiProvider.getCalypsoCardApiFactory()
  private val storageCardApiFactory: StorageCardApiFactory? =
      keypopApiProvider.getStorageCardApiFactory()

  init {
    if (storageCardApiFactory == null) {
      logger.w("Storage card extension not available: storage cards are not supported")
    }
  }

  private lateinit var calypsoSam: LegacySam
  private lateinit var cardSelectionManager: CardSelectionManager
  private lateinit var cardSecuritySettings: SymmetricCryptoSecuritySetting

  /** The last card selected. */
  private var selectedCard: SmartCard? = null

  private var indexOfKeypleGenericCardSelection = 0
  private var indexOfCdLightGtmlCardSelection = 0
  private var indexOfCalypsoLightCardSelection = 0
  private var indexOfNavigoIdfCardSelection = 0
  private var indexOfMifareCardSelection = 0
  private var indexOfST25CardSelection = 0
  private var indexOfMifareClassic1KCardSelection = 0

  /** The reader of the presented cards. */
  val cardReader: CardReader
    get() = checkNotNull(terminalReaders.getCardReader()) { "Card reader not initialized" }

  /** Security setting of the Calypso secure sessions, null if no SAM is available. */
  val cardSecuritySetting: SymmetricCryptoSecuritySetting?
    get() = if (::cardSecuritySettings.isInitialized) cardSecuritySettings else null

  /**
   * Initializes the user feedback, the card reader and the SAM.
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
   * @throws IllegalStateException if no SAM reader is available or SAM selection fails.
   */
  fun init(observer: CardReaderObserverSpi?, terminalType: TerminalType, uiContext: UiContext) {
    // Init user feedback and register plugin
    userFeedback.init(terminalType, uiContext)
    terminalReaders.registerPlugin(terminalType, uiContext)

    // Init card reader
    val cardReader: CardReader? = terminalReaders.initCardReader()

    // Init SAM reader
    val samReaders = terminalReaders.initSamReaders()
    check(samReaders.isNotEmpty()) { "No SAM reader available" }

    // Register a card event observer and init the ticketing session
    cardReader?.let { reader ->
      (reader as ObservableCardReader).addObserver(observer)
      // attempts to select a SAM, if any, sets the isSecureSessionMode flag accordingly
      val samReader = terminalReaders.getSamReader()
      if (samReader == null || !selectSam(samReader)) {
        throw IllegalStateException("SAM not available")
      }
      cardSecuritySettings = createSecuritySettings()
      cardSecuritySettings.initCryptoContextForNextTransaction()
    }
    isInitialized = true
  }

  /**
   * Starts the repeated detection of the cards, with the selection scenario of the cards, the user
   * feedback signaling that the terminal waits for a card.
   */
  fun startCardDetection() {
    // Provide the CardReader with the selection operation to be processed when a Card is inserted.
    prepareAndScheduleCardSelectionScenario()
    (terminalReaders.getCardReader() as ObservableCardReader).startCardDetection(
        ObservableCardReader.DetectionMode.REPEATING)
    userFeedback.displayWaiting()
  }

  /** Stops the detection of the cards. */
  fun stopCardDetection() {
    try {
      // notify reader that se detection has been switched off
      (terminalReaders.getCardReader() as ObservableCardReader).stopCardDetection()
    } catch (e: Exception) {
      logger.w("Error while stopping the card detection [cause=${e.message}]")
    }
  }

  /** Releases the readers and the user feedback, detaching the given [observer] from the reader. */
  fun release(observer: CardReaderObserverSpi?) {
    isInitialized = false
    terminalReaders.release(observer)
    userFeedback.release()
  }

  /**
   * Ends the processing of the presented card, the reader waiting for its removal, and the user
   * feedback signaling that the terminal waits for a card.
   */
  fun endCardProcessing() {
    try {
      logger.d("Ending the card processing")
      (terminalReaders.getCardReader() as ObservableCardReader).finalizeCardProcessing()
    } catch (e: Exception) {
      logger.w("Error while ending the card processing [cause=${e.message}]")
    }
    userFeedback.displayWaiting()
  }

  /**
   * Prepares the cryptographic context of the next Calypso transaction, after the end of the
   * previous one, so that the time spent is not added to the user time.
   */
  fun prepareNextTransaction() {
    if (selectedCard is CalypsoCard) {
      cardSecuritySettings.initCryptoContextForNextTransaction()
    }
  }

  /**
   * Prepares the card selection scenario for supported AIDs and product types, then schedules it to
   * run automatically upon card presentation.
   */
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
              storageCardApiFactory.createStorageCardSelectionExtension(MIFARE_ULTRALIGHT))
      indexOfST25CardSelection =
          cardSelectionManager.prepareSelection(
              readerApiFactory
                  .createBasicCardSelector()
                  .filterByCardProtocol(CardProtocol.ST25_SRT512.name),
              storageCardApiFactory.createStorageCardSelectionExtension(ST25_SRT512))
      indexOfMifareClassic1KCardSelection =
          cardSelectionManager.prepareSelection(
              readerApiFactory
                  .createBasicCardSelector()
                  .filterByCardProtocol(CardProtocol.MIFARE_CLASSIC.name),
              storageCardApiFactory.createStorageCardSelectionExtension(MIFARE_CLASSIC_1K))
    }

    // Schedule the execution of the prepared card selection scenario as soon as a card is presented
    cardSelectionManager.scheduleCardSelectionScenario(
        terminalReaders.getCardReader() as ObservableCardReader,
        ObservableCardReader.NotificationMode.MATCHED_ONLY)
  }

  /**
   * Analyzes the result of the selection scenario executed when a card has been presented, and
   * checks the selected Calypso application (DF name and file structure).
   */
  fun selectCard(scheduledCardSelectionsResponse: ScheduledCardSelectionsResponse): CardSelection {
    val cardSelectionResult: CardSelectionResult =
        cardSelectionManager.parseScheduledCardSelectionsResponse(scheduledCardSelectionsResponse)
    if (cardSelectionResult.activeSelectionIndex == -1) {
      selectedCard = null
      return CardSelection.NotSelected(SelectionError.CARD_NOT_RECOGNIZED)
    }
    val smartCard = cardSelectionResult.activeSmartCard
    selectedCard = smartCard
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

  private fun createSecuritySettings(): SymmetricCryptoSecuritySetting {
    return calypsoCardApiFactory
        .createSymmetricCryptoSecuritySetting(
            keypopApiProvider
                .getLegacySamApiFactory()
                .createSymmetricCryptoCardTransactionManagerFactory(
                    terminalReaders.getSamReader(), calypsoSam))
        .assignDefaultKif(WriteAccessLevel.PERSONALIZATION, DefaultKifs.PERSONALIZATION)
        .assignDefaultKif(WriteAccessLevel.LOAD, DefaultKifs.LOAD)
        .assignDefaultKif(WriteAccessLevel.DEBIT, DefaultKifs.DEBIT)
        .enableRatificationMechanism()
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
      calypsoSam = samSelectionResult.activeSmartCard!! as LegacySam
      return true
    } catch (e: Exception) {
      logger.e("Error while selecting the SAM", e)
    }
    return false
  }
}
