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

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Number of days since January 1st, 2010 (being date 0).<br> Maximum value is 16,383, last complete
 * year being 2053.<br> All dates are in legal local time.
 */
data class DateCompact(val value: Int) {

  /** Creates the compact date of the provided date. */
  constructor(date: LocalDate) : this(ChronoUnit.DAYS.between(REFERENCE_DATE, date).toInt())

  /** The date coded by this value. */
  val date: LocalDate
    get() = REFERENCE_DATE.plusDays(value.toLong())

  override fun toString(): String = "$value"

  private companion object {
    val REFERENCE_DATE: LocalDate = LocalDate.of(2010, 1, 1)
  }
}
