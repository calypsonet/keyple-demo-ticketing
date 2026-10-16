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
package org.calypsonet.keyple.demo.inspection.data

import android.app.Activity
import android.media.MediaPlayer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import org.calypsonet.keyple.demo.inspection.R
import org.calypsonet.keyple.demo.inspection.domain.model.TerminalType
import org.calypsonet.keyple.demo.inspection.domain.spi.Logger
import org.calypsonet.keyple.demo.inspection.domain.spi.UiContext
import org.calypsonet.keyple.demo.inspection.domain.spi.UserFeedback

/**
 * User feedback based on Android MediaPlayer (success/error sounds).
 *
 * The sounds are played by a dedicated thread: starting a MediaPlayer may take several hundred
 * milliseconds (wake-up of the audio output), which would delay the display of the result.
 */
class MediaPlayerUserFeedback @Inject constructor(private val logger: Logger) : UserFeedback {

  private var successMedia: MediaPlayer? = null
  private var failureMedia: MediaPlayer? = null
  private var soundExecutor: ExecutorService? = null

  override fun init(terminalType: TerminalType, uiContext: UiContext) {
    val activity = uiContext.adaptTo(Activity::class.java)
    successMedia = MediaPlayer.create(activity, R.raw.success)
    failureMedia = MediaPlayer.create(activity, R.raw.error)
    soundExecutor = Executors.newSingleThreadExecutor()
  }

  override fun signalSuccess() {
    play { successMedia }
  }

  override fun signalFailure() {
    play { failureMedia }
  }

  override fun release() {
    // Released by the thread playing the sounds, after the sounds being started
    soundExecutor?.execute {
      successMedia?.stop()
      successMedia?.release()
      successMedia = null
      failureMedia?.stop()
      failureMedia?.release()
      failureMedia = null
    }
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
}
