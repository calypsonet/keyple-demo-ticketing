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
import java.lang.Exception
import java.lang.IllegalStateException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityPersonalizationBinding
import org.calypsonet.keyple.demo.reload.remote.di.scopes.ActivityScoped
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceEnum
import org.calypsonet.keyple.demo.reload.remote.domain.model.Status
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiCardReaderResponse
import org.eclipse.keypop.reader.CardReaderEvent
import timber.log.Timber

@ActivityScoped
class PersonalizationActivity : AbstractCardActivity() {
  private lateinit var activityPersonalizationBinding: ActivityPersonalizationBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityPersonalizationBinding = ActivityPersonalizationBinding.inflate(layoutInflater)
    toolbarBinding = activityPersonalizationBinding.appBarLayout
    setContentView(activityPersonalizationBinding.root)
  }

  override fun initReaders() {
    try {
      if (device == DeviceEnum.CONTACTLESS_CARD) {
        showPresentNfcCardInstructions()
        initAndActivateCardReader()
      } else {
        showNowPersonalizingInformation()
        initOmapiReader { lifecycleScope.launch(Dispatchers.Default) { remoteServiceExecution() } }
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  override fun onPause() {
    activityPersonalizationBinding.cardAnimation.cancelAnimation()
    activityPersonalizationBinding.loadingAnimation.cancelAnimation()
    try {
      deactivateAndClearReader()
    } catch (e: Exception) {
      Timber.e(e)
    }
    super.onPause()
  }

  private fun showPresentNfcCardInstructions() {
    activityPersonalizationBinding.presentTxt.text =
        getString(R.string.present_card_personalization)
    activityPersonalizationBinding.cardAnimation.visibility = View.VISIBLE
    activityPersonalizationBinding.cardAnimation.playAnimation()
    activityPersonalizationBinding.loadingAnimation.cancelAnimation()
    activityPersonalizationBinding.loadingAnimation.visibility = View.INVISIBLE
  }

  private fun showNowPersonalizingInformation() {
    activityPersonalizationBinding.presentTxt.text = getString(R.string.personalization_in_progress)
    activityPersonalizationBinding.loadingAnimation.visibility = View.VISIBLE
    activityPersonalizationBinding.loadingAnimation.playAnimation()
    activityPersonalizationBinding.cardAnimation.cancelAnimation()
    activityPersonalizationBinding.cardAnimation.visibility = View.INVISIBLE
  }

  override fun changeDisplay(
      cardReaderResponse: UiCardReaderResponse,
      applicationSerialNumber: String?,
      finishActivity: Boolean?
  ) {
    val intent = Intent(this, ReloadResultActivity::class.java)
    intent.putExtra(ReloadResultActivity.IS_PERSONALIZATION_RESULT, true)
    intent.putExtra(ReloadResultActivity.STATUS, cardReaderResponse.status.name)
    intent.putExtra(ReloadResultActivity.MESSAGE, cardReaderResponse.errorMessage)
    intent.putExtra(CARD_CONTENT, cardReaderResponse)
    intent.putExtra(CARD_APPLICATION_NUMBER, applicationSerialNumber)
    startActivity(intent)
    if (finishActivity == true) {
      finish()
    }
  }

  override fun onReaderEvent(event: CardReaderEvent?) {
    if (event?.type == CardReaderEvent.Type.CARD_INSERTED) {
      runOnUiThread { showNowPersonalizingInformation() }
      lifecycleScope.launch(Dispatchers.Default) { remoteServiceExecution() }
    }
  }

  private suspend fun remoteServiceExecution() {
    withContext(Dispatchers.IO) {
      try {
        val result = ticketingService.personalizeCard()
        if (result.status == RemoteServiceStatus.SUCCESS) {
          runOnUiThread {
            changeDisplay(
                UiCardReaderResponse(Status.SUCCESS, result.card.description, arrayListOf()),
                applicationSerialNumber = result.card.serialNumber,
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
}
