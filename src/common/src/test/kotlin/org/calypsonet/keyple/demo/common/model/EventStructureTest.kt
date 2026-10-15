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
package org.calypsonet.keyple.demo.common.model

import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.junit.jupiter.api.Test

/** Event record of the card. */
class EventStructureTest {

  private val event =
      EventStructure(
          eventVersionNumber = VersionNumber.CURRENT_VERSION,
          eventDateStamp = DateCompact(LocalDate.of(2026, 10, 15)),
          eventTimeStamp = TimeCompact(LocalDateTime.of(2026, 10, 15, 8, 30)),
          eventLocation = 1,
          eventContractUsed = 2,
          contractPriorities =
              listOf(
                  PriorityCode.SEASON_PASS,
                  PriorityCode.MULTI_TRIP,
                  PriorityCode.FORBIDDEN,
                  PriorityCode.FORBIDDEN))

  @Test
  fun eventDateTime_combinesTheDateAndTheTime() {
    assertThat(event.eventDateTime).isEqualTo(LocalDateTime.of(2026, 10, 15, 8, 30))
  }

  @Test
  fun contractPriority_isGivenByTheContractNumber() {
    assertThat(event.getContractPriority(1)).isEqualTo(PriorityCode.SEASON_PASS)
    assertThat(event.getContractPriority(2)).isEqualTo(PriorityCode.MULTI_TRIP)
  }

  @Test
  fun withContractPriority_returnsACopy_theEventBeingUnchanged() {
    val updatedEvent = event.withContractPriority(2, PriorityCode.EXPIRED)

    assertThat(updatedEvent.contractPriorities)
        .containsExactly(
            PriorityCode.SEASON_PASS,
            PriorityCode.EXPIRED,
            PriorityCode.FORBIDDEN,
            PriorityCode.FORBIDDEN)
    assertThat(event.getContractPriority(2)).isEqualTo(PriorityCode.MULTI_TRIP)
  }

  @Test
  fun event_hasAPriorityForEachOfThe4Contracts() {
    assertThatThrownBy { event.copy(contractPriorities = listOf(PriorityCode.SEASON_PASS)) }
        .isInstanceOf(IllegalArgumentException::class.java)
  }
}
