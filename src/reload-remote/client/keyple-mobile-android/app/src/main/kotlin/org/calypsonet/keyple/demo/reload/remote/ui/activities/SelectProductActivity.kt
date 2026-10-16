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
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivitySelectProductBinding

@AndroidEntryPoint
class SelectProductActivity : BaseActivity() {
  private lateinit var activitySelectProductBinding: ActivitySelectProductBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    activitySelectProductBinding = ActivitySelectProductBinding.inflate(layoutInflater)
    toolbarBinding = activitySelectProductBinding.appBarLayout
    setContentView(activitySelectProductBinding.root)

    activitySelectProductBinding.multiTrip1Label.text =
        resources.getQuantityString(R.plurals.trips_count, 1, 1)
    activitySelectProductBinding.multiTrip2Label.text =
        resources.getQuantityString(R.plurals.trips_count, 2, 2)
    activitySelectProductBinding.multiTrip3Label.text =
        resources.getQuantityString(R.plurals.trips_count, 3, 3)
    activitySelectProductBinding.multiTrip4Label.text =
        resources.getQuantityString(R.plurals.trips_count, 4, 4)
    activitySelectProductBinding.multiTrip1Price.text = getString(R.string.product_price, 1)
    activitySelectProductBinding.multiTrip2Price.text = getString(R.string.product_price, 2)
    activitySelectProductBinding.multiTrip3Price.text = getString(R.string.product_price, 3)
    activitySelectProductBinding.multiTrip4Price.text = getString(R.string.product_price, 4)
    activitySelectProductBinding.seasonPassPrice.text = getString(R.string.product_price, 20)

    activitySelectProductBinding.multiTrip1Btn.setOnClickListener {
      startCheckoutActivity(PriorityCode.MULTI_TRIP, 1)
    }
    activitySelectProductBinding.multiTrip2Btn.setOnClickListener {
      startCheckoutActivity(PriorityCode.MULTI_TRIP, 2)
    }
    activitySelectProductBinding.multiTrip3Btn.setOnClickListener {
      startCheckoutActivity(PriorityCode.MULTI_TRIP, 3)
    }
    activitySelectProductBinding.multiTrip4Btn.setOnClickListener {
      startCheckoutActivity(PriorityCode.MULTI_TRIP, 4)
    }

    activitySelectProductBinding.seasonPassBtn.setOnClickListener {
      startCheckoutActivity(PriorityCode.SEASON_PASS)
    }
  }

  private fun startCheckoutActivity(contractTariff: PriorityCode, tripsToLoad: Int? = null) {
    val intent = Intent(this, CheckoutActivity::class.java)
    intent.putExtra(CONTRACT_TARIFF, contractTariff.code)
    if (tripsToLoad != null) {
      intent.putExtra(TRIPS_TO_LOAD, tripsToLoad)
    }
    getIntent().getStringExtra(BaseCardActivity.APPLICATION_SERIAL_NUMBER)?.let {
      intent.putExtra(BaseCardActivity.APPLICATION_SERIAL_NUMBER, it)
    }
    startActivity(intent)
    this@SelectProductActivity.finish()
  }

  companion object {
    const val CONTRACT_TARIFF = "contractTariff"
    const val TRIPS_TO_LOAD = "tripsToLoad"
  }
}
