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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.calypsonet.keyple.demo.validation.R
import org.calypsonet.keyple.demo.validation.domain.spi.Logger

/**
 * Standard user feedback of the other terminals (Bluebird, Famoco).
 *
 * Uses Android MediaPlayer for success/error sounds. No LED control. This variant is also compiled
 * when AndroidParkeonCommon-release.aar is absent from libs/ (mock mode for Arrive hardware).
 *
 * The sounds are played by a dedicated thread: starting a MediaPlayer may take time (wake-up of the
 * audio output), which would delay the display of the validation result.
 */
internal class AndroidFeedbackDevice(private val context: Context, private val logger: Logger) :
    FeedbackDevice {
  private var successMedia: MediaPlayer? = null
  private var failureMedia: MediaPlayer? = null
  private var soundExecutor: ExecutorService? = null

  override fun init(onReady: () -> Unit) {
    successMedia = MediaPlayer.create(context, R.raw.success)
    failureMedia = MediaPlayer.create(context, R.raw.error)
    soundExecutor = Executors.newSingleThreadExecutor()
    onReady()
  }

  override fun signalSuccess() {
    play { successMedia }
  }

  override fun signalFailure() {
    play { failureMedia }
  }

  override fun signalWaiting() {}

  override fun release() {
    // Released by the thread playing the sounds, after the sounds being started
    soundExecutor?.execute { releaseSounds() }
    soundExecutor?.shutdown()
    soundExecutor = null
  }

  /** Starts the provided sound in the thread playing the sounds. */
  private fun play(media: () -> MediaPlayer?) {
    soundExecutor?.execute {
      try {
        media()?.start()
      } catch (e: IllegalStateException) {
        logger.w("Error while playing the sound [cause=${e.message}]")
      }
    }
  }

  private fun releaseSounds() {
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
