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
import java.util.*

/** Time in minutes, value = hour * 60 + minute (0 to 1,439). */
class TimeCompact {

  val value: Int

  constructor(value: Int) {
    this.value = value
  }

  constructor(date: LocalDateTime) {
    this.value = (date.minute + (date.hour * 60))
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as TimeCompact
    if (value != other.value) return false
    return true
  }

  override fun hashCode(): Int {
    return value
  }

  override fun toString(): String {
    return "$value"
  }
}
