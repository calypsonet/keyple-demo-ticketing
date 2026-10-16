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
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityCardMediumSettingsBinding
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMedium
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMediumVisibility

@AndroidEntryPoint
class CardMediumSettingsActivity : BaseActivity() {
  private lateinit var activityCardMediumSettingsBinding: ActivityCardMediumSettingsBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCardMediumSettingsBinding = ActivityCardMediumSettingsBinding.inflate(layoutInflater)
    toolbarBinding = activityCardMediumSettingsBinding.appBarLayout
    setContentView(activityCardMediumSettingsBinding.root)

    activityCardMediumSettingsBinding.backBtn.setOnClickListener {
      onBackPressedDispatcher.onBackPressed()
    }

    CardMedium.entries.forEach { updateRadioButtons(it) }
  }

  fun onContactlessRadioButtonClicked(view: View) =
      onRadioButtonClicked(view, CardMedium.CONTACTLESS_CARD)

  fun onSimRadioButtonClicked(view: View) = onRadioButtonClicked(view, CardMedium.SIM)

  private fun onRadioButtonClicked(view: View, cardMedium: CardMedium) {
    if (view !is RadioButton) {
      return
    }
    if (view.isChecked) {
      val (enableButton, disableButton, _) = radioButtons(cardMedium)
      appSettings.setCardMediumVisibility(
          cardMedium,
          when (view) {
            enableButton -> CardMediumVisibility.ENABLE
            disableButton -> CardMediumVisibility.DISABLE
            else -> CardMediumVisibility.HIDE
          })
    }
    updateRadioButtons(cardMedium)
  }

  /** Returns the "enable", "disable" and "hide" radio buttons of the given card medium. */
  private fun radioButtons(cardMedium: CardMedium): Triple<RadioButton, RadioButton, RadioButton> =
      with(activityCardMediumSettingsBinding) {
        when (cardMedium) {
          CardMedium.CONTACTLESS_CARD ->
              Triple(contactlessCardEnable, contactlessCardDisable, contactlessCardHide)
          CardMedium.SIM -> Triple(simCardEnable, simCardDisable, simCardHide)
        }
      }

  private fun updateRadioButtons(cardMedium: CardMedium) {
    val visibility = appSettings.getCardMediumVisibility(cardMedium)
    val (enableButton, disableButton, hideButton) = radioButtons(cardMedium)
    setRadioButtonChecked(enableButton, visibility == CardMediumVisibility.ENABLE)
    setRadioButtonChecked(disableButton, visibility == CardMediumVisibility.DISABLE)
    setRadioButtonChecked(hideButton, visibility == CardMediumVisibility.HIDE)
  }

  private fun setRadioButtonChecked(radioButton: RadioButton, checked: Boolean) {
    radioButton.isChecked = checked
    radioButton.setTextColor(
        ContextCompat.getColor(this, if (checked) R.color.dark_blue else R.color.light_grey))
  }
}
