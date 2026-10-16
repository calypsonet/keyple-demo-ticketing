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
package org.calypsonet.keyple.demo.inspection.ui.activities

import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import java.util.Timer
import java.util.TimerTask
import org.calypsonet.keyple.demo.inspection.databinding.ActivitySplashBinding
import org.calypsonet.keyple.demo.inspection.ui.activities.terminalselection.TerminalSelectionActivity

@AndroidEntryPoint
class SplashActivity : BaseActivity() {
  private lateinit var activitySplashBinding: ActivitySplashBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    // Make sure this is before calling super.onCreate
    super.onCreate(savedInstanceState)
    activitySplashBinding = ActivitySplashBinding.inflate(layoutInflater)
    setContentView(activitySplashBinding.root)
    Timer()
        .schedule(
            object : TimerTask() {
              override fun run() {
                if (!isFinishing) {
                  startActivity(Intent(applicationContext, TerminalSelectionActivity::class.java))
                  finish()
                }
              }
            },
            SPLASH_MAX_DELAY_MS.toLong())
  }

  companion object {
    private const val SPLASH_MAX_DELAY_MS = 2000
  }
}
