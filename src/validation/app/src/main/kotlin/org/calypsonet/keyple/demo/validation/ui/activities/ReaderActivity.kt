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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.validation.R
import org.calypsonet.keyple.demo.validation.databinding.ActivityCardReaderBinding
import org.calypsonet.keyple.demo.validation.databinding.LayoutCardSummaryOverlayBinding
import org.calypsonet.keyple.demo.validation.di.scope.ActivityScoped
import org.calypsonet.keyple.demo.validation.domain.model.AppSettings
import org.calypsonet.keyple.demo.validation.domain.model.ReaderType
import org.calypsonet.keyple.demo.validation.domain.model.Status
import org.calypsonet.keyple.demo.validation.ui.adapters.UiContextImpl
import org.calypsonet.keyple.demo.validation.ui.mappers.toUi
import org.calypsonet.keyple.demo.validation.ui.model.UiValidationResult
import org.eclipse.keypop.reader.CardReaderEvent
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

@ActivityScoped
class ReaderActivity : BaseActivity() {

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
        LayoutCardSummaryOverlayBinding.bind(activityCardReaderBinding.cardSummaryStub!!.inflate())

    // Pre-warm the IO thread pool to eliminate the ~145ms dispatch gap on first card detection
    lifecycleScope.launch(Dispatchers.IO) {}
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

    if (!ticketingService.areReadersInitialized) {
      lifecycleScope.launch {
        withContext(Dispatchers.Main) { showProgress() }
        withContext(Dispatchers.IO) {
          try {
            cardReaderObserver = CardReaderObserver()
            ticketingService.init(
                cardReaderObserver, AppSettings.readerType, UiContextImpl(this@ReaderActivity))
            handleAppEvents(AppState.WAIT_CARD, null)
            ticketingService.startNfcDetection()
            ticketingService.displayWaiting()
          } catch (e: Exception) {
            Timber.e(e)
            withContext(Dispatchers.Main) {
              dismissProgress()
              showNoProxyReaderDialog(e)
            }
          }
        }
        if (ticketingService.areReadersInitialized) {
          withContext(Dispatchers.Main) { dismissProgress() }
        }
      }
    } else {
      ticketingService.displayWaiting()
      ticketingService.startNfcDetection()
    }
    if (AppSettings.batteryPowered) {
      timer = Timer() // Need to reinit timer after cancel
      timer.schedule(
          object : TimerTask() {
            override fun run() {
              runOnUiThread { onBackPressed() }
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
    if (ticketingService.areReadersInitialized) {
      ticketingService.stopNfcDetection()
      Timber.d("stopNfcDetection")
    }
  }

  override fun onDestroy() {
    timer.cancel()
    ticketingService.onDestroy(cardReaderObserver)
    cardReaderObserver = null
    super.onDestroy()
  }

  @Suppress("OVERRIDE_DEPRECATION")
  override fun onBackPressed() {
    if (summaryBinding?.root?.visibility == View.VISIBLE) {
      hideSummaryOverlay()
    } else {
      super.onBackPressed()
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
        ticketingService.analyseSelectionResult(readerEvent.scheduledCardSelectionsResponse)
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
                  withContext(Dispatchers.IO) { ticketingService.executeValidationProcedure() }
              if (validationResult.status == Status.CARD_LOST) {
                // Card removed during transaction: silent reset, no display, no sound
                currentAppState = AppState.WAIT_CARD
                playWaitingAnimation()
              } else {
                changeDisplay(validationResult.toUi())
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

  private fun changeDisplay(validationResult: UiValidationResult?) {
    if (validationResult != null) {
      if (validationResult.status === Status.PROCESSING) {
        activityCardReaderBinding.presentCardTv.visibility = View.GONE
        activityCardReaderBinding.mainView.setBackgroundColor(
            ContextCompat.getColor(this, R.color.turquoise))
        supportActionBar?.show()
        playWaitingAnimation()
      } else {
        activityCardReaderBinding.animation.cancelAnimation()
        showSummaryOverlay(validationResult)
      }
    } else {
      activityCardReaderBinding.presentCardTv.visibility = View.VISIBLE
    }
  }

  private fun showSummaryOverlay(result: UiValidationResult) {
    val b = summaryBinding!!

    // Card type label + transaction time
    val elapsedMs = System.currentTimeMillis() - cardInsertedAt
    if (result.cardType.isNotBlank()) {
      b.cardTypeLabel.visibility = View.VISIBLE
      b.cardTypeLabel.text = getString(R.string.card_type, result.cardType) + " • ${elapsedMs} ms"
    } else {
      b.cardTypeLabel.visibility = View.GONE
    }

    val animationFile: String
    when (result.status) {
      Status.SUCCESS -> {
        ticketingService.displayResultSuccess()
        animationFile = "tick_white.json"
        b.summaryMainView.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        b.bigText.setText(R.string.valid_main_desc)
        val eventDate =
            result.eventDateTime!!.format(
                DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", Locale.ENGLISH))
        b.locationTime.text =
            getString(
                R.string.valid_location_time, result.validationData?.location?.name, eventDate)
        val nbTickets = result.nbTicketsLeft
        if (nbTickets != null) {
          b.smallDesc.text =
              when (nbTickets) {
                0 -> getString(R.string.valid_trips_left_zero)
                1 -> getString(R.string.valid_trips_left_single)
                else -> getString(R.string.valid_trips_left_multiple, nbTickets)
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
        if (result.nbTicketsLeft != null || result.passValidityEndDate != null) {
          b.smallDesc.visibility = View.VISIBLE
        }
      }
      Status.INVALID_CARD -> {
        ticketingService.displayResultFailed()
        animationFile = "error_white.json"
        b.summaryMainView.setBackgroundColor(ContextCompat.getColor(this, R.color.orange))
        b.bigText.setText(R.string.card_invalid_main_desc)
        b.locationTime.text = result.errorMessage
        b.mediumText.visibility = View.INVISIBLE
        b.smallDesc.visibility = View.INVISIBLE
      }
      Status.EMPTY_CARD -> {
        ticketingService.displayResultFailed()
        animationFile = "error_white.json"
        b.summaryMainView.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        b.bigText.text = result.errorMessage
        b.locationTime.setText(R.string.no_tickets_small_desc)
        b.mediumText.visibility = View.INVISIBLE
        b.smallDesc.visibility = View.INVISIBLE
      }
      else -> {
        ticketingService.displayResultFailed()
        animationFile = "error_white.json"
        b.summaryMainView.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        b.bigText.setText(R.string.error_main_desc)
        b.locationTime.text = result.errorMessage ?: getString(R.string.error_small_desc)
        b.mediumText.visibility = View.INVISIBLE
        b.smallDesc.visibility = View.INVISIBLE
      }
    }

    ticketingService.stopNfcDetection()
    b.summaryMainView.visibility = View.VISIBLE
    b.animation.setAnimation(animationFile)
    b.animation.playAnimation()

    lifecycleScope.launch(Dispatchers.IO) { ticketingService.initCryptoContextForNextTransaction() }

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
    ticketingService.displayWaiting()
    ticketingService.startNfcDetection()
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
    activityCardReaderBinding.progressOverlay?.visibility = View.VISIBLE
  }

  private fun dismissProgress() {
    activityCardReaderBinding.progressOverlay?.visibility = View.GONE
  }

  /**
   * Starts the waiting animation. On Arrive terminals, plays once and freezes on the last frame
   * (card at reader) with no ongoing CPU cost. On other terminals, loops indefinitely.
   */
  private fun playWaitingAnimation() {
    if (AppSettings.readerType != ReaderType.ARRIVE) {
      activityCardReaderBinding.animation.repeatCount = LottieDrawable.INFINITE
      activityCardReaderBinding.animation.playAnimation()
    } else {
      activityCardReaderBinding.animation.repeatCount = 0
      activityCardReaderBinding.animation.playAnimation()
    }
  }

  companion object {
    private const val RETURN_DELAY_MS = 30000
    private const val SUMMARY_DELAY_MS = 6000
  }

  private inner class CardReaderObserver : CardReaderObserverSpi {

    override fun onReaderEvent(readerEvent: CardReaderEvent?) {
      handleAppEvents(currentAppState, readerEvent)
    }
  }
}
