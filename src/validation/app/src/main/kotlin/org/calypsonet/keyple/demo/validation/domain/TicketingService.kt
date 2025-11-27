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

import java.time.LocalDateTime
import javax.inject.Inject
import org.calypsonet.keyple.demo.common.constants.CardConstants
import org.calypsonet.keyple.demo.common.data.LocationRepository
import org.calypsonet.keyple.demo.common.model.Location
import org.calypsonet.keyple.demo.validation.di.scope.AppScoped
import org.calypsonet.keyple.demo.validation.domain.managers.CalypsoCardValidationManager
import org.calypsonet.keyple.demo.validation.domain.managers.StorageCardValidationManager
import org.calypsonet.keyple.demo.validation.domain.model.CardProtocolEnum
import org.calypsonet.keyple.demo.validation.domain.model.ReaderType
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.validation.domain.spi.Logger
import org.calypsonet.keyple.demo.validation.domain.spi.ReaderManager
import org.calypsonet.keyple.demo.validation.domain.spi.UiContext
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
import org.eclipse.keypop.storagecard.card.ProductType.MIFARE_ULTRALIGHT
import org.eclipse.keypop.storagecard.card.ProductType.ST25_SRT512
import org.eclipse.keypop.storagecard.card.StorageCard

/**
 * Ticketing service orchestrating reader initialization, card selection and validation flows.
 *
 * Responsibilities:
 * - Register and initialize card and SAM readers using the provided [ReaderManager].
 * - Prepare and schedule selection scenarios for supported cards (Calypso and storage cards).
 * - Analyse selection results and run validation procedures for the detected card.
 * - Manage SAM selection and provide Calypso security settings for secured transactions.
 *
 * This class is UI-agnostic and relies on [UiContext] to adapt to platform specifics when needed.
 *
 * Thread-safety: instances are designed to be used on the UI thread / main scope coordinating
 * reader events; no internal synchronization is provided.
 */
@AppScoped
class TicketingService
@Inject
constructor(
    private var keypopApiProvider: KeypopApiProvider,
    private var readerManager: ReaderManager,
    private var logger: Logger
) {

  /** Indicates whether readers have been successfully initialized via [init]. */
  var areReadersInitialized = false
    private set

  private val readerApiFactory: ReaderApiFactory = keypopApiProvider.getReaderApiFactory()
  private val calypsoCardApiFactory: CalypsoCardApiFactory =
      keypopApiProvider.getCalypsoCardApiFactory()
  private val storageCardApiFactory = keypopApiProvider.getStorageCardApiFactory()

  private lateinit var calypsoSam: LegacySam
  private lateinit var smartCard: SmartCard
  private lateinit var cardSelectionManager: CardSelectionManager

  private var indexOfKeypleGenericCardSelection = 0
  private var indexOfCdLightGtmlCardSelection = 0
  private var indexOfCalypsoLightCardSelection = 0
  private var indexOfNavigoIdfCardSelection = 0
  private var indexOfMifareCardSelection = 0
  private var indexOfST25CardSelection = 0

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
    readerManager.registerPlugin(readerType, uiContext)

    // Init card reader
    val cardReader: CardReader? = readerManager.initCardReader()

    // Init SAM reader
    val samReaders = readerManager.initSamReaders()
    check(samReaders.isNotEmpty()) { "No SAM reader available" }

    // Register a card event observer and init the ticketing session
    cardReader?.let { reader ->
      (reader as ObservableCardReader).addObserver(observer)
      // attempts to select a SAM, if any, sets the isSecureSessionMode flag accordingly
      val samReader = readerManager.getSamReader()
      if (samReader == null || !selectSam(samReader)) {
        throw IllegalStateException("SAM reader or SAM not available")
      }
    }
    areReadersInitialized = true
  }

  /** Starts repeating NFC card detection after preparing the selection scenario. */
  fun startNfcDetection() {
    // Provide the CardReader with the selection operation to be processed when a Card is inserted.
    prepareAndScheduleCardSelectionScenario()
    (readerManager.getCardReader() as ObservableCardReader).startCardDetection(
        ObservableCardReader.DetectionMode.REPEATING)
  }

  /** Stops NFC card detection if the reader supports it. Swallows exceptions silently. */
  fun stopNfcDetection() {
    try {
      // notify reader that se detection has been switched off
      (readerManager.getCardReader() as ObservableCardReader).stopCardDetection()
    } catch (e: Exception) {
      // NOP
    }
  }

  /** Releases resources and detaches the given [observer] from the reader, if any. */
  fun onDestroy(observer: CardReaderObserverSpi?) {
    areReadersInitialized = false
    readerManager.onDestroy(observer)
  }

  /**
   * Asks the UI layer to display a success feedback (sound, haptics, message...).
   *
   * @return true if handled by the UI, false otherwise.
   */
  fun displayResultSuccess(): Boolean = readerManager.displayResultSuccess()

  /**
   * Asks the UI layer to display a failure feedback (sound, haptics, message...).
   *
   * @return true if handled by the UI, false otherwise.
   */
  fun displayResultFailed(): Boolean = readerManager.displayResultFailed()

  /** Returns the list of available locations used during validation. */
  fun getLocations(): List<Location> = LocationRepository.getLocations()

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
              storageCardApiFactory.createStorageCardSelectionExtension(MIFARE_ULTRALIGHT))
      indexOfST25CardSelection =
          cardSelectionManager.prepareSelection(
              readerApiFactory
                  .createBasicCardSelector()
                  .filterByCardProtocol(CardProtocolEnum.ST25_SRT512_LOGICAL_PROTOCOL.name),
              storageCardApiFactory.createStorageCardSelectionExtension(ST25_SRT512))
    }

    // Schedule the execution of the prepared card selection scenario as soon as a card is presented
    cardSelectionManager.scheduleCardSelectionScenario(
        readerManager.getCardReader() as ObservableCardReader,
        ObservableCardReader.NotificationMode.ALWAYS)
  }

  /**
   * Analyses the provided scheduled selection response, binds the active [SmartCard], and performs
   * sanity checks (DF name and file structure for Calypso).
   *
   * @return null if the selection is valid and a compatible card is active; otherwise a localized
   *   human-readable error message describing the selection issue.
   */
  fun analyseSelectionResult(
      scheduledCardSelectionsResponse: ScheduledCardSelectionsResponse
  ): String? {
    val cardSelectionResult: CardSelectionResult =
        cardSelectionManager.parseScheduledCardSelectionsResponse(scheduledCardSelectionsResponse)
    if (cardSelectionResult.activeSelectionIndex == -1) {
      return "Selection error: AID not found"
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

  /**
   * Executes the appropriate validation procedure based on the active [smartCard] type.
   *
   * @return The validation result produced by the corresponding manager.
   * @throws IllegalStateException if the active card type is unsupported.
   */
  fun executeValidationProcedure(): ValidationResult {
    return when (smartCard) {
      is CalypsoCard -> {
        CalypsoCardValidationManager()
            .executeValidationProcedure(
                validationDateTime = LocalDateTime.now(),
                validationAmount = 1,
                cardReader = readerManager.getCardReader()!!,
                calypsoCard = smartCard as CalypsoCard,
                cardSecuritySettings = getSecuritySettings()!!,
                locations = LocationRepository.getLocations(),
                keypopApiProvider = keypopApiProvider)
      }
      is StorageCard -> {
        StorageCardValidationManager()
            .executeValidationProcedure(
                validationDateTime = LocalDateTime.now(),
                validationAmount = 1,
                cardReader = readerManager.getCardReader()!!,
                storageCard = smartCard as StorageCard,
                locations = LocationRepository.getLocations(),
                keypopApiProvider = keypopApiProvider)
      }
      else -> {
        error("Unsupported card type")
      }
    }
  }

  private fun getSecuritySettings(): SymmetricCryptoSecuritySetting? {
    return calypsoCardApiFactory
        .createSymmetricCryptoSecuritySetting(
            keypopApiProvider
                .getLegacySamApiFactory()
                .createSymmetricCryptoCardTransactionManagerFactory(
                    readerManager.getSamReader(), calypsoSam))
        .assignDefaultKif(
            WriteAccessLevel.PERSONALIZATION, CardConstants.DEFAULT_KIF_PERSONALIZATION)
        .assignDefaultKif(WriteAccessLevel.LOAD, CardConstants.DEFAULT_KIF_LOAD)
        .assignDefaultKif(WriteAccessLevel.DEBIT, CardConstants.DEFAULT_KIF_DEBIT)
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
      logger.e("An exception occurred while selecting the SAM. ${e.message}", e)
    }
    return false
  }
}
