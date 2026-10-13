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
package org.calypsonet.keyple.demo.control.domain.mappers

import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.control.domain.model.Contract

/**
 * Returns the contract displayed by the control, built from this contract read in the card and from
 * the result of its control.
 *
 * @param record The number of the record of the contract in the card.
 */
fun ContractStructure.toContract(
    record: Int,
    contractValidated: Boolean,
    contractExpired: Boolean,
    validationDateTime: LocalDateTime?,
    remainingTrips: Int?
): Contract =
    Contract(
        name = contractTariff.label,
        valid = contractValidated,
        record = record,
        validationDateTime = validationDateTime,
        expired = contractExpired,
        contractValidityStartDate = contractSaleDate.date,
        contractValidityEndDate = contractValidityEndDate.date,
        remainingTrips = remainingTrips)
