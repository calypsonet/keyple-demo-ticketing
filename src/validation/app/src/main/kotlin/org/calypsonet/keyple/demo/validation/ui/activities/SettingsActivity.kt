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
package org.calypsonet.keyple.demo.validation.ui.activities

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.common.model.Location
import org.calypsonet.keyple.demo.validation.BuildConfig
import org.calypsonet.keyple.demo.validation.R
import org.calypsonet.keyple.demo.validation.databinding.ActivitySettingsBinding
import org.calypsonet.keyple.demo.validation.databinding.LogoToolbarBinding

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {

  private lateinit var activitySettingsBinding: ActivitySettingsBinding
  private lateinit var logoToolbarBinding: LogoToolbarBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activitySettingsBinding = ActivitySettingsBinding.inflate(layoutInflater)
    logoToolbarBinding = activitySettingsBinding.appBarLayout
    setContentView(activitySettingsBinding.root)
    setSupportActionBar(logoToolbarBinding.toolbar)
    // Init location spinner
    val locations = ticketingService.getLocations()
    val locationsAdapter =
        ArrayAdapter(this, R.layout.spinner_item_location, R.id.spinner_item_text, locations)
    activitySettingsBinding.spinnerLocationList.adapter = locationsAdapter
    activitySettingsBinding.timeBtn.setOnClickListener {
      startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
    }
    activitySettingsBinding.startBtn.setOnClickListener {
      appSettings.location = activitySettingsBinding.spinnerLocationList.selectedItem as Location
      appSettings.batteryPowered = activitySettingsBinding.batteryPoweredBox.isChecked
      if (appSettings.batteryPowered) {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
      } else {
        startActivity(Intent(this, ReaderActivity::class.java))
      }
    }
    activitySettingsBinding.appVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)
  }
}
