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
package org.calypsonet.keyple.demo.control.domain.spi

import org.calypsonet.keyple.demo.common.model.Location
import org.calypsonet.keyple.demo.control.domain.model.TerminalType

/** Port giving access to the application settings chosen by the user. */
interface AppSettingsRepository {

  /** The type of reader (terminal) the application runs on. */
  var terminalType: TerminalType

  /** The location where the control takes place. */
  var location: Location

  /** The period (in minutes) during which a validation is considered valid. */
  var validationPeriod: Int
}
