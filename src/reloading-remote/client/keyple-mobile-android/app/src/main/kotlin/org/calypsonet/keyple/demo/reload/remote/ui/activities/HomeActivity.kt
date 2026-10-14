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
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMedium
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMediumVisibility

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
      activityHomeBinding.chooseCardMediumTv.append(" ")
      activityHomeBinding.chooseCardMediumTv.append(getString(R.string.to_be_personalized))
    }
    toolbarBinding.menuBtn.visibility = View.VISIBLE
    toolbarBinding.menuBtn.setOnClickListener {
      startActivity(Intent(this, SettingsMenuActivity::class.java))
    }
  }

  override fun onResume() {
    super.onResume()
    setUpCardMediumButton(activityHomeBinding.contactlessCardBtn, CardMedium.CONTACTLESS_CARD)
    setUpCardMediumButton(activityHomeBinding.simCardBtn, CardMedium.SIM)
    setUpCardMediumButton(activityHomeBinding.wearableBtn, CardMedium.WEARABLE)
    setUpCardMediumButton(activityHomeBinding.embeddedElemBtn, CardMedium.EMBEDDED)
  }

  private fun setUpCardMediumButton(button: View, cardMedium: CardMedium) {
    button.setOnClickListener {
      appSettings.cardMedium = cardMedium
      if (intent.getBooleanExtra(CHOOSE_DEVICE_FOR_PERSO, false)) {
        intent.putExtras(intent)
        startActivity(Intent(this, PersonalizeCardActivity::class.java))
        this.finish()
      } else startActivity(Intent(this, CardReaderActivity::class.java))
    }
    when (appSettings.getCardMediumVisibility(cardMedium)) {
      CardMediumVisibility.ENABLE -> {
        button.visibility = View.VISIBLE
        button.background = ContextCompat.getDrawable(this, R.drawable.white_card)
        button.isEnabled = true
      }
      CardMediumVisibility.DISABLE -> {
        button.visibility = View.VISIBLE
        button.background = ContextCompat.getDrawable(this, R.drawable.grey_card)
        button.isEnabled = false
      }
      CardMediumVisibility.HIDE -> {
        button.visibility = View.GONE
      }
    }
  }

  companion object {
    const val CHOOSE_DEVICE_FOR_PERSO = "CHOOSE_DEVICE_FOR_PERSO"
  }
}
