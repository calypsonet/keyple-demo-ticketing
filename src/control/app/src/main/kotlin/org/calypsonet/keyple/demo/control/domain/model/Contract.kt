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
package org.calypsonet.keyple.demo.control.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/** Contract present in the card, with the result of its control. */
data class Contract(
    val name: String?,
    /**
     * Indicates whether the last validation used this contract, at the location of the control and
     * within the validation period.
     */
    val isValidated: Boolean,
    val validationDateTime: LocalDateTime?,
    val recordNumber: Int,
    /** Indicates whether the validity end date of the contract is in the past. */
    val isExpired: Boolean,
    val saleDate: LocalDate,
    val validityEndDate: LocalDate,
    val remainingTrips: Int? = null
)
