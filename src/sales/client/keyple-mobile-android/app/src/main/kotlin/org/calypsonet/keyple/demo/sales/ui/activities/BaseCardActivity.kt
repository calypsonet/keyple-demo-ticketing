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
package org.calypsonet.keyple.demo.sales.ui.activities

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import kotlin.jvm.Throws
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus
import org.calypsonet.keyple.demo.sales.R
import org.calypsonet.keyple.demo.sales.domain.CardReaderSession
import org.calypsonet.keyple.demo.sales.domain.model.CardInfo
import org.calypsonet.keyple.demo.sales.domain.model.TerminalType
import org.calypsonet.keyple.demo.sales.ui.ActivityUiContext
import org.calypsonet.keyple.demo.sales.ui.model.ResultType
import org.calypsonet.keyple.demo.sales.ui.model.UiCardResult
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

abstract class BaseCardActivity :
    BaseActivity(), CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  @Inject lateinit var cardReaderSession: CardReaderSession

  val isBluebirdTerminal = Build.MANUFACTURER?.lowercase()?.contains("bluebird") == true

  private val terminalType: TerminalType
    get() = if (isBluebirdTerminal) TerminalType.BLUEBIRD else TerminalType.NFC_TERMINAL

  override fun onResume() {
    super.onResume()
    lifecycleScope.launch {
      // The reader released by the previous screen, in background, is initialized again once
      // released (immediately, unless the release is still in progress)
      pendingRelease?.join()
      if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        initReaders()
      }
    }
  }

  /** Android NFC Reader is strongly dependent and Android Activity component. */
  @Throws(UnsupportedOperationException::class)
  fun initCardReader() {
    cardReaderSession.init(
        terminalType,
        ActivityUiContext(this@BaseCardActivity),
        this@BaseCardActivity,
        this@BaseCardActivity)

    cardReaderSession.startCardDetection()
  }

  /**
   * Releases the reader outside the main thread: the stop of the card detection may be long (about
   * 440 ms on the Bluebird terminals), which would delay the display of the next screen.
   */
  fun releaseCardReader() {
    val observer: CardReaderObserverSpi = this
    pendingRelease =
        readerScope.launch {
          try {
            cardReaderSession.stopCardDetection()
            cardReaderSession.release(observer)
          } catch (e: Exception) {
            Timber.e(e, "Error while releasing the readers")
          }
        }
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
      RemoteServiceStatus.EVENT_WRONG_VERSION ->
          showInvalidCard(card.description, getString(R.string.rejection_event_wrong_version))
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
      displayResult(UiCardResult(ResultType.INVALID_CARD, cardType, emptyList(), message))
    }
  }

  fun showCardCommunicationError() {
    runOnUiThread {
      displayResult(
          UiCardResult(
              ResultType.ERROR, "", emptyList(), getString(R.string.card_communication_error)))
    }
  }

  fun showServerError() {
    runOnUiThread { displayResult(UiCardResult(ResultType.ERROR, "", emptyList())) }
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
    const val CARD_RESULT = "cardResult"

    /** Thread of the releases of the reader, executed one after the other. */
    private val readerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    /** Last release of the reader, awaited before initializing it again (main thread only). */
    private var pendingRelease: Job? = null
  }
}
