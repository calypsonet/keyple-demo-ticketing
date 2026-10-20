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
package org.calypsonet.keyple.demo.validation.domain.procedures

import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.validation.domain.model.RejectionReason
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.eclipse.keypop.reader.selection.spi.SmartCard
import org.junit.Test

/**
 * Business rules of the validation (annex "Card procedures" of the functional documentation,
 * section "Validation").
 */
class BaseValidationProcedureTest {

  private val rules =
      object : BaseValidationProcedure() {
        override fun supports(card: SmartCard): Boolean = false

        override fun execute(context: ValidationContext): ValidationResult =
            throw UnsupportedOperationException()
      }

  private val today = LocalDate.of(2026, 10, 15)

  // ========== Environment and versions ==========

  @Test
  fun environment_isRejected_whenItsVersionIsNotTheCurrentOne() {
    assertThat(rules.environmentRejection(VersionNumber.UNDEFINED, today.plusYears(1), today))
        .isEqualTo(RejectionReason.ENVIRONMENT_WRONG_VERSION)
  }

  @Test
  fun environment_isRejected_whenItsEndDateIsInThePast() {
    assertThat(rules.environmentRejection(VersionNumber.CURRENT_VERSION, today.minusDays(1), today))
        .isEqualTo(RejectionReason.ENVIRONMENT_EXPIRED)
  }

  @Test
  fun environment_isAccepted_untilItsEndDateIncluded() {
    assertThat(rules.environmentRejection(VersionNumber.CURRENT_VERSION, today, today)).isNull()
  }

  @Test
  fun eventOfACardNeverLoaded_meansNoValidContract() {
    assertThat(rules.eventVersionRejection(VersionNumber.UNDEFINED))
        .isEqualTo(RejectionReason.NO_VALID_CONTRACT)
  }

  @Test
  fun eventOfAnUnexpectedVersion_isRejected() {
    assertThat(rules.eventVersionRejection(VersionNumber.RESERVED))
        .isEqualTo(RejectionReason.EVENT_WRONG_VERSION)
    assertThat(rules.eventVersionRejection(VersionNumber.CURRENT_VERSION)).isNull()
  }

  @Test
  fun contractOfAnUnexpectedVersion_isRejected() {
    assertThat(rules.contractVersionRejection(VersionNumber.UNDEFINED))
        .isEqualTo(RejectionReason.CONTRACT_WRONG_VERSION)
    assertThat(rules.contractVersionRejection(VersionNumber.CURRENT_VERSION)).isNull()
  }

  // ========== Anti-passback ==========

  /** Time of the last validation, as recorded in the event (to the minute). */
  private val lastEventDateTime = LocalDateTime.of(2026, 10, 15, 8, 30)

  @Test
  fun antiPassback_rejectsASecondValidation_inTheSameMinute() {
    assertThat(rules.isWithinAntiPassbackDelay(lastEventDateTime, at(8, 30, 59))).isTrue()
  }

  @Test
  fun antiPassback_rejectsASecondValidation_aFewSecondsAfterAChangeOfMinute() {
    // First validation at 08:30:59 (recorded 08:30), second one 2 seconds later: the double
    // validation of the same ticket must be rejected
    assertThat(rules.isWithinAntiPassbackDelay(lastEventDateTime, at(8, 31, 1))).isTrue()
  }

  @Test
  fun antiPassback_rejectsUntilTheEndOfTheMinuteFollowingTheDelay() {
    assertThat(rules.isWithinAntiPassbackDelay(lastEventDateTime, at(8, 31, 59))).isTrue()
  }

  @Test
  fun antiPassback_acceptsAValidationAfterTheDelay() {
    assertThat(rules.isWithinAntiPassbackDelay(lastEventDateTime, at(8, 32, 0))).isFalse()
  }

  @Test
  fun antiPassback_acceptsAValidationOnAnotherDay() {
    assertThat(
            rules.isWithinAntiPassbackDelay(
                lastEventDateTime.minusDays(1), lastEventDateTime.plusSeconds(10)))
        .isFalse()
  }

  private fun at(hour: Int, minute: Int, second: Int): LocalDateTime =
      LocalDateTime.of(2026, 10, 15, hour, minute, second)

  // ========== Contracts ==========

  @Test
  fun contract_isValid_untilItsValidityEndDateIncluded() {
    assertThat(rules.isContractExpired(today, today)).isFalse()
    assertThat(rules.isContractExpired(today.minusDays(1), today)).isTrue()
  }

  @Test
  fun multiTripTicket_canBeUsed_whileTripsAreLeft() {
    assertThat(rules.hasTripsLeft(1)).isTrue()
    assertThat(rules.hasTripsLeft(0)).isFalse()
  }

  @Test
  fun onlyTheMultiTripTicket_usesACounter_decrementedByOneTrip() {
    assertThat(rules.isCounterBasedContract(PriorityCode.MULTI_TRIP)).isTrue()
    assertThat(rules.isCounterBasedContract(PriorityCode.SEASON_PASS)).isFalse()
    assertThat(rules.calculateDecrementAmount(PriorityCode.MULTI_TRIP)).isEqualTo(1)
    assertThat(rules.calculateDecrementAmount(PriorityCode.SEASON_PASS)).isZero()
  }

  // ========== Best contract search ==========

  @Test
  fun emptyExpiredAndUnknownContracts_areNotAnalyzed() {
    val priorities =
        listOf(
            1 to PriorityCode.FORBIDDEN,
            2 to PriorityCode.EXPIRED,
            3 to PriorityCode.UNKNOWN,
            4 to PriorityCode.MULTI_TRIP)

    assertThat(rules.filterValidContractPriorities(priorities))
        .containsExactly(4 to PriorityCode.MULTI_TRIP)
  }

  @Test
  fun seasonPass_isAnalyzedBeforeMultiTripTicket_whateverTheirRecords() {
    val priorities =
        listOf(
            1 to PriorityCode.MULTI_TRIP,
            2 to PriorityCode.SEASON_PASS,
            3 to PriorityCode.MULTI_TRIP)

    assertThat(rules.sortContractPriorities(priorities))
        .containsExactly(
            2 to PriorityCode.SEASON_PASS,
            1 to PriorityCode.MULTI_TRIP,
            3 to PriorityCode.MULTI_TRIP)
  }
}
