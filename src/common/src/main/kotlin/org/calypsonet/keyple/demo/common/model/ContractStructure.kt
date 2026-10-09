/* ******************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.common.model

import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.VersionNumber

/**
 * Contract record of the card (see the data model in the README), with the value of its counter.
 *
 * @property counterValue The value of the counter associated with the contract (number of trips
 *   left), null if it has not been read.
 */
data class ContractStructure
@JvmOverloads
constructor(
    val contractVersionNumber: VersionNumber,
    val contractTariff: PriorityCode,
    val contractSaleDate: DateCompact,
    val contractValidityEndDate: DateCompact,
    val contractSaleSam: Int?,
    val contractSaleCounter: Int?,
    val contractAuthKvc: Int?,
    val contractAuthenticator: Int?,
    val counterValue: Int? = null
) {

  /** Returns a copy of this contract with the provided counter value. */
  fun withCounterValue(counterValue: Int?): ContractStructure = copy(counterValue = counterValue)
}
