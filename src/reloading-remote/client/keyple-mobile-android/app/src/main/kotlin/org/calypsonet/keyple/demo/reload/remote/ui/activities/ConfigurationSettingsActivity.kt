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
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityConfigurationSettingsBinding
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceEnum
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceVisibility

class ConfigurationSettingsActivity : AbstractDemoActivity() {
  private lateinit var activityConfigurationSettingsBinding: ActivityConfigurationSettingsBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityConfigurationSettingsBinding =
        ActivityConfigurationSettingsBinding.inflate(layoutInflater)
    toolbarBinding = activityConfigurationSettingsBinding.appBarLayout
    setContentView(activityConfigurationSettingsBinding.root)

    activityConfigurationSettingsBinding.backBtn.setOnClickListener { onBackPressed() }

    DeviceEnum.values().forEach { updateRadioButtons(it) }
  }

  fun onContactlessRadioButtonClicked(view: View) =
      onRadioButtonClicked(view, DeviceEnum.CONTACTLESS_CARD)

  fun onSimRadioButtonClicked(view: View) = onRadioButtonClicked(view, DeviceEnum.SIM)

  fun onWearableRadioButtonClicked(view: View) = onRadioButtonClicked(view, DeviceEnum.WEARABLE)

  fun onEmbeddedRadioButtonClicked(view: View) = onRadioButtonClicked(view, DeviceEnum.EMBEDDED)

  private fun onRadioButtonClicked(view: View, device: DeviceEnum) {
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
  private fun radioButtons(device: DeviceEnum): Triple<RadioButton, RadioButton, RadioButton> =
      with(activityConfigurationSettingsBinding) {
        when (device) {
          DeviceEnum.CONTACTLESS_CARD ->
              Triple(contactlessCardEnable, contactlessCardDisable, contactlessCardHide)
          DeviceEnum.SIM -> Triple(simCardEnable, simCardDisable, simCardHide)
          DeviceEnum.WEARABLE -> Triple(wearableCardEnable, wearableCardDisable, wearableCardHide)
          DeviceEnum.EMBEDDED -> Triple(embeddedCardEnable, embeddedCardDisable, embeddedCardHide)
        }
      }

  private fun updateRadioButtons(device: DeviceEnum) {
    val visibility = appSettings.getDeviceVisibility(device)
    val (enableBtn, disableBtn, hideBtn) = radioButtons(device)
    setRadioButtonChecked(enableBtn, visibility == DeviceVisibility.ENABLE)
    setRadioButtonChecked(disableBtn, visibility == DeviceVisibility.DISABLE)
    setRadioButtonChecked(hideBtn, visibility == DeviceVisibility.HIDE)
  }

  private fun setRadioButtonChecked(radioButton: RadioButton, checked: Boolean) {
    radioButton.isChecked = checked
    radioButton.setTextColor(
        resources.getColor(if (checked) R.color.dark_blue else R.color.light_grey))
  }
}
