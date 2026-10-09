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
package org.calypsonet.keyple.demo.reload.remote.domain.mappers

import java.time.LocalDate
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.reload.remote.domain.model.Contract

/**
 * Builds the contract present in a contract record, and evaluates its validity at the provided
 * date.
 */
fun ContractStructure.toContract(today: LocalDate): Contract {
  val saleDate = contractSaleDate.getDate()
  val validityEndDate = contractValidityEndDate.getDate()
  val isValid =
      when (contractTariff) {
        PriorityCode.MULTI_TRIP -> (counterValue ?: 0) >= 1
        PriorityCode.SEASON_PASS -> !saleDate.isAfter(today) && !validityEndDate.isBefore(today)
        else -> false
      }
  return Contract(contractTariff, counterValue, saleDate, validityEndDate, isValid)
}
