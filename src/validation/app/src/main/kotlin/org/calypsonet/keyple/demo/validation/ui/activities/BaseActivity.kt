/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
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
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.domain.CardReaderSession
import org.calypsonet.keyple.demo.validation.domain.spi.AppSettingsRepository

abstract class BaseActivity : AppCompatActivity() {

  @Inject lateinit var cardReaderSession: CardReaderSession
  @Inject lateinit var appSettings: AppSettingsRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // A validator must stay readable: prevent the screen from dimming or turning off.
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    // Reapplied on each focus gain, as dialogs or system interactions may show the bars again.
    if (hasFocus) {
      hideSystemBars()
    }
  }

  /**
   * Hides the status and navigation bars (immersive mode), the bars being temporarily shown by a
   * swipe from the screen edge.
   */
  private fun hideSystemBars() {
    WindowCompat.getInsetsController(window, window.decorView).apply {
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      hide(WindowInsetsCompat.Type.systemBars())
    }
  }
}
