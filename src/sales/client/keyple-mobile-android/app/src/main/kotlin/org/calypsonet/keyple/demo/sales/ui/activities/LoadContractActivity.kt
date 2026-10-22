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
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.sales.R
import org.calypsonet.keyple.demo.sales.databinding.ActivityCardReaderBinding
import org.calypsonet.keyple.demo.sales.domain.usecases.LoadContract
import org.calypsonet.keyple.demo.sales.ui.model.ResultType
import org.calypsonet.keyple.demo.sales.ui.model.UiCardResult
import org.eclipse.keypop.reader.CardReaderEvent
import timber.log.Timber

@AndroidEntryPoint
class LoadContractActivity : BaseCardActivity() {

  @Inject lateinit var loadContract: LoadContract
  private lateinit var activityCardReaderBinding: ActivityCardReaderBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCardReaderBinding = ActivityCardReaderBinding.inflate(layoutInflater)
    toolbarBinding = activityCardReaderBinding.appBarLayout
    setContentView(activityCardReaderBinding.root)
  }

  override fun initReaders() {
    try {
      showPresentCardInstructions()
      initCardReader()
    } catch (e: Exception) {
      Timber.e(e, "Error while initializing the readers")
    }
  }

  override fun onPause() {
    activityCardReaderBinding.cardAnimation.cancelAnimation()
    activityCardReaderBinding.loadingAnimation.cancelAnimation()
    releaseCardReader()
    super.onPause()
  }

  override fun onReaderEvent(event: CardReaderEvent?) {
    if (event?.type == CardReaderEvent.Type.CARD_INSERTED) {
      runOnUiThread { showLoadingInProgress() }
      lifecycleScope.launch(Dispatchers.Default) { executeRemoteService() }
    }
  }

  private suspend fun executeRemoteService() {
    withContext(Dispatchers.IO) {
      try {
        val tripsToLoad = intent.getIntExtra(SelectProductActivity.TRIPS_TO_LOAD, 0)
        val result =
            loadContract(
                intent.getStringExtra(APPLICATION_SERIAL_NUMBER),
                PriorityCode.fromCode(intent.getIntExtra(SelectProductActivity.CONTRACT_TARIFF, 0)),
                tripsToLoad)
        if (result.status == RemoteServiceStatus.SUCCESS) {
          runOnUiThread {
            displayResult(
                UiCardResult(ResultType.SUCCESS, result.card.description, emptyList()),
                finishActivity = true)
          }
        } else {
          showStatusError(result.card, result.status)
        }
      } catch (e: IllegalStateException) {
        Timber.w("Card not supported while loading the contract [cause=${e.message}]")
        showInvalidCard(getString(R.string.undetermined_card_type), "")
      } catch (e: Exception) {
        Timber.e(e, "Error while loading the contract")
        showError(getString(R.string.error_label))
      } finally {
        cardReaderSession.endCardProcessing()
      }
    }
  }

  override fun displayResult(
      cardResult: UiCardResult,
      applicationSerialNumber: String?,
      finishActivity: Boolean?
  ) {
    activityCardReaderBinding.loadingAnimation.cancelAnimation()
    activityCardReaderBinding.cardAnimation.cancelAnimation()
    val intent = Intent(this, CardOperationResultActivity::class.java)
    intent.putExtra(CardOperationResultActivity.RESULT_TYPE, cardResult.resultType.name)
    intent.putExtra(CardOperationResultActivity.MESSAGE, cardResult.errorMessage)
    intent.putExtra(CARD_RESULT, cardResult)
    intent.putExtra(APPLICATION_SERIAL_NUMBER, applicationSerialNumber)
    startActivity(intent)
    if (finishActivity == true) {
      finish()
    }
  }

  private fun showPresentCardInstructions() {
    activityCardReaderBinding.presentTxt.text = getString(R.string.present_card)
    activityCardReaderBinding.cardAnimation.visibility = View.VISIBLE
    activityCardReaderBinding.cardAnimation.playAnimation()
    activityCardReaderBinding.loadingAnimation.cancelAnimation()
    activityCardReaderBinding.loadingAnimation.visibility = View.INVISIBLE
  }

  private fun showLoadingInProgress() {
    activityCardReaderBinding.presentTxt.text = getString(R.string.loading_in_progress)
    activityCardReaderBinding.loadingAnimation.visibility = View.VISIBLE
    activityCardReaderBinding.loadingAnimation.playAnimation()
    activityCardReaderBinding.cardAnimation.cancelAnimation()
    activityCardReaderBinding.cardAnimation.visibility = View.INVISIBLE
  }
}
