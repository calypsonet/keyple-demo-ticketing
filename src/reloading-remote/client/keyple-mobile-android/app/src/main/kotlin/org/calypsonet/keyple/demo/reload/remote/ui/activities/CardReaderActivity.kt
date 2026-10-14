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
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import java.lang.IllegalStateException
import javax.inject.Inject
import kotlin.Exception
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityCardReaderBinding
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMedium
import org.calypsonet.keyple.demo.reload.remote.domain.usecases.ReadContracts
import org.calypsonet.keyple.demo.reload.remote.ui.activities.cardcontent.CardContentActivity
import org.calypsonet.keyple.demo.reload.remote.ui.mappers.toUi
import org.calypsonet.keyple.demo.reload.remote.ui.model.ResultType
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiCardResult
import org.eclipse.keypop.reader.CardReaderEvent
import org.eclipse.keypop.reader.ReaderCommunicationException
import timber.log.Timber

@AndroidEntryPoint
class CardReaderActivity : BaseCardActivity() {

  @Inject lateinit var readContracts: ReadContracts

  private lateinit var activityCardReaderBinding: ActivityCardReaderBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCardReaderBinding = ActivityCardReaderBinding.inflate(layoutInflater)
    toolbarBinding = activityCardReaderBinding.appBarLayout
    setContentView(activityCardReaderBinding.root)
  }

  override fun initReaders() {
    try {
      when (cardMedium) {
        CardMedium.CONTACTLESS_CARD -> {
          if (!isBluebirdTerminal) {
            val nfcManager = getSystemService(NFC_SERVICE) as NfcManager
            if (nfcManager.defaultAdapter?.isEnabled == true) {
              showPresentCardInstructions()
              initCardReader()
            } else {
              showError(getString(R.string.nfc_not_activated), finishActivity = true)
            }
          } else {
            showPresentCardInstructions()
            initCardReader()
          }
        }
        CardMedium.SIM -> {
          showReadInProgress()
          initOmapiReader { lifecycleScope.launch(Dispatchers.Default) { executeRemoteService() } }
        }
      }
    } catch (e: ReaderCommunicationException) {
      Timber.e(e, "Reader communication error while initializing the readers")
      showError(getString(R.string.error_label), true)
    } catch (e: Exception) {
      Timber.e(e, "Error while initializing the readers")
    }
  }

  override fun onPause() {
    activityCardReaderBinding.cardAnimation.cancelAnimation()
    activityCardReaderBinding.loadingAnimation.cancelAnimation()
    try {
      releaseCardReader()
    } catch (e: Exception) {
      Timber.e(e, "Error while releasing the readers")
    }
    super.onPause()
  }

  override fun onReaderEvent(event: CardReaderEvent?) {
    if (event?.type == CardReaderEvent.Type.CARD_INSERTED) {
      // We'll select Card when SmartCard is presented in field
      runOnUiThread { showReadInProgress() }
      lifecycleScope.launch(Dispatchers.Default) { executeRemoteService() }
    }
  }

  private suspend fun executeRemoteService() {
    withContext(Dispatchers.IO) {
      try {
        val result = readContracts()
        if (result.status == RemoteServiceStatus.SUCCESS) {
          runOnUiThread {
            val resultType =
                if (result.contracts.isNotEmpty()) ResultType.CONTRACTS_FOUND
                else ResultType.EMPTY_CARD
            displayResult(
                UiCardResult(
                    resultType,
                    result.card.description,
                    result.contracts.map { it.toUi(resources) }),
                result.card.serialNumber,
                shouldFinishActivityAfterResult())
          }
        } else {
          showStatusError(result.card, result.status)
        }
      } catch (e: IllegalStateException) {
        Timber.w("Card not supported while reading the contracts [cause=${e.message}]")
        showInvalidCard(getString(R.string.undetermined_card_type), "")
      } catch (e: Exception) {
        Timber.e(e, "Error while reading the contracts")
        showError(getString(R.string.server_error), shouldFinishActivityAfterResult())
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
    val intent = Intent(this, CardContentActivity::class.java)
    intent.putExtra(CARD_CONTENT, cardResult)
    intent.putExtra(APPLICATION_SERIAL_NUMBER, applicationSerialNumber)
    startActivity(intent)
    if (finishActivity == true) {
      finish()
    }
  }

  private fun showPresentCardInstructions() {
    activityCardReaderBinding.presentTxt.text = getString(R.string.present_card_label)
    activityCardReaderBinding.cardAnimation.visibility = View.VISIBLE
    activityCardReaderBinding.cardAnimation.playAnimation()
    activityCardReaderBinding.loadingAnimation.cancelAnimation()
    activityCardReaderBinding.loadingAnimation.visibility = View.INVISIBLE
  }

  private fun showReadInProgress() {
    activityCardReaderBinding.presentTxt.text = getString(R.string.read_in_progress)
    activityCardReaderBinding.loadingAnimation.visibility = View.VISIBLE
    activityCardReaderBinding.loadingAnimation.playAnimation()
    activityCardReaderBinding.cardAnimation.cancelAnimation()
    activityCardReaderBinding.cardAnimation.visibility = View.INVISIBLE
  }
}
