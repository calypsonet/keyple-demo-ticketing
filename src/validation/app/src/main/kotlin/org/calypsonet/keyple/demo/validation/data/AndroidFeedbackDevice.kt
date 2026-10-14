/* ******************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.validation.data

import android.content.Context
import android.media.MediaPlayer
import org.calypsonet.keyple.demo.validation.R
import org.calypsonet.keyple.demo.validation.domain.spi.Logger

/**
 * Standard user feedback of the other terminals (Bluebird, Famoco).
 *
 * Uses Android MediaPlayer for success/error sounds. No LED control. This variant is also compiled
 * when AndroidParkeonCommon-release.aar is absent from libs/ (mock mode for Arrive hardware).
 */
internal class AndroidFeedbackDevice(private val context: Context, private val logger: Logger) :
    FeedbackDevice {
  private var successMedia: MediaPlayer? = null
  private var failureMedia: MediaPlayer? = null

  override fun init(onReady: () -> Unit) {
    successMedia = MediaPlayer.create(context, R.raw.success)
    failureMedia = MediaPlayer.create(context, R.raw.error)
    onReady()
  }

  override fun signalSuccess() {
    successMedia?.start()
  }

  override fun signalFailure() {
    failureMedia?.start()
  }

  override fun signalWaiting() {}

  override fun release() {
    try {
      successMedia?.stop()
      successMedia?.release()
    } catch (e: Exception) {
      logger.e("Error while releasing the success sound", e)
    } finally {
      successMedia = null
    }
    try {
      failureMedia?.stop()
      failureMedia?.release()
    } catch (e: Exception) {
      logger.e("Error while releasing the error sound", e)
    } finally {
      failureMedia = null
    }
  }
}
