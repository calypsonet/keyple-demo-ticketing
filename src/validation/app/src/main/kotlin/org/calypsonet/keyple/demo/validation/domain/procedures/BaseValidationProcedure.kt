/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
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

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.validation.domain.model.RejectionReason

/** Base class of the validation procedures, with the business rules common to them. */
abstract class BaseValidationProcedure : ValidationProcedure {

  private companion object {
    const val SINGLE_VALIDATION_AMOUNT = 1
    const val ANTI_PASSBACK_DELAY_MINUTES = 1L
  }

  /**
   * Calculates the amount to decrement from the counter based on the contract type.
   *
   * @param contractTariff The tariff of the contract (its type).
   * @return The amount to decrement
   */
  fun calculateDecrementAmount(contractTariff: PriorityCode): Int {
    return when (contractTariff) {
      PriorityCode.MULTI_TRIP -> SINGLE_VALIDATION_AMOUNT
      else -> 0
    }
  }

  /**
   * Filters a list of contract priorities to keep only valid ones (not FORBIDDEN, EXPIRED or
   * UNKNOWN).
   *
   * @param priorities List of pairs (contract index, priority code)
   * @return Filtered list of valid contract priorities
   */
  fun filterValidContractPriorities(
      priorities: List<Pair<Int, PriorityCode>>
  ): List<Pair<Int, PriorityCode>> {
    return priorities.filter { (_, priority) ->
      priority != PriorityCode.FORBIDDEN &&
          priority != PriorityCode.EXPIRED &&
          priority != PriorityCode.UNKNOWN
    }
  }

  /**
   * Sorts contract priorities by their priority key (lower value = higher priority).
   *
   * @param priorities List of pairs (contract index, priority code)
   * @return Sorted list of contract priorities
   */
  fun sortContractPriorities(
      priorities: List<Pair<Int, PriorityCode>>
  ): List<Pair<Int, PriorityCode>> {
    return priorities.sortedBy { it.second.code }
  }

  /**
   * Indicates whether a contract uses a counter (multi-trip ticket).
   *
   * @param contractTariff The tariff of the contract (its type).
   * @return true if the contract type uses a counter, false otherwise
   */
  fun isCounterBasedContract(contractTariff: PriorityCode): Boolean {
    return contractTariff == PriorityCode.MULTI_TRIP
  }

  // ========== Business rules, returning the reason of the rejection of the card ==========

  /**
   * Checks the environment: its version number must be the current one, and its end date must not
   * be in the past.
   *
   * @return The reason why the card is rejected, null if the environment is valid.
   */
  fun environmentRejection(
      envVersionNumber: VersionNumber,
      envEndDate: LocalDate,
      validationDate: LocalDate
  ): RejectionReason? =
      when {
        envVersionNumber != VersionNumber.CURRENT_VERSION ->
            RejectionReason.ENVIRONMENT_WRONG_VERSION
        envEndDate.isBefore(validationDate) -> RejectionReason.ENVIRONMENT_EXPIRED
        else -> null
      }

  /**
   * Checks the version number of the last event: an undefined version means that the card has never
   * been loaded.
   *
   * @return The reason why the card is rejected, null if the version is the current one.
   */
  fun eventVersionRejection(eventVersionNumber: VersionNumber): RejectionReason? =
      when (eventVersionNumber) {
        VersionNumber.CURRENT_VERSION -> null
        VersionNumber.UNDEFINED -> RejectionReason.NO_VALID_CONTRACT
        else -> RejectionReason.EVENT_WRONG_VERSION
      }

  /**
   * Indicates whether the last event is within the anti-passback delay.
   *
   * The time of the event being recorded to the minute (seconds lost), the times are compared to
   * the minute: the card is rejected until the end of the minute following the delay, the effective
   * delay being never shorter than the configured one (between 1 and 2 minutes for 1 minute).
   * Comparing the exact time with the truncated one would accept a second validation a few seconds
   * after the first one, as soon as the minute changes.
   */
  fun isWithinAntiPassbackDelay(
      lastEventDateTime: LocalDateTime,
      validationDateTime: LocalDateTime
  ): Boolean =
      Duration.between(lastEventDateTime, validationDateTime.truncatedTo(ChronoUnit.MINUTES))
          .toMinutes() <= ANTI_PASSBACK_DELAY_MINUTES

  /**
   * Checks the version number of a contract.
   *
   * @return The reason why the card is rejected, null if the version is the current one.
   */
  fun contractVersionRejection(contractVersionNumber: VersionNumber): RejectionReason? =
      if (contractVersionNumber != VersionNumber.CURRENT_VERSION)
          RejectionReason.CONTRACT_WRONG_VERSION
      else null

  /** Indicates whether the validity end date of a contract is in the past. */
  fun isContractExpired(contractValidityEndDate: LocalDate, validationDate: LocalDate): Boolean =
      contractValidityEndDate.isBefore(validationDate)

  /** Indicates whether a multi-trip contract has trips left. */
  fun hasTripsLeft(counterValue: Int): Boolean = counterValue > 0
}
