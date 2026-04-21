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
package org.calypsonet.keyple.demo.reload.remote.ui.activities

import android.os.Build
import android.os.Bundle
import javax.inject.Inject
import kotlin.jvm.Throws
import org.calypsonet.keyple.demo.common.constants.CardConstants
import org.calypsonet.keyple.demo.reload.remote.data.ReaderManagerImpl
import org.calypsonet.keyple.demo.reload.remote.domain.TicketingService
import org.calypsonet.keyple.demo.reload.remote.domain.model.AppSettings
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceEnum
import org.calypsonet.keyple.demo.reload.remote.domain.model.Status
import org.calypsonet.keyple.demo.reload.remote.ui.adapters.UiContextImpl
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiCardReaderResponse
import org.calypsonet.keyple.plugin.bluebird.BluebirdConstants
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConstants
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiPlugin
import org.eclipse.keyple.plugin.android.omapi.AndroidOmapiReader
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

abstract class AbstractCardActivity :
    AbstractDemoActivity(), CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  @Inject lateinit var ticketingService: TicketingService
  @Inject lateinit var readerManager: ReaderManagerImpl
  lateinit var selectedDeviceReaderName: String
  lateinit var device: DeviceEnum
  lateinit var pluginType: String

  val isBluebirdDevice = Build.MANUFACTURER?.lowercase()?.contains("bluebird") == true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    device = DeviceEnum.getDeviceEnum(prefData.loadDeviceType()!!)
    selectedDeviceReaderName =
        when (device) {
          DeviceEnum.CONTACTLESS_CARD -> {
            pluginType = if (isBluebirdDevice) "Bluebird" else "Android Nfc"
            AppSettings.aidEnums.clear()
            AppSettings.aidEnums.add(CardConstants.AID_KEYPLE_GENERIC)
            AppSettings.aidEnums.add(CardConstants.AID_CD_LIGHT_GTML)
            AppSettings.aidEnums.add(CardConstants.AID_CALYPSO_LIGHT)
            AppSettings.aidEnums.add(CardConstants.AID_NORMALIZED_IDF)
            if (isBluebirdDevice) BluebirdConstants.CARD_READER_NAME
            else AndroidNfcConstants.READER_NAME
          }
          DeviceEnum.SIM -> {
            pluginType = "Android OMAPI"
            AppSettings.aidEnums.clear()
            AppSettings.aidEnums.add(CardConstants.AID_CD_LIGHT_GTML)
            AppSettings.aidEnums.add(CardConstants.AID_NORMALIZED_IDF)
            AndroidOmapiReader.READER_NAME_SIM_1
          }
          DeviceEnum.WEARABLE -> {
            pluginType = "Android WEARABLE"
            AppSettings.aidEnums.clear()
            AppSettings.aidEnums.add(CardConstants.AID_CD_LIGHT_GTML)
            "WEARABLE"
          }
          DeviceEnum.EMBEDDED -> {
            pluginType = "Android EMBEDDED"
            AppSettings.aidEnums.clear()
            AppSettings.aidEnums.add(CardConstants.AID_CD_LIGHT_GTML)
            "EMBEDDED"
          }
        }
  }

  override fun onResume() {
    super.onResume()
    initReaders()
  }

  /** Android Nfc Reader is strongly dependent and Android Activity component. */
  @Throws(UnsupportedOperationException::class)
  fun initAndActivateCardReader() {
    ticketingService.init(
        AppSettings.readerType,
        UiContextImpl(this@AbstractCardActivity),
        device,
        this@AbstractCardActivity,
        this@AbstractCardActivity,
        null)

    ticketingService.startNfcDetection(selectedDeviceReaderName)
  }

  @Throws(UnsupportedOperationException::class)
  fun deactivateAndClearCardReader() {
    ticketingService.stopNfcDetection(selectedDeviceReaderName)
    ticketingService.onDestroy(this@AbstractCardActivity)
  }


  /**
   * Initialisation of AndroidOmapiPlugin is async and take time and cannot be observed. So we'll
   * trigger process only when the plugin is registered
   */
  @Throws(UnsupportedOperationException::class)
  fun initOmapiReader(callback: () -> Unit) {
      ticketingService.init(
          AppSettings.readerType,
          UiContextImpl(this@AbstractCardActivity),
          device,
          null,
          null, callback)
  }

  @Throws(UnsupportedOperationException::class)
  fun deactivateAndClearOmapiReader() {
    readerManager.unregisterPlugin(AndroidOmapiPlugin.PLUGIN_NAME)
  }

  fun launchInvalidCardResponse(cardType: String, message: String) {
    runOnUiThread {
      changeDisplay(
          UiCardReaderResponse(
              Status.INVALID_CARD, cardType, 0, arrayListOf(), arrayListOf(), "", message),
          finishActivity =
              device !=
                  DeviceEnum.CONTACTLESS_CARD // /Only with NFC we can come back to 'wait for device
          // screen'
          )
    }
  }

  fun launchCardCommunicationErrorResponse() {
    runOnUiThread {
      changeDisplay(
          UiCardReaderResponse(
              Status.ERROR, "", 0, arrayListOf(), arrayListOf(), "", "Card communication error"),
          finishActivity =
              device !=
                  DeviceEnum.CONTACTLESS_CARD // /Only with NFC we can come back to 'wait for device
          // screen'
          )
    }
  }

  fun launchServerErrorResponse() {
    runOnUiThread {
      changeDisplay(
          UiCardReaderResponse(Status.ERROR, "", 0, arrayListOf(), arrayListOf(), ""),
          finishActivity =
              device !=
                  DeviceEnum.CONTACTLESS_CARD // /Only with NFC we can come back to 'wait for device
          // screen'
          )
    }
  }

  fun launchExceptionResponse(e: Exception, finishActivity: Boolean? = false) {
    runOnUiThread {
      changeDisplay(
          UiCardReaderResponse(Status.ERROR, "", 0, arrayListOf(), arrayListOf(), "", e.message),
          finishActivity = finishActivity)
    }
  }

  protected abstract fun changeDisplay(
      cardReaderResponse: UiCardReaderResponse,
      applicationSerialNumber: String? = null,
      finishActivity: Boolean? = false
  )

  override fun onReaderObservationError(contextInfo: String?, readerName: String?, e: Throwable?) {
    Timber.e(e)
    Timber.d("Error on $contextInfo, $readerName")
    this@AbstractCardActivity.finish()
  }

  protected abstract fun initReaders()

  companion object {
    const val CARD_APPLICATION_NUMBER = "cardApplicationNumber"
    const val CARD_CONTENT = "cardContent"
  }
}
