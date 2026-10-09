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
package org.calypsonet.keyple.demo.control.domain.model

/** Result of the control procedure, translated into texts by the UI. */
sealed interface ControlResult {

  /**
   * The content of the card has been read.
   *
   * @property authenticationMode The authentication mode of the card used for the reading.
   * @property contracts The contracts present in the card.
   * @property lastValidation The last validation of the card, null if it has never been validated.
   */
  data class CardContent(
      val authenticationMode: AuthenticationMode,
      val contracts: List<Contract>,
      val lastValidation: Validation?
  ) : ControlResult

  /** The card has never been loaded (clean card). */
  data object EmptyCard : ControlResult

  /** The card is refused for the provided reason. */
  data class Rejected(val reason: RejectionReason) : ControlResult

  /**
   * A technical error prevented the control.
   *
   * @property detail The message of the error, if any.
   */
  data class Failed(val error: TechnicalError, val detail: String? = null) : ControlResult
}
