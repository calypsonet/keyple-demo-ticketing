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
package org.calypsonet.keyple.demo.control.domain.model

/** Reason why a card is refused by the control procedure. */
enum class RejectionReason {
  /** The version number of the environment is not the current one. */
  ENVIRONMENT_WRONG_VERSION,
  /** The end date of the environment is in the past. */
  ENVIRONMENT_EXPIRED,
  /** The version number of the last event is not the current one. */
  EVENT_WRONG_VERSION,
  /** The version number of the contract is not the current one (storage cards). */
  CONTRACT_WRONG_VERSION
}
