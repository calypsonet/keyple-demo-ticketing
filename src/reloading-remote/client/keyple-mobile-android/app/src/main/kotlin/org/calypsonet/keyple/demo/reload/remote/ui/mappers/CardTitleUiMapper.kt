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

import java.time.format.DateTimeFormatter
import java.util.Locale
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardTitle
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiCardTitle

private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

fun CardTitle.toUi(): UiCardTitle =
    when (contractTariff) {
      PriorityCode.MULTI_TRIP ->
          UiCardTitle(
              "Multi trip",
              counterValue?.let { if (it > 1) "$it trips left" else "$it trip left" }
                  ?: "No counter",
              isValid)
      PriorityCode.SEASON_PASS -> UiCardTitle("Season pass", validityPeriod(), isValid)
      PriorityCode.EXPIRED -> UiCardTitle("Season pass - Expired", validityPeriod(), isValid)
      PriorityCode.FORBIDDEN -> UiCardTitle("FORBIDDEN", "", isValid)
      PriorityCode.STORED_VALUE -> UiCardTitle("STORED_VALUE", "", isValid)
      else -> UiCardTitle("UNKNOWN", "", isValid)
    }

private fun CardTitle.validityPeriod(): String =
    "From ${saleDate.format(dateFormatter)} to ${validityEndDate.format(dateFormatter)}"
