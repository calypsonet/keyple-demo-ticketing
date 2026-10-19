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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.sales.BuildConfig
import org.calypsonet.keyple.demo.sales.R
import org.calypsonet.keyple.demo.sales.databinding.ActivitySettingsMenuBinding

@AndroidEntryPoint
class SettingsMenuActivity : BaseActivity() {
  private lateinit var activitySettingsMenuBinding: ActivitySettingsMenuBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activitySettingsMenuBinding = ActivitySettingsMenuBinding.inflate(layoutInflater)
    toolbarBinding = activitySettingsMenuBinding.appBarLayout
    setContentView(activitySettingsMenuBinding.root)

    // Handle edge-to-edge display with proper window insets for Android 15+
    ViewCompat.setOnApplyWindowInsetsListener(activitySettingsMenuBinding.root) { view, windowInsets
      ->
      val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(top = insets.top, bottom = insets.bottom)
      WindowInsetsCompat.CONSUMED
    }

    activitySettingsMenuBinding.serverBtn.setOnClickListener {
      val intent = Intent(this, ServerSettingsActivity::class.java)
      startActivity(intent)
    }

    activitySettingsMenuBinding.cardMediumBtn.setOnClickListener {
      val intent = Intent(this, CardMediumSettingsActivity::class.java)
      startActivity(intent)
    }

    activitySettingsMenuBinding.personalizationBtn.setOnClickListener {
      val intent = Intent(this, HomeActivity::class.java)
      intent.putExtra(HomeActivity.CHOOSE_CARD_MEDIUM_FOR_PERSONALIZATION, true)
      startActivity(intent)
    }

    activitySettingsMenuBinding.versionName.text =
        getString(R.string.version, BuildConfig.VERSION_NAME)
  }
}
