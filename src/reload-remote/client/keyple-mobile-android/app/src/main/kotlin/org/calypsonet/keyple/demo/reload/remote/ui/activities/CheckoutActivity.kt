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
import java.text.SimpleDateFormat
import java.util.Calendar
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityCheckoutBinding

@AndroidEntryPoint
class CheckoutActivity : BaseActivity() {

  private lateinit var activityCheckoutBinding: ActivityCheckoutBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityCheckoutBinding = ActivityCheckoutBinding.inflate(layoutInflater)
    toolbarBinding = activityCheckoutBinding.appBarLayout
    setContentView(activityCheckoutBinding.root)

    val contractTariff =
        PriorityCode.fromCode(
            intent.getIntExtra(SelectProductActivity.CONTRACT_TARIFF, PriorityCode.MULTI_TRIP.code))
    val tripsToLoad: Int = intent.getIntExtra(SelectProductActivity.TRIPS_TO_LOAD, 0)

    if (contractTariff == PriorityCode.SEASON_PASS) {
      activityCheckoutBinding.selectionLabel.text = getString(R.string.season_pass_label)
      activityCheckoutBinding.selectionPrice.text = getString(R.string.product_price, 20)
    } else {
      activityCheckoutBinding.selectionLabel.text =
          resources.getQuantityString(R.plurals.trips_count, tripsToLoad, tripsToLoad)
      activityCheckoutBinding.selectionPrice.text = getString(R.string.product_price, tripsToLoad)
    }
    activityCheckoutBinding.validateBtn.setOnClickListener {
      val intent = Intent(this, PaymentConfirmedActivity::class.java)
      intent.putExtras(getIntent())
      startActivity(intent)
      this@CheckoutActivity.finish()
    }

    val now = Calendar.getInstance().time
    val expiryDateFormat = SimpleDateFormat("MM/yy")
    activityCheckoutBinding.expiryValue.text = expiryDateFormat.format(now)
  }
}
