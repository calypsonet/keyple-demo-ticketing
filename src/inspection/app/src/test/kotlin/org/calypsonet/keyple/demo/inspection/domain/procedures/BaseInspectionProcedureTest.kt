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
import org.assertj.core.api.Assertions.assertThat
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.common.reference.Location
import org.calypsonet.keyple.demo.inspection.domain.model.InspectionResult
import org.eclipse.keypop.reader.selection.spi.SmartCard
import org.junit.Test

/** Business rules of the inspection (data model of the demo, Inspection process). */
class BaseInspectionProcedureTest {

  private val rules =
      object : BaseInspectionProcedure() {
        override fun supports(card: SmartCard): Boolean = false

        override fun execute(context: InspectionContext): InspectionResult =
            throw UnsupportedOperationException()
      }

  private val inspectionLocation = Location(1, "Place de la Bastille")
  private val validationPeriodMinutes = 10

  /** Event of a validation at 08:30 (recorded to the minute) at the location of the inspection. */
  private val event = event(LocalDateTime.of(2026, 10, 15, 8, 30), location = 1)

  private fun isLastValidationValid(
      inspectionDateTime: LocalDateTime,
      event: EventStructure = this.event,
      validationPeriodMinutes: Int = this.validationPeriodMinutes
  ): Boolean =
      rules.isLastValidationValid(
          event, inspectionLocation, inspectionDateTime, validationPeriodMinutes)

  // ========== Validation period ==========

  @Test
  fun validation_isValid_duringTheValidationPeriod() {
    assertThat(isLastValidationValid(at(8, 35, 0))).isTrue()
  }

  @Test
  fun validation_isValid_untilTheEndOfTheMinuteOfTheEndOfTheValidationPeriod() {
    // The validation made at 08:30:59 is recorded at 08:30: it remains valid until 08:40:59
    assertThat(isLastValidationValid(at(8, 40, 59))).isTrue()
  }

  @Test
  fun validation_isNotValid_afterTheValidationPeriod() {
    assertThat(isLastValidationValid(at(8, 41, 0))).isFalse()
  }

  @Test
  fun validation_isValid_onlyDuringItsMinute_whenTheValidationPeriodIsZero() {
    assertThat(isLastValidationValid(at(8, 30, 59), validationPeriodMinutes = 0)).isTrue()
    assertThat(isLastValidationValid(at(8, 31, 0), validationPeriodMinutes = 0)).isFalse()
  }

  // ========== Location and day ==========

  @Test
  fun validation_isNotValid_atAnotherLocation() {
    assertThat(isLastValidationValid(at(8, 35, 0), event(event.eventDateTime, location = 2)))
        .isFalse()
  }

  @Test
  fun validation_isNotValid_theDayBefore_evenWithinTheValidationPeriod() {
    val eventTheDayBefore = event(LocalDateTime.of(2026, 10, 14, 23, 55), location = 1)

    assertThat(isLastValidationValid(at(0, 5, 0), eventTheDayBefore, 60)).isFalse()
  }

  // ========== Event displayed ==========

  @Test
  fun eventOfACardNeverValidated_isNotDisplayed() {
    val eventNeverValidated =
        event.copy(eventDateStamp = DateCompact(0), eventTimeStamp = TimeCompact(0))

    assertThat(rules.isValidationEvent(eventNeverValidated)).isFalse()
    assertThat(rules.isValidationEvent(event)).isTrue()
  }

  // ========== Test data ==========

  private fun at(hour: Int, minute: Int, second: Int): LocalDateTime =
      LocalDateTime.of(2026, 10, 15, hour, minute, second)

  private fun event(dateTime: LocalDateTime, location: Int): EventStructure =
      EventStructure(
          eventVersionNumber = VersionNumber.CURRENT_VERSION,
          eventDateStamp = DateCompact(dateTime.toLocalDate()),
          eventTimeStamp = TimeCompact(dateTime),
          eventLocation = location,
          eventContractUsed = 1,
          contractPriorities =
              listOf(
                  PriorityCode.MULTI_TRIP,
                  PriorityCode.FORBIDDEN,
                  PriorityCode.FORBIDDEN,
                  PriorityCode.FORBIDDEN))
}
