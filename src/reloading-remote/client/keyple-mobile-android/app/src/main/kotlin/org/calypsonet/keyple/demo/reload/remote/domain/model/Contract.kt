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
package org.calypsonet.keyple.demo.reload.remote.domain.model

import java.time.LocalDate
import org.calypsonet.keyple.demo.common.model.type.PriorityCode

/** Contract present in the card. */
data class Contract(
    val contractTariff: PriorityCode,
    /** Number of remaining trips of a multi-trip contract, null if there is no counter. */
    val counterValue: Int?,
    val saleDate: LocalDate,
    val validityEndDate: LocalDate,
    /** Indicates whether the contract can currently be used. */
    val isValid: Boolean
)
