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
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.domain.TicketingService
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardInfo
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceEnum
import org.calypsonet.keyple.demo.reload.remote.domain.model.ReaderType
import org.calypsonet.keyple.demo.reload.remote.domain.model.Status
import org.calypsonet.keyple.demo.reload.remote.ui.adapters.UiContextImpl
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiCardReaderResponse
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

abstract class AbstractCardActivity :
    AbstractDemoActivity(), CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  @Inject lateinit var ticketingService: TicketingService
  lateinit var device: DeviceEnum

  val isBluebirdDevice = Build.MANUFACTURER?.lowercase()?.contains("bluebird") == true

  private val readerType: ReaderType
    get() = if (isBluebirdDevice) ReaderType.BLUEBIRD else ReaderType.NFC_TERMINAL

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    device = appSettings.deviceType
  }

  override fun onResume() {
    super.onResume()
    initReaders()
  }

  /** Android NFC Reader is strongly dependent and Android Activity component. */
  @Throws(UnsupportedOperationException::class)
  fun initAndActivateCardReader() {
    ticketingService.init(
        readerType,
        device,
        UiContextImpl(this@AbstractCardActivity),
        this@AbstractCardActivity,
        this@AbstractCardActivity,
        null)

    ticketingService.startNfcDetection()
  }

  /**
   * Initialisation of AndroidOmapiPlugin is async and take time and cannot be observed. So we'll
   * trigger process only when the plugin is registered
   */
  @Throws(UnsupportedOperationException::class)
  fun initOmapiReader(callback: () -> Unit) {
    ticketingService.init(
        readerType, device, UiContextImpl(this@AbstractCardActivity), null, null, callback)
  }

  @Throws(UnsupportedOperationException::class)
  fun deactivateAndClearReader() {
    if (device == DeviceEnum.CONTACTLESS_CARD) {
      ticketingService.stopNfcDetection()
    }
    ticketingService.onDestroy(this@AbstractCardActivity)
  }

  /** Displays the error corresponding to the status returned by the server for the given card. */
  fun launchStatusErrorResponse(card: CardInfo, status: RemoteServiceStatus) {
    when (status) {
      RemoteServiceStatus.CARD_COMMUNICATION_ERROR -> launchCardCommunicationErrorResponse()
      RemoteServiceStatus.SERVER_ERROR -> launchServerErrorResponse()
      RemoteServiceStatus.CARD_REJECTED ->
          launchInvalidCardResponse(
              card.description,
              if (card.isStorageCard) getString(R.string.storage_card_invalid)
              else
                  String.format(
                      getString(R.string.card_invalid_structure), card.applicationSubtype))
      RemoteServiceStatus.CARD_NOT_PERSONALIZED ->
          launchInvalidCardResponse(card.description, getString(R.string.card_not_personalized))
      RemoteServiceStatus.EXPIRED_ENVIRONMENT ->
          launchInvalidCardResponse(card.description, getString(R.string.expired_environment))
      RemoteServiceStatus.DIFFERENT_CARD ->
          launchInvalidCardResponse(card.description, getString(R.string.not_the_same_card))
      RemoteServiceStatus.SUCCESS -> {
        // Not an error
      }
    }
  }

  fun launchInvalidCardResponse(cardType: String, message: String) {
    runOnUiThread {
      changeDisplay(
          UiCardReaderResponse(
              Status.INVALID_CARD, cardType, 0, arrayListOf(), arrayListOf(), "", message),
          finishActivity = isFinishActivityAfterResult())
    }
  }

  fun launchCardCommunicationErrorResponse() {
    runOnUiThread {
      changeDisplay(
          UiCardReaderResponse(
              Status.ERROR, "", 0, arrayListOf(), arrayListOf(), "", "Card communication error"),
          finishActivity = isFinishActivityAfterResult())
    }
  }

  fun launchServerErrorResponse() {
    runOnUiThread {
      changeDisplay(
          UiCardReaderResponse(Status.ERROR, "", 0, arrayListOf(), arrayListOf(), ""),
          finishActivity = isFinishActivityAfterResult())
    }
  }

  fun launchExceptionResponse(e: Exception, finishActivity: Boolean? = false) {
    runOnUiThread {
      changeDisplay(
          UiCardReaderResponse(Status.ERROR, "", 0, arrayListOf(), arrayListOf(), "", e.message),
          finishActivity = finishActivity)
    }
  }

  /** Only with NFC we can come back to the 'wait for device' screen after a result. */
  protected fun isFinishActivityAfterResult(): Boolean = device != DeviceEnum.CONTACTLESS_CARD

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
