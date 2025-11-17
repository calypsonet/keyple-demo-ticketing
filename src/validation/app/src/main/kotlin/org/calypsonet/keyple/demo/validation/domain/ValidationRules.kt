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
package org.calypsonet.keyple.demo.validation.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.validation.data.model.Messages
import org.calypsonet.keyple.demo.validation.data.model.Status

/** Business rules for card validation. */
object ValidationRules {

  private const val SINGLE_VALIDATION_AMOUNT = 1
  private const val ANTI_PASSBACK_DELAY_MINUTES = 1L

  /**
   * Calculates the amount to decrement from the counter based on contract type.
   *
   * @param contractPriority The contract priority/type
   * @param validationAmount The validation amount for stored-value contracts
   * @return The amount to decrement
   */
  fun calculateDecrementAmount(contractPriority: PriorityCode, validationAmount: Int): Int {
    return when (contractPriority) {
      PriorityCode.MULTI_TRIP -> SINGLE_VALIDATION_AMOUNT
      PriorityCode.STORED_VALUE -> validationAmount
      else -> 0
    }
  }

  /**
   * Filters a list of contract priorities to keep only valid ones (not FORBIDDEN or EXPIRED).
   *
   * @param priorities List of pairs (contract index, priority code)
   * @return Filtered list of valid contract priorities
   */
  fun filterValidContractPriorities(
      priorities: List<Pair<Int, PriorityCode>>
  ): List<Pair<Int, PriorityCode>> {
    return priorities.filter { (_, priority) ->
      priority != PriorityCode.FORBIDDEN && priority != PriorityCode.EXPIRED
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
    return priorities.sortedBy { it.second.key }
  }

  /**
   * Checks if a priority code represents a contract with a counter (MULTI_TRIP or STORED_VALUE).
   *
   * @param priority The priority code to check
   * @return true if the contract type uses a counter, false otherwise
   */
  fun isCounterBasedContract(priority: PriorityCode): Boolean {
    return priority == PriorityCode.MULTI_TRIP || priority == PriorityCode.STORED_VALUE
  }

  // ========== Validation methods that throw ValidationException ==========

  /**
   * Validates anti-passback rule and throws if violated.
   *
   * @param lastEventDateTime The date/time of the last event
   * @param validationDateTime The current validation date/time
   * @param isDfRatified Whether the card's DF is ratified
   * @throws ValidationException with Status.INVALID_CARD if card already tapped and ratified,
   *   Status.SUCCESS if recovering from broken session
   */
  fun validateAntiPassbackOrThrow(
      lastEventDateTime: LocalDateTime,
      validationDateTime: LocalDateTime,
      isDfRatified: Boolean
  ) {
    if (Duration.between(lastEventDateTime, validationDateTime).toMinutes() <
        ANTI_PASSBACK_DELAY_MINUTES) {
      if (isDfRatified) {
        throw ValidationException(Messages.EXCEPTION_CARD_ALREADY_TAPPED, Status.INVALID_CARD)
      } else {
        throw ValidationException(Messages.EXCEPTION_RECOVER_BROKEN_SESSION, Status.SUCCESS)
      }
    }
  }

  /**
   * Validates environment version and throws if invalid.
   *
   * @param envVersionNumber The environment version number to validate
   * @throws ValidationException with Status.INVALID_CARD if version is invalid
   */
  fun validateEnvironmentVersionOrThrow(envVersionNumber: VersionNumber) {
    if (envVersionNumber != VersionNumber.CURRENT_VERSION) {
      throw ValidationException(Messages.EXCEPTION_ENVIRONMENT_WRONG_VERSION, Status.INVALID_CARD)
    }
  }

  /**
   * Validates environment date and throws if expired.
   *
   * @param envEndDate The environment end date
   * @param validationDate The current validation date
   * @throws ValidationException with Status.INVALID_CARD if environment date is expired
   */
  fun validateEnvironmentDateOrThrow(envEndDate: LocalDate, validationDate: LocalDate) {
    if (envEndDate.isBefore(validationDate)) {
      throw ValidationException(
          Messages.EXCEPTION_ENVIRONMENT_END_DATE_EXPIRED, Status.INVALID_CARD)
    }
  }

  /**
   * Validates event version and throws if invalid or undefined.
   *
   * @param eventVersionNumber The event version number to validate
   * @throws ValidationException with Status.EMPTY_CARD if undefined, Status.INVALID_CARD if invalid
   */
  fun validateEventVersionOrThrow(eventVersionNumber: VersionNumber) {
    when {
      eventVersionNumber == VersionNumber.CURRENT_VERSION -> {
        // Valid, do nothing
      }
      eventVersionNumber == VersionNumber.UNDEFINED -> {
        throw ValidationException(Messages.ERROR_NO_VALID_TITLE_DETECTED, Status.EMPTY_CARD)
      }
      else -> {
        throw ValidationException(Messages.EXCEPTION_EVENT_WRONG_VERSION, Status.INVALID_CARD)
      }
    }
  }

  /**
   * Validates contract version and throws if invalid.
   *
   * @param contractVersionNumber The contract version number to validate
   * @throws ValidationException with Status.INVALID_CARD if version is invalid
   */
  fun validateContractVersionOrThrow(contractVersionNumber: VersionNumber) {
    if (contractVersionNumber != VersionNumber.CURRENT_VERSION) {
      throw ValidationException(Messages.EXCEPTION_CONTRACT_VERSION_ERROR, Status.INVALID_CARD)
    }
  }

  /**
   * Validates contract date and throws if expired.
   *
   * @param contractValidityEndDate The contract validity end date
   * @param validationDate The current validation date
   * @throws ValidationException with Status.EMPTY_CARD if contract date is expired
   */
  fun validateContractDateOrThrow(contractValidityEndDate: LocalDate, validationDate: LocalDate) {
    if (contractValidityEndDate.isBefore(validationDate)) {
      throw ValidationException(Messages.EXCEPTION_EXPIRED_TITLE, Status.EMPTY_CARD)
    }
  }

  /**
   * Validates that trips are available and throws if not.
   *
   * @param counterValue The current counter value
   * @throws ValidationException with Status.EMPTY_CARD if no trips available
   */
  fun validateTripsAvailableOrThrow(counterValue: Int) {
    if (counterValue <= 0) {
      throw ValidationException(Messages.EXCEPTION_NO_TRIPS_LEFT, Status.EMPTY_CARD)
    }
  }

  /**
   * Validates that sufficient stored value is available and throws if not.
   *
   * @param counterValue The current counter value
   * @param validationAmount The amount required for validation
   * @throws ValidationException with Status.EMPTY_CARD if insufficient stored value
   */
  fun validateSufficientStoredValueOrThrow(counterValue: Int, validationAmount: Int) {
    if (counterValue < validationAmount) {
      throw ValidationException(Messages.EXCEPTION_INSUFFICIENT_STORED_VALUE, Status.EMPTY_CARD)
    }
  }

  /**
   * Validates that valid contracts exist and throws if none found.
   *
   * @param priorities List of contract priorities
   * @throws ValidationException with Status.EMPTY_CARD if no valid contracts
   */
  fun validateHasValidContractsOrThrow(priorities: List<Pair<Int, PriorityCode>>) {
    val validPriorities = filterValidContractPriorities(priorities)
    if (validPriorities.isEmpty()) {
      throw ValidationException(Messages.ERROR_NO_VALID_TITLE_DETECTED, Status.EMPTY_CARD)
    }
  }
}
