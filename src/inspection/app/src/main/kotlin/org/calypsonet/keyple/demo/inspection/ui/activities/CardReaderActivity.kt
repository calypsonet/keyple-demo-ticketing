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
package org.calypsonet.keyple.demo.inspection.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.inspection.R
import org.calypsonet.keyple.demo.inspection.databinding.ActivityCardReaderBinding
import org.calypsonet.keyple.demo.inspection.databinding.LayoutCardContentOverlayBinding
import org.calypsonet.keyple.demo.inspection.databinding.LayoutInvalidCardOverlayBinding
import org.calypsonet.keyple.demo.inspection.databinding.ToolbarBinding
import org.calypsonet.keyple.demo.inspection.domain.model.AuthenticationMode
import org.calypsonet.keyple.demo.inspection.domain.model.Contract
import org.calypsonet.keyple.demo.inspection.domain.model.InspectionResult
import org.calypsonet.keyple.demo.inspection.domain.model.RejectionReason
import org.calypsonet.keyple.demo.inspection.domain.model.SelectionError
import org.calypsonet.keyple.demo.inspection.domain.model.TechnicalError
import org.calypsonet.keyple.demo.inspection.domain.model.Validation
import org.calypsonet.keyple.demo.inspection.domain.usecases.InspectCard
import org.calypsonet.keyple.demo.inspection.ui.ActivityUiContext
import org.calypsonet.keyple.demo.inspection.ui.activities.cardcontent.ContractsRecyclerAdapter
import org.calypsonet.keyple.demo.inspection.ui.activities.cardcontent.ValidationsRecyclerAdapter
import org.calypsonet.keyple.demo.inspection.ui.setDivider
import org.eclipse.keypop.reader.CardReaderEvent
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

/**
 * Card reader screen: waits for a card, inspects it, then displays the result of the inspection
 * (card content or invalid card) in an overlay of the screen, until a new inspection is requested.
 */
@AndroidEntryPoint
class CardReaderActivity : BaseActivity() {

  @Inject lateinit var inspectCard: InspectCard

  private lateinit var activityCardReaderBinding: ActivityCardReaderBinding
  private lateinit var toolbarBinding: ToolbarBinding
  private lateinit var cardContentBinding: LayoutCardContentOverlayBinding
  private lateinit var invalidCardBinding: LayoutInvalidCardOverlayBinding

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
    onBackPressedDispatcher.addCallback(this, backCallback)

    // The result overlays are inflated in advance, so that the result is displayed at once
    cardContentBinding =
        LayoutCardContentOverlayBinding.bind(activityCardReaderBinding.cardContentStub.inflate())
    cardContentBinding.contractsList.layoutManager = LinearLayoutManager(this)
    cardContentBinding.lastValidationList.layoutManager = LinearLayoutManager(this)
    cardContentBinding.lastValidationList.setDivider(R.drawable.recycler_view_divider)
    cardContentBinding.presentBtn.setOnClickListener { hideResult() }
    invalidCardBinding =
        LayoutInvalidCardOverlayBinding.bind(activityCardReaderBinding.invalidCardStub.inflate())
    invalidCardBinding.appBarLayout.toolbarLogo.setImageResource(R.drawable.ic_logo_white)
    invalidCardBinding.presentBtn.setOnClickListener { hideResult() }
  }

  override fun onResume() {
    super.onResume()
    activityCardReaderBinding.loadingAnimation.playAnimation()
    if (!cardReaderSession.isInitialized) {
      lifecycleScope.launch(Dispatchers.Default) {
        withContext(readerDispatcher) {
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
            withContext(Dispatchers.Main) { showReaderInitializationErrorDialog() }
          }
        }
      }
    } else {
      readerScope.launch { cardReaderSession.startCardDetection() }
    }
  }

  override fun onPause() {
    super.onPause()
    activityCardReaderBinding.loadingAnimation.cancelAnimation()
    // The screen is displayed again ready for a new inspection
    if (isResultDisplayed) {
      hideResult()
    }
    // Stopped outside the main thread: the stop may be long (about 440 ms on the Bluebird
    // terminals, end of the monitoring of the card removal)
    readerScope.launch {
      if (cardReaderSession.isInitialized) {
        cardReaderSession.stopCardDetection()
        Timber.d("Card detection stopped")
      }
    }
  }

  override fun onDestroy() {
    val observer = cardReaderObserver
    readerScope.launch { cardReaderSession.release(observer) }
    cardReaderObserver = null
    super.onDestroy()
  }

  /** Back hides the result of the inspection when it is displayed, otherwise closes the screen. */
  private val backCallback =
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (isResultDisplayed) {
            hideResult()
          } else {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
          }
        }
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
                // Select and inspect the card
                val inspectionResult =
                    withContext(Dispatchers.IO) {
                      inspectCard(readerEvent.scheduledCardSelectionsResponse)
                    }
                withContext(Dispatchers.Main) {
                  if (inspectionResult is InspectionResult.CardContent) {
                    when (inspectionResult.authenticationMode) {
                      AuthenticationMode.SAM ->
                          showToast(getString(R.string.authentication_mode_sam))
                      AuthenticationMode.PKI ->
                          showToast(getString(R.string.authentication_mode_pki))
                      AuthenticationMode.NO_AUTHENTICATION ->
                          showToast(getString(R.string.authentication_mode_no_authentication))
                    }
                  }
                  displayResult(inspectionResult)
                }
              } catch (e: CancellationException) {
                // The activity has been destroyed
                throw e
              } catch (e: IllegalStateException) {
                Timber.e(e, "Error while displaying the inspection result")
                withContext(Dispatchers.Main) {
                  displayResult(InspectionResult.Failed(TechnicalError.UNEXPECTED))
                }
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

  /**
   * Displays the result of the inspection above the card reader screen. The processing of the card
   * is ended when the result is hidden, the next card being detected after a new inspection is
   * requested.
   */
  private fun displayResult(result: InspectionResult) {
    activityCardReaderBinding.loadingAnimation.cancelAnimation()
    when (result) {
      is InspectionResult.CardContent -> showCardContent(result.contracts, result.lastValidation)
      InspectionResult.EmptyCard -> showCardContent(emptyList(), null)
      is InspectionResult.NotRecognized -> showInvalidCard(selectionErrorMessage(result))
      is InspectionResult.Rejected -> showInvalidCard(rejectionMessage(result.reason))
      is InspectionResult.Failed -> showInvalidCard(errorMessage(result.error))
    }
  }

  /** Shows the content of the card: its contracts and its last validation, if any. */
  private fun showCardContent(contracts: List<Contract>, lastValidation: Validation?) {
    cardContentBinding.contractsList.adapter = ContractsRecyclerAdapter(contracts)
    cardContentBinding.contractsList.visibility =
        if (contracts.isNotEmpty()) View.VISIBLE else View.GONE
    cardContentBinding.emptyContract.visibility =
        if (contracts.isEmpty()) View.VISIBLE else View.GONE
    if (lastValidation != null) {
      cardContentBinding.lastValidationList.adapter =
          ValidationsRecyclerAdapter(listOf(lastValidation))
      cardContentBinding.lastValidationListContainer.visibility = View.VISIBLE
    } else {
      cardContentBinding.lastValidationListContainer.visibility = View.GONE
    }
    cardContentBinding.root.visibility = View.VISIBLE
  }

  /** Shows that the card is invalid, with the reason why. */
  private fun showInvalidCard(message: String) {
    invalidCardBinding.invalidDescription.text = message
    invalidCardBinding.root.visibility = View.VISIBLE
    invalidCardBinding.animation.playAnimation()
  }

  /**
   * Hides the result of the inspection and ends the processing of the card, for a new inspection.
   */
  private fun hideResult() {
    cardContentBinding.root.visibility = View.GONE
    invalidCardBinding.root.visibility = View.GONE
    invalidCardBinding.animation.cancelAnimation()
    activityCardReaderBinding.loadingAnimation.playAnimation()
    readerScope.launch { cardReaderSession.endCardProcessing() }
  }

  /** Indicates whether the result of an inspection is displayed. */
  private val isResultDisplayed: Boolean
    get() =
        cardContentBinding.root.visibility == View.VISIBLE ||
            invalidCardBinding.root.visibility == View.VISIBLE

  /** Returns the message displayed when the presented card cannot be selected. */
  private fun selectionErrorMessage(result: InspectionResult.NotRecognized): String =
      when (result.error) {
        SelectionError.CARD_NOT_RECOGNIZED -> getString(R.string.selection_card_not_recognized)
        SelectionError.UNEXPECTED_DF_NAME -> getString(R.string.selection_unexpected_df_name)
        SelectionError.UNSUPPORTED_FILE_STRUCTURE ->
            getString(R.string.selection_unsupported_file_structure, result.fileStructure)
      }

  /** Returns the message displayed for the reason why the card is rejected. */
  private fun rejectionMessage(reason: RejectionReason): String =
      getString(
          when (reason) {
            RejectionReason.ENVIRONMENT_WRONG_VERSION ->
                R.string.rejection_environment_wrong_version
            RejectionReason.ENVIRONMENT_EXPIRED -> R.string.rejection_environment_expired
            RejectionReason.EVENT_WRONG_VERSION -> R.string.rejection_event_wrong_version
            RejectionReason.CONTRACT_WRONG_VERSION -> R.string.rejection_contract_wrong_version
          })

  /** Returns the message displayed for a technical error. */
  private fun errorMessage(error: TechnicalError): String =
      getString(
          when (error) {
            TechnicalError.MIFARE_CLASSIC_AUTHENTICATION_FAILED ->
                R.string.error_mifare_classic_authentication
            TechnicalError.MIFARE_CLASSIC_READING_FAILED -> R.string.error_mifare_classic_reading
            TechnicalError.UNEXPECTED -> R.string.error_card_reading
          })

  /** Displays the failure of the initialization of the readers, the application being then quit. */
  private fun showReaderInitializationErrorDialog() {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(R.string.error_title)
    builder.setMessage(R.string.reader_initialization_error)
    builder.setNegativeButton(R.string.quit) { _, _ -> finish() }
    val dialog = builder.create()
    dialog.setCancelable(false)
    dialog.show()
  }

  companion object {
    /**
     * Thread of the operations on the readers (initialization, start and stop of the card
     * detection, end of the card processing, release), executed one after the other in their order
     * of request.
     */
    private val readerDispatcher = Dispatchers.IO.limitedParallelism(1)

    /** Scope of the operations on the readers which must be completed after the screen is left. */
    private val readerScope = CoroutineScope(SupervisorJob() + readerDispatcher)
  }

  private inner class CardReaderObserver : CardReaderObserverSpi {

    override fun onReaderEvent(readerEvent: CardReaderEvent?) {
      Timber.i("Reader event received [type=${readerEvent?.type?.name}]")
      handleAppEvents(currentAppState, readerEvent)
    }
  }
}
