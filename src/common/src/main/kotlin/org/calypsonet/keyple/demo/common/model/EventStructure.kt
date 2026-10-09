/* ******************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.common.model

import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber

/**
 * Event record of the card (see the data model in the README).
 *
 * @property contractPriorities The priorities of the contract records, in the order of the records
 *   (contract numbers 1 to [CONTRACT_COUNT]).
 */
data class EventStructure(
    val eventVersionNumber: VersionNumber,
    val eventDateStamp: DateCompact,
    val eventTimeStamp: TimeCompact,
    val eventLocation: Int,
    val eventContractUsed: Int,
    val contractPriorities: List<PriorityCode>
) {

  init {
    require(contractPriorities.size == CONTRACT_COUNT) {
      "$CONTRACT_COUNT contract priorities expected, got ${contractPriorities.size}"
    }
  }

  /** The date and time of the event. */
  val eventDatetime: LocalDateTime
    get() = eventDateStamp.date.atStartOfDay().plusMinutes(eventTimeStamp.value.toLong())

  /** Returns the priority of the contract record having the provided number (1 to 4). */
  fun getContractPriority(contractNumber: Int): PriorityCode =
      contractPriorities[contractNumber - 1]

  /**
   * Returns a copy of this event with the provided priority for the contract record having the
   * provided number (1 to 4).
   */
  fun withContractPriority(contractNumber: Int, priority: PriorityCode): EventStructure =
      copy(
          contractPriorities =
              contractPriorities.toMutableList().also { it[contractNumber - 1] = priority })

  companion object {
    /** Number of contract records, each one having a priority in the event. */
    const val CONTRACT_COUNT = 4
  }
}
