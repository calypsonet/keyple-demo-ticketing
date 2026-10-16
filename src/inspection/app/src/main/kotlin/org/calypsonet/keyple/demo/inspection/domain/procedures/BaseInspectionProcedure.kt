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
package org.calypsonet.keyple.demo.inspection.domain.procedures

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.reference.Location

/** Base class of the inspection procedures, with the business rules common to them. */
abstract class BaseInspectionProcedure : InspectionProcedure {

  /**
   * Indicates whether the last validation of the card is valid for the inspection (steps 7 to 9 of
   * the procedure): made at the location of the inspection, the same day, and within the validation
   * period.
   *
   * The time of the event being recorded to the minute, the times are compared to the minute: the
   * validation is valid until the end of the minute of the end of the validation period.
   */
  fun isLastValidationValid(
      event: EventStructure,
      inspectionLocation: Location,
      inspectionDateTime: LocalDateTime,
      validationPeriodMinutes: Int
  ): Boolean =
      event.eventLocation == inspectionLocation.id &&
          !event.eventDateTime.isBefore(inspectionDateTime.toLocalDate().atStartOfDay()) &&
          !event.eventDateTime
              .plusMinutes(validationPeriodMinutes.toLong())
              .isBefore(inspectionDateTime.truncatedTo(ChronoUnit.MINUTES))

  /**
   * Indicates whether the last event is a validation, displayed by the inspection: its date or time
   * has been set by a validation (the event of a card never validated having neither).
   */
  fun isValidationEvent(event: EventStructure): Boolean =
      event.eventTimeStamp.value != 0 || event.eventDateStamp.value != 0
}
