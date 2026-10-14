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
package org.calypsonet.keyple.demo.control.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.databinding.ActivityCardReaderBinding
import org.calypsonet.keyple.demo.control.databinding.ToolbarBinding
import org.calypsonet.keyple.demo.control.domain.model.AuthenticationMode
import org.calypsonet.keyple.demo.control.domain.model.ControlResult
import org.calypsonet.keyple.demo.control.domain.usecases.ControlCard
import org.calypsonet.keyple.demo.control.ui.ActivityUiContext
import org.calypsonet.keyple.demo.control.ui.activities.cardcontent.CardContentActivity
import org.calypsonet.keyple.demo.control.ui.mappers.toUi
import org.calypsonet.keyple.demo.control.ui.model.ResultType
import org.calypsonet.keyple.demo.control.ui.model.UiControlResult
import org.eclipse.keypop.reader.CardReaderEvent
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

@AndroidEntryPoint
class CardReaderActivity : BaseActivity() {

  @Inject lateinit var controlCard: ControlCard

  private lateinit var activityCardReaderBinding: ActivityCardReaderBinding
  private lateinit var toolbarBinding: ToolbarBinding

  private var cardReaderObserver: CardReaderObserver? = null
  var currentAppState = AppState.WAIT_SYSTEM_READY

  // application states
  enum class AppState {
    UNSPECIFIED,
    WAIT_SYSTEM_READY,
    WAIT_CARD,
    CARD_STATUS
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCardReaderBinding = ActivityCardReaderBinding.inflate(layoutInflater)
    toolbarBinding = activityCardReaderBinding.appBarLayout
    setContentView(activityCardReaderBinding.root)
    setSupportActionBar(toolbarBinding.toolbar)
  }

  override fun onResume() {
    super.onResume()
    activityCardReaderBinding.loadingAnimation.playAnimation()
    if (!cardReaderSession.isInitialized) {
      lifecycleScope.launch(Dispatchers.Default) {
        withContext(Dispatchers.Main) { showProgress() }
        withContext(Dispatchers.IO) {
          try {
            cardReaderObserver = CardReaderObserver()
            cardReaderSession.init(
                cardReaderObserver,
                appSettings.terminalType,
                ActivityUiContext(this@CardReaderActivity))
            showToast(
                getString(
                    if (cardReaderSession.isSamAvailable) R.string.sam_available
                    else R.string.sam_not_available))
            handleAppEvents(AppState.WAIT_CARD, null)
            cardReaderSession.startCardDetection()
          } catch (e: Exception) {
            Timber.e(e, "Error while initializing the readers")
            withContext(Dispatchers.Main) {
              dismissProgress()
              showNoProxyReaderDialog(e)
            }
          }
        }
        if (cardReaderSession.isInitialized) {
          withContext(Dispatchers.Main) { dismissProgress() }
        }
      }
    } else {
      cardReaderSession.startCardDetection()
    }
  }

  override fun onPause() {
    super.onPause()
    activityCardReaderBinding.loadingAnimation.cancelAnimation()
    if (cardReaderSession.isInitialized) {
      cardReaderSession.stopCardDetection()
      Timber.d("Card detection stopped")
    }
  }

  override fun onDestroy() {
    cardReaderSession.release(cardReaderObserver)
    cardReaderObserver = null
    super.onDestroy()
  }

  /**
   * main app state machine handle
   *
   * @param appState
   * @param readerEvent
   */
  private fun handleAppEvents(appState: AppState, readerEvent: CardReaderEvent?) {
    var newAppState = appState
    Timber.d(
        "Handling a reader event [state=$currentAppState, newState=$newAppState, eventType=${readerEvent?.type}]")
    when (readerEvent?.type) {
      CardReaderEvent.Type.CARD_INSERTED,
      CardReaderEvent.Type.CARD_MATCHED -> {
        if (newAppState == AppState.WAIT_SYSTEM_READY) {
          return
        }
        Timber.i("Card presented")
        newAppState = AppState.CARD_STATUS
      }
      CardReaderEvent.Type.CARD_REMOVED -> {
        currentAppState = AppState.WAIT_SYSTEM_READY
      }
      else -> {
        Timber.w("Reader event not handled [type=${readerEvent?.type}]")
      }
    }
    when (newAppState) {
      AppState.WAIT_SYSTEM_READY,
      AppState.WAIT_CARD -> {
        currentAppState = newAppState
      }
      AppState.CARD_STATUS -> {
        currentAppState = newAppState
        when (readerEvent?.type) {
          CardReaderEvent.Type.CARD_INSERTED,
          CardReaderEvent.Type.CARD_MATCHED -> {
            lifecycleScope.launch(Dispatchers.Default) {
              try {
                // Select and control the card
                withContext(Dispatchers.Main) { showProgress() }
                val controlResult =
                    withContext(Dispatchers.IO) {
                      controlCard(readerEvent.scheduledCardSelectionsResponse)
                    }
                withContext(Dispatchers.Main) {
                  if (controlResult is ControlResult.CardContent) {
                    when (controlResult.authenticationMode) {
                      AuthenticationMode.SAM ->
                          showToast(getString(R.string.authentication_mode_sam))
                      AuthenticationMode.PKI ->
                          showToast(getString(R.string.authentication_mode_pki))
                      AuthenticationMode.NO_AUTHENTICATION ->
                          showToast(getString(R.string.authentication_mode_no_authentication))
                    }
                  }
                  dismissProgress()
                  displayResult(controlResult.toUi(resources))
                }
              } catch (e: CancellationException) {
                // The activity has been destroyed
                throw e
              } catch (e: IllegalStateException) {
                Timber.e(e, "Error while displaying the control result")
                withContext(Dispatchers.Main) { dismissProgress() }
                displayResult(
                    UiControlResult(resultType = ResultType.ERROR, contractsList = emptyList()))
              } finally {
                cardReaderSession.endCardProcessing()
              }
            }
          }
          else -> {
            // Do nothing
          }
        }
      }
      AppState.UNSPECIFIED -> {
        Toast.makeText(this, getString(R.string.status_unspecified), Toast.LENGTH_SHORT).show()
      }
    }
    Timber.d("Application state changed [state=$currentAppState]")
  }

  private fun displayResult(uiControlResult: UiControlResult?) {
    if (uiControlResult == null) {
      return
    }

    runOnUiThread { activityCardReaderBinding.loadingAnimation.cancelAnimation() }
    when (uiControlResult.resultType) {
      ResultType.CONTRACTS_FOUND,
      ResultType.EMPTY_CARD -> {
        val intent = Intent(this@CardReaderActivity, CardContentActivity::class.java)
        intent.putExtra(CARD_CONTENT, uiControlResult)
        startActivity(intent)
      }
      ResultType.ERROR,
      ResultType.INVALID_CARD -> {
        val intent = Intent(this@CardReaderActivity, InvalidCardActivity::class.java)
        intent.putExtra(CARD_CONTENT, uiControlResult)
        startActivity(intent)
      }
    }
  }

  private fun showNoProxyReaderDialog(t: Throwable) {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(R.string.error_title)
    builder.setMessage(t.message)
    builder.setNegativeButton(R.string.quit) { _, _ -> finish() }
    val dialog = builder.create()
    dialog.setCancelable(false)
    dialog.show()
  }

  private fun showProgress() {
    activityCardReaderBinding.progressOverlay.visibility = View.VISIBLE
  }

  private fun dismissProgress() {
    activityCardReaderBinding.progressOverlay.visibility = View.GONE
  }

  companion object {
    const val CARD_CONTENT = "cardContent"
  }

  private inner class CardReaderObserver : CardReaderObserverSpi {

    override fun onReaderEvent(readerEvent: CardReaderEvent?) {
      Timber.i("Reader event received [type=${readerEvent?.type?.name}]")
      handleAppEvents(currentAppState, readerEvent)
    }
  }
}
