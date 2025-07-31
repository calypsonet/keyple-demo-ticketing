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

import java.time.Duration
import java.time.LocalDate

/**
 * Number of days since January 1st, 2010 (being date 0).<br> Maximum value is 16,383, last complete
 * year being 2053.<br> All dates are in legal local time.
 */
class DateCompact {

  companion object {
    private val REF_DATE = LocalDate.of(2010, 1, 1)
  }

  val value: Int

  constructor(value: Int) {
    this.value = value
  }

  constructor(date: LocalDate) {
    this.value = Duration.between(REF_DATE.atStartOfDay(), date.atStartOfDay()).toDays().toInt()
  }

  fun getDate(): LocalDate {
    return REF_DATE.plusDays(value.toLong())
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as DateCompact
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
