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
package org.calypsonet.keyple.demo.validation.domain.spi

import java.time.LocalDateTime

/**
 * Source of the current date and time of the terminal.
 *
 * The domain never reads the system clock directly: the clock is a platform service, provided by an
 * adapter, which can be replaced by a fixed date (for example, in the tests).
 */
interface Clock {

  /**
   * Returns the current local date and time of the terminal.
   *
   * @return A not null [LocalDateTime].
   */
  fun now(): LocalDateTime
}
