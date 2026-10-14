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
package org.calypsonet.keyple.demo.reload.remote.ui.activities.cardcontent

import android.content.Intent
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityCardContentBinding
import org.calypsonet.keyple.demo.reload.remote.ui.activities.BaseActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.BaseCardActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.SelectProductActivity
import org.calypsonet.keyple.demo.reload.remote.ui.model.ResultType
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiCardResult

@AndroidEntryPoint
class CardContentActivity : BaseActivity() {

  private lateinit var contractsLayoutManager: LinearLayoutManager
  private lateinit var contractsAdapter: ContractsRecyclerAdapter
  private lateinit var activityCardContentBinding: ActivityCardContentBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCardContentBinding = ActivityCardContentBinding.inflate(layoutInflater)
    toolbarBinding = activityCardContentBinding.appBarLayout
    setContentView(activityCardContentBinding.root)

    val cardContent: UiCardResult =
        IntentCompat.getParcelableExtra(
            intent, BaseCardActivity.CARD_CONTENT, UiCardResult::class.java)!!

    contractsLayoutManager = LinearLayoutManager(this)
    activityCardContentBinding.contractsList.layoutManager = contractsLayoutManager

    contractsAdapter = ContractsRecyclerAdapter(cardContent.contracts)
    activityCardContentBinding.contractsList.adapter = contractsAdapter

    when (cardContent.resultType) {
      ResultType.INVALID_CARD -> {
        activityCardContentBinding.animation.setAnimation("error_orange_anim.json")
        activityCardContentBinding.animation.playAnimation()
        activityCardContentBinding.bigText.setText(R.string.card_invalid_label)
        activityCardContentBinding.bigText.setTextColor(
            ContextCompat.getColor(this, R.color.orange))
        activityCardContentBinding.smallDesc.text = cardContent.errorMessage
        activityCardContentBinding.smallDesc.setTextColor(
            ContextCompat.getColor(this, R.color.orange))
        activityCardContentBinding.buyBtn.visibility = View.INVISIBLE
        activityCardContentBinding.contractsList.visibility = View.GONE
        activityCardContentBinding.lastValidationContent.visibility = View.GONE
        activityCardContentBinding.contentTitle.visibility = View.GONE
        scheduleErrorAutoReturn(R.color.orange)
      }
      ResultType.CONTRACTS_FOUND,
      ResultType.SUCCESS -> {
        activityCardContentBinding.contractsList.visibility = View.VISIBLE
        activityCardContentBinding.animation.visibility = View.GONE
        activityCardContentBinding.bigText.visibility = View.GONE
        activityCardContentBinding.smallDesc.visibility = View.INVISIBLE
        activityCardContentBinding.buyBtn.visibility = View.VISIBLE
        activityCardContentBinding.lastValidationContent.visibility = View.VISIBLE
        activityCardContentBinding.contentTitle.visibility = View.VISIBLE
      }
      ResultType.EMPTY_CARD -> {
        activityCardContentBinding.animation.setAnimation("error_anim.json")
        activityCardContentBinding.animation.playAnimation()
        activityCardContentBinding.bigText.text = getString(R.string.no_contract_label)
        activityCardContentBinding.bigText.setTextColor(ContextCompat.getColor(this, R.color.red))
        activityCardContentBinding.smallDesc.visibility = View.VISIBLE
        activityCardContentBinding.smallDesc.setTextColor(ContextCompat.getColor(this, R.color.red))
        activityCardContentBinding.smallDesc.text = getString(R.string.no_contract_desc)
        activityCardContentBinding.buyBtn.visibility = View.VISIBLE
        activityCardContentBinding.contractsList.visibility = View.GONE
        activityCardContentBinding.lastValidationContent.visibility = View.VISIBLE
        activityCardContentBinding.contentTitle.visibility = View.GONE
      }
      ResultType.ERROR -> {
        activityCardContentBinding.animation.setAnimation("error_anim.json")
        activityCardContentBinding.animation.playAnimation()
        if (cardContent.errorMessage != null)
            activityCardContentBinding.bigText.text = cardContent.errorMessage
        else activityCardContentBinding.bigText.setText(R.string.error_label)
        activityCardContentBinding.bigText.setTextColor(ContextCompat.getColor(this, R.color.red))
        activityCardContentBinding.smallDesc.visibility = View.INVISIBLE
        activityCardContentBinding.buyBtn.visibility = View.INVISIBLE
        activityCardContentBinding.contractsList.visibility = View.GONE
        activityCardContentBinding.lastValidationContent.visibility = View.GONE
        activityCardContentBinding.contentTitle.visibility = View.GONE
        scheduleErrorAutoReturn(R.color.red)
      }
      else -> {
        activityCardContentBinding.animation.setAnimation("error_anim.json")
        activityCardContentBinding.animation.playAnimation()
        activityCardContentBinding.bigText.setText(R.string.error_label)
        activityCardContentBinding.bigText.setTextColor(ContextCompat.getColor(this, R.color.red))
        activityCardContentBinding.smallDesc.visibility = View.INVISIBLE
        activityCardContentBinding.buyBtn.visibility = View.INVISIBLE
        activityCardContentBinding.contractsList.visibility = View.GONE
        activityCardContentBinding.lastValidationContent.visibility = View.GONE
        activityCardContentBinding.contentTitle.visibility = View.GONE
        scheduleErrorAutoReturn(R.color.red)
      }
    }

    if (!cardContent.cardType.isNullOrBlank()) {
      activityCardContentBinding.cardTypeLabel.visibility = View.VISIBLE
      activityCardContentBinding.cardTypeLabel.text =
          getString(R.string.card_type, cardContent.cardType)
    }

    activityCardContentBinding.animation.playAnimation()

    // Play sound
    val mp: MediaPlayer = MediaPlayer.create(this, R.raw.reading_sound)
    mp.start()
    activityCardContentBinding.buyBtn.setOnClickListener {
      val intent = Intent(this, SelectProductActivity::class.java)
      getIntent().getStringExtra(BaseCardActivity.APPLICATION_SERIAL_NUMBER)?.let {
        intent.putExtra(BaseCardActivity.APPLICATION_SERIAL_NUMBER, it)
      }
      startActivity(intent)
      this@CardContentActivity.finish()
    }
  }

  /**
   * Returns to the card presentation screen (or the home screen) to try again after the error
   * display duration, the remaining time being shown by a bar of the color of the message.
   */
  private fun scheduleErrorAutoReturn(@ColorRes color: Int) {
    val tint = ColorStateList.valueOf(ContextCompat.getColor(this, color))
    activityCardContentBinding.autoReturnProgress.progressTintList = tint
    activityCardContentBinding.autoReturnProgress.progressBackgroundTintList = tint
    scheduleAutoReturn(ERROR_RETURN_DELAY_MS, activityCardContentBinding.autoReturnProgress)
  }
}
