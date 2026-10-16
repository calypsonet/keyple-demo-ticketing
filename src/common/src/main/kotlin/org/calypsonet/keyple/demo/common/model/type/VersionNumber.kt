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
 * Version number of a structure of the card (EnvVersionNumber, EventVersionNumber and
 * ContractVersionNumber fields): see the data model in the README.
 *
 * @property code The value stored in the card.
 * @property label The description of the version.
 */
enum class VersionNumber(val code: Int, val label: String) {
  UNDEFINED(0, "Forbidden (undefined)"),
  CURRENT_VERSION(1, "Current version"),
  RESERVED(255, "Forbidden (reserved)"),
  UNKNOWN(-1, "Unknown");

  companion object {
    /** Returns the version number having the provided value, or [UNKNOWN] if there is none. */
    @JvmStatic fun fromCode(code: Int): VersionNumber = entries.find { it.code == code } ?: UNKNOWN
  }
}
