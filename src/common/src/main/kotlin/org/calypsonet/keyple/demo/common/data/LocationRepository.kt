/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.common.data

import org.calypsonet.keyple.demo.common.model.Location

object LocationRepository {

  private val locationList: List<Location> =
      listOf(
          Location(0, "Bruxelles"),
          Location(1, "Konstanz"),
          Location(2, "Lisboa"),
          Location(3, "Milan"),
          Location(4, "Munich"),
          Location(5, "Paris"),
          Location(6, "Riga"),
          Location(7, "Roma"),
          Location(8, "Strasbourg"),
          Location(9, "Torino"),
          Location(10, "Venice"),
          Location(11, "Barcelona"))

  fun getLocations(): List<Location> {
    return locationList
  }
}
