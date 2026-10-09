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
package org.calypsonet.keyple.demo.control.ui.activities.cardcontent

import android.os.Bundle
import android.view.View
import androidx.core.content.IntentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.databinding.ActivityCardContentBinding
import org.calypsonet.keyple.demo.control.databinding.ToolbarBinding
import org.calypsonet.keyple.demo.control.setDivider
import org.calypsonet.keyple.demo.control.ui.activities.BaseActivity
import org.calypsonet.keyple.demo.control.ui.activities.CardReaderActivity.Companion.CARD_CONTENT
import org.calypsonet.keyple.demo.control.ui.model.UiControlResult
import timber.log.Timber

@AndroidEntryPoint
class CardContentActivity : BaseActivity() {

  private lateinit var activityCardContentBinding: ActivityCardContentBinding
  private lateinit var toolbarBinding: ToolbarBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCardContentBinding = ActivityCardContentBinding.inflate(layoutInflater)
    toolbarBinding = activityCardContentBinding.appBarLayout
    setContentView(activityCardContentBinding.root)
    setSupportActionBar(toolbarBinding.toolbar)
    activityCardContentBinding.presentBtn.setOnClickListener {
      onBackPressedDispatcher.onBackPressed()
    }
    val cardContent: UiControlResult =
        IntentCompat.getParcelableExtra(intent, CARD_CONTENT, UiControlResult::class.java)!!
    activityCardContentBinding.lastValidationList.layoutManager = LinearLayoutManager(this)
    activityCardContentBinding.titlesList.layoutManager = LinearLayoutManager(this)
    if (cardContent.titlesList.isNotEmpty()) {
      activityCardContentBinding.titlesList.adapter = TitlesRecyclerAdapter(cardContent.titlesList)
      activityCardContentBinding.titlesList.visibility = View.VISIBLE
      activityCardContentBinding.emptyContract.visibility = View.GONE
    } else {
      activityCardContentBinding.titlesList.visibility = View.GONE
      activityCardContentBinding.emptyContract.visibility = View.VISIBLE
    }
    if (cardContent.lastValidationsList != null) {
      activityCardContentBinding.lastValidationListContainer.visibility = View.VISIBLE
      activityCardContentBinding.lastValidationList.adapter =
          ValidationsRecyclerAdapter(cardContent.lastValidationsList)
      activityCardContentBinding.lastValidationList.setDivider(R.drawable.recycler_view_divider)
    } else {
      activityCardContentBinding.lastValidationListContainer.visibility = View.GONE
    }
  }

  override fun onResume() {
    super.onResume()
    if (ticketingService.readersInitialized) {
      ticketingService.stopNfcDetection()
      Timber.d("stopNfcDetection")
    }
  }
}
