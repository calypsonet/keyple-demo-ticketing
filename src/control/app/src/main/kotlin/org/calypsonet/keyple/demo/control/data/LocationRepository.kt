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
package org.calypsonet.keyple.demo.control.data

import android.content.Context
import org.calypsonet.keyple.demo.common.data.LocationRepository
import org.calypsonet.keyple.demo.common.model.Location
import javax.inject.Inject

class LocationRepository @Inject constructor(context: Context) {

  val locations: List<Location> = LocationRepository.getLocations()
}
