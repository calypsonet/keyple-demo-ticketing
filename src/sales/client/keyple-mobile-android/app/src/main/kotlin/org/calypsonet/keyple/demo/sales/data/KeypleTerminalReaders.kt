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
package org.calypsonet.keyple.demo.sales.data

import android.app.Activity
import javax.inject.Inject
import org.calypsonet.keyple.demo.sales.domain.model.CardMedium
import org.calypsonet.keyple.demo.sales.domain.model.CardProtocol
import org.calypsonet.keyple.demo.sales.domain.model.TerminalType
import org.calypsonet.keyple.demo.sales.domain.spi.Logger
import org.calypsonet.keyple.demo.sales.domain.spi.TerminalReaders
import org.calypsonet.keyple.demo.sales.domain.spi.UiContext
import org.calypsonet.keyple.plugin.bluebird.BluebirdConstants
import org.calypsonet.keyple.plugin.bluebird.BluebirdContactlessProtocols
import org.calypsonet.keyple.plugin.bluebird.BluebirdPluginFactoryProvider
import org.calypsonet.keyple.plugin.storagecard.ApduInterpreterFactoryProvider
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConfig
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConstants
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiPlugin
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiPluginFactoryProvider
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiReader
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.ConfigurableCardReader
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.ReaderCommunicationException
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi

/** [TerminalReaders] adapter based on the Keyple reader plugins of the terminal. */
class KeypleTerminalReaders @Inject constructor(private val logger: Logger) : TerminalReaders {

  private lateinit var terminalType: TerminalType

  private lateinit var cardMedium: CardMedium
  // Card
  private lateinit var cardPluginName: String
  private lateinit var cardReaderName: String
  private var cardReaderProtocols = mutableMapOf<String, String>()
  private var cardReader: CardReader? = null
  private var isStorageCardSupported = false

  private fun initTerminalReader(terminalType: TerminalType) {
    when (terminalType) {
      TerminalType.BLUEBIRD -> initBluebirdReader()
      TerminalType.NFC_TERMINAL -> initNfcTerminalReader()
    }
  }

  private fun initBluebirdReader() {
    terminalType = TerminalType.BLUEBIRD
    cardPluginName = BluebirdConstants.PLUGIN_NAME
    cardReaderName = BluebirdConstants.CARD_READER_NAME
    cardReaderProtocols[BluebirdContactlessProtocols.ISO_14443_4_A.name] =
        CardProtocol.ISO_14443_4.name
    cardReaderProtocols[BluebirdContactlessProtocols.ISO_14443_4_B.name] =
        CardProtocol.ISO_14443_4.name
    cardReaderProtocols[BluebirdContactlessProtocols.MIFARE_ULTRALIGHT.name] =
        CardProtocol.MIFARE_ULTRALIGHT.name
    cardReaderProtocols[BluebirdContactlessProtocols.ST25_SRT512.name] =
        CardProtocol.ST25_SRT512.name
    cardReaderProtocols[BluebirdContactlessProtocols.MIFARE_CLASSIC.name] =
        CardProtocol.MIFARE_CLASSIC.name
    isStorageCardSupported = true
  }

  private fun initNfcTerminalReader() {
    terminalType = TerminalType.NFC_TERMINAL
    cardPluginName = AndroidNfcConstants.PLUGIN_NAME
    cardReaderName = AndroidNfcConstants.READER_NAME
    cardReaderProtocols[AndroidNfcSupportedProtocols.ISO_14443_4.name] =
        CardProtocol.ISO_14443_4.name
    cardReaderProtocols[AndroidNfcSupportedProtocols.MIFARE_ULTRALIGHT.name] =
        CardProtocol.MIFARE_ULTRALIGHT.name
    cardReaderProtocols[AndroidNfcSupportedProtocols.MIFARE_CLASSIC_1K.name] =
        CardProtocol.MIFARE_CLASSIC.name
  }

  /** Register any keyple plugin */
  override fun registerPlugin(
      terminalType: TerminalType,
      uiContext: UiContext,
      cardMedium: CardMedium,
      onPluginRegistered: (() -> Unit)?
  ) {
    this.cardMedium = cardMedium
    val activity = uiContext.adaptTo(Activity::class.java)
    if (cardMedium != CardMedium.CONTACTLESS_CARD) {
      // The OMAPI plugin factory is provided asynchronously, once the SE service is connected.
      AndroidOmapiPluginFactoryProvider(activity) { factory ->
        SmartCardServiceProvider.getService().registerPlugin(factory)
        onPluginRegistered?.invoke()
      }
      return
    }
    initTerminalReader(terminalType)
    val pluginFactory =
        when (terminalType) {
          TerminalType.BLUEBIRD ->
              BluebirdPluginFactoryProvider.provideFactory(
                  activity,
                  ApduInterpreterFactoryProvider.provideFactory(),
                  DemoMifareClassicKeyProvider())

          TerminalType.NFC_TERMINAL ->
              AndroidNfcPluginFactoryProvider.provideFactory(
                  AndroidNfcConfig(
                      activity = activity,
                      apduInterpreterFactory = ApduInterpreterFactoryProvider.provideFactory(),
                      keyProvider = DemoMifareClassicKeyProvider()))
        }
    SmartCardServiceProvider.getService().registerPlugin(pluginFactory)
  }

  override fun initCardReader(
      observer: CardReaderObserverSpi?,
      readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi?
  ): CardReader? {
    // Only the contactless card reader is observed; the OMAPI readers are retrieved by name.
    if (cardMedium != CardMedium.CONTACTLESS_CARD) {
      return null
    }
    cardReader =
        SmartCardServiceProvider.getService().getPlugin(cardPluginName)?.getReader(cardReaderName)

    cardReader?.let { reader ->
      cardReaderProtocols.forEach { (physicalName, logicalName) ->
        (reader as ConfigurableCardReader).activateProtocol(physicalName, logicalName)
      }
      (reader as ObservableCardReader).setReaderObservationExceptionHandler(
          readerObservationExceptionHandler)
      reader.addObserver(observer)
    }

    return cardReader
  }

  override fun getCardReader(): CardReader? {
    return cardReader
  }

  private fun clear() {
    cardReaderProtocols.keys.forEach { physicalName ->
      (cardReader as ConfigurableCardReader).deactivateProtocol(physicalName)
    }

    if (cardMedium != CardMedium.CONTACTLESS_CARD) {
      unregisterPlugin(AndroidOmapiPlugin.PLUGIN_NAME)
    }
  }

  override fun release(observer: CardReaderObserverSpi?) {
    clear()
    if (observer != null && cardReader != null) {
      (cardReader as ObservableCardReader).removeObserver(observer)
    }
    val smartCardService = SmartCardServiceProvider.getService()
    smartCardService.plugins.forEach { smartCardService.unregisterPlugin(it.name) }
  }

  override fun getReaderName(terminalType: TerminalType, cardMedium: CardMedium): String =
      when (cardMedium) {
        CardMedium.CONTACTLESS_CARD ->
            if (terminalType == TerminalType.BLUEBIRD) BluebirdConstants.CARD_READER_NAME
            else AndroidNfcConstants.READER_NAME
        CardMedium.SIM -> AndroidOmapiReader.READER_NAME_SIM_1
      }

  /** Unregisters the given Keyple plugin, logging the failure. */
  private fun unregisterPlugin(pluginName: String) {
    try {
      SmartCardServiceProvider.getService().unregisterPlugin(pluginName)
    } catch (e: Exception) {
      logger.e("Error while unregistering the plugin [plugin=$pluginName]", e)
    }
  }

  /** Retrieve a registered reader */
  @Throws(ReaderCommunicationException::class)
  override fun getReader(readerName: String): CardReader {
    var reader: CardReader? = null
    SmartCardServiceProvider.getService().plugins.forEach { reader = it.getReader(readerName) }
    return reader ?: throw ReaderCommunicationException("Reader not found [reader=$readerName]")
  }
}
