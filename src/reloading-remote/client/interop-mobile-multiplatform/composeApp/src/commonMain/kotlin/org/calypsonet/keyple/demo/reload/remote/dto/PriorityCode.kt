/* ******************************************************************************
 * Copyright (c) 2024 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.reload.remote.dto

import kotlinx.serialization.Serializable

/**
 * Tariff of a contract (`PriorityCode` of the ticketing data model), exchanged by name.
 *
 * Mirror of `org.calypsonet.keyple.demo.common.model.type.PriorityCode`.
 */
@Serializable
enum class PriorityCode {
  FORBIDDEN,
  SEASON_PASS,
  MULTI_TRIP,
  EXPIRED,
  UNKNOWN
}
