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

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.util.Timer
import java.util.TimerTask
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

  private lateinit var activitySplashBinding: ActivitySplashBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    // Make sure this is before calling super.onCreate
    super.onCreate(savedInstanceState)
    activitySplashBinding = ActivitySplashBinding.inflate(layoutInflater)
    setContentView(activitySplashBinding.root)

    // Handle edge-to-edge display with proper window insets for Android 15+
    ViewCompat.setOnApplyWindowInsetsListener(activitySplashBinding.root) { view, windowInsets ->
      val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(top = insets.top, bottom = insets.bottom)
      WindowInsetsCompat.CONSUMED
    }

    // Shows the splash screen for a moment, then the home screen
    Timer()
        .schedule(
            object : TimerTask() {
              override fun run() {
                if (!isFinishing) {
                  startActivity(Intent(applicationContext, HomeActivity::class.java))
                  finish()
                }
              }
            },
            SPLASH_MAX_DELAY_MS.toLong())
  }

  companion object {
    private const val SPLASH_MAX_DELAY_MS = 6000
  }
}
