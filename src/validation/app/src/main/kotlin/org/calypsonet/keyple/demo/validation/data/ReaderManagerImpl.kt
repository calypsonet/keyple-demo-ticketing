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
package org.calypsonet.keyple.demo.validation.data

import android.app.Activity
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.validation.domain.model.CardProtocolEnum
import org.calypsonet.keyple.demo.validation.domain.model.ReaderType
import org.calypsonet.keyple.demo.validation.domain.spi.ReaderManager
import org.calypsonet.keyple.demo.validation.domain.spi.UiContext
import org.calypsonet.keyple.plugin.arrive.ArriveConstants
import org.calypsonet.keyple.plugin.arrive.ArriveContactlessProtocols
import org.calypsonet.keyple.plugin.arrive.ArrivePluginFactoryProvider
import org.calypsonet.keyple.plugin.bluebird.BluebirdConstants
import org.calypsonet.keyple.plugin.bluebird.BluebirdContactlessProtocols
import org.calypsonet.keyple.plugin.bluebird.BluebirdPluginFactoryProvider
import org.calypsonet.keyple.plugin.coppernic.*
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

class ReaderManagerImpl
@Inject
constructor(
    private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi
) : ReaderManager {

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
  // IHM
  private lateinit var uiManager: UiManager

  private fun initReaderType(readerType: ReaderType) {
    when (readerType) {
      ReaderType.ARRIVE -> initArriveReader()
      ReaderType.BLUEBIRD -> initBluebirdReader()
      ReaderType.COPPERNIC -> initCoppernicReader()
      ReaderType.FAMOCO -> initFamocoReader()
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
        CardProtocolEnum.MIFARE_ULTRALIGHT_LOGICAL_PROTOCOL.name
    cardReaderProtocols[BluebirdContactlessProtocols.ST25_SRT512.name] =
        CardProtocolEnum.ST25_SRT512_LOGICAL_PROTOCOL.name
    cardReaderProtocols[BluebirdContactlessProtocols.MIFARE_CLASSIC.name] =
        CardProtocolEnum.MIFARE_CLASSIC_LOGICAL_PROTOCOL.name
    samPluginName = BluebirdConstants.PLUGIN_NAME
    samReaderNameRegex = ".*ContactReader"
    samReaderName = BluebirdConstants.SAM_READER_NAME
    samReaderProtocolPhysicalName = ContactCardCommonProtocols.ISO_7816_3.name
    samReaderProtocolLogicalName = CardProtocolEnum.ISO_7816_LOGICAL_PROTOCOL.name
    isStorageCardSupported = true
  }

  private fun initCoppernicReader() {
    readerType = ReaderType.COPPERNIC
    cardPluginName = Cone2Plugin.PLUGIN_NAME
    cardReaderName = Cone2ContactlessReader.READER_NAME
    cardReaderProtocols[ParagonSupportedContactlessProtocols.ISO_14443.name] =
        CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name
    samPluginName = Cone2Plugin.PLUGIN_NAME
    samReaderNameRegex = ".*ContactReader_1"
    samReaderName = "${Cone2ContactReader.READER_NAME}_1"
    samReaderProtocolPhysicalName =
        ParagonSupportedContactProtocols.INNOVATRON_HIGH_SPEED_PROTOCOL.name
    samReaderProtocolLogicalName = CardProtocolEnum.ISO_7816_LOGICAL_PROTOCOL.name
  }

  private fun initFamocoReader() {
    readerType = ReaderType.FAMOCO
    cardPluginName = AndroidNfcConstants.PLUGIN_NAME
    cardReaderName = AndroidNfcConstants.READER_NAME
    cardReaderProtocols[AndroidNfcSupportedProtocols.ISO_14443_4.name] =
        CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name
    cardReaderProtocols[AndroidNfcSupportedProtocols.MIFARE_CLASSIC_1K.name] =
        CardProtocolEnum.MIFARE_CLASSIC_LOGICAL_PROTOCOL.name
    samPluginName = AndroidFamocoPlugin.PLUGIN_NAME
    samReaderNameRegex = ".*FamocoReader"
    samReaderName = AndroidFamocoReader.READER_NAME
    samReaderProtocolPhysicalName = ContactCardCommonProtocols.ISO_7816_3.name
    samReaderProtocolLogicalName = CardProtocolEnum.ISO_7816_LOGICAL_PROTOCOL.name
  }

  private fun initArriveReader() {
    readerType = ReaderType.ARRIVE
    cardPluginName = ArriveConstants.PLUGIN_NAME
    cardReaderName = ArriveConstants.CARD_READER_NAME
    cardReaderProtocols[ArriveContactlessProtocols.ISO_14443_4.name] =
        CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name
    samPluginName = ArriveConstants.PLUGIN_NAME
    samReaderNameRegex = ".*SAM.*"
    samReaderName = ArriveConstants.SAM.SAM_1.readerName
    samReaderProtocolPhysicalName = null
    samReaderProtocolLogicalName = null
  }

  @Throws(KeyplePluginException::class)
  override fun registerPlugin(readerType: ReaderType, uiContext: UiContext) {
    initReaderType(readerType)
    val activity = uiContext.adaptTo(Activity::class.java)
    uiManager =
        if (readerType == ReaderType.ARRIVE) {
          ArriveUiManagerImpl(activity).also { it.init() }
        } else {
          AndroidUiManagerImpl(activity).also { it.init() }
        }
    runBlocking {
      // Plugin
      val pluginFactory =
          withContext(Dispatchers.IO) {
            when (readerType) {
              ReaderType.ARRIVE -> {
                ArrivePluginFactoryProvider.provideFactory(context = activity)
              }
              ReaderType.BLUEBIRD ->
                  BluebirdPluginFactoryProvider.provideFactory(
                      activity,
                      ApduInterpreterFactoryProvider.provideFactory(),
                      MifareClassicKeyProviderImpl())
              ReaderType.COPPERNIC -> Cone2PluginFactoryProvider.getFactory(activity)
              ReaderType.FAMOCO ->
                  AndroidNfcPluginFactoryProvider.provideFactory(
                      AndroidNfcConfig(
                          activity = activity,
                          apduInterpreterFactory = ApduInterpreterFactoryProvider.provideFactory(),
                          keyProvider = MifareClassicKeyProviderImpl()))
            }
          }
      SmartCardServiceProvider.getService().registerPlugin(pluginFactory)
      // SAM plugin (if different of card plugin)
      if (readerType == ReaderType.FAMOCO) {
        val samPluginFactory =
            withContext(Dispatchers.IO) { AndroidFamocoPluginFactoryProvider.getFactory() }
        SmartCardServiceProvider.getService().registerPlugin(samPluginFactory)
      }
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
        if (readerType == ReaderType.FAMOCO) {
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
    uiManager.release()
  }

  override fun onDestroy(observer: CardReaderObserverSpi?) {
    clear()
    if (observer != null && cardReader != null) {
      (cardReader as ObservableCardReader).removeObserver(observer)
    }
    val smartCardService = SmartCardServiceProvider.getService()
    smartCardService.plugins.forEach { smartCardService.unregisterPlugin(it.name) }
  }

  override fun displayResultSuccess(): Boolean {
    uiManager.displayResultSuccess()
    return true
  }

  override fun displayResultFailed(): Boolean {
    uiManager.displayResultFailed()
    return true
  }

  override fun displayWaiting() {
    uiManager.displayWaiting()
  }
}
