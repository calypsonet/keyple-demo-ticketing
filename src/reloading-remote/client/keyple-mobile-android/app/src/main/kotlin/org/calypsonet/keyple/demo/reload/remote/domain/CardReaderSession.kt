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
package org.calypsonet.keyple.demo.reload.remote.domain

import org.calypsonet.keyple.demo.common.constants.CalypsoAids
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMedium
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardProtocol
import org.calypsonet.keyple.demo.reload.remote.domain.model.TerminalType
import org.calypsonet.keyple.demo.reload.remote.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.reload.remote.domain.spi.Logger
import org.calypsonet.keyple.demo.reload.remote.domain.spi.TerminalReaders
import org.calypsonet.keyple.demo.reload.remote.domain.spi.UiContext
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.selection.spi.SmartCard
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import org.eclipse.keypop.storagecard.StorageCardApiFactory
import org.eclipse.keypop.storagecard.card.ProductType.MIFARE_CLASSIC_1K
import org.eclipse.keypop.storagecard.card.ProductType.MIFARE_ULTRALIGHT
import org.eclipse.keypop.storagecard.card.ProductType.ST25_SRT512

/**
 * Session of the card reader of the selected card medium: initialization of the reader, detection
 * and selection of the presented cards, on which the remote ticketing services are executed.
 */
class CardReaderSession(
    private val keypopApiProvider: KeypopApiProvider,
    private val terminalReaders: TerminalReaders,
    private val logger: Logger
) {

  private val storageCardApiFactory: StorageCardApiFactory? =
      keypopApiProvider.getStorageCardApiFactory()

  init {
    if (storageCardApiFactory == null) {
      logger.w("Storage card extension not available: storage cards are not supported")
    }
  }

  /** Indicates whether the reader has been initialized by [init]. */
  var isInitialized = false
    private set

  /** The name of the reader of the card medium. */
  lateinit var readerName: String
    private set

  /** The plugin type reported to the server, depending on the terminal and the card medium. */
  lateinit var pluginType: String
    private set

  private var aids: List<ByteArray> = emptyList()

  /**
   * Initializes the reader of the given card medium.
   *
   * @param terminalType The type of terminal.
   * @param cardMedium The card medium (contactless card, SIM...) to read.
   * @param uiContext Platform-specific context used to register the plugins.
   * @param observer Optional observer of the card reader events (contactless cards only).
   * @param readerObservationExceptionHandler Optional handler of the reader observation errors.
   * @param callback Optional callback invoked once the plugin is registered (asynchronous
   *   registration of the OMAPI plugin).
   */
  fun init(
      terminalType: TerminalType,
      cardMedium: CardMedium,
      uiContext: UiContext,
      observer: CardReaderObserverSpi?,
      readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi?,
      callback: (() -> Unit)?
  ) {
    readerName = terminalReaders.getReaderName(terminalType, cardMedium)
    pluginType = getPluginType(terminalType, cardMedium)
    aids = getAids(cardMedium)

    terminalReaders.registerPlugin(terminalType, uiContext, cardMedium, callback)
    terminalReaders.initCardReader(observer, readerObservationExceptionHandler)

    isInitialized = true
  }

  /** Releases the reader, detaching the given [observer] from it. */
  fun release(observer: CardReaderObserverSpi?) {
    isInitialized = false
    terminalReaders.release(observer)
  }

  /** Starts the repeated detection of the cards. */
  fun startCardDetection() {
    (terminalReaders.getReader(readerName) as ObservableCardReader).startCardDetection(
        ObservableCardReader.DetectionMode.REPEATING)
  }

  /** Stops the detection of the cards, if the reader is initialized. */
  fun stopCardDetection() {
    if (!isInitialized) {
      return
    }
    (terminalReaders.getReader(readerName) as ObservableCardReader).stopCardDetection()
  }

  /** Ends the processing of the presented card, the reader waiting for its removal. */
  fun endCardProcessing() {
    try {
      logger.d("Ending the card processing")
      (terminalReaders.getCardReader() as ObservableCardReader).finalizeCardProcessing()
    } catch (e: Exception) {
      logger.w("Error while ending the card processing [cause=${e.message}]")
    }
  }

  /**
   * Selects the presented card among the supported AIDs and storage card types.
   *
   * @throws IllegalStateException If no supported card is selected.
   */
  fun selectCard(): SmartCard {
    val readerApiFactory = keypopApiProvider.getReaderApiFactory()
    val reader = terminalReaders.getReader(readerName)
    val cardSelectionManager = readerApiFactory.createCardSelectionManager()

    aids.forEach {
      // Generic selection: configures a CardSelector with all the desired attributes to perform
      // the selection and read additional information afterward
      val calypsoCardSelector =
          readerApiFactory
              .createIsoCardSelector()
              .filterByCardProtocol(CardProtocol.ISO_14443_4.name)
              .filterByDfName(it)
      cardSelectionManager.prepareSelection(
          calypsoCardSelector,
          keypopApiProvider.getCalypsoCardApiFactory().createCalypsoCardSelectionExtension())
    }

    if (storageCardApiFactory != null) {
      cardSelectionManager.prepareSelection(
          readerApiFactory
              .createBasicCardSelector()
              .filterByCardProtocol(CardProtocol.MIFARE_ULTRALIGHT.name),
          storageCardApiFactory.createStorageCardSelectionExtension(MIFARE_ULTRALIGHT))
      cardSelectionManager.prepareSelection(
          readerApiFactory
              .createBasicCardSelector()
              .filterByCardProtocol(CardProtocol.ST25_SRT512.name),
          storageCardApiFactory.createStorageCardSelectionExtension(ST25_SRT512))
      cardSelectionManager.prepareSelection(
          readerApiFactory
              .createBasicCardSelector()
              .filterByCardProtocol(CardProtocol.MIFARE_CLASSIC.name),
          storageCardApiFactory.createStorageCardSelectionExtension(MIFARE_CLASSIC_1K))
    }

    return cardSelectionManager.processCardSelectionScenario(reader).activeSmartCard
        ?: throw IllegalStateException("No matching card found")
  }

  /**
   * Returns the plugin type reported to the server, depending on the terminal and the card medium.
   */
  private fun getPluginType(terminalType: TerminalType, cardMedium: CardMedium): String =
      when (cardMedium) {
        CardMedium.CONTACTLESS_CARD ->
            if (terminalType == TerminalType.BLUEBIRD) "Bluebird" else "Android NFC"
        CardMedium.SIM -> "Android OMAPI"
      }

  /** Returns the AIDs of the Calypso applications to select, depending on the card medium. */
  private fun getAids(cardMedium: CardMedium): List<ByteArray> =
      when (cardMedium) {
        CardMedium.CONTACTLESS_CARD ->
            listOf(
                CalypsoAids.KEYPLE_GENERIC,
                CalypsoAids.CD_LIGHT_GTML,
                CalypsoAids.CALYPSO_LIGHT,
                CalypsoAids.NORMALIZED_IDF)
        CardMedium.SIM -> listOf(CalypsoAids.CD_LIGHT_GTML, CalypsoAids.NORMALIZED_IDF)
      }
}
