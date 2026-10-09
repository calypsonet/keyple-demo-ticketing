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
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Exception
import java.lang.IllegalStateException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityCardReaderBinding
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMedium
import org.calypsonet.keyple.demo.reload.remote.domain.model.Status
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiCardReaderResponse
import org.eclipse.keypop.reader.CardReaderEvent
import timber.log.Timber

@AndroidEntryPoint
class ReloadActivity : BaseCardActivity() {
  private lateinit var activityCardReaderBinding: ActivityCardReaderBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCardReaderBinding = ActivityCardReaderBinding.inflate(layoutInflater)
    toolbarBinding = activityCardReaderBinding.appBarLayout
    setContentView(activityCardReaderBinding.root)
  }

  override fun initReaders() {
    try {
      if (cardMedium == CardMedium.CONTACTLESS_CARD) {
        showPresentNfcCardInstructions()
        initAndActivateCardReader()
      } else {
        showNowLoadingInformation()
        initOmapiReader { lifecycleScope.launch(Dispatchers.Default) { remoteServiceExecution() } }
      }
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
      runOnUiThread { showNowLoadingInformation() }
      lifecycleScope.launch(Dispatchers.Default) { remoteServiceExecution() }
    }
  }

  private suspend fun remoteServiceExecution() {
    withContext(Dispatchers.IO) {
      try {
        val tripsToLoad = intent.getIntExtra(SelectTicketsActivity.TRIPS_TO_LOAD, 0)
        val result =
            ticketingService.reloadCard(
                intent.getStringExtra(CARD_APPLICATION_NUMBER),
                PriorityCode.fromCode(
                    intent.getIntExtra(SelectTicketsActivity.SELECTED_TICKET_PRIORITY_CODE, 0)),
                tripsToLoad)
        if (result.status == RemoteServiceStatus.SUCCESS) {
          runOnUiThread {
            changeDisplay(
                UiCardReaderResponse(Status.SUCCESS, result.card.description, emptyList()),
                finishActivity = true)
          }
        } else {
          launchStatusErrorResponse(result.card, result.status)
        }
      } catch (e: IllegalStateException) {
        Timber.e(e)
        launchInvalidCardResponse(getString(R.string.undetermined_card_type), e.message!!)
      } catch (e: Exception) {
        Timber.e(e)
        launchExceptionResponse(e)
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
    val intent = Intent(this, ReloadResultActivity::class.java)
    intent.putExtra(ReloadResultActivity.TRIPS_TO_LOAD, 0)
    intent.putExtra(ReloadResultActivity.STATUS, cardReaderResponse.status.name)
    intent.putExtra(ReloadResultActivity.MESSAGE, cardReaderResponse.errorMessage)
    intent.putExtra(CARD_CONTENT, cardReaderResponse)
    intent.putExtra(CARD_APPLICATION_NUMBER, applicationSerialNumber)
    startActivity(intent)
    if (finishActivity == true) {
      finish()
    }
  }

  private fun showPresentNfcCardInstructions() {
    activityCardReaderBinding.presentTxt.text = getString(R.string.present_card)
    activityCardReaderBinding.cardAnimation.visibility = View.VISIBLE
    activityCardReaderBinding.cardAnimation.playAnimation()
    activityCardReaderBinding.loadingAnimation.cancelAnimation()
    activityCardReaderBinding.loadingAnimation.visibility = View.INVISIBLE
  }

  private fun showNowLoadingInformation() {
    activityCardReaderBinding.presentTxt.text = getString(R.string.loading_in_progress)
    activityCardReaderBinding.loadingAnimation.visibility = View.VISIBLE
    activityCardReaderBinding.loadingAnimation.playAnimation()
    activityCardReaderBinding.cardAnimation.cancelAnimation()
    activityCardReaderBinding.cardAnimation.visibility = View.INVISIBLE
  }
}
