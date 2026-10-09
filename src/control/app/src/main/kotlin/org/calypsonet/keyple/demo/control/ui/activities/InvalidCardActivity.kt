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
package org.calypsonet.keyple.demo.control.ui.activities

import android.os.Bundle
import androidx.core.content.IntentCompat
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.databinding.ActivityInvalidCardBinding
import org.calypsonet.keyple.demo.control.databinding.ToolbarBinding
import org.calypsonet.keyple.demo.control.ui.activities.CardReaderActivity.Companion.CARD_CONTENT
import org.calypsonet.keyple.demo.control.ui.model.UiControlResult
import timber.log.Timber

@AndroidEntryPoint
class InvalidCardActivity : BaseActivity() {
  private lateinit var activityInvalidCardBinding: ActivityInvalidCardBinding
  private lateinit var toolbarBinding: ToolbarBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityInvalidCardBinding = ActivityInvalidCardBinding.inflate(layoutInflater)
    toolbarBinding = activityInvalidCardBinding.appBarLayout
    setContentView(activityInvalidCardBinding.root)
    setSupportActionBar(toolbarBinding.toolbar)
    toolbarBinding.toolbarLogo.setImageResource(R.drawable.ic_logo_white)
    val cardContent: UiControlResult? =
        IntentCompat.getParcelableExtra(intent, CARD_CONTENT, UiControlResult::class.java)
    activityInvalidCardBinding.invalidDescription.text = cardContent?.errorMessage
    activityInvalidCardBinding.presentBtn.setOnClickListener {
      onBackPressedDispatcher.onBackPressed()
    }
  }

  override fun onResume() {
    super.onResume()
    if (ticketingService.readersInitialized) {
      ticketingService.stopNfcDetection()
      Timber.d("stopNfcDetection")
    }
  }
}
