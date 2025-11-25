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
package org.calypsonet.keyple.demo.validation.domain.builders

import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.Location
import org.calypsonet.keyple.demo.validation.domain.model.ValidationData

object ValidationDataBuilder {

  fun buildFrom(event: EventStructure, locations: List<Location>): ValidationData {

    val location =
        locations.firstOrNull { it.id == event.eventLocation }
            ?: throw IllegalArgumentException("No location found for id=${event.eventLocation}")

    return ValidationData(
        name = "Event name",
        dateTime = event.eventDatetime,
        location = location,
        destination = null,
        provider = null)
  }
}
