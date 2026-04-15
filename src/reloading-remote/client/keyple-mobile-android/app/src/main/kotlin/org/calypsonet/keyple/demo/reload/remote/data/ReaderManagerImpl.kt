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
package org.calypsonet.keyple.demo.reload.remote.data

import android.app.Activity
import javax.inject.Inject
import kotlin.collections.set
import kotlin.jvm.Throws
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardProtocolEnum
import org.calypsonet.keyple.demo.reload.remote.domain.model.ReaderType
import org.calypsonet.keyple.demo.reload.remote.domain.spi.ReaderManager
import org.calypsonet.keyple.demo.reload.remote.domain.spi.UiContext
import org.calypsonet.keyple.plugin.bluebird.BluebirdConstants
import org.calypsonet.keyple.plugin.bluebird.BluebirdContactlessProtocols
import org.calypsonet.keyple.plugin.bluebird.BluebirdPluginFactoryProvider
import org.calypsonet.keyple.plugin.storagecard.ApduInterpreterFactoryProvider
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.util.protocol.ContactCardCommonProtocol
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConfig
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConstants
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.ConfigurableCardReader
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.ReaderCommunicationException
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

/**
 * Manager provided to encapsulate slight differences between readers provides methods to improve
 * code readability.
 */
class ReaderManagerImpl @Inject constructor() : ReaderManager {

  private lateinit var readerType: ReaderType
  // Card
  private lateinit var cardPluginName: String
  private lateinit var cardReaderName: String
  private var cardReaderProtocols = mutableMapOf<String, String>()
  private var cardReader: CardReader? = null
  private var isStorageCardSupported = false
  // SAM
  private lateinit var samPluginName: String
  private lateinit var samReaderNameRegex: String
  private lateinit var samReaderName: String
  private var samReaderProtocolPhysicalName: String? = null
  private var samReaderProtocolLogicalName: String? = null
  private var samReaders: MutableList<CardReader> = mutableListOf()

  private fun initReaderType(readerType: ReaderType) {
    when (readerType) {
      ReaderType.BLUEBIRD -> initBluebirdReader()
      ReaderType.NFC_TERMINAL -> initNfcTerminalReader()
    }
  }

  private fun initBluebirdReader() {
    readerType = ReaderType.BLUEBIRD
    cardPluginName = BluebirdConstants.PLUGIN_NAME
    cardReaderName = BluebirdConstants.CARD_READER_NAME
    cardReaderProtocols[BluebirdContactlessProtocols.ISO_14443_4_A.name] =
        CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name
    cardReaderProtocols[BluebirdContactlessProtocols.ISO_14443_4_B.name] =
        CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name
    cardReaderProtocols[BluebirdContactlessProtocols.MIFARE_ULTRALIGHT.name] =
        CardProtocolEnum.ST25_SRT512_LOGICAL_PROTOCOL.name
    cardReaderProtocols[BluebirdContactlessProtocols.ST25_SRT512.name] =
      CardProtocolEnum.ST25_SRT512_LOGICAL_PROTOCOL.name
    cardReaderProtocols[BluebirdContactlessProtocols.MIFARE_CLASSIC.name] =
        CardProtocolEnum.MIFARE_CLASSIC_LOGICAL_PROTOCOL.name
    samPluginName = BluebirdConstants.PLUGIN_NAME
    samReaderNameRegex = ".*ContactReader"
    samReaderProtocolPhysicalName = ContactCardCommonProtocol.ISO_7816_3.name
    samReaderProtocolLogicalName = CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name
    isStorageCardSupported = true
  }

  private fun initNfcTerminalReader() {
    readerType = ReaderType.NFC_TERMINAL
    cardPluginName = AndroidNfcConstants.PLUGIN_NAME
    cardReaderName = AndroidNfcConstants.READER_NAME
    cardReaderProtocols[AndroidNfcSupportedProtocols.ISO_14443_4.name] =
        CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name
    cardReaderProtocols[AndroidNfcSupportedProtocols.MIFARE_ULTRALIGHT.name] =
      CardProtocolEnum.MIFARE_ULTRALIGHT_LOGICAL_PROTOCOL.name
    cardReaderProtocols[AndroidNfcSupportedProtocols.MIFARE_CLASSIC_1K.name] =
      CardProtocolEnum.MIFARE_CLASSIC_LOGICAL_PROTOCOL.name
    samPluginName = ""
    samReaderNameRegex = ""
    samReaderName = ""
    samReaderProtocolPhysicalName = ""
    samReaderProtocolLogicalName = ""
  }

  /** Register any keyple plugin */
  override fun registerPlugin(readerType: ReaderType, uiContext: UiContext) {
    initReaderType(readerType)
    val activity = uiContext.adaptTo(Activity::class.java)
    runBlocking {
      val pluginFactory =
          withContext(Dispatchers.IO) {
            when (readerType) {
              ReaderType.BLUEBIRD ->
                  BluebirdPluginFactoryProvider.provideFactory(
                      activity,
                      ApduInterpreterFactoryProvider.provideFactory(),
                      MifareClassicKeyProviderImpl())
              ReaderType.NFC_TERMINAL ->
                  AndroidNfcPluginFactoryProvider.provideFactory(
                      AndroidNfcConfig(
                          activity = activity,
                          apduInterpreterFactory = ApduInterpreterFactoryProvider.provideFactory(),
                          keyProvider = MifareClassicKeyProviderImpl()))
            }
          }
      SmartCardServiceProvider.getService().registerPlugin(pluginFactory)
    }
  }

  override fun initCardReader(
      observer: CardReaderObserverSpi?,
      readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi?
  ): CardReader? {
    cardReader =
        SmartCardServiceProvider.getService().getPlugin(cardPluginName)?.getReader(cardReaderName)

    cardReader?.let {
      cardReaderProtocols.forEach {
        entry ->  (it as ConfigurableCardReader).activateProtocol(entry.key, entry.value)

        (cardReader as ObservableCardReader).setReaderObservationExceptionHandler(readerObservationExceptionHandler)
        (cardReader as ObservableCardReader).addObserver(observer)
      }
    }

    return cardReader
  }

  private fun clear() {
    cardReaderProtocols.forEach { entry ->
      (cardReader as ConfigurableCardReader).deactivateProtocol(entry.key)
    }
    samReaders.forEach {
      if (it is ConfigurableCardReader) {
        it.deactivateProtocol(samReaderProtocolPhysicalName)
      }
    }
  }

  override fun onDestroy(observer: CardReaderObserverSpi?) {
    clear()
    if (observer != null && cardReader != null) {
      (cardReader as ObservableCardReader).removeObserver(observer)
    }
    val smartCardService = SmartCardServiceProvider.getService()
    smartCardService.plugins.forEach { smartCardService.unregisterPlugin(it.name) }
  }

  /** Unregister any keyple plugin */
  override fun unregisterPlugin(pluginName: String) {
    try {
      SmartCardServiceProvider.getService().unregisterPlugin(pluginName)
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  /** Retrieve a registered reader */
  @Throws(ReaderCommunicationException::class)
  override fun getReader(readerName: String): CardReader {
    var reader: CardReader? = null
    SmartCardServiceProvider.getService().plugins.forEach { reader = it.getReader(readerName) }
    return reader ?: throw ReaderCommunicationException("$readerName not found")
  }

  // TODO: delete function below
  /** Retrieve a registered observable reader. */
  @Throws(Exception::class)
  private fun getObservableReader(readerName: String): ObservableCardReader {
    val reader = getReader(readerName)
    return reader as? ObservableCardReader ?: throw Exception("$readerName not found")
  }
}
