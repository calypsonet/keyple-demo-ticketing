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
package org.calypsonet.keyple.demo.control.data

import android.app.Activity
import android.media.MediaPlayer
import javax.inject.Inject
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.domain.model.TerminalType
import org.calypsonet.keyple.demo.control.domain.spi.UiContext
import org.calypsonet.keyple.demo.control.domain.spi.UserFeedback

/** User feedback based on Android MediaPlayer (success/error sounds). */
class UserFeedbackImpl @Inject constructor() : UserFeedback {

  private var successMedia: MediaPlayer? = null
  private var errorMedia: MediaPlayer? = null

  override fun init(terminalType: TerminalType, uiContext: UiContext) {
    val activity = uiContext.adaptTo(Activity::class.java)
    successMedia = MediaPlayer.create(activity, R.raw.success)
    errorMedia = MediaPlayer.create(activity, R.raw.error)
  }

  override fun displayResultSuccess() {
    successMedia?.start()
  }

  override fun displayResultFailed() {
    errorMedia?.start()
  }

  override fun release() {
    successMedia?.stop()
    successMedia?.release()
    successMedia = null
    errorMedia?.stop()
    errorMedia?.release()
    errorMedia = null
  }
}
