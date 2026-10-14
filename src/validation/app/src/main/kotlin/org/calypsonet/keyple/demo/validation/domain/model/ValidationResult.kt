/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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

import java.time.LocalDate
import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.reference.Location

/** Result of the validation procedure, translated into texts by the UI. */
sealed interface ValidationResult {

  /** The presented card, null if it is unknown. */
  val card: CardDescription?

  /**
   * The card is validated.
   *
   * @property dateTime The date and time of the validation.
   * @property location The location of the validation (location of the terminal).
   * @property remainingTrips The trips left of the multi-trip contract used, null otherwise.
   * @property seasonPassValidityEndDate The validity end date of the season pass used, null
   *   otherwise.
   */
  data class Accepted(
      override val card: CardDescription,
      val dateTime: LocalDateTime,
      val location: Location,
      val remainingTrips: Int? = null,
      val seasonPassValidityEndDate: LocalDate? = null
  ) : ValidationResult

  /**
   * The presented card cannot be selected (unknown or unsupported card).
   *
   * @property fileStructure The file structure of the Calypso application, in hexadecimal, when it
   *   is not supported.
   */
  data class NotSelected(val error: SelectionError, val fileStructure: String? = null) :
      ValidationResult {
    override val card: CardDescription?
      get() = null
  }

  /** The card is refused for the provided reason. */
  data class Rejected(override val card: CardDescription, val reason: RejectionReason) :
      ValidationResult

  /** The card has been removed during the transaction. */
  data class CardLost(override val card: CardDescription) : ValidationResult

  /** A technical error prevented the validation. */
  data class Failed(override val card: CardDescription?, val error: TechnicalError) :
      ValidationResult
}
