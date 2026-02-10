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
import android.media.MediaPlayer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.data.model.CardProtocolEnum
import org.calypsonet.keyple.demo.control.data.model.ReaderType
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

class ReaderRepository
@Inject
constructor(
    private val readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi
) {

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
  private lateinit var successMedia: MediaPlayer
  private lateinit var errorMedia: MediaPlayer

  private fun initReaderType(readerType: ReaderType) {
    when (readerType) {
      ReaderType.BLUEBIRD -> initBluebirdReader()
      ReaderType.COPPERNIC -> initCoppernicReader()
      ReaderType.FAMOCO -> initFamocoReader()
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
    isStorageCardSupported = true
  }

  private fun initNfcTerminalReader() {
    readerType = ReaderType.NFC_TERMINAL
    cardPluginName = AndroidNfcConstants.PLUGIN_NAME
    cardReaderName = AndroidNfcConstants.READER_NAME
    cardReaderProtocols[AndroidNfcSupportedProtocols.ISO_14443_4.name] =
        CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name
    samPluginName = ""
    samReaderNameRegex = ""
    samReaderName = ""
    samReaderProtocolPhysicalName = ""
    samReaderProtocolLogicalName = ""
  }

  @Throws(KeyplePluginException::class)
  fun registerPlugin(activity: Activity, readerType: ReaderType) {
    initReaderType(readerType)
    if (readerType != ReaderType.NFC_TERMINAL) {
      successMedia = MediaPlayer.create(activity, R.raw.success)
      errorMedia = MediaPlayer.create(activity, R.raw.error)
    }
    runBlocking {
      // Plugin
      val pluginFactory =
          withContext(Dispatchers.IO) {
            when (readerType) {
              ReaderType.BLUEBIRD ->
                  BluebirdPluginFactoryProvider.provideFactory(
                      activity,
                      ApduInterpreterFactoryProvider.provideFactory(),
                      MifareClassicKeyProvider())
              ReaderType.COPPERNIC -> Cone2PluginFactoryProvider.getFactory(activity)
              ReaderType.FAMOCO ->
                  AndroidNfcPluginFactoryProvider.provideFactory(
                      AndroidNfcConfig(
                          activity = activity,
                          apduInterpreterFactory = ApduInterpreterFactoryProvider.provideFactory(),
                          keyProvider = MifareClassicKeyProvider()))
              ReaderType.NFC_TERMINAL ->
                  AndroidNfcPluginFactoryProvider.provideFactory(
                      AndroidNfcConfig(
                          activity = activity,
                          apduInterpreterFactory = ApduInterpreterFactoryProvider.provideFactory(),
                          keyProvider = MifareClassicKeyProvider()))
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
  fun initCardReader(): CardReader? {
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

  fun getCardReader(): CardReader? {
    return cardReader
  }

  @Throws(KeyplePluginException::class)
  fun initSamReaders(): List<CardReader> {
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

  fun getSamReader(): CardReader? {
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

  fun isStorageCardSupported(): Boolean {
    return isStorageCardSupported
  }

  fun clear() {
    cardReaderProtocols.forEach { entry ->
      (cardReader as ConfigurableCardReader).deactivateProtocol(entry.key)
    }
    samReaders.forEach {
      if (it is ConfigurableCardReader) {
        it.deactivateProtocol(samReaderProtocolPhysicalName)
      }
    }
    if (readerType != ReaderType.NFC_TERMINAL) {
      successMedia.stop()
      successMedia.release()
      errorMedia.stop()
      errorMedia.release()
    }
  }

  fun displayResultSuccess(): Boolean {
    if (readerType != ReaderType.NFC_TERMINAL) successMedia.start()
    return true
  }

  fun displayResultFailed(): Boolean {
    if (readerType != ReaderType.NFC_TERMINAL) errorMedia.start()
    return true
  }
}
