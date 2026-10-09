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

/** Result of the validation procedure, translated into texts by the UI. */
sealed interface ValidationResult {

  /** The presented card. */
  val card: CardDescription

  /**
   * The card is validated.
   *
   * @property dateTime The date and time of the validation.
   * @property validationData The validation event written in the card, null when a validation
   *   interrupted by the removal of the card has been recovered.
   * @property remainingTrips The trips left of the multi-trip contract used, null otherwise.
   * @property passValidityEndDate The validity end date of the season pass used, null otherwise.
   */
  data class Accepted(
      override val card: CardDescription,
      val dateTime: LocalDateTime,
      val validationData: ValidationData?,
      val remainingTrips: Int? = null,
      val passValidityEndDate: LocalDate? = null
  ) : ValidationResult

  /** The card is refused for the provided reason. */
  data class Rejected(override val card: CardDescription, val reason: RejectionReason) :
      ValidationResult

  /** The card has been removed during the transaction. */
  data class CardLost(override val card: CardDescription) : ValidationResult

  /**
   * A technical error prevented the validation.
   *
   * @property detail The message of the error, if any.
   */
  data class Failed(
      override val card: CardDescription,
      val error: TechnicalError,
      val detail: String? = null
  ) : ValidationResult
}
