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
package org.calypsonet.keyple.demo.inspection.domain.builders

import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.reference.Location
import org.calypsonet.keyple.demo.inspection.domain.model.Validation

/**
 * Builds the last validation displayed by the inspection, from the last event read in the card,
 * from the contract it used and from the locations of the network.
 *
 * @param event The last event read in the card.
 * @param contract The contract used by the event, null if unknown.
 * @param locations The locations of the network, among which the location of the event.
 * @throws IndexOutOfBoundsException If the location of the event is unknown.
 */
fun buildValidation(
    event: EventStructure,
    contract: ContractStructure?,
    locations: List<Location>
): Validation =
    Validation(
        name = contract?.contractTariff?.label ?: "Event",
        dateTime = event.eventDateTime,
        location = findLocation(event, locations))

/** Returns the location of the event, among the provided locations. */
private fun findLocation(event: EventStructure, locations: List<Location>): Location =
    locations.filter { event.eventLocation == it.id }[0]
