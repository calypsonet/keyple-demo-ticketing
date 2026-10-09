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

/**
 * Code of the ContractTariff field of a contract (type of contract) and of the ContractPriority
 * fields of the event (priority of a contract record): see the data model in the README.
 *
 * @property code The value stored in the card.
 * @property label The description of the code.
 */
enum class PriorityCode(val code: Int, val label: String) {
  FORBIDDEN(0, "Forbidden (present in clean records only)"),
  SEASON_PASS(1, "Season Pass"),
  MULTI_TRIP(2, "Multi-trip ticket"),
  EXPIRED(31, "Expired"),
  UNKNOWN(-1, "Unknown");

  companion object {
    /** Returns the priority code having the provided value, or [UNKNOWN] if there is none. */
    @JvmStatic fun fromCode(code: Int): PriorityCode = entries.find { it.code == code } ?: UNKNOWN
  }
}
