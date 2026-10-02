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

import android.app.Activity
import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.domain.model.ReaderType
import org.calypsonet.keyple.demo.validation.domain.spi.Logger
import org.calypsonet.keyple.demo.validation.domain.spi.UiContext
import org.calypsonet.keyple.demo.validation.domain.spi.UiManager

/**
 * User feedback of the terminal, delegated to the feedback device matching the reader type (Arrive
 * SDK for Arrive terminals, Android MediaPlayer for the others).
 */
class UiManagerImpl @Inject constructor(private val logger: Logger) : UiManager {

  private var feedbackDevice: FeedbackDevice? = null

  override fun init(readerType: ReaderType, uiContext: UiContext) {
    val activity = uiContext.adaptTo(Activity::class.java)
    feedbackDevice =
        if (readerType == ReaderType.ARRIVE) {
          ArriveFeedbackDevice(activity, logger).also { it.init() }
        } else {
          AndroidFeedbackDevice(activity, logger).also { it.init() }
        }
  }

  override fun displayResultSuccess() {
    feedbackDevice?.displayResultSuccess()
  }

  override fun displayResultFailed() {
    feedbackDevice?.displayResultFailed()
  }

  override fun displayWaiting() {
    feedbackDevice?.displayWaiting()
  }

  override fun release() {
    feedbackDevice?.release()
    feedbackDevice = null
  }
}
