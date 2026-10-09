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

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ToolbarBinding
import org.calypsonet.keyple.demo.reload.remote.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.reload.remote.domain.spi.ServerStatusProvider

/** Each Activity of the app should show status connexion result */
abstract class BaseActivity : AppCompatActivity() {

  @Inject lateinit var appSettings: AppSettingsRepository
  @Inject lateinit var serverStatusProvider: ServerStatusProvider
  protected lateinit var toolbarBinding: ToolbarBinding
  private var autoReturnJob: Job? = null
  private var autoReturnAnimator: ObjectAnimator? = null
  private var autoReturnProgress: ProgressBar? = null
  private var autoReturnCountdown: ((Int?) -> Unit)? = null

  override fun onResume() {
    super.onResume()
    checkServerStatus()
  }

  override fun onPause() {
    super.onPause()
    // The automatic return is abandoned when the user leaves the screen (e.g. with a button)
    cancelAutoReturn()
  }

  /**
   * Closes the screen after the provided delay, returning to the previous screen (e.g. the card
   * presentation screen to try again, or the home screen).
   *
   * @param delayMs The display duration, in milliseconds.
   * @param progress The bar emptied during the delay to show the remaining time, if any.
   * @param countdown Called with the remaining seconds every second (e.g. to display them in the
   *   label of the button performing the same action), then with null if the automatic return is
   *   cancelled.
   */
  protected fun scheduleAutoReturn(
      delayMs: Long,
      progress: ProgressBar? = null,
      countdown: ((Int?) -> Unit)? = null
  ) {
    cancelAutoReturn()
    autoReturnProgress = progress
    autoReturnCountdown = countdown
    progress?.let {
      it.max = AUTO_RETURN_PROGRESS_MAX
      it.visibility = View.VISIBLE
      autoReturnAnimator =
          ObjectAnimator.ofInt(it, "progress", AUTO_RETURN_PROGRESS_MAX, 0).apply {
            duration = delayMs
            interpolator = LinearInterpolator()
            start()
          }
    }
    autoReturnJob =
        lifecycleScope.launch {
          for (seconds in (delayMs / 1000).toInt() downTo 1) {
            countdown?.invoke(seconds)
            delay(1000)
          }
          delay(delayMs % 1000)
          finish()
        }
  }

  /** Stops the automatic return in progress, if any, and restores the display. */
  private fun cancelAutoReturn() {
    if (autoReturnJob?.isActive == true) {
      autoReturnJob?.cancel()
      autoReturnAnimator?.cancel()
      autoReturnProgress?.visibility = View.GONE
      autoReturnCountdown?.invoke(null)
    }
    autoReturnJob = null
    autoReturnAnimator = null
    autoReturnProgress = null
    autoReturnCountdown = null
  }

  private fun updateServerStatusIndicator() {
    if (appSettings.lastServerStatus)
        toolbarBinding.serverStatus.setImageResource(R.drawable.ic_connection_success)
    else toolbarBinding.serverStatus.setImageResource(R.drawable.ic_connection_wait)
  }

  /** Requests the server status in background, then updates the indicator in the main thread. */
  private fun checkServerStatus() {
    val serverConfig = appSettings.serverConfig
    lifecycleScope.launch {
      val isUp =
          withContext(Dispatchers.IO) {
            try {
              serverStatusProvider.isSamReady(serverConfig)
            } catch (e: Exception) {
              false
            }
          }
      appSettings.lastServerStatus = isUp
      updateServerStatusIndicator()
    }
  }

  companion object {
    /** Display duration of a successful result before the automatic return. */
    const val SUCCESS_RETURN_DELAY_MS = 5000L
    /** Display duration of an error before the automatic return, longer to read the message. */
    const val ERROR_RETURN_DELAY_MS = 8000L
    /** Resolution of the progress bar showing the remaining time (smooth animation). */
    private const val AUTO_RETURN_PROGRESS_MAX = 1000
  }
}
