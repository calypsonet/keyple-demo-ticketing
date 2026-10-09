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
   * @param contractPriority The contract priority/type
   * @return The amount to decrement
   */
  fun calculateDecrementAmount(contractPriority: PriorityCode): Int {
    return when (contractPriority) {
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
  fun sortContractPrioritiesByPriority(
      priorities: List<Pair<Int, PriorityCode>>
  ): List<Pair<Int, PriorityCode>> {
    return priorities.sortedBy { it.second.code }
  }

  /**
   * Checks if a priority code represents a contract with a counter (MULTI_TRIP).
   *
   * @param priority The priority code to check
   * @return true if the contract type uses a counter, false otherwise
   */
  fun isCounterBasedContract(priority: PriorityCode): Boolean {
    return priority == PriorityCode.MULTI_TRIP
  }

  // ========== Business rules, returning the reason of the refusal of the card ==========

  /**
   * Checks the environment: its version number must be the current one, and its end date must not
   * be in the past.
   *
   * @return The reason why the card is refused, null if the environment is valid.
   */
  fun checkEnvironment(
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
   * @return The reason why the card is refused, null if the version is the current one.
   */
  fun checkEventVersion(eventVersionNumber: VersionNumber): RejectionReason? =
      when (eventVersionNumber) {
        VersionNumber.CURRENT_VERSION -> null
        VersionNumber.UNDEFINED -> RejectionReason.NO_VALID_CONTRACT
        else -> RejectionReason.EVENT_WRONG_VERSION
      }

  /** Indicates whether the last event is within the anti-passback delay. */
  fun isWithinAntiPassbackDelay(
      lastEventDateTime: LocalDateTime,
      validationDateTime: LocalDateTime
  ): Boolean =
      Duration.between(lastEventDateTime, validationDateTime).toMinutes() <
          ANTI_PASSBACK_DELAY_MINUTES

  /**
   * Checks the version number of a contract.
   *
   * @return The reason why the card is refused, null if the version is the current one.
   */
  fun checkContractVersion(contractVersionNumber: VersionNumber): RejectionReason? =
      if (contractVersionNumber != VersionNumber.CURRENT_VERSION)
          RejectionReason.CONTRACT_WRONG_VERSION
      else null

  /** Indicates whether the validity end date of a contract is in the past. */
  fun isContractExpired(contractValidityEndDate: LocalDate, validationDate: LocalDate): Boolean =
      contractValidityEndDate.isBefore(validationDate)

  /** Indicates whether a multi-trip contract has trips left. */
  fun hasTripsLeft(counterValue: Int): Boolean = counterValue > 0
}
