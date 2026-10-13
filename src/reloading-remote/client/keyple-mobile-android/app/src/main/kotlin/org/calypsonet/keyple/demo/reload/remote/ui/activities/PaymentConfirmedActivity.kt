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
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityPaymentConfirmedBinding

@AndroidEntryPoint
class PaymentConfirmedActivity : BaseActivity() {

  private lateinit var activityPaymentConfirmedBinding: ActivityPaymentConfirmedBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityPaymentConfirmedBinding = ActivityPaymentConfirmedBinding.inflate(layoutInflater)
    toolbarBinding = activityPaymentConfirmedBinding.appBarLayout
    setContentView(activityPaymentConfirmedBinding.root)
    activityPaymentConfirmedBinding.chargeBtn.text = getString(R.string.load_card)
    activityPaymentConfirmedBinding.chargeBtn.setOnClickListener {
      val intent = Intent(this, LoadContractActivity::class.java)
      intent.putExtras(getIntent())
      startActivity(intent)
      this@PaymentConfirmedActivity.finish()
    }
  }

  override fun onResume() {
    super.onResume()
    activityPaymentConfirmedBinding.animation.setAnimation("tick_anim.json")
    activityPaymentConfirmedBinding.animation.repeatCount = 0
    activityPaymentConfirmedBinding.animation.playAnimation()
  }
}
