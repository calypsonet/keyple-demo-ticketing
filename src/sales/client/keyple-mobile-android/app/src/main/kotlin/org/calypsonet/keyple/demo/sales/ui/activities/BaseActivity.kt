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

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.sales.R
import org.calypsonet.keyple.demo.sales.databinding.ToolbarBinding
import org.calypsonet.keyple.demo.sales.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.sales.domain.spi.ServerReadinessChecker
import timber.log.Timber

/** Each Activity of the app should show status connexion result */
abstract class BaseActivity : AppCompatActivity() {

  @Inject lateinit var appSettings: AppSettingsRepository
  @Inject lateinit var serverReadinessChecker: ServerReadinessChecker
  protected lateinit var toolbarBinding: ToolbarBinding
  private var autoReturnJob: Job? = null
  private var autoReturnProgress: ProgressBar? = null
  private var onAutoReturnCountdown: ((Int?) -> Unit)? = null

  /** True if the toolbar displays the back arrow (all the screens but the home screen). */
  protected open val showsBackArrow: Boolean = true

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    // The back arrow has the same effect as the back key of the system
    if (::toolbarBinding.isInitialized && showsBackArrow) {
      toolbarBinding.backBtn.visibility = View.VISIBLE
      toolbarBinding.backBtn.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
  }

  override fun onResume() {
    super.onResume()
    checkServerReadiness()
  }

  /**
   * Closes the screen after the provided delay, returning to the previous screen (for example, the
   * card presentation screen to try again, or the home screen).
   *
   * The countdown runs while the screen is visible: it is stopped when the screen is hidden (for
   * example, when the user leaves it with a button), and restarted from the beginning when it is
   * visible again. A pause of the screen while it stays visible (for example, a transparent window
   * of the system, as the NFC service of some terminals opens) does not stop it.
   *
   * @param delayMs The display duration, in milliseconds.
   * @param progress The bar emptied during the delay to show the remaining time, if any.
   * @param onCountdown Called with the remaining seconds every second (for example, to display them
   *   in the label of the button performing the same action), then with null if the automatic
   *   return is canceled.
   */
  protected fun scheduleAutoReturn(
      delayMs: Long,
      progress: ProgressBar? = null,
      onCountdown: ((Int?) -> Unit)? = null
  ) {
    cancelAutoReturn()
    autoReturnProgress = progress
    onAutoReturnCountdown = onCountdown
    progress?.let {
      it.max = AUTO_RETURN_PROGRESS_MAX
      it.progress = AUTO_RETURN_PROGRESS_MAX
      it.visibility = View.VISIBLE
    }
    // The bar is updated from the elapsed time, and not with an animator: the animators follow the
    // animation duration scale of the system, which some terminals disable (the bar would then be
    // empty from the start)
    autoReturnJob =
        lifecycleScope.launch {
          repeatOnLifecycle(Lifecycle.State.STARTED) {
            val start = SystemClock.elapsedRealtime()
            var displayedSeconds = -1
            while (true) {
              val remainingMs = delayMs - (SystemClock.elapsedRealtime() - start)
              if (remainingMs <= 0) break
              progress?.progress = (remainingMs * AUTO_RETURN_PROGRESS_MAX / delayMs).toInt()
              val seconds = ((remainingMs + 999) / 1000).toInt()
              if (seconds != displayedSeconds) {
                displayedSeconds = seconds
                onCountdown?.invoke(seconds)
              }
              delay(AUTO_RETURN_UPDATE_PERIOD_MS)
            }
            progress?.progress = 0
            finish()
          }
        }
  }

  /**
   * Stops the automatic return in progress, if any (before a new one), and restores the display.
   */
  private fun cancelAutoReturn() {
    if (autoReturnJob?.isActive == true) {
      autoReturnJob?.cancel()
      autoReturnProgress?.visibility = View.GONE
      onAutoReturnCountdown?.invoke(null)
    }
    autoReturnJob = null
    autoReturnProgress = null
    onAutoReturnCountdown = null
  }

  private fun updateServerReadinessIndicator() {
    if (appSettings.wasServerReady)
        toolbarBinding.serverStatus.setImageResource(R.drawable.ic_connection_success)
    else toolbarBinding.serverStatus.setImageResource(R.drawable.ic_connection_wait)
  }

  /** Checks the server readiness in background, then updates the indicator in the main thread. */
  private fun checkServerReadiness() {
    val serverConfig = appSettings.serverConfig
    lifecycleScope.launch {
      var cause = "SAM not available"
      val isServerReady =
          withContext(Dispatchers.IO) {
            try {
              serverReadinessChecker.isServerReady(serverConfig)
            } catch (e: Exception) {
              // Logged when the readiness of the server changes
              cause = e.message ?: e.toString()
              false
            }
          }
      // Only the changes of the readiness are logged, the server being checked at each screen
      if (isServerReady != appSettings.wasServerReady) {
        if (isServerReady) {
          Timber.i("Server ready [url=${serverConfig.url}]")
        } else {
          Timber.w("Server not ready [url=${serverConfig.url}, cause=$cause]")
        }
      }
      appSettings.wasServerReady = isServerReady
      updateServerReadinessIndicator()
    }
  }

  companion object {
    /** Display duration of a successful result before the automatic return. */
    const val SUCCESS_RETURN_DELAY_MS = 5000L
    /** Display duration of an error before the automatic return. */
    const val ERROR_RETURN_DELAY_MS = 5000L
    /** Resolution of the progress bar showing the remaining time. */
    private const val AUTO_RETURN_PROGRESS_MAX = 1000
    /** Update period of the progress bar showing the remaining time, in milliseconds. */
    private const val AUTO_RETURN_UPDATE_PERIOD_MS = 40L
  }
}
