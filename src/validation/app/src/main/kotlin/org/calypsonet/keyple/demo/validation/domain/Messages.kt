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

/** Constants for validation messages and strings. */
object Messages {
  // User error messages
  const val ERROR_NO_VALID_TITLE_DETECTED = "No valid title detected"

  // Exception messages
  const val EXCEPTION_ENVIRONMENT_WRONG_VERSION = "Environment error: wrong version number"
  const val EXCEPTION_ENVIRONMENT_END_DATE_EXPIRED = "Environment error: end date expired"
  const val EXCEPTION_EVENT_WRONG_VERSION = "Event error: wrong version number"
  const val EXCEPTION_CONTRACT_VERSION_ERROR = "Contract Version Number error (!= CURRENT_VERSION)"
  const val EXCEPTION_CARD_ALREADY_TAPPED = "Card already tapped.\nPlease wait before retrying."
  const val EXCEPTION_RECOVER_BROKEN_SESSION = "Recover previous broken valid session"
  const val EXCEPTION_EXPIRED_TITLE = "Expired title"
  const val EXCEPTION_NO_TRIPS_LEFT = "No trips left"
  const val EXCEPTION_INSUFFICIENT_STORED_VALUE = "Insufficient stored value"
  const val EXCEPTION_CONTRACT_FORBIDDEN_OR_EXPIRED = "Contract is forbidden or expired"

  // Log messages
  const val LOG_VALIDATION_SUCCESS = "Validation procedure result: SUCCESS"
  const val LOG_VALIDATION_FAILED_NO_CONTRACT =
      "Validation procedure result: Failed - No valid contract found"

  // Card type prefix
  const val CARD_TYPE_CALYPSO_PREFIX = "CALYPSO: DF name "

  // Empty contract value
  const val EMPTY_CONTRACT = ""
}
