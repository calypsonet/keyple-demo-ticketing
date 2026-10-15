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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.validation.R
import org.calypsonet.keyple.demo.validation.databinding.ActivityCardReaderBinding
import org.calypsonet.keyple.demo.validation.databinding.LayoutValidationResultOverlayBinding
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
  private var autoReturnTimer = Timer()

  private var validationResultBinding: LayoutValidationResultOverlayBinding? = null
  private var validationResultTimer: Timer? = null
  private var cardDetectedAt = 0L

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

    // Pre-inflate the validation result overlay so ConstraintLayout resolves all constraints
    // (including dimensionRatio on the Lottie view) before first use, avoiding
    // GONE→VISIBLE sizing issues where the animation would cover the text views.
    validationResultBinding =
        LayoutValidationResultOverlayBinding.bind(
            activityCardReaderBinding.validationResultStub.inflate())
  }

  override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == android.R.id.home) {
      finish()
    }
    return super.onOptionsItemSelected(menuItem)
  }

  override fun onResume() {
    super.onResume()

    // If the validation result overlay is visible (e.g. app returned from background), dismiss it
    // first
    validationResultBinding?.root?.let {
      if (it.visibility == View.VISIBLE) {
        validationResultTimer?.cancel()
        validationResultTimer = null
        it.visibility = View.GONE
      }
    }

    playWaitingAnimation()

    if (!cardReaderSession.isInitialized) {
      lifecycleScope.launch {
        withContext(readerDispatcher) {
          try {
            cardReaderObserver = CardReaderObserver()
            cardReaderSession.init(
                cardReaderObserver,
                appSettings.terminalType,
                ActivityUiContext(this@CardReaderActivity))
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
    if (appSettings.isBatteryPowered) {
      autoReturnTimer = Timer() // A cancelled timer cannot be reused
      autoReturnTimer.schedule(
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
    autoReturnTimer.cancel()
    validationResultTimer?.cancel()
    validationResultTimer = null
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
    autoReturnTimer.cancel()
    val observer = cardReaderObserver
    readerScope.launch { cardReaderSession.release(observer) }
    cardReaderObserver = null
    super.onDestroy()
  }

  /**
   * Back closes the validation result overlay when it is visible, otherwise closes the activity.
   */
  private val backCallback =
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (validationResultBinding?.root?.visibility == View.VISIBLE) {
            hideResultOverlay()
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
        cardDetectedAt = System.currentTimeMillis()
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
            lifecycleScope.launch {
              activityCardReaderBinding.animation.cancelAnimation()
              val validationResult =
                  withContext(Dispatchers.IO) {
                    validateCard(readerEvent.scheduledCardSelectionsResponse)
                  }
              if (validationResult is ValidationResult.CardLost) {
                // Card removed during transaction: silent reset, no display, no sound
                currentAppState = AppState.WAIT_CARD
                readerScope.launch { cardReaderSession.endCardProcessing() }
                playWaitingAnimation()
              } else {
                displayResult(validationResult)
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

  /** Returns the label of the presented card. */
  private fun cardTypeLabel(cardDescription: CardDescription): String =
      when (cardDescription) {
        is CardDescription.Calypso -> getString(R.string.card_type_calypso, cardDescription.dfName)
        is CardDescription.Storage ->
            when (cardDescription.productType) {
              "MIFARE_CLASSIC_1K" -> getString(R.string.card_type_mifare_classic_1k)
              "MIFARE_CLASSIC_4K" -> getString(R.string.card_type_mifare_classic_4k)
              "MIFARE_ULTRALIGHT" -> getString(R.string.card_type_mifare_ultralight)
              "ST25_SRT512" -> getString(R.string.card_type_st25_srt512)
              else -> cardDescription.productType.replace('_', ' ')
            }
      }

  /**
   * Indicates whether the card is refused because of its data (displayed as an invalid card),
   * instead of having no contract to use.
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
  private fun errorMessage(result: ValidationResult.Failed): String =
      when (result.error) {
        TechnicalError.MIFARE_CLASSIC_AUTHENTICATION_FAILED ->
            getString(R.string.error_mifare_classic_authentication)
        TechnicalError.MIFARE_CLASSIC_TRANSACTION_FAILED ->
            getString(R.string.error_mifare_classic_transaction)
        TechnicalError.UNEXPECTED -> getString(R.string.error_small_desc)
      }

  /**
   * Displays the result of the validation, the waiting screen being displayed again after a delay.
   */
  private fun displayResult(result: ValidationResult) {
    activityCardReaderBinding.animation.cancelAnimation()
    val resultBinding = validationResultBinding!!

    // Card type label + transaction time
    val elapsedMs = System.currentTimeMillis() - cardDetectedAt
    val cardDescription = result.cardDescription
    if (cardDescription != null) {
      resultBinding.cardTypeLabel.visibility = View.VISIBLE
      resultBinding.cardTypeLabel.text =
          getString(R.string.card_type, cardTypeLabel(cardDescription)) + " • ${elapsedMs} ms"
    } else {
      resultBinding.cardTypeLabel.visibility = View.GONE
    }

    val animationFileName: String
    when (result) {
      is ValidationResult.Accepted -> {
        animationFileName = "tick_white.json"
        resultBinding.validationResultView.setBackgroundColor(
            ContextCompat.getColor(this, R.color.green))
        resultBinding.bigText.setText(R.string.valid_main_desc)
        val validationDateTime =
            result.dateTime.format(
                DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", Locale.ENGLISH))
        resultBinding.locationTime.text =
            getString(R.string.valid_location_time, result.location.name, validationDateTime)
        val remainingTrips = result.remainingTrips
        if (remainingTrips != null) {
          resultBinding.smallDesc.text =
              when (remainingTrips) {
                0 -> getString(R.string.valid_trips_left_zero)
                1 -> getString(R.string.valid_trips_left_single)
                else -> getString(R.string.valid_trips_left_multiple, remainingTrips)
              }
        } else if (result.seasonPassValidityEndDate != null) {
          val validityEndDate =
              result.seasonPassValidityEndDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
          resultBinding.smallDesc.text = getString(R.string.valid_season_pass, validityEndDate)
        } else {
          // Recovered broken session: no contract data available
          resultBinding.smallDesc.visibility = View.INVISIBLE
        }
        resultBinding.mediumText.setText(R.string.valid_last_desc)
        resultBinding.mediumText.visibility = View.VISIBLE
        if (result.remainingTrips != null || result.seasonPassValidityEndDate != null) {
          resultBinding.smallDesc.visibility = View.VISIBLE
        }
      }
      is ValidationResult.NotSelected -> {
        // Card not selected (unknown or unsupported card): refused without validation
        animationFileName = "error_white.json"
        resultBinding.validationResultView.setBackgroundColor(
            ContextCompat.getColor(this, R.color.orange))
        resultBinding.bigText.setText(R.string.card_invalid_main_desc)
        resultBinding.locationTime.text = selectionErrorMessage(result)
        resultBinding.mediumText.visibility = View.INVISIBLE
        resultBinding.smallDesc.visibility = View.INVISIBLE
      }
      is ValidationResult.Rejected -> {
        animationFileName = "error_white.json"
        if (isInvalidCard(result.reason)) {
          // Card refused (card data)
          resultBinding.validationResultView.setBackgroundColor(
              ContextCompat.getColor(this, R.color.orange))
          resultBinding.bigText.setText(R.string.card_invalid_main_desc)
          resultBinding.locationTime.text = rejectionMessage(result.reason)
        } else {
          // No contract to use
          resultBinding.validationResultView.setBackgroundColor(
              ContextCompat.getColor(this, R.color.red))
          resultBinding.bigText.text = rejectionMessage(result.reason)
          resultBinding.locationTime.setText(R.string.no_contract_desc)
        }
        resultBinding.mediumText.visibility = View.INVISIBLE
        resultBinding.smallDesc.visibility = View.INVISIBLE
      }
      is ValidationResult.Failed,
      is ValidationResult.CardLost -> {
        animationFileName = "error_white.json"
        resultBinding.validationResultView.setBackgroundColor(
            ContextCompat.getColor(this, R.color.red))
        resultBinding.bigText.setText(R.string.error_main_desc)
        resultBinding.locationTime.text =
            (result as? ValidationResult.Failed)?.let { errorMessage(it) }
                ?: getString(R.string.error_small_desc)
        resultBinding.mediumText.visibility = View.INVISIBLE
        resultBinding.smallDesc.visibility = View.INVISIBLE
      }
    }

    // The preparation of the next transaction is performed asynchronously after the end of the
    // previous transaction so that the time spent is not added to the user time.
    if (result !is ValidationResult.NotSelected) {
      readerScope.launch { cardReaderSession.prepareNextTransaction() }
    }

    showResultOverlay(animationFileName)
  }

  /** Returns the message displayed when the presented card cannot be selected. */
  private fun selectionErrorMessage(result: ValidationResult.NotSelected): String =
      when (result.error) {
        SelectionError.CARD_NOT_RECOGNIZED -> getString(R.string.selection_card_not_recognized)
        SelectionError.UNEXPECTED_DF_NAME -> getString(R.string.selection_unexpected_df_name)
        SelectionError.UNSUPPORTED_FILE_STRUCTURE ->
            getString(R.string.selection_unsupported_file_structure, result.fileStructure)
      }

  /** Shows the validation result overlay with the provided animation, hidden after a delay. */
  private fun showResultOverlay(animationFileName: String) {
    val resultBinding = validationResultBinding!!
    resultBinding.validationResultView.visibility = View.VISIBLE
    resultBinding.animation.setAnimation(animationFileName)
    resultBinding.animation.playAnimation()

    validationResultTimer = Timer()
    validationResultTimer!!.schedule(
        object : TimerTask() {
          override fun run() {
            runOnUiThread { hideResultOverlay() }
          }
        },
        VALIDATION_RESULT_DELAY_MS.toLong())
  }

  private fun hideResultOverlay() {
    validationResultTimer?.cancel()
    validationResultTimer = null
    validationResultBinding?.root?.visibility = View.GONE
    validationResultBinding?.animation?.cancelAnimation()
    activityCardReaderBinding.mainView.setBackgroundResource(R.drawable.ic_img_bg_valideur)
    activityCardReaderBinding.presentCardTv.visibility = View.VISIBLE
    playWaitingAnimation()
    readerScope.launch { cardReaderSession.endCardProcessing() }
  }

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
    private const val VALIDATION_RESULT_DELAY_MS = 2500

    /**
     * Thread of the operations on the readers (initialization, start and stop of the card
     * detection, preparation of the next transaction, end of the card processing, release),
     * executed one after the other in their order of request.
     */
    private val readerDispatcher = Dispatchers.IO.limitedParallelism(1)

    /** Scope of the operations on the readers which must be completed after the screen is left. */
    private val readerScope = CoroutineScope(SupervisorJob() + readerDispatcher)
  }

  private inner class CardReaderObserver : CardReaderObserverSpi {

    override fun onReaderEvent(readerEvent: CardReaderEvent?) {
      handleAppEvents(currentAppState, readerEvent)
    }
  }
}
