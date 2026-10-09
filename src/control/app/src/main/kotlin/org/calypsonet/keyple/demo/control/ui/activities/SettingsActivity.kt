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

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.widget.ArrayAdapter
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.common.data.LocationRepository
import org.calypsonet.keyple.demo.common.model.Location
import org.calypsonet.keyple.demo.control.BuildConfig
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.databinding.ActivitySettingsBinding
import org.calypsonet.keyple.demo.control.databinding.ToolbarBinding

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {

  private lateinit var activitySettingsBinding: ActivitySettingsBinding
  private lateinit var toolbarBinding: ToolbarBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activitySettingsBinding = ActivitySettingsBinding.inflate(layoutInflater)
    toolbarBinding = activitySettingsBinding.appBarLayout
    setContentView(activitySettingsBinding.root)
    setSupportActionBar(toolbarBinding.toolbar)

    activitySettingsBinding.spinnerLocationList.adapter =
        ArrayAdapter(
            this,
            R.layout.spinner_item_location,
            R.id.spinner_item_text,
            LocationRepository.getLocations())
    activitySettingsBinding.validationPeriodEdit.text =
        Editable.Factory.getInstance().newEditable("10")
    activitySettingsBinding.appVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)
    activitySettingsBinding.timeBtn.setOnClickListener {
      startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
    }
    activitySettingsBinding.startBtn.setOnClickListener {
      appSettings.location = activitySettingsBinding.spinnerLocationList.selectedItem as Location
      val validationPeriod = activitySettingsBinding.validationPeriodEdit.text.toString()
      if (validationPeriod.isNotBlank()) {
        appSettings.validationPeriod = validationPeriod.toInt()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
      } else {
        showToast(getString(R.string.msg_location_period_empty))
      }
    }
  }
}
