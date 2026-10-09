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

import java.time.LocalDate
import org.calypsonet.keyple.demo.common.constants.CardConstants
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsInputDto
import org.calypsonet.keyple.demo.common.dto.CardIssuanceInputDto
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus
import org.calypsonet.keyple.demo.common.dto.WriteContractInputDto
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.reload.remote.domain.mappers.toCardTitle
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardInfo
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardOperationResult
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardProtocol
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceType
import org.calypsonet.keyple.demo.reload.remote.domain.model.ReadContractsResult
import org.calypsonet.keyple.demo.reload.remote.domain.model.ReaderType
import org.calypsonet.keyple.demo.reload.remote.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.reload.remote.domain.spi.Logger
import org.calypsonet.keyple.demo.reload.remote.domain.spi.ReaderManager
import org.calypsonet.keyple.demo.reload.remote.domain.spi.RemoteServiceManager
import org.calypsonet.keyple.demo.reload.remote.domain.spi.UiContext
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.selection.spi.SmartCard
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import org.eclipse.keypop.storagecard.StorageCardApiFactory
import org.eclipse.keypop.storagecard.card.ProductType.MIFARE_CLASSIC_1K
import org.eclipse.keypop.storagecard.card.ProductType.MIFARE_ULTRALIGHT
import org.eclipse.keypop.storagecard.card.ProductType.ST25_SRT512
import org.eclipse.keypop.storagecard.card.StorageCard

/**
 * Entry point of the UI: manages the reader of the selected device, and executes the remote
 * ticketing services (contracts reading, reload, personalization) on the presented card.
 */
class TicketingService(
    private var keypopApiProvider: KeypopApiProvider,
    private var readerManager: ReaderManager,
    private var logger: Logger,
    private var remoteServiceManager: RemoteServiceManager
) {

  private val storageCardApiFactory: StorageCardApiFactory? =
      keypopApiProvider.getStorageCardApiFactory()

  init {
    if (storageCardApiFactory == null) {
      logger.w("Storage card extension not available: storage cards are not supported")
    }
  }

  /** Indicates whether readers have been successfully initialized via [init]. */
  var areReadersInitialized = false
    private set

  private lateinit var readerName: String
  private lateinit var pluginType: String
  private var aids: List<ByteArray> = emptyList()

  /**
   * Initializes the reader of the given device.
   *
   * @param readerType The type of terminal.
   * @param deviceType The type of device (contactless card, SIM...) to read.
   * @param uiContext Platform-specific context used to register the plugins.
   * @param observer Optional observer of the card reader events (contactless cards only).
   * @param readerObservationExceptionHandler Optional handler of the reader observation errors.
   * @param callback Optional callback invoked once the plugin is registered (asynchronous
   *   registration of the OMAPI plugin).
   */
  fun init(
      readerType: ReaderType,
      deviceType: DeviceType,
      uiContext: UiContext,
      observer: CardReaderObserverSpi?,
      readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi?,
      callback: (() -> Unit)?
  ) {
    readerName = readerManager.getReaderName(readerType, deviceType)
    pluginType = getPluginType(readerType, deviceType)
    aids = getAids(deviceType)

    readerManager.registerPlugin(readerType, uiContext, deviceType, callback)
    readerManager.initCardReader(observer, readerObservationExceptionHandler)

    areReadersInitialized = true
  }

  fun onDestroy(observer: CardReaderObserverSpi?) {
    areReadersInitialized = false
    readerManager.onDestroy(observer)
  }

  fun startNfcDetection() {
    (readerManager.getReader(readerName) as ObservableCardReader).startCardDetection(
        ObservableCardReader.DetectionMode.REPEATING)
  }

  fun stopNfcDetection() {
    if (!areReadersInitialized) {
      return
    }
    (readerManager.getReader(readerName) as ObservableCardReader).stopCardDetection()
  }

  fun endCardProcessing() {
    try {
      logger.i("endCardProcessing")
      (readerManager.getCardReader() as ObservableCardReader).finalizeCardProcessing()
    } catch (e: Exception) {
      logger.i("cannot end card processing: $e")
    }
  }

  /**
   * Selects the presented card and asks the server to read and analyze its contracts.
   *
   * @throws IllegalStateException If no supported card is selected.
   */
  fun readCardContracts(): ReadContractsResult {
    val smartCard = selectCard()
    val output =
        remoteServiceManager.analyzeContracts(
            readerName, smartCard, AnalyzeContractsInputDto(pluginType))
    val status = RemoteServiceStatus.fromCode(output.statusCode)
    val today = LocalDate.now()
    val titles =
        if (status == RemoteServiceStatus.SUCCESS)
            output.validContracts.map { it.toCardTitle(today) }
        else emptyList()
    return ReadContractsResult(smartCard.toCardInfo(), status, titles)
  }

  /**
   * Selects the presented card and asks the server to load the given contract.
   *
   * @param expectedSerialNumber Serial number of the card for which the contract has been bought.
   * @param contractTariff The contract to load.
   * @param ticketsToLoad The number of trips to load (multi-trip contract).
   * @return The result of the operation, with the status [RemoteServiceStatus.DIFFERENT_CARD] if
   *   the presented card is not the one for which the contract has been bought.
   * @throws IllegalStateException If no supported card is selected.
   */
  fun reloadCard(
      expectedSerialNumber: String?,
      contractTariff: PriorityCode,
      ticketsToLoad: Int
  ): CardOperationResult {
    val smartCard = selectCard()
    val card = smartCard.toCardInfo()
    if (card.serialNumber != expectedSerialNumber) {
      // The contract has been bought for the card read at the first step: the reload must be done
      // on the same card.
      return CardOperationResult(card, RemoteServiceStatus.DIFFERENT_CARD)
    }
    remoteServiceManager.analyzeContracts(
        readerName, smartCard, AnalyzeContractsInputDto(pluginType))
    val output =
        remoteServiceManager.writeContract(
            readerName, smartCard, WriteContractInputDto(contractTariff, ticketsToLoad, pluginType))
    return CardOperationResult(card, RemoteServiceStatus.fromCode(output.statusCode))
  }

  /**
   * Selects the presented card and asks the server to personalize it.
   *
   * @throws IllegalStateException If no supported card is selected.
   */
  fun personalizeCard(): CardOperationResult {
    val smartCard = selectCard()
    val output =
        remoteServiceManager.personalizeCard(
            readerName, smartCard, CardIssuanceInputDto(pluginType))
    return CardOperationResult(
        smartCard.toCardInfo(), RemoteServiceStatus.fromCode(output.statusCode))
  }

  /** Selects the presented card among the supported AIDs and storage card types. */
  private fun selectCard(): SmartCard {
    val readerApiFactory = keypopApiProvider.getReaderApiFactory()
    val reader = readerManager.getReader(readerName)
    val cardSelectionManager = readerApiFactory.createCardSelectionManager()

    aids.forEach {
      // Generic selection: configures a CardSelector with all the desired attributes to perform
      // the selection and read additional information afterward
      val calypsoCardSelector =
          readerApiFactory
              .createIsoCardSelector()
              .filterByCardProtocol(CardProtocol.ISO_14443_4_LOGICAL_PROTOCOL.name)
              .filterByDfName(it)
      cardSelectionManager.prepareSelection(
          calypsoCardSelector,
          keypopApiProvider.getCalypsoCardApiFactory().createCalypsoCardSelectionExtension())
    }

    if (storageCardApiFactory != null) {
      cardSelectionManager.prepareSelection(
          readerApiFactory
              .createBasicCardSelector()
              .filterByCardProtocol(CardProtocol.MIFARE_ULTRALIGHT_LOGICAL_PROTOCOL.name),
          storageCardApiFactory.createStorageCardSelectionExtension(MIFARE_ULTRALIGHT))
      cardSelectionManager.prepareSelection(
          readerApiFactory
              .createBasicCardSelector()
              .filterByCardProtocol(CardProtocol.ST25_SRT512_LOGICAL_PROTOCOL.name),
          storageCardApiFactory.createStorageCardSelectionExtension(ST25_SRT512))
      cardSelectionManager.prepareSelection(
          readerApiFactory
              .createBasicCardSelector()
              .filterByCardProtocol(CardProtocol.MIFARE_CLASSIC_LOGICAL_PROTOCOL.name),
          storageCardApiFactory.createStorageCardSelectionExtension(MIFARE_CLASSIC_1K))
    }

    return cardSelectionManager.processCardSelectionScenario(reader).activeSmartCard
        ?: throw IllegalStateException("Matching smartcard not found")
  }

  private fun SmartCard.toCardInfo(): CardInfo =
      when (this) {
        is CalypsoCard ->
            CardInfo(
                description = "CALYPSO: DF name " + HexUtil.toHex(dfName),
                serialNumber = HexUtil.toHex(applicationSerialNumber),
                isStorageCard = false,
                applicationSubtype = HexUtil.toHex(applicationSubtype))
        is StorageCard ->
            CardInfo(
                description = productType.name,
                serialNumber = HexUtil.toHex(uid),
                isStorageCard = true)
        else -> throw IllegalStateException("Unexpected card type")
      }

  /** Returns the plugin type reported to the server, depending on the terminal and the device. */
  private fun getPluginType(readerType: ReaderType, deviceType: DeviceType): String =
      when (deviceType) {
        DeviceType.CONTACTLESS_CARD ->
            if (readerType == ReaderType.BLUEBIRD) "Bluebird" else "Android NFC"
        DeviceType.SIM -> "Android OMAPI"
        DeviceType.WEARABLE -> "Android WEARABLE"
        DeviceType.EMBEDDED -> "Android EMBEDDED"
      }

  /** Returns the AIDs of the Calypso applications to select, depending on the device. */
  private fun getAids(deviceType: DeviceType): List<ByteArray> =
      when (deviceType) {
        DeviceType.CONTACTLESS_CARD ->
            listOf(
                CardConstants.AID_KEYPLE_GENERIC,
                CardConstants.AID_CD_LIGHT_GTML,
                CardConstants.AID_CALYPSO_LIGHT,
                CardConstants.AID_NORMALIZED_IDF)
        DeviceType.SIM -> listOf(CardConstants.AID_CD_LIGHT_GTML, CardConstants.AID_NORMALIZED_IDF)
        DeviceType.WEARABLE,
        DeviceType.EMBEDDED -> listOf(CardConstants.AID_CD_LIGHT_GTML)
      }
}
