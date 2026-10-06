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
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardTitle
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiCardTitle

private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

fun CardTitle.toUi(resources: Resources): UiCardTitle =
    when (contractTariff) {
      PriorityCode.MULTI_TRIP ->
          UiCardTitle(
              resources.getString(R.string.title_multi_trip),
              counterValue?.let { resources.getQuantityString(R.plurals.trips_left, it, it) }
                  ?: resources.getString(R.string.no_counter),
              isValid)
      PriorityCode.SEASON_PASS ->
          UiCardTitle(
              resources.getString(R.string.title_season_pass), validityPeriod(resources), isValid)
      PriorityCode.EXPIRED ->
          UiCardTitle(
              resources.getString(R.string.title_season_pass_expired),
              validityPeriod(resources),
              isValid)
      PriorityCode.FORBIDDEN ->
          UiCardTitle(resources.getString(R.string.title_forbidden), "", isValid)
      else -> UiCardTitle(resources.getString(R.string.title_unknown), "", isValid)
    }

private fun CardTitle.validityPeriod(resources: Resources): String =
    resources.getString(
        R.string.validity_period,
        saleDate.format(dateFormatter),
        validityEndDate.format(dateFormatter))
