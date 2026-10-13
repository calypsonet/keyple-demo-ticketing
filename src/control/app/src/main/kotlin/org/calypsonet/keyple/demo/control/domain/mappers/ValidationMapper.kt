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

import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.Location
import org.calypsonet.keyple.demo.control.domain.model.Validation

/**
 * Returns the last validation displayed by the control, built from this event read in the card.
 *
 * @param contract The contract used by the event, null if unknown.
 * @param locations The locations of the network, among which the location of the event.
 */
fun EventStructure.toValidation(
    contract: ContractStructure?,
    locations: List<Location>
): Validation =
    Validation(
        name = contract?.contractTariff?.label ?: "Event",
        dateTime = eventDatetime,
        location = toLocation(locations),
    )
