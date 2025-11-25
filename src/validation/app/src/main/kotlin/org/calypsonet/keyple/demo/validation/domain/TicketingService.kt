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

import android.app.Activity
import java.time.LocalDateTime
import javax.inject.Inject
import org.calypsonet.keyple.demo.common.constant.CardConstant
import org.calypsonet.keyple.demo.common.data.LocationRepository
import org.calypsonet.keyple.demo.common.model.Location
import org.calypsonet.keyple.demo.validation.di.scope.AppScoped
import org.calypsonet.keyple.demo.validation.domain.model.CardProtocolEnum
import org.calypsonet.keyple.demo.validation.domain.model.ReaderType
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.validation.domain.spi.ReaderRepository
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
import timber.log.Timber

@AppScoped
class TicketingService
@Inject
constructor(
    private var keypopApiProvider: KeypopApiProvider,
    private var readerRepository: ReaderRepository
) {

  private val readerApiFactory: ReaderApiFactory = keypopApiProvider.getReaderApiFactory()
  private val calypsoCardApiFactory: CalypsoCardApiFactory =
      keypopApiProvider.getCalypsoCardApiFactory()
  private val storageCardApiFactory = keypopApiProvider.getStorageCardApiFactory()

  private lateinit var calypsoSam: LegacySam
  private lateinit var smartCard: SmartCard
  private lateinit var cardSelectionManager: CardSelectionManager
  var areReadersInitialized = false
    private set

  private var indexOfKeypleGenericCardSelection = 0
  private var indexOfCdLightGtmlCardSelection = 0
  private var indexOfCalypsoLightCardSelection = 0
  private var indexOfNavigoIdfCardSelection = 0
  private var indexOfMifareCardSelection = 0
  private var indexOfST25CardSelection = 0

  fun init(observer: CardReaderObserverSpi?, activity: Activity, readerType: ReaderType) {
    // Register plugin
    readerRepository.registerPlugin(activity, readerType)

    // Init card reader
    val cardReader: CardReader? = readerRepository.initCardReader()

    // Init SAM reader
    val samReaders = readerRepository.initSamReaders()
    check(samReaders.isNotEmpty()) { "No SAM reader available" }

    // Register a card event observer and init the ticketing session
    cardReader?.let { reader ->
      (reader as ObservableCardReader).addObserver(observer)
      // attempts to select a SAM, if any, sets the isSecureSessionMode flag accordingly
      val samReader = readerRepository.getSamReader()
      if (samReader == null || !selectSam(samReader)) {
        throw IllegalStateException("SAM reader or SAM not available")
      }
    }
    areReadersInitialized = true
  }

  fun startNfcDetection() {
    // Provide the CardReader with the selection operation to be processed when a Card is inserted.
    prepareAndScheduleCardSelectionScenario()
    (readerRepository.getCardReader() as ObservableCardReader).startCardDetection(
        ObservableCardReader.DetectionMode.REPEATING)
  }

  fun stopNfcDetection() {
    try {
      // notify reader that se detection has been switched off
      (readerRepository.getCardReader() as ObservableCardReader).stopCardDetection()
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  fun onDestroy(observer: CardReaderObserverSpi?) {
    areReadersInitialized = false
    readerRepository.onDestroy(observer)
  }

  fun displayResultSuccess(): Boolean = readerRepository.displayResultSuccess()

  fun displayResultFailed(): Boolean = readerRepository.displayResultFailed()

  fun getLocations(): List<Location> = LocationRepository.getLocations()

  private fun prepareAndScheduleCardSelectionScenario() {
    // Get a new card selection manager
    cardSelectionManager = readerApiFactory.createCardSelectionManager()

    // Prepare card selection case #1: Keyple generic
    indexOfKeypleGenericCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CardConstant.AID_KEYPLE_GENERIC)
                .filterByCardProtocol(CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Prepare card selection case #2: CD LIGHT/GTML
    indexOfCdLightGtmlCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CardConstant.AID_CD_LIGHT_GTML)
                .filterByCardProtocol(CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Prepare card selection case #3: CALYPSO LIGHT
    indexOfCalypsoLightCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CardConstant.AID_CALYPSO_LIGHT)
                .filterByCardProtocol(CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    // Prepare card selection case #4: Navigo IDF
    indexOfNavigoIdfCardSelection =
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createIsoCardSelector()
                .filterByDfName(CardConstant.AID_NORMALIZED_IDF)
                .filterByCardProtocol(CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name),
            calypsoCardApiFactory.createCalypsoCardSelectionExtension())

    if (readerRepository.isStorageCardSupported()) {
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
        readerRepository.getCardReader() as ObservableCardReader,
        ObservableCardReader.NotificationMode.ALWAYS)
  }

  fun analyseSelectionResult(
      scheduledCardSelectionsResponse: ScheduledCardSelectionsResponse
  ): String? {
    Timber.i("selectionResponse = $scheduledCardSelectionsResponse")
    val cardSelectionResult: CardSelectionResult =
        cardSelectionManager.parseScheduledCardSelectionsResponse(scheduledCardSelectionsResponse)
    if (cardSelectionResult.activeSelectionIndex == -1) {
      return "Selection error: AID not found"
    }
    smartCard = cardSelectionResult.activeSmartCard
    when (smartCard) {
      is CalypsoCard -> { // check is the DF name is the expected one (Req. TL-SEL-AIDMATCH.1)
        if ((cardSelectionResult.activeSelectionIndex == indexOfKeypleGenericCardSelection &&
            !CardConstant.aidMatch(
                CardConstant.AID_KEYPLE_GENERIC, (smartCard as CalypsoCard).dfName)) ||
            (cardSelectionResult.activeSelectionIndex == indexOfCdLightGtmlCardSelection &&
                !CardConstant.aidMatch(
                    CardConstant.AID_CD_LIGHT_GTML, (smartCard as CalypsoCard).dfName)) ||
            (cardSelectionResult.activeSelectionIndex == indexOfCalypsoLightCardSelection &&
                !CardConstant.aidMatch(
                    CardConstant.AID_CALYPSO_LIGHT, (smartCard as CalypsoCard).dfName)) ||
            (cardSelectionResult.activeSelectionIndex == indexOfNavigoIdfCardSelection &&
                !CardConstant.aidMatch(
                    CardConstant.AID_NORMALIZED_IDF, (smartCard as CalypsoCard).dfName))) {
          return "Unexpected DF name"
        }
        if ((smartCard as CalypsoCard).applicationSubtype !in
            CardConstant.ALLOWED_FILE_STRUCTURES) {
          return "Invalid card\nFile structure " +
              HexUtil.toHex((smartCard as CalypsoCard).applicationSubtype) +
              "h not supported"
        }
        Timber.i("Card DF Name = %s", HexUtil.toHex((smartCard as CalypsoCard).dfName))
      }
      is StorageCard -> {
        Timber.i(
            "%s Card UID = %s",
            (smartCard as StorageCard).productType.name,
            HexUtil.toHex((smartCard as StorageCard).uid))
      }
    }
    return null
  }

  fun executeValidationProcedure(): ValidationResult {
    return when (smartCard) {
      is CalypsoCard -> {
        CalypsoCardValidationManager()
            .executeValidationProcedure(
                validationDateTime = LocalDateTime.now(),
                validationAmount = 1,
                cardReader = readerRepository.getCardReader()!!,
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
                cardReader = readerRepository.getCardReader()!!,
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
                    readerRepository.getSamReader(), calypsoSam))
        .assignDefaultKif(
            WriteAccessLevel.PERSONALIZATION, CardConstant.DEFAULT_KIF_PERSONALIZATION)
        .assignDefaultKif(WriteAccessLevel.LOAD, CardConstant.DEFAULT_KIF_LOAD)
        .assignDefaultKif(WriteAccessLevel.DEBIT, CardConstant.DEFAULT_KIF_DEBIT)
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
      Timber.e(e)
      Timber.e("An exception occurred while selecting the SAM.  ${e.message}")
    }
    return false
  }
}
