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
package org.calypsonet.keyple.demo.control.domain.procedures

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.reference.Location

/** Base class of the control procedures, with the business rules common to them. */
abstract class BaseControlProcedure : ControlProcedure {

  /**
   * Indicates whether the last validation of the card is valid for the control (steps 7 to 9 of the
   * procedure): made at the location of the control, the same day, and within the validation
   * period.
   *
   * The time of the event being recorded to the minute, the times are compared to the minute: the
   * validation is valid until the end of the minute of the end of the validation period.
   */
  fun isLastValidationValid(
      event: EventStructure,
      controlLocation: Location,
      controlDateTime: LocalDateTime,
      validationPeriodMinutes: Int
  ): Boolean =
      event.eventLocation == controlLocation.id &&
          !event.eventDateTime.isBefore(controlDateTime.toLocalDate().atStartOfDay()) &&
          !event.eventDateTime
              .plusMinutes(validationPeriodMinutes.toLong())
              .isBefore(controlDateTime.truncatedTo(ChronoUnit.MINUTES))

  /**
   * Indicates whether the last event is a validation, displayed by the control: its date or time
   * has been set by a validation (the event of a card never validated having neither).
   */
  fun isValidationEvent(event: EventStructure): Boolean =
      event.eventTimeStamp.value != 0 || event.eventDateStamp.value != 0
}
