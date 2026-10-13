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
package org.calypsonet.keyple.demo.control.domain.builders

import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.control.domain.model.Contract

/**
 * Builds the contract displayed by the control, from the contract read in the card and from the
 * result of its control.
 *
 * @param contract The contract read in the card.
 * @param record The number of the record of the contract in the card.
 */
fun buildContract(
    contract: ContractStructure,
    record: Int,
    contractValidated: Boolean,
    contractExpired: Boolean,
    validationDateTime: LocalDateTime?,
    remainingTrips: Int?
): Contract =
    Contract(
        name = contract.contractTariff.label,
        valid = contractValidated,
        record = record,
        validationDateTime = validationDateTime,
        expired = contractExpired,
        contractValidityStartDate = contract.contractSaleDate.date,
        contractValidityEndDate = contract.contractValidityEndDate.date,
        remainingTrips = remainingTrips)
