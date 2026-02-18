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
package org.calypsonet.keyple.demo.validation.data

import android.content.Context
import android.media.MediaPlayer
import org.calypsonet.keyple.demo.validation.R
import timber.log.Timber

/**
 * Standard UI feedback for non-Arrive terminals (Bluebird, Coppernic, Famoco).
 *
 * Uses Android MediaPlayer for success/error sounds. No LED control. This variant is also compiled
 * when AndroidParkeonCommon-release.aar is absent from libs/ (mock mode for Arrive hardware).
 */
internal class AndroidUiManagerImpl(private val context: Context) : UiManager {
  private var successMedia: MediaPlayer? = null
  private var errorMedia: MediaPlayer? = null

  override fun init(onReady: () -> Unit) {
    successMedia = MediaPlayer.create(context, R.raw.success)
    errorMedia = MediaPlayer.create(context, R.raw.error)
    onReady()
  }

  override fun displayResultSuccess() {
    successMedia?.start()
  }

  override fun displayResultFailed() {
    errorMedia?.start()
  }

  override fun displayWaiting() {}

  override fun release() {
    try {
      successMedia?.stop()
      successMedia?.release()
    } catch (e: Exception) {
      Timber.e(e, "AndroidUiManagerImpl: error releasing success media")
    } finally {
      successMedia = null
    }
    try {
      errorMedia?.stop()
      errorMedia?.release()
    } catch (e: Exception) {
      Timber.e(e, "AndroidUiManagerImpl: error releasing error media")
    } finally {
      errorMedia = null
    }
  }
}
