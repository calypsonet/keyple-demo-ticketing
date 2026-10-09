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
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.airbnb.lottie.LottieDrawable
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityReloadResultBinding
import org.calypsonet.keyple.demo.reload.remote.domain.model.Status
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiCardReaderResponse

@AndroidEntryPoint
class ReloadResultActivity : BaseActivity() {

  private lateinit var activityReloadResultBinding: ActivityReloadResultBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityReloadResultBinding = ActivityReloadResultBinding.inflate(layoutInflater)
    toolbarBinding = activityReloadResultBinding.appBarLayout
    setContentView(activityReloadResultBinding.root)
    toolbarBinding.toolbarLogo.setImageResource(R.drawable.ic_logo_white)

    // Status name provided by the calling activity (error status by default)
    val statusName = intent.getStringExtra(STATUS)
    val status = Status.entries.firstOrNull { it.name == statusName } ?: Status.ERROR
    val cardContent: UiCardReaderResponse? =
        IntentCompat.getParcelableExtra(
            intent, BaseCardActivity.CARD_CONTENT, UiCardReaderResponse::class.java)

    activityReloadResultBinding.tryBtn.setOnClickListener {
      onBackPressedDispatcher.onBackPressed()
    }
    activityReloadResultBinding.cancelBtn.setOnClickListener {
      val intent = Intent(this, HomeActivity::class.java)
      startActivity(intent)
    }

    if (cardContent != null && !cardContent.cardType.isNullOrBlank()) {
      activityReloadResultBinding.cardTypeLabel.visibility = View.VISIBLE
      activityReloadResultBinding.cardTypeLabel.text =
          getString(R.string.card_type, cardContent.cardType)
    } else {
      activityReloadResultBinding.cardTypeLabel.visibility = View.GONE
    }

    when (status) {
      Status.LOADING -> {
        activityReloadResultBinding.animation.setAnimation("loading_anim.json")
        activityReloadResultBinding.animation.repeatCount = LottieDrawable.INFINITE
        activityReloadResultBinding.bigText.visibility = View.INVISIBLE
        activityReloadResultBinding.btnLayout.visibility = View.INVISIBLE
      }
      Status.SUCCESS -> {
        activityReloadResultBinding.mainBackground.setBackgroundColor(
            ContextCompat.getColor(this, R.color.green))
        activityReloadResultBinding.animation.setAnimation("tick_white.json")
        activityReloadResultBinding.animation.repeatCount = 0
        activityReloadResultBinding.animation.playAnimation()
        activityReloadResultBinding.bigText.setText(R.string.charging_success_label)
        activityReloadResultBinding.bigText.visibility = View.VISIBLE
        activityReloadResultBinding.btnLayout.visibility = View.INVISIBLE

        if (intent.getBooleanExtra(IS_PERSONALIZATION_RESULT, false)) {
          activityReloadResultBinding.bigText.setText(R.string.perso_success_label)
        } else {
          activityReloadResultBinding.bigText.setText(R.string.charging_success_label)
        }

        scheduleAutoReturn(SUCCESS_RETURN_DELAY_MS, activityReloadResultBinding.autoReturnProgress)
      }
      else -> {
        activityReloadResultBinding.mainBackground.setBackgroundColor(
            ContextCompat.getColor(this, R.color.red))
        activityReloadResultBinding.animation.setAnimation("error_white.json")
        activityReloadResultBinding.animation.repeatCount = 0
        activityReloadResultBinding.animation.playAnimation()

        val message = intent.getStringExtra(MESSAGE) ?: ""
        if (intent.getBooleanExtra(IS_PERSONALIZATION_RESULT, false)) {
          activityReloadResultBinding.bigText.setText(R.string.perso_failed_label)
          activityReloadResultBinding.bigText.append(":\n")
          activityReloadResultBinding.bigText.append(message)
        } else {
          activityReloadResultBinding.bigText.setText(R.string.transaction_cancelled_label)
          activityReloadResultBinding.bigText.append(":\n")
          activityReloadResultBinding.bigText.append(message)
        }
        activityReloadResultBinding.bigText.visibility = View.VISIBLE
        activityReloadResultBinding.btnLayout.visibility = View.VISIBLE

        // Same as the "try again" button, unless the user chooses before: its label shows the
        // remaining seconds
        scheduleAutoReturn(ERROR_RETURN_DELAY_MS, activityReloadResultBinding.autoReturnProgress) {
            seconds ->
          activityReloadResultBinding.tryBtn.text =
              if (seconds != null) getString(R.string.try_again_countdown, seconds)
              else getString(R.string.try_again)
        }
      }
    }

    // Play sound
    val mp: MediaPlayer = MediaPlayer.create(this, R.raw.reading_sound)
    mp.start()
  }

  companion object {
    const val TRIPS_TO_LOAD = "ticketsNumber"
    const val STATUS = "status"
    const val MESSAGE = "message"
    const val IS_PERSONALIZATION_RESULT = "isPersonalizationResult"
  }
}
