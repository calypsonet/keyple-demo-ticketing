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
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityHomeBinding
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceType
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceVisibility

@AndroidEntryPoint
class HomeActivity : BaseActivity() {

  private lateinit var activityHomeBinding: ActivityHomeBinding

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

    if (intent.getBooleanExtra(CHOOSE_DEVICE_FOR_PERSO, false)) {
      activityHomeBinding.chooseDeviceTv.append(" ")
      activityHomeBinding.chooseDeviceTv.append(getString(R.string.to_be_personalized))
    }
    toolbarBinding.menuBtn.visibility = View.VISIBLE
    toolbarBinding.menuBtn.setOnClickListener {
      startActivity(Intent(this, SettingsMenuActivity::class.java))
    }
  }

  override fun onResume() {
    super.onResume()
    setupBtn(activityHomeBinding.contactlessCardBtn, DeviceType.CONTACTLESS_CARD)
    setupBtn(activityHomeBinding.simCardBtn, DeviceType.SIM)
    setupBtn(activityHomeBinding.wearableBtn, DeviceType.WEARABLE)
    setupBtn(activityHomeBinding.embeddedElemBtn, DeviceType.EMBEDDED)
  }

  private fun setupBtn(btn: View, type: DeviceType) {
    btn.setOnClickListener {
      appSettings.deviceType = type
      if (intent.getBooleanExtra(CHOOSE_DEVICE_FOR_PERSO, false)) {
        intent.putExtras(intent)
        startActivity(Intent(this, PersonalizationActivity::class.java))
        this.finish()
      } else startActivity(Intent(this, CardReaderActivity::class.java))
    }
    when (appSettings.getDeviceVisibility(type)) {
      DeviceVisibility.ENABLE -> {
        btn.visibility = View.VISIBLE
        btn.background = ContextCompat.getDrawable(this, R.drawable.white_card)
        btn.isEnabled = true
      }
      DeviceVisibility.DISABLE -> {
        btn.visibility = View.VISIBLE
        btn.background = ContextCompat.getDrawable(this, R.drawable.grey_card)
        btn.isEnabled = false
      }
      DeviceVisibility.HIDE -> {
        btn.visibility = View.GONE
      }
    }
  }

  companion object {
    const val CHOOSE_DEVICE_FOR_PERSO = "CHOOSE_DEVICE_FOR_PERSO"
  }
}
