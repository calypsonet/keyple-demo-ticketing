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
package org.calypsonet.keyple.demo.reload.remote.domain.model

/** Visibility of a device type (contactless card, SIM...) in the home screen. */
enum class DeviceVisibility {
  /** The device type is displayed and can be selected. */
  ENABLE,
  /** The device type is displayed but cannot be selected. */
  DISABLE,
  /** The device type is not displayed. */
  HIDE
}
