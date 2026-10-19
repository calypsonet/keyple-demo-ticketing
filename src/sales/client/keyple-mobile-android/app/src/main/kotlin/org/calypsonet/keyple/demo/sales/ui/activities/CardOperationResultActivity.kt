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
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.airbnb.lottie.LottieDrawable
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.sales.R
import org.calypsonet.keyple.demo.sales.databinding.ActivityCardOperationResultBinding
import org.calypsonet.keyple.demo.sales.ui.model.ResultType
import org.calypsonet.keyple.demo.sales.ui.model.UiCardResult

@AndroidEntryPoint
class CardOperationResultActivity : BaseActivity() {

  private lateinit var activityCardOperationResultBinding: ActivityCardOperationResultBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCardOperationResultBinding = ActivityCardOperationResultBinding.inflate(layoutInflater)
    toolbarBinding = activityCardOperationResultBinding.appBarLayout
    setContentView(activityCardOperationResultBinding.root)
    toolbarBinding.toolbarLogo.setImageResource(R.drawable.ic_logo_white)

    // Result type provided by the calling activity (error by default)
    val resultTypeName = intent.getStringExtra(RESULT_TYPE)
    val resultType =
        ResultType.entries.firstOrNull { it.name == resultTypeName } ?: ResultType.ERROR
    val cardResult: UiCardResult? =
        IntentCompat.getParcelableExtra(
            intent, BaseCardActivity.CARD_RESULT, UiCardResult::class.java)

    activityCardOperationResultBinding.tryBtn.setOnClickListener {
      onBackPressedDispatcher.onBackPressed()
    }
    activityCardOperationResultBinding.cancelBtn.setOnClickListener {
      val intent = Intent(this, HomeActivity::class.java)
      startActivity(intent)
    }

    if (cardResult != null && !cardResult.cardType.isNullOrBlank()) {
      activityCardOperationResultBinding.cardTypeLabel.visibility = View.VISIBLE
      activityCardOperationResultBinding.cardTypeLabel.text =
          getString(R.string.card_type, cardResult.cardType)
    } else {
      activityCardOperationResultBinding.cardTypeLabel.visibility = View.GONE
    }

    when (resultType) {
      ResultType.LOADING -> {
        activityCardOperationResultBinding.animation.setAnimation("loading_anim.json")
        activityCardOperationResultBinding.animation.repeatCount = LottieDrawable.INFINITE
        activityCardOperationResultBinding.bigText.visibility = View.INVISIBLE
        activityCardOperationResultBinding.btnLayout.visibility = View.INVISIBLE
      }
      ResultType.SUCCESS -> {
        activityCardOperationResultBinding.mainBackground.setBackgroundColor(
            ContextCompat.getColor(this, R.color.green))
        activityCardOperationResultBinding.animation.setAnimation("tick_white.json")
        activityCardOperationResultBinding.animation.repeatCount = 0
        activityCardOperationResultBinding.animation.playAnimation()
        activityCardOperationResultBinding.bigText.setText(R.string.loading_success_label)
        activityCardOperationResultBinding.bigText.visibility = View.VISIBLE
        activityCardOperationResultBinding.btnLayout.visibility = View.INVISIBLE

        if (intent.getBooleanExtra(IS_PERSONALIZATION_RESULT, false)) {
          activityCardOperationResultBinding.bigText.setText(R.string.personalization_success_label)
        } else {
          activityCardOperationResultBinding.bigText.setText(R.string.loading_success_label)
        }

        scheduleAutoReturn(
            SUCCESS_RETURN_DELAY_MS, activityCardOperationResultBinding.autoReturnProgress)
      }
      else -> {
        activityCardOperationResultBinding.mainBackground.setBackgroundColor(
            ContextCompat.getColor(this, R.color.red))
        activityCardOperationResultBinding.animation.setAnimation("error_white.json")
        activityCardOperationResultBinding.animation.repeatCount = 0
        activityCardOperationResultBinding.animation.playAnimation()

        val message = intent.getStringExtra(MESSAGE) ?: ""
        if (intent.getBooleanExtra(IS_PERSONALIZATION_RESULT, false)) {
          activityCardOperationResultBinding.bigText.setText(R.string.personalization_failed_label)
          activityCardOperationResultBinding.bigText.append(":\n")
          activityCardOperationResultBinding.bigText.append(message)
        } else {
          activityCardOperationResultBinding.bigText.setText(R.string.loading_failed_label)
          activityCardOperationResultBinding.bigText.append(":\n")
          activityCardOperationResultBinding.bigText.append(message)
        }
        activityCardOperationResultBinding.bigText.visibility = View.VISIBLE
        activityCardOperationResultBinding.btnLayout.visibility = View.VISIBLE

        // Same as the "try again" button, unless the user chooses before: its label shows the
        // remaining seconds
        scheduleAutoReturn(
            ERROR_RETURN_DELAY_MS, activityCardOperationResultBinding.autoReturnProgress) { seconds
              ->
              activityCardOperationResultBinding.tryBtn.text =
                  if (seconds != null) getString(R.string.try_again_countdown, seconds)
                  else getString(R.string.try_again)
            }
      }
    }

    // Play sound
    val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.reading_sound)
    mediaPlayer.start()
  }

  companion object {
    const val RESULT_TYPE = "resultType"
    const val MESSAGE = "message"
    const val IS_PERSONALIZATION_RESULT = "isPersonalizationResult"
  }
}
