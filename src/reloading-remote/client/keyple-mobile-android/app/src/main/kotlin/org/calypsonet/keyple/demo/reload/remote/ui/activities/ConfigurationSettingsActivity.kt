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
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMedium
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMediumVisibility

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

    CardMedium.values().forEach { updateRadioButtons(it) }
  }

  fun onContactlessRadioButtonClicked(view: View) =
      onRadioButtonClicked(view, CardMedium.CONTACTLESS_CARD)

  fun onSimRadioButtonClicked(view: View) = onRadioButtonClicked(view, CardMedium.SIM)

  fun onWearableRadioButtonClicked(view: View) = onRadioButtonClicked(view, CardMedium.WEARABLE)

  fun onEmbeddedRadioButtonClicked(view: View) = onRadioButtonClicked(view, CardMedium.EMBEDDED)

  private fun onRadioButtonClicked(view: View, cardMedium: CardMedium) {
    if (view !is RadioButton) {
      return
    }
    if (view.isChecked) {
      val (enableBtn, disableBtn, _) = radioButtons(cardMedium)
      appSettings.setCardMediumVisibility(
          cardMedium,
          when (view) {
            enableBtn -> CardMediumVisibility.ENABLE
            disableBtn -> CardMediumVisibility.DISABLE
            else -> CardMediumVisibility.HIDE
          })
    }
    updateRadioButtons(cardMedium)
  }

  /** Returns the "enable", "disable" and "hide" radio buttons of the given card medium. */
  private fun radioButtons(cardMedium: CardMedium): Triple<RadioButton, RadioButton, RadioButton> =
      with(activityConfigurationSettingsBinding) {
        when (cardMedium) {
          CardMedium.CONTACTLESS_CARD ->
              Triple(contactlessCardEnable, contactlessCardDisable, contactlessCardHide)
          CardMedium.SIM -> Triple(simCardEnable, simCardDisable, simCardHide)
          CardMedium.WEARABLE -> Triple(wearableCardEnable, wearableCardDisable, wearableCardHide)
          CardMedium.EMBEDDED -> Triple(embeddedCardEnable, embeddedCardDisable, embeddedCardHide)
        }
      }

  private fun updateRadioButtons(cardMedium: CardMedium) {
    val visibility = appSettings.getCardMediumVisibility(cardMedium)
    val (enableBtn, disableBtn, hideBtn) = radioButtons(cardMedium)
    setRadioButtonChecked(enableBtn, visibility == CardMediumVisibility.ENABLE)
    setRadioButtonChecked(disableBtn, visibility == CardMediumVisibility.DISABLE)
    setRadioButtonChecked(hideBtn, visibility == CardMediumVisibility.HIDE)
  }

  private fun setRadioButtonChecked(radioButton: RadioButton, checked: Boolean) {
    radioButton.isChecked = checked
    radioButton.setTextColor(
        ContextCompat.getColor(this, if (checked) R.color.dark_blue else R.color.light_grey))
  }
}
