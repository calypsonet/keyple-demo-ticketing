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
package org.calypsonet.keyple.demo.validation.domain.model

/** Description of the presented card, displayed with the result of the validation. */
sealed interface CardDescription {

  /** Calypso card, identified by the DF name of its application, in hexadecimal. */
  data class Calypso(val dfName: String) : CardDescription

  /** Storage card, identified by the name of its product type (e.g. MIFARE_ULTRALIGHT). */
  data class Storage(val productType: String) : CardDescription
}
