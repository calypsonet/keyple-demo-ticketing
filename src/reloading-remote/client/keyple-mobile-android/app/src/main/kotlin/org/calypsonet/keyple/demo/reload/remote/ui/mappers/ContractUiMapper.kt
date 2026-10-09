/* ******************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.reload.remote.ui.mappers

import android.content.res.Resources
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.domain.model.Contract
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiContract

private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

fun Contract.toUi(resources: Resources): UiContract =
    when (contractTariff) {
      PriorityCode.MULTI_TRIP ->
          UiContract(
              resources.getString(R.string.contract_multi_trip),
              counterValue?.let { resources.getQuantityString(R.plurals.trips_left, it, it) }
                  ?: resources.getString(R.string.no_counter),
              isValid)
      PriorityCode.SEASON_PASS ->
          UiContract(
              resources.getString(R.string.contract_season_pass),
              validityPeriod(resources),
              isValid)
      PriorityCode.EXPIRED ->
          UiContract(
              resources.getString(R.string.contract_season_pass_expired),
              validityPeriod(resources),
              isValid)
      PriorityCode.FORBIDDEN ->
          UiContract(resources.getString(R.string.contract_forbidden), "", isValid)
      else -> UiContract(resources.getString(R.string.contract_unknown), "", isValid)
    }

private fun Contract.validityPeriod(resources: Resources): String =
    resources.getString(
        R.string.validity_period,
        saleDate.format(dateFormatter),
        validityEndDate.format(dateFormatter))
