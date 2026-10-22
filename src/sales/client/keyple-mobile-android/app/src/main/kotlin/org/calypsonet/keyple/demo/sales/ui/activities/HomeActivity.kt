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
package org.calypsonet.keyple.demo.sales.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.sales.databinding.ActivityHomeBinding

@AndroidEntryPoint
class HomeActivity : BaseActivity() {

  private lateinit var activityHomeBinding: ActivityHomeBinding

  override val showsBackArrow = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)
    toolbarBinding = activityHomeBinding.appBarLayout
    setContentView(activityHomeBinding.root)

    // Handle edge-to-edge display with proper window insets for Android 15+
    ViewCompat.setOnApplyWindowInsetsListener(activityHomeBinding.root) { view, windowInsets ->
      val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(top = insets.top, bottom = insets.bottom)
      WindowInsetsCompat.CONSUMED
    }

    toolbarBinding.menuBtn.visibility = View.VISIBLE
    toolbarBinding.menuBtn.setOnClickListener {
      startActivity(Intent(this, SettingsMenuActivity::class.java))
    }
  }

  override fun onResume() {
    super.onResume()
    activityHomeBinding.contactlessCardBtn.setOnClickListener {
      startActivity(Intent(this, CardReaderActivity::class.java))
    }
  }
}
