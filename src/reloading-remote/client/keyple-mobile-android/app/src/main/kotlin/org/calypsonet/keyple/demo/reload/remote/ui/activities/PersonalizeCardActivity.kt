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
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityPersonalizeCardBinding
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMedium
import org.calypsonet.keyple.demo.reload.remote.domain.usecases.PersonalizeCard
import org.calypsonet.keyple.demo.reload.remote.ui.model.ResultType
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiCardResult
import org.eclipse.keypop.reader.CardReaderEvent
import timber.log.Timber

@AndroidEntryPoint
class PersonalizeCardActivity : BaseCardActivity() {

  @Inject lateinit var personalizeCard: PersonalizeCard
  private lateinit var activityPersonalizeCardBinding: ActivityPersonalizeCardBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityPersonalizeCardBinding = ActivityPersonalizeCardBinding.inflate(layoutInflater)
    toolbarBinding = activityPersonalizeCardBinding.appBarLayout
    setContentView(activityPersonalizeCardBinding.root)
  }

  override fun initReaders() {
    try {
      if (cardMedium == CardMedium.CONTACTLESS_CARD) {
        showPresentCardInstructions()
        initCardReader()
      } else {
        showPersonalizationInProgress()
        initOmapiReader { lifecycleScope.launch(Dispatchers.Default) { executeRemoteService() } }
      }
    } catch (e: Exception) {
      Timber.e(e, "Error while initializing the readers")
    }
  }

  override fun onPause() {
    activityPersonalizeCardBinding.cardAnimation.cancelAnimation()
    activityPersonalizeCardBinding.loadingAnimation.cancelAnimation()
    releaseCardReader()
    super.onPause()
  }

  private fun showPresentCardInstructions() {
    activityPersonalizeCardBinding.presentTxt.text =
        getString(R.string.present_card_personalization)
    activityPersonalizeCardBinding.cardAnimation.visibility = View.VISIBLE
    activityPersonalizeCardBinding.cardAnimation.playAnimation()
    activityPersonalizeCardBinding.loadingAnimation.cancelAnimation()
    activityPersonalizeCardBinding.loadingAnimation.visibility = View.INVISIBLE
  }

  private fun showPersonalizationInProgress() {
    activityPersonalizeCardBinding.presentTxt.text = getString(R.string.personalization_in_progress)
    activityPersonalizeCardBinding.loadingAnimation.visibility = View.VISIBLE
    activityPersonalizeCardBinding.loadingAnimation.playAnimation()
    activityPersonalizeCardBinding.cardAnimation.cancelAnimation()
    activityPersonalizeCardBinding.cardAnimation.visibility = View.INVISIBLE
  }

  override fun displayResult(
      cardResult: UiCardResult,
      applicationSerialNumber: String?,
      finishActivity: Boolean?
  ) {
    val intent = Intent(this, CardOperationResultActivity::class.java)
    intent.putExtra(CardOperationResultActivity.IS_PERSONALIZATION_RESULT, true)
    intent.putExtra(CardOperationResultActivity.RESULT_TYPE, cardResult.resultType.name)
    intent.putExtra(CardOperationResultActivity.MESSAGE, cardResult.errorMessage)
    intent.putExtra(CARD_RESULT, cardResult)
    intent.putExtra(APPLICATION_SERIAL_NUMBER, applicationSerialNumber)
    startActivity(intent)
    if (finishActivity == true) {
      finish()
    }
  }

  override fun onReaderEvent(event: CardReaderEvent?) {
    if (event?.type == CardReaderEvent.Type.CARD_INSERTED) {
      runOnUiThread { showPersonalizationInProgress() }
      lifecycleScope.launch(Dispatchers.Default) { executeRemoteService() }
    }
  }

  private suspend fun executeRemoteService() {
    withContext(Dispatchers.IO) {
      try {
        val result = personalizeCard()
        if (result.status == RemoteServiceStatus.SUCCESS) {
          runOnUiThread {
            displayResult(
                UiCardResult(ResultType.SUCCESS, result.card.description, emptyList()),
                applicationSerialNumber = result.card.serialNumber,
                finishActivity = true)
          }
        } else {
          showStatusError(result.card, result.status)
        }
      } catch (e: IllegalStateException) {
        Timber.w("Card not supported while personalizing the card [cause=${e.message}]")
        showInvalidCard(getString(R.string.undetermined_card_type), "")
      } catch (e: Exception) {
        Timber.e(e, "Error while personalizing the card")
        showError(getString(R.string.error_label))
      } finally {
        cardReaderSession.endCardProcessing()
      }
    }
  }
}
