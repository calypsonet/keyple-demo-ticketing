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
import org.calypsonet.keyple.demo.reload.remote.domain.CardReaderSession
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardInfo
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMedium
import org.calypsonet.keyple.demo.reload.remote.domain.model.TerminalType
import org.calypsonet.keyple.demo.reload.remote.ui.ActivityUiContext
import org.calypsonet.keyple.demo.reload.remote.ui.model.ResultType
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiCardResult
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

abstract class BaseCardActivity :
    BaseActivity(), CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  @Inject lateinit var cardReaderSession: CardReaderSession
  lateinit var cardMedium: CardMedium

  val isBluebirdTerminal = Build.MANUFACTURER?.lowercase()?.contains("bluebird") == true

  private val terminalType: TerminalType
    get() = if (isBluebirdTerminal) TerminalType.BLUEBIRD else TerminalType.NFC_TERMINAL

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    cardMedium = appSettings.cardMedium
  }

  override fun onResume() {
    super.onResume()
    initReaders()
  }

  /** Android NFC Reader is strongly dependent and Android Activity component. */
  @Throws(UnsupportedOperationException::class)
  fun initCardReader() {
    cardReaderSession.init(
        terminalType,
        cardMedium,
        ActivityUiContext(this@BaseCardActivity),
        this@BaseCardActivity,
        this@BaseCardActivity,
        null)

    cardReaderSession.startCardDetection()
  }

  /**
   * Initialisation of AndroidOmapiPlugin is async and take time and cannot be observed. So we'll
   * trigger process only when the plugin is registered
   */
  @Throws(UnsupportedOperationException::class)
  fun initOmapiReader(callback: () -> Unit) {
    cardReaderSession.init(
        terminalType, cardMedium, ActivityUiContext(this@BaseCardActivity), null, null, callback)
  }

  @Throws(UnsupportedOperationException::class)
  fun releaseCardReader() {
    if (cardMedium == CardMedium.CONTACTLESS_CARD) {
      cardReaderSession.stopCardDetection()
    }
    cardReaderSession.release(this@BaseCardActivity)
  }

  /** Displays the error corresponding to the status returned by the server for the given card. */
  fun showStatusError(card: CardInfo, status: RemoteServiceStatus) {
    when (status) {
      RemoteServiceStatus.CARD_COMMUNICATION_ERROR -> showCardCommunicationError()
      RemoteServiceStatus.SERVER_ERROR -> showServerError()
      RemoteServiceStatus.CARD_REJECTED ->
          showInvalidCard(
              card.description,
              if (card.isStorageCard) getString(R.string.storage_card_invalid)
              else
                  String.format(
                      getString(R.string.card_invalid_structure), card.applicationSubtype))
      RemoteServiceStatus.CARD_NOT_PERSONALIZED ->
          showInvalidCard(card.description, getString(R.string.rejection_environment_wrong_version))
      RemoteServiceStatus.ENVIRONMENT_EXPIRED ->
          showInvalidCard(card.description, getString(R.string.rejection_environment_expired))
      RemoteServiceStatus.DIFFERENT_CARD ->
          showInvalidCard(card.description, getString(R.string.not_the_same_card))
      RemoteServiceStatus.CARD_FULL ->
          showInvalidCard(card.description, getString(R.string.card_full))
      RemoteServiceStatus.SUCCESS -> {
        // Not an error
      }
    }
  }

  fun showInvalidCard(cardType: String, message: String) {
    runOnUiThread {
      displayResult(
          UiCardResult(ResultType.INVALID_CARD, cardType, emptyList(), message),
          finishActivity = shouldFinishActivityAfterResult())
    }
  }

  fun showCardCommunicationError() {
    runOnUiThread {
      displayResult(
          UiCardResult(
              ResultType.ERROR, "", emptyList(), getString(R.string.card_communication_error)),
          finishActivity = shouldFinishActivityAfterResult())
    }
  }

  fun showServerError() {
    runOnUiThread {
      displayResult(
          UiCardResult(ResultType.ERROR, "", emptyList()),
          finishActivity = shouldFinishActivityAfterResult())
    }
  }

  /**
   * Displays an error with the provided text, written for the user (never the message of an
   * exception).
   */
  fun showError(message: String, finishActivity: Boolean? = false) {
    runOnUiThread {
      displayResult(
          UiCardResult(ResultType.ERROR, "", emptyList(), message), finishActivity = finishActivity)
    }
  }

  /** Only with NFC we can come back to the 'present the card' screen after a result. */
  protected fun shouldFinishActivityAfterResult(): Boolean =
      cardMedium != CardMedium.CONTACTLESS_CARD

  protected abstract fun displayResult(
      cardResult: UiCardResult,
      applicationSerialNumber: String? = null,
      finishActivity: Boolean? = false
  )

  override fun onReaderObservationError(contextInfo: String?, readerName: String?, e: Throwable?) {
    Timber.e(e, "Reader observation error [context=$contextInfo, reader=$readerName]")
    this@BaseCardActivity.finish()
  }

  protected abstract fun initReaders()

  companion object {
    const val APPLICATION_SERIAL_NUMBER = "applicationSerialNumber"
    const val CARD_CONTENT = "cardContent"
  }
}
