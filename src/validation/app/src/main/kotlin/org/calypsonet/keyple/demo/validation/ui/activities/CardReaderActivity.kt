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
package org.calypsonet.keyple.demo.validation.ui.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import dagger.hilt.android.AndroidEntryPoint
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.validation.R
import org.calypsonet.keyple.demo.validation.databinding.ActivityCardReaderBinding
import org.calypsonet.keyple.demo.validation.databinding.LayoutCardSummaryOverlayBinding
import org.calypsonet.keyple.demo.validation.domain.model.CardDescription
import org.calypsonet.keyple.demo.validation.domain.model.RejectionReason
import org.calypsonet.keyple.demo.validation.domain.model.SelectionError
import org.calypsonet.keyple.demo.validation.domain.model.TechnicalError
import org.calypsonet.keyple.demo.validation.domain.model.TerminalType
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.usecases.ValidateCard
import org.calypsonet.keyple.demo.validation.ui.ActivityUiContext
import org.eclipse.keypop.reader.CardReaderEvent
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

@AndroidEntryPoint
class CardReaderActivity : BaseActivity() {

  @Inject lateinit var validateCard: ValidateCard

  private lateinit var activityCardReaderBinding: ActivityCardReaderBinding

  private var cardReaderObserver: CardReaderObserver? = null
  var currentAppState = AppState.WAIT_SYSTEM_READY
  private var timer = Timer()

  private var summaryBinding: LayoutCardSummaryOverlayBinding? = null
  private var summaryTimer: Timer? = null
  private var cardInsertedAt = 0L

  /* application states */
  enum class AppState {
    WAIT_SYSTEM_READY,
    WAIT_CARD,
    CARD_STATUS
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCardReaderBinding = ActivityCardReaderBinding.inflate(layoutInflater)
    setContentView(activityCardReaderBinding.root)
    onBackPressedDispatcher.addCallback(this, backCallback)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    // Enable merge paths so the waiting animation renders efficiently (avoids fallback path)
    activityCardReaderBinding.animation.enableMergePathsForKitKatAndAbove(true)

    // Pre-load result animations into Lottie cache so the overlay displays instantly
    LottieCompositionFactory.fromAsset(this, "tick_white.json")
    LottieCompositionFactory.fromAsset(this, "error_white.json")

    // Pre-inflate the card summary overlay so ConstraintLayout resolves all constraints
    // (including dimensionRatio on the Lottie view) before first use, avoiding
    // GONE→VISIBLE sizing issues where the animation would cover the text views.
    summaryBinding =
        LayoutCardSummaryOverlayBinding.bind(activityCardReaderBinding.cardSummaryStub.inflate())
  }

  override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == android.R.id.home) {
      finish()
    }
    return super.onOptionsItemSelected(menuItem)
  }

  override fun onResume() {
    super.onResume()

    // If the summary overlay is visible (e.g. app returned from background), dismiss it first
    summaryBinding?.root?.let {
      if (it.visibility == View.VISIBLE) {
        summaryTimer?.cancel()
        summaryTimer = null
        it.visibility = View.GONE
      }
    }

    playWaitingAnimation()

    if (!cardReaderSession.isInitialized) {
      lifecycleScope.launch {
        withContext(Dispatchers.Main) { showProgress() }
        withContext(Dispatchers.IO) {
          try {
            cardReaderObserver = CardReaderObserver()
            cardReaderSession.init(
                cardReaderObserver,
                appSettings.terminalType,
                ActivityUiContext(this@CardReaderActivity))
            handleAppEvents(AppState.WAIT_CARD, null)
            cardReaderSession.startCardDetection()
          } catch (e: Exception) {
            Timber.e(e)
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
    if (appSettings.batteryPowered) {
      timer = Timer() // Need to reinit timer after cancel
      timer.schedule(
          object : TimerTask() {
            override fun run() {
              runOnUiThread { onBackPressedDispatcher.onBackPressed() }
            }
          },
          RETURN_DELAY_MS.toLong())
    }
  }

  override fun onPause() {
    super.onPause()
    activityCardReaderBinding.animation.cancelAnimation()
    timer.cancel()
    summaryTimer?.cancel()
    summaryTimer = null
    if (cardReaderSession.isInitialized) {
      cardReaderSession.stopCardDetection()
      Timber.d("stopCardDetection")
    }
  }

  override fun onDestroy() {
    timer.cancel()
    cardReaderSession.release(cardReaderObserver)
    cardReaderObserver = null
    super.onDestroy()
  }

  /** Back closes the summary overlay when it is visible, otherwise closes the activity. */
  private val backCallback =
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (summaryBinding?.root?.visibility == View.VISIBLE) {
            hideSummaryOverlay()
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
    when (readerEvent?.type) {
      CardReaderEvent.Type.CARD_INSERTED,
      CardReaderEvent.Type.CARD_MATCHED -> {
        if (newAppState == AppState.WAIT_SYSTEM_READY) {
          return
        }
        cardInsertedAt = System.currentTimeMillis()
        newAppState = AppState.CARD_STATUS
      }
      CardReaderEvent.Type.CARD_REMOVED -> {
        currentAppState = AppState.WAIT_SYSTEM_READY
      }
      else -> {
        Timber.w("Event type not handled.")
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
            lifecycleScope.launch {
              activityCardReaderBinding.animation.cancelAnimation()
              val validationResult =
                  withContext(Dispatchers.IO) {
                    validateCard(readerEvent.scheduledCardSelectionsResponse)
                  }
              if (validationResult is ValidationResult.CardLost) {
                // Card removed during transaction: silent reset, no display, no sound
                Timber.i("Card removed during transaction")
                currentAppState = AppState.WAIT_CARD
                cardReaderSession.endCardProcessing()
                playWaitingAnimation()
              } else {
                changeDisplay(validationResult)
              }
            }
          }
          else -> {
            // Do nothing
          }
        }
      }
    }
  }

  private fun changeDisplay(validationResult: ValidationResult) {
    activityCardReaderBinding.animation.cancelAnimation()
    showSummaryOverlay(validationResult)
  }

  /** Returns the label of the presented card. */
  private fun cardTypeLabel(card: CardDescription): String =
      when (card) {
        is CardDescription.Calypso -> getString(R.string.card_type_calypso, card.dfName)
        is CardDescription.Storage ->
            when (card.productType) {
              "MIFARE_CLASSIC_1K" -> getString(R.string.card_type_mifare_classic_1k)
              "MIFARE_CLASSIC_4K" -> getString(R.string.card_type_mifare_classic_4k)
              "MIFARE_ULTRALIGHT" -> getString(R.string.card_type_mifare_ultralight)
              "ST25_SRT512" -> getString(R.string.card_type_st25_srt512)
              else -> card.productType.replace('_', ' ')
            }
      }

  /**
   * Indicates whether the card is refused because of its data (displayed as an invalid card),
   * instead of having no ticket to use.
   */
  private fun isInvalidCard(reason: RejectionReason): Boolean =
      when (reason) {
        RejectionReason.ENVIRONMENT_WRONG_VERSION,
        RejectionReason.ENVIRONMENT_EXPIRED,
        RejectionReason.EVENT_WRONG_VERSION,
        RejectionReason.CONTRACT_WRONG_VERSION,
        RejectionReason.ALREADY_VALIDATED -> true
        RejectionReason.NO_VALID_CONTRACT,
        RejectionReason.EXPIRED_CONTRACT,
        RejectionReason.NO_TRIPS_LEFT,
        RejectionReason.CONTRACT_FORBIDDEN_OR_EXPIRED -> false
      }

  /** Returns the message displayed for the provided rejection reason. */
  private fun rejectionMessage(reason: RejectionReason): String =
      getString(
          when (reason) {
            RejectionReason.ENVIRONMENT_WRONG_VERSION ->
                R.string.rejection_environment_wrong_version
            RejectionReason.ENVIRONMENT_EXPIRED -> R.string.rejection_environment_expired
            RejectionReason.EVENT_WRONG_VERSION -> R.string.rejection_event_wrong_version
            RejectionReason.CONTRACT_WRONG_VERSION -> R.string.rejection_contract_wrong_version
            RejectionReason.ALREADY_VALIDATED -> R.string.rejection_already_validated
            RejectionReason.NO_VALID_CONTRACT -> R.string.rejection_no_valid_contract
            RejectionReason.EXPIRED_CONTRACT -> R.string.rejection_expired_contract
            RejectionReason.NO_TRIPS_LEFT -> R.string.rejection_no_trips_left
            RejectionReason.CONTRACT_FORBIDDEN_OR_EXPIRED ->
                R.string.rejection_contract_forbidden_or_expired
          })

  /** Returns the message displayed for the provided technical error. */
  private fun errorMessage(result: ValidationResult.Failed): String? =
      when (result.error) {
        TechnicalError.MIFARE_CLASSIC_AUTHENTICATION_FAILED ->
            getString(R.string.error_mifare_classic_authentication)
        TechnicalError.MIFARE_CLASSIC_TRANSACTION_FAILED ->
            getString(R.string.error_mifare_classic_transaction)
        TechnicalError.UNEXPECTED -> result.detail
      }

  private fun showSummaryOverlay(result: ValidationResult) {
    val b = summaryBinding!!

    // Card type label + transaction time
    val elapsedMs = System.currentTimeMillis() - cardInsertedAt
    val card = result.card
    if (card != null) {
      b.cardTypeLabel.visibility = View.VISIBLE
      b.cardTypeLabel.text =
          getString(R.string.card_type, cardTypeLabel(card)) + " • ${elapsedMs} ms"
    } else {
      b.cardTypeLabel.visibility = View.GONE
    }

    val animationFile: String
    when (result) {
      is ValidationResult.Accepted -> {
        animationFile = "tick_white.json"
        b.summaryMainView.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        b.bigText.setText(R.string.valid_main_desc)
        val eventDate =
            result.dateTime.format(
                DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", Locale.ENGLISH))
        b.locationTime.text =
            getString(R.string.valid_location_time, result.location.name, eventDate)
        val remainingTrips = result.remainingTrips
        if (remainingTrips != null) {
          b.smallDesc.text =
              when (remainingTrips) {
                0 -> getString(R.string.valid_trips_left_zero)
                1 -> getString(R.string.valid_trips_left_single)
                else -> getString(R.string.valid_trips_left_multiple, remainingTrips)
              }
        } else if (result.passValidityEndDate != null) {
          val validityEndDate =
              result.passValidityEndDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
          b.smallDesc.text = getString(R.string.valid_season_ticket, validityEndDate)
        } else {
          // Recovered broken session: no ticket/pass data available
          b.smallDesc.visibility = View.INVISIBLE
        }
        b.mediumText.setText(R.string.valid_last_desc)
        b.mediumText.visibility = View.VISIBLE
        if (result.remainingTrips != null || result.passValidityEndDate != null) {
          b.smallDesc.visibility = View.VISIBLE
        }
      }
      is ValidationResult.NotSelected -> {
        // Card not selected (unknown or unsupported card): refused without validation
        animationFile = "error_white.json"
        b.summaryMainView.setBackgroundColor(ContextCompat.getColor(this, R.color.orange))
        b.bigText.setText(R.string.card_invalid_main_desc)
        b.locationTime.text = selectionErrorMessage(result)
        b.mediumText.visibility = View.INVISIBLE
        b.smallDesc.visibility = View.INVISIBLE
      }
      is ValidationResult.Rejected -> {
        animationFile = "error_white.json"
        if (isInvalidCard(result.reason)) {
          // Card refused (card data)
          b.summaryMainView.setBackgroundColor(ContextCompat.getColor(this, R.color.orange))
          b.bigText.setText(R.string.card_invalid_main_desc)
          b.locationTime.text = rejectionMessage(result.reason)
        } else {
          // No ticket to use
          b.summaryMainView.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
          b.bigText.text = rejectionMessage(result.reason)
          b.locationTime.setText(R.string.no_tickets_small_desc)
        }
        b.mediumText.visibility = View.INVISIBLE
        b.smallDesc.visibility = View.INVISIBLE
      }
      is ValidationResult.Failed,
      is ValidationResult.CardLost -> {
        animationFile = "error_white.json"
        b.summaryMainView.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        b.bigText.setText(R.string.error_main_desc)
        b.locationTime.text =
            (result as? ValidationResult.Failed)?.let { errorMessage(it) }
                ?: getString(R.string.error_small_desc)
        b.mediumText.visibility = View.INVISIBLE
        b.smallDesc.visibility = View.INVISIBLE
      }
    }

    // The preparation of the next transaction is performed asynchronously after the end of the
    // previous transaction so that the time spent is not added to the user time.
    if (result !is ValidationResult.NotSelected) {
      lifecycleScope.launch(Dispatchers.IO) { cardReaderSession.prepareNextTransaction() }
    }

    displaySummary(animationFile)
  }

  /** Returns the message displayed when the presented card cannot be selected. */
  private fun selectionErrorMessage(result: ValidationResult.NotSelected): String =
      when (result.error) {
        SelectionError.CARD_NOT_RECOGNIZED -> getString(R.string.selection_card_not_recognized)
        SelectionError.UNEXPECTED_DF_NAME -> getString(R.string.selection_unexpected_df_name)
        SelectionError.UNSUPPORTED_FILE_STRUCTURE ->
            getString(R.string.selection_unsupported_file_structure, result.fileStructure)
      }

  /** Shows the summary overlay with the provided animation, hidden after a delay. */
  private fun displaySummary(animationFile: String) {
    val b = summaryBinding!!
    b.summaryMainView.visibility = View.VISIBLE
    b.animation.setAnimation(animationFile)
    b.animation.playAnimation()

    summaryTimer = Timer()
    summaryTimer!!.schedule(
        object : TimerTask() {
          override fun run() {
            runOnUiThread { hideSummaryOverlay() }
          }
        },
        SUMMARY_DELAY_MS.toLong())
  }

  private fun hideSummaryOverlay() {
    summaryTimer?.cancel()
    summaryTimer = null
    summaryBinding?.root?.visibility = View.GONE
    summaryBinding?.animation?.cancelAnimation()
    activityCardReaderBinding.mainView.setBackgroundResource(R.drawable.ic_img_bg_valideur)
    activityCardReaderBinding.presentCardTv.visibility = View.VISIBLE
    playWaitingAnimation()
    cardReaderSession.endCardProcessing()
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

  /**
   * Starts the waiting animation. On Arrive terminals, plays once and freezes on the last frame
   * (card at reader) with no ongoing CPU cost. On other terminals, loops indefinitely.
   */
  private fun playWaitingAnimation() {
    if (appSettings.terminalType != TerminalType.ARRIVE) {
      activityCardReaderBinding.animation.repeatCount = LottieDrawable.INFINITE
      activityCardReaderBinding.animation.playAnimation()
    } else {
      activityCardReaderBinding.animation.repeatCount = 0
      activityCardReaderBinding.animation.playAnimation()
    }
  }

  companion object {
    private const val RETURN_DELAY_MS = 30000
    private const val SUMMARY_DELAY_MS = 2500
  }

  private inner class CardReaderObserver : CardReaderObserverSpi {

    override fun onReaderEvent(readerEvent: CardReaderEvent?) {
      handleAppEvents(currentAppState, readerEvent)
    }
  }
}
