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

import android.content.Intent
import android.nfc.NfcManager
import android.os.Bundle
import android.view.View
import java.lang.IllegalStateException
import kotlin.Exception
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityCardReaderBinding
import org.calypsonet.keyple.demo.reload.remote.di.scopes.ActivityScoped
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceEnum
import org.calypsonet.keyple.demo.reload.remote.domain.model.Status
import org.calypsonet.keyple.demo.reload.remote.ui.activities.cardsummary.CardSummaryActivity
import org.calypsonet.keyple.demo.reload.remote.ui.mappers.toUi
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiCardReaderResponse
import org.eclipse.keypop.reader.CardReaderEvent
import org.eclipse.keypop.reader.ReaderCommunicationException
import timber.log.Timber

@ActivityScoped
class CardReaderActivity : AbstractCardActivity() {

  private lateinit var activityCardReaderBinding: ActivityCardReaderBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCardReaderBinding = ActivityCardReaderBinding.inflate(layoutInflater)
    toolbarBinding = activityCardReaderBinding.appBarLayout
    setContentView(activityCardReaderBinding.root)
  }

  override fun initReaders() {
    try {
      when (device) {
        DeviceEnum.CONTACTLESS_CARD -> {
          if (!isBluebirdDevice) {
            val nfcManager = getSystemService(NFC_SERVICE) as NfcManager
            if (nfcManager.defaultAdapter?.isEnabled == true) {
              showPresentNfcCardInstructions()
              initAndActivateCardReader()
            } else {
              launchExceptionResponse(
                  IllegalStateException(getString(R.string.nfc_not_activated)),
                  finishActivity = true)
            }
          } else {
            showPresentNfcCardInstructions()
            initAndActivateCardReader()
          }
        }
        DeviceEnum.SIM -> {
          showNowLoadingInformation()
          initOmapiReader { GlobalScope.launch { remoteServiceExecution() } }
        }
        DeviceEnum.WEARABLE -> {
          throw UnsupportedOperationException("Wearable")
        }
        DeviceEnum.EMBEDDED -> {
          throw UnsupportedOperationException("Embedded")
        }
      }
    } catch (e: ReaderCommunicationException) {
      Timber.e(e)
      launchExceptionResponse(e, true)
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  override fun onPause() {
    activityCardReaderBinding.cardAnimation.cancelAnimation()
    activityCardReaderBinding.loadingAnimation.cancelAnimation()
    try {
      deactivateAndClearReader()
    } catch (e: Exception) {
      Timber.e(e)
    }
    super.onPause()
  }

  override fun onReaderEvent(event: CardReaderEvent?) {
    if (event?.type == CardReaderEvent.Type.CARD_INSERTED) {
      // We'll select Card when SmartCard is presented in field
      runOnUiThread { showNowLoadingInformation() }
      GlobalScope.launch { remoteServiceExecution() }
    }
  }

  private suspend fun remoteServiceExecution() {
    withContext(Dispatchers.IO) {
      try {
        val result = ticketingService.readCardContracts()
        if (result.status == RemoteServiceStatus.SUCCESS) {
          runOnUiThread {
            val status = if (result.titles.isNotEmpty()) Status.TICKETS_FOUND else Status.EMPTY_CARD
            changeDisplay(
                UiCardReaderResponse(
                    status,
                    result.card.description,
                    result.titles.size,
                    result.titles.map { it.toUi(resources) },
                    arrayListOf(),
                    ""),
                result.card.serialNumber,
                isFinishActivityAfterResult())
          }
        } else {
          launchStatusErrorResponse(result.card, result.status)
        }
      } catch (e: IllegalStateException) {
        Timber.e(e)
        launchInvalidCardResponse(getString(R.string.undetermined_card_type), e.message!!)
      } catch (e: Exception) {
        Timber.e(e)
        launchExceptionResponse(
            IllegalStateException(getString(R.string.server_error, e.message)),
            isFinishActivityAfterResult())
      } finally {
        ticketingService.endCardProcessing()
      }
    }
  }

  override fun changeDisplay(
      cardReaderResponse: UiCardReaderResponse,
      applicationSerialNumber: String?,
      finishActivity: Boolean?
  ) {
    activityCardReaderBinding.loadingAnimation.cancelAnimation()
    activityCardReaderBinding.cardAnimation.cancelAnimation()
    val intent = Intent(this, CardSummaryActivity::class.java)
    intent.putExtra(CARD_CONTENT, cardReaderResponse)
    intent.putExtra(CARD_APPLICATION_NUMBER, applicationSerialNumber)
    startActivity(intent)
    if (finishActivity == true) {
      finish()
    }
  }

  private fun showPresentNfcCardInstructions() {
    activityCardReaderBinding.presentTxt.text = getString(R.string.present_travel_card_label)
    activityCardReaderBinding.cardAnimation.visibility = View.VISIBLE
    activityCardReaderBinding.cardAnimation.playAnimation()
    activityCardReaderBinding.loadingAnimation.cancelAnimation()
    activityCardReaderBinding.loadingAnimation.visibility = View.INVISIBLE
  }

  private fun showNowLoadingInformation() {
    activityCardReaderBinding.presentTxt.text = getString(R.string.read_in_progress)
    activityCardReaderBinding.loadingAnimation.visibility = View.VISIBLE
    activityCardReaderBinding.loadingAnimation.playAnimation()
    activityCardReaderBinding.cardAnimation.cancelAnimation()
    activityCardReaderBinding.cardAnimation.visibility = View.INVISIBLE
  }
}
