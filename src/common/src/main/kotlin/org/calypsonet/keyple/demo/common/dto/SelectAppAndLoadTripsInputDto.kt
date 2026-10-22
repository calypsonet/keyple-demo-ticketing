/* ******************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.common.dto

/**
 * - applicationSerialNumber: Serial number of the card read before (output of the service
 *   SELECT_APP_AND_READ_CONTRACTS): the trips are only loaded in this card.
 * - tripsToLoad: Number of trips to load.
 */
data class SelectAppAndLoadTripsInputDto(var applicationSerialNumber: String, var tripsToLoad: Int)
