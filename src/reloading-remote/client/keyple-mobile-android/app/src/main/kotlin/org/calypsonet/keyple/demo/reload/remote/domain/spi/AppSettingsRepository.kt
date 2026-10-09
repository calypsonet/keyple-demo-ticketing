/* ******************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.reload.remote.domain.spi

import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceType
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceVisibility
import org.calypsonet.keyple.demo.reload.remote.domain.model.ServerConfig

/** Port giving access to the persisted application settings. */
interface AppSettingsRepository {

  /** Address of the reloading server. */
  var serverConfig: ServerConfig

  /** Type of device (contactless card, SIM...) chosen by the user. */
  var deviceType: DeviceType

  /** Last known status of the server (true if the server and its SAM are ready). */
  var lastServerStatus: Boolean

  /** Returns the visibility of the given device type in the home screen. */
  fun getDeviceVisibility(device: DeviceType): DeviceVisibility

  /** Sets the visibility of the given device type in the home screen. */
  fun setDeviceVisibility(device: DeviceType, visibility: DeviceVisibility)
}
