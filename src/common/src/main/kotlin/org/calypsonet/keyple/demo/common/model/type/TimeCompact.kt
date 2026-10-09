/* ******************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.common.model.type

import java.time.LocalDateTime

/** Time in minutes, value = hour * 60 + minute (0 to 1,439). */
data class TimeCompact(val value: Int) {

  /** Creates the compact time of the time of the provided date and time. */
  constructor(dateTime: LocalDateTime) : this(dateTime.hour * 60 + dateTime.minute)

  override fun toString(): String = "$value"
}
