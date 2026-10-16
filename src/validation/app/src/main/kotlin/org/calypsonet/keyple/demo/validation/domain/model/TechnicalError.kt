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

/** Technical error preventing the validation of a card. */
enum class TechnicalError {
  /** The authentication of the MIFARE Classic sector failed. */
  MIFARE_CLASSIC_AUTHENTICATION_FAILED,
  /** The transaction with the MIFARE Classic card failed. */
  MIFARE_CLASSIC_TRANSACTION_FAILED,
  /** Unexpected error (card, reader or SAM). */
  UNEXPECTED
}
