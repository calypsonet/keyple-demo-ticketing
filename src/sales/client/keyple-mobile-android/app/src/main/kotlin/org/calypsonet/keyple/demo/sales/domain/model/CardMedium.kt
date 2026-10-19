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
package org.calypsonet.keyple.demo.sales.domain.model

/**
 * Medium holding the card application: a contactless card (NFC reader) or a SIM card (OMAPI
 * reader).
 */
enum class CardMedium {
  CONTACTLESS_CARD,
  SIM;

  companion object {
    /** Returns the card medium having the given name, or [CONTACTLESS_CARD] if it is unknown. */
    fun fromName(name: String): CardMedium =
        entries.firstOrNull { it.name == name.uppercase() } ?: CONTACTLESS_CARD
  }
}
