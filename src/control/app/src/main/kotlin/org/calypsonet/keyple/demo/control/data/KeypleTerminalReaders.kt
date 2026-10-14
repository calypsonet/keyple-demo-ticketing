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
package org.calypsonet.keyple.demo.control.data

import android.app.Activity
import javax.inject.Inject
import org.calypsonet.keyple.demo.control.domain.model.CardProtocol
import org.calypsonet.keyple.demo.control.domain.model.TerminalType
import org.calypsonet.keyple.demo.control.domain.spi.TerminalReaders
import org.calypsonet.keyple.demo.control.domain.spi.UiContext
import org.calypsonet.keyple.plugin.bluebird.BluebirdConstants
import org.calypsonet.keyple.plugin.bluebird.BluebirdContactlessProtocols
import org.calypsonet.keyple.plugin.bluebird.BluebirdPluginFactoryProvider
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoPlugin
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoPluginFactoryProvider
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoReader
import org.calypsonet.keyple.plugin.famoco.utils.ContactCardCommonProtocols
import org.calypsonet.keyple.plugin.storagecard.ApduInterpreterFactoryProvider
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConfig
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConstants
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.ConfigurableCardReader
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi

/** [TerminalReaders] adapter based on the Keyple reader plugins of the terminal. */
class KeypleTerminalReaders
@Inject
constructor(
    private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi
) : TerminalReaders {

  private lateinit var terminalType: TerminalType
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

  private fun initTerminalReader(terminalType: TerminalType) {
    when (terminalType) {
      TerminalType.BLUEBIRD -> initBluebirdReader()
      TerminalType.FAMOCO -> initFamocoReader()
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
    samPluginName = BluebirdConstants.PLUGIN_NAME
    samReaderNameRegex = ".*ContactReader"
    samReaderName = BluebirdConstants.SAM_READER_NAME
    samReaderProtocolPhysicalName = ContactCardCommonProtocols.ISO_7816_3.name
    samReaderProtocolLogicalName = CardProtocol.ISO_7816.name
    isStorageCardSupported = true
  }

  private fun initFamocoReader() {
    terminalType = TerminalType.FAMOCO
    cardPluginName = AndroidNfcConstants.PLUGIN_NAME
    cardReaderName = AndroidNfcConstants.READER_NAME
    cardReaderProtocols[AndroidNfcSupportedProtocols.ISO_14443_4.name] =
        CardProtocol.ISO_14443_4.name
    cardReaderProtocols[AndroidNfcSupportedProtocols.MIFARE_CLASSIC_1K.name] =
        CardProtocol.MIFARE_CLASSIC.name
    samPluginName = AndroidFamocoPlugin.PLUGIN_NAME
    samReaderNameRegex = ".*FamocoReader"
    samReaderName = AndroidFamocoReader.READER_NAME
    samReaderProtocolPhysicalName = ContactCardCommonProtocols.ISO_7816_3.name
    samReaderProtocolLogicalName = CardProtocol.ISO_7816.name
    isStorageCardSupported = true
  }

  private fun initNfcTerminalReader() {
    terminalType = TerminalType.NFC_TERMINAL
    cardPluginName = AndroidNfcConstants.PLUGIN_NAME
    cardReaderName = AndroidNfcConstants.READER_NAME
    cardReaderProtocols[AndroidNfcSupportedProtocols.ISO_14443_4.name] =
        CardProtocol.ISO_14443_4.name
    samPluginName = ""
    samReaderNameRegex = ""
    samReaderName = ""
    samReaderProtocolPhysicalName = ""
    samReaderProtocolLogicalName = ""
  }

  @Throws(KeyplePluginException::class)
  override fun registerPlugin(terminalType: TerminalType, uiContext: UiContext) {
    initTerminalReader(terminalType)
    val activity = uiContext.adaptTo(Activity::class.java)
    // Plugin
    val pluginFactory =
        when (terminalType) {
          TerminalType.BLUEBIRD ->
              BluebirdPluginFactoryProvider.provideFactory(
                  activity,
                  ApduInterpreterFactoryProvider.provideFactory(),
                  DemoMifareClassicKeyProvider())
          TerminalType.FAMOCO ->
              AndroidNfcPluginFactoryProvider.provideFactory(
                  AndroidNfcConfig(
                      activity = activity,
                      apduInterpreterFactory = ApduInterpreterFactoryProvider.provideFactory(),
                      keyProvider = DemoMifareClassicKeyProvider()))
          TerminalType.NFC_TERMINAL ->
              AndroidNfcPluginFactoryProvider.provideFactory(
                  AndroidNfcConfig(
                      activity = activity,
                      apduInterpreterFactory = ApduInterpreterFactoryProvider.provideFactory(),
                      keyProvider = DemoMifareClassicKeyProvider()))
        }
    SmartCardServiceProvider.getService().registerPlugin(pluginFactory)
    // SAM plugin (if different of card plugin)
    if (terminalType == TerminalType.FAMOCO) {
      val samPluginFactory = AndroidFamocoPluginFactoryProvider.getFactory()
      SmartCardServiceProvider.getService().registerPlugin(samPluginFactory)
    }
  }

  @Throws(KeyplePluginException::class)
  override fun initCardReader(): CardReader? {
    cardReader =
        SmartCardServiceProvider.getService().getPlugin(cardPluginName)?.getReader(cardReaderName)
    cardReader?.let {
      cardReaderProtocols.forEach { entry ->
        (it as ConfigurableCardReader).activateProtocol(entry.key, entry.value)
      }
      (cardReader as ObservableCardReader).setReaderObservationExceptionHandler(
          readerObservationExceptionHandler)
    }
    return cardReader
  }

  override fun getCardReader(): CardReader? {
    return cardReader
  }

  @Throws(KeyplePluginException::class)
  override fun initSamReaders(): List<CardReader> {
    samReaders =
        if (terminalType == TerminalType.FAMOCO) {
          SmartCardServiceProvider.getService()
              .getPlugin(samPluginName)
              ?.readers
              ?.filter { it.name == samReaderName }
              ?.toMutableList() ?: mutableListOf()
        } else {
          SmartCardServiceProvider.getService()
              .getPlugin(samPluginName)
              ?.readers
              ?.filter { !it.isContactless }
              ?.toMutableList() ?: mutableListOf()
        }
    samReaders.forEach {
      if (it is ConfigurableCardReader) {
        it.activateProtocol(samReaderProtocolPhysicalName, samReaderProtocolLogicalName)
      }
    }
    return samReaders
  }

  override fun getSamReader(): CardReader? {
    return if (samReaders.isNotEmpty()) {
      val filteredByName = samReaders.filter { it.name == samReaderName }
      return if (filteredByName.isEmpty()) {
        samReaders.first()
      } else {
        filteredByName.first()
      }
    } else {
      null
    }
  }

  override fun isStorageCardSupported(): Boolean {
    return isStorageCardSupported
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

  override fun release(observer: CardReaderObserverSpi?) {
    clear()
    if (observer != null && cardReader != null) {
      (cardReader as ObservableCardReader).removeObserver(observer)
    }
    val smartCardService = SmartCardServiceProvider.getService()
    smartCardService.plugins.forEach { smartCardService.unregisterPlugin(it.name) }
  }
}
