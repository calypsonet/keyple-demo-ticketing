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
package org.calypsonet.keyple.demo.common.parser

import fr.devnied.bitlib.BytesUtils
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.junit.jupiter.api.Test

class EventStructureParserTest {

  private val eventStructureParser = EventStructureParser()

  @Test
  fun parseEvent1() {
    val content = BytesUtils.fromString(DATA_EVENT_1)

    val event = eventStructureParser.parse(content)

    assertThat(event).isNotNull
    assertThat(event.eventVersionNumber).isEqualTo(VersionNumber.CURRENT_VERSION)
    assertThat(event.eventDateStamp.value).isEqualTo(4031)
    assertThat(event.eventTimeStamp.value).isEqualTo(840)
    assertThat(event.eventDateStamp.getDate()).isEqualTo(LocalDate.of(2021, 1, 14))
    assertThat(event.eventDatetime).isEqualTo(LocalDateTime.of(2021, 1, 14, 14, 0, 0))
    assertThat(event.eventLocation).isEqualTo(1)
    assertThat(event.eventContractUsed).isEqualTo(1)
    assertThat(event.contractPriority1).isEqualTo(PriorityCode.SEASON_PASS)
    assertThat(event.contractPriority2).isEqualTo(PriorityCode.FORBIDDEN)
    assertThat(event.contractPriority3).isEqualTo(PriorityCode.FORBIDDEN)
    assertThat(event.contractPriority4).isEqualTo(PriorityCode.FORBIDDEN)
  }

  @Test
  fun generateEvent1() {
    val eventDate = LocalDateTime.of(2021, 1, 14, 14, 0, 0)

    val eventStructure =
        EventStructure(
            eventVersionNumber = VersionNumber.CURRENT_VERSION,
            eventDateStamp = DateCompact(eventDate.toLocalDate()),
            eventTimeStamp = TimeCompact(eventDate),
            eventLocation = 1,
            eventContractUsed = 1,
            contractPriority1 = PriorityCode.SEASON_PASS,
            contractPriority2 = PriorityCode.FORBIDDEN,
            contractPriority3 = PriorityCode.FORBIDDEN,
            contractPriority4 = PriorityCode.FORBIDDEN)

    val content = EventStructureParser().generate(eventStructure)

    assertThat(BytesUtils.bytesToString(content)).isEqualTo(DATA_EVENT_1)
  }

  companion object {
    private const val DATA_EVENT_1 =
        "01 0F BF 03 48 00 00 00 01 01 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
  }
}
