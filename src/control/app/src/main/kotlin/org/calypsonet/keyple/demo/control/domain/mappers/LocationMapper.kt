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

import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.Location

/**
 * Returns the location of this event, among the provided locations of the network.
 *
 * @throws IndexOutOfBoundsException If the location of the event is unknown.
 */
fun EventStructure.toLocation(locations: List<Location>): Location =
    locations.filter { eventLocation == it.id }[0]
