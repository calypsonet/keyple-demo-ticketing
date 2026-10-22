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
package org.calypsonet.keyple.demo.inspection.domain.model

/** Result of the inspection procedure, translated into texts by the UI. */
sealed interface InspectionResult {

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
  ) : InspectionResult

  /** The card has never been loaded (clean card). */
  data object EmptyCard : InspectionResult

  /**
   * The presented card cannot be selected (unknown or unsupported card).
   *
   * @property fileStructure The file structure of the Calypso application, in hexadecimal, when it
   *   is not supported.
   */
  data class NotRecognized(val error: SelectionError, val fileStructure: String? = null) :
      InspectionResult

  /** The card is rejected for the provided reason. */
  data class Rejected(val reason: RejectionReason) : InspectionResult

  /** A technical error prevented the inspection. */
  data class Failed(val error: TechnicalError) : InspectionResult
}
