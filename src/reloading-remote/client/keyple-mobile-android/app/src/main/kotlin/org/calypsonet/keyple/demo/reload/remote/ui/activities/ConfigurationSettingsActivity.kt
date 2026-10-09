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

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityConfigurationSettingsBinding
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceType
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceVisibility

@AndroidEntryPoint
class ConfigurationSettingsActivity : BaseActivity() {
  private lateinit var activityConfigurationSettingsBinding: ActivityConfigurationSettingsBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityConfigurationSettingsBinding =
        ActivityConfigurationSettingsBinding.inflate(layoutInflater)
    toolbarBinding = activityConfigurationSettingsBinding.appBarLayout
    setContentView(activityConfigurationSettingsBinding.root)

    activityConfigurationSettingsBinding.backBtn.setOnClickListener {
      onBackPressedDispatcher.onBackPressed()
    }

    DeviceType.values().forEach { updateRadioButtons(it) }
  }

  fun onContactlessRadioButtonClicked(view: View) =
      onRadioButtonClicked(view, DeviceType.CONTACTLESS_CARD)

  fun onSimRadioButtonClicked(view: View) = onRadioButtonClicked(view, DeviceType.SIM)

  fun onWearableRadioButtonClicked(view: View) = onRadioButtonClicked(view, DeviceType.WEARABLE)

  fun onEmbeddedRadioButtonClicked(view: View) = onRadioButtonClicked(view, DeviceType.EMBEDDED)

  private fun onRadioButtonClicked(view: View, device: DeviceType) {
    if (view !is RadioButton) {
      return
    }
    if (view.isChecked) {
      val (enableBtn, disableBtn, _) = radioButtons(device)
      appSettings.setDeviceVisibility(
          device,
          when (view) {
            enableBtn -> DeviceVisibility.ENABLE
            disableBtn -> DeviceVisibility.DISABLE
            else -> DeviceVisibility.HIDE
          })
    }
    updateRadioButtons(device)
  }

  /** Returns the "enable", "disable" and "hide" radio buttons of the given device type. */
  private fun radioButtons(device: DeviceType): Triple<RadioButton, RadioButton, RadioButton> =
      with(activityConfigurationSettingsBinding) {
        when (device) {
          DeviceType.CONTACTLESS_CARD ->
              Triple(contactlessCardEnable, contactlessCardDisable, contactlessCardHide)
          DeviceType.SIM -> Triple(simCardEnable, simCardDisable, simCardHide)
          DeviceType.WEARABLE -> Triple(wearableCardEnable, wearableCardDisable, wearableCardHide)
          DeviceType.EMBEDDED -> Triple(embeddedCardEnable, embeddedCardDisable, embeddedCardHide)
        }
      }

  private fun updateRadioButtons(device: DeviceType) {
    val visibility = appSettings.getDeviceVisibility(device)
    val (enableBtn, disableBtn, hideBtn) = radioButtons(device)
    setRadioButtonChecked(enableBtn, visibility == DeviceVisibility.ENABLE)
    setRadioButtonChecked(disableBtn, visibility == DeviceVisibility.DISABLE)
    setRadioButtonChecked(hideBtn, visibility == DeviceVisibility.HIDE)
  }

  private fun setRadioButtonChecked(radioButton: RadioButton, checked: Boolean) {
    radioButton.isChecked = checked
    radioButton.setTextColor(
        ContextCompat.getColor(this, if (checked) R.color.dark_blue else R.color.light_grey))
  }
}
